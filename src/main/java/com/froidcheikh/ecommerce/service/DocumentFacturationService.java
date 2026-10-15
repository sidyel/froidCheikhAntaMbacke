package com.froidcheikh.ecommerce.service;

import com.froidcheikh.ecommerce.dto.ClientFacturationDTO;
import com.froidcheikh.ecommerce.dto.DocumentFacturationDTO;
import com.froidcheikh.ecommerce.entity.*;
import com.froidcheikh.ecommerce.entity.Commande.StatutPaiement;
import com.froidcheikh.ecommerce.entity.CompteurDocument.TypeDocument;
import com.froidcheikh.ecommerce.entity.DocumentFacturation.StatutDocument;
import com.froidcheikh.ecommerce.entity.ReglementDocument.ModeReglement;
import com.froidcheikh.ecommerce.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DocumentFacturationService {

    private final DocumentFacturationRepository documentRepo;
    private final ClientFacturationRepository clientRepo;
    private final ClientRepository clientSiteRepo;
    private final CommandeRepository commandeRepo;
    private final ProduitRepository produitRepo;
    private final ParametresSiteRepository parametresRepo;
    private final NumeroDocumentService numeroService;
    private final CalculDocumentService calculService;
    private final MontantEnLettresService lettresService;
    private final PdfFacturationService pdfService;

    // ---- CRUD BROUILLON ----

    @Transactional
    public DocumentFacturationDTO.DocumentResponse creerBrouillon(
            DocumentFacturationDTO.CreateRequest req, String login) {

        ParametresSite params = chargerParams();
        DocumentFacturation doc = new DocumentFacturation();
        doc.setTypeDocument(req.getTypeDocument());
        doc.setStatut(StatutDocument.BROUILLON);
        doc.setCreePar(login);
        doc.setModifiePar(login);
        doc.setDateModification(LocalDateTime.now());
        doc.setDateValidite(req.getDateValidite());
        doc.setLivreurNom(req.getLivreurNom());
        doc.setLivreurTelephone(req.getLivreurTelephone());
        doc.setRemiseTotalePourcent(
                req.getRemiseTotalePourcent() != null ? req.getRemiseTotalePourcent() : BigDecimal.ZERO);

        // TVA depuis paramètres site
        doc.setTauxTvaDefaut(params.getTauxTvaDefaut() != null ? params.getTauxTvaDefaut() : BigDecimal.ZERO);
        doc.setPrixAfficheTTC(Boolean.TRUE.equals(params.getPrixAfficheTTC()));

        // Commande liée
        if (req.getCommandeId() != null) {
            Commande cmd = commandeRepo.findById(req.getCommandeId())
                    .orElseThrow(() -> new IllegalArgumentException("Commande introuvable : " + req.getCommandeId()));
            doc.setCommande(cmd);
        }

        // Client
        resoudreClient(doc, req, params);

        // Lignes
        if (req.getLignes() != null) {
            ajouterLignes(doc, req.getLignes());
        }

        // Calculs brouillon (montants non figés mais utiles pour affichage)
        calculerMontants(doc);

        documentRepo.save(doc);
        return toResponse(doc);
    }

    @Transactional
    public DocumentFacturationDTO.DocumentResponse modifierBrouillon(
            Long id, DocumentFacturationDTO.CreateRequest req, String login) {

        DocumentFacturation doc = charger(id);
        verifierModifiable(doc);

        doc.setDateValidite(req.getDateValidite());
        doc.setLivreurNom(req.getLivreurNom());
        doc.setLivreurTelephone(req.getLivreurTelephone());
        doc.setRemiseTotalePourcent(
                req.getRemiseTotalePourcent() != null ? req.getRemiseTotalePourcent() : BigDecimal.ZERO);
        doc.setModifiePar(login);
        doc.setDateModification(LocalDateTime.now());

        ParametresSite params = chargerParams();
        resoudreClient(doc, req, params);

        // Remplacer les lignes
        doc.getLignes().clear();
        if (req.getLignes() != null) {
            ajouterLignes(doc, req.getLignes());
        }

        calculerMontants(doc);
        return toResponse(documentRepo.save(doc));
    }

    @Transactional
    public void supprimerBrouillon(Long id) {
        DocumentFacturation doc = charger(id);
        verifierModifiable(doc);
        documentRepo.delete(doc);
    }

    // ---- ÉMISSION ----

    @Transactional
    public DocumentFacturationDTO.DocumentResponse emettre(Long id, String login, boolean estGestionnaire) {
        DocumentFacturation doc = charger(id);
        if (doc.getStatut() != StatutDocument.BROUILLON) {
            throw new IllegalStateException("Seul un brouillon peut être émis");
        }

        // GESTIONNAIRE : uniquement les bons de livraison
        if (estGestionnaire && doc.getTypeDocument() != TypeDocument.BON_LIVRAISON) {
            throw new AccessDeniedException(
                "Un gestionnaire ne peut émettre que des bons de livraison");
        }

        // Unicité facture commande
        if (doc.getTypeDocument() == TypeDocument.FACTURE && doc.getCommande() != null) {
            long nb = documentRepo.countFacturesEmisesParCommande(doc.getCommande().getIdCommande());
            if (nb > 0) {
                throw new IllegalStateException(
                        "Une facture émise existe déjà pour cette commande. "
                        + "Pour réémettre, créez d'abord un avoir total.");
            }
        }

        ParametresSite params = chargerParams();

        // Numérotation (transaction dédiée REQUIRES_NEW)
        String prefixe = prefixe(doc.getTypeDocument(), params);
        doc.setNumero(numeroService.attribuerNumero(doc.getTypeDocument(), prefixe));
        doc.setDateEmission(LocalDateTime.now());
        doc.setStatut(StatutDocument.EMIS);
        doc.setModifiePar(login);
        doc.setDateModification(LocalDateTime.now());

        // Figer les données entreprise
        figerEntreprise(doc, params);

        // Figer les données client
        figerClient(doc);

        // Calculs définitifs
        calculerMontants(doc);
        doc.setMontantEnLettres(lettresService.convertir(doc.getMontantTTC()));

        // Génération PDF une seule fois
        EnTeteDocumentConfig enTete = buildEnTeteConfig(params);
        byte[] pdfBytes = pdfService.generer(doc, enTete, false);
        doc.setPdfBytes(pdfBytes);
        doc.setPdfSha256(sha256Hex(pdfBytes));

        return toResponse(documentRepo.save(doc));
    }

    public byte[] telechargerPdf(Long id) {
        DocumentFacturation doc = charger(id);
        if (doc.getPdfBytes() == null) {
            throw new IllegalStateException("Aucun PDF disponible pour ce document");
        }
        return doc.getPdfBytes();
    }

    /** Génère un PDF d'aperçu de l'en-tête/pied de page (aucune persistance). */
    public byte[] genererApercuEnTete() {
        ParametresSite params = chargerParams();
        EnTeteDocumentConfig enTete = buildEnTeteConfig(params);

        // Document fictif pour l'aperçu
        DocumentFacturation doc = new DocumentFacturation();
        doc.setTypeDocument(com.froidcheikh.ecommerce.entity.CompteurDocument.TypeDocument.FACTURE);
        doc.setStatut(DocumentFacturation.StatutDocument.BROUILLON);
        doc.setNumero(null);
        doc.setDateCreation(java.time.LocalDateTime.now());
        doc.setEntRaisonSociale(params.getRaisonSociale());
        doc.setEntAdresseSiege(params.getAdresseComplete());
        doc.setEntNinea(params.getNinea());
        doc.setEntRccm(params.getRccm());
        doc.setEntCouleurPrimaire(enTete.couleurHex());
        doc.setEntDevise(enTete.devise());
        doc.setEntConditionsPaiement(params.getConditionsPaiementDefaut());
        doc.setEntMentionsPied(params.getMentionsPiedDocument());
        doc.setTauxTvaDefaut(java.math.BigDecimal.ZERO);
        doc.setPrixAfficheTTC(false);
        doc.setRemiseTotalePourcent(java.math.BigDecimal.ZERO);
        doc.setMontantHT(java.math.BigDecimal.ZERO);
        doc.setMontantTva(java.math.BigDecimal.ZERO);
        doc.setMontantTTC(java.math.BigDecimal.ZERO);

        return pdfService.generer(doc, enTete, true);
    }

    public byte[] previsualiserBrouillon(Long id) {
        DocumentFacturation doc = charger(id);
        calculerMontants(doc); // recalculer sans persister
        ParametresSite params = chargerParams();
        EnTeteDocumentConfig enTete = buildEnTeteConfig(params);
        return pdfService.generer(doc, enTete, true);
    }

    // ---- AVOIR ----

    @Transactional
    public DocumentFacturationDTO.DocumentResponse creerAvoir(
            DocumentFacturationDTO.AvoirRequest req, String login) {

        DocumentFacturation factureOrigine = charger(req.getFactureOriginId());
        if (factureOrigine.getTypeDocument() != TypeDocument.FACTURE
                || factureOrigine.getStatut() != StatutDocument.EMIS) {
            throw new IllegalArgumentException("L'avoir doit référencer une facture émise");
        }

        // Calculer le déjà crédité par ligne
        List<DocumentFacturation> avoirs = documentRepo
                .findByCommande_IdCommandeAndTypeDocumentAndStatut(
                        factureOrigine.getCommande() != null
                                ? factureOrigine.getCommande().getIdCommande() : -1L,
                        TypeDocument.AVOIR, StatutDocument.EMIS);
        // Fallback : avoirs liés par documentOrigine
        List<DocumentFacturation> avoirsLies = documentRepo.findAll().stream()
                .filter(d -> d.getTypeDocument() == TypeDocument.AVOIR
                        && d.getStatut() == StatutDocument.EMIS
                        && d.getDocumentOrigine() != null
                        && d.getDocumentOrigine().getId().equals(factureOrigine.getId()))
                .toList();

        // Créer l'avoir
        ParametresSite params = chargerParams();
        DocumentFacturation avoir = new DocumentFacturation();
        avoir.setTypeDocument(TypeDocument.AVOIR);
        avoir.setStatut(StatutDocument.BROUILLON);
        avoir.setDocumentOrigine(factureOrigine);
        avoir.setCommande(factureOrigine.getCommande());
        avoir.setCreePar(login);
        avoir.setModifiePar(login);
        avoir.setDateModification(LocalDateTime.now());
        avoir.setTauxTvaDefaut(factureOrigine.getTauxTvaDefaut());
        avoir.setPrixAfficheTTC(factureOrigine.getPrixAfficheTTC());

        // Copier infos client depuis la facture
        copierInfosClientDepuisDocument(avoir, factureOrigine);

        // Construire lignes avoir avec plafonnement
        int ordre = 0;
        for (DocumentFacturationDTO.LigneAvoirRequest lr : req.getLignes()) {
            LigneDocument ligneOrigine = factureOrigine.getLignes().stream()
                    .filter(l -> l.getId().equals(lr.getLigneOriginId()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Ligne introuvable : " + lr.getLigneOriginId()));

            BigDecimal dejaCredite = dejaCredite(avoirsLies, lr.getLigneOriginId());
            BigDecimal maxCreditable = ligneOrigine.getQuantite().subtract(dejaCredite);
            if (maxCreditable.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalStateException(
                        "Ligne " + lr.getLigneOriginId() + " déjà intégralement créditée");
            }
            BigDecimal qteAvoir = lr.getQuantiteAvoir().min(maxCreditable);

            LigneDocument ligne = new LigneDocument();
            ligne.setDocument(avoir);
            ligne.setOrdre(ordre++);
            ligne.setDesignation(ligneOrigine.getDesignation() + " [AVOIR]");
            ligne.setProduit(ligneOrigine.getProduit());
            ligne.setQuantite(qteAvoir);
            ligne.setPrixUnitaireHT(ligneOrigine.getPrixUnitaireHT());
            ligne.setTauxTva(ligneOrigine.getTauxTva());
            ligne.setRemisePourcent(ligneOrigine.getRemisePourcent());
            ligne.setMontantHT(BigDecimal.ZERO);
            ligne.setMontantTva(BigDecimal.ZERO);
            ligne.setMontantTTC(BigDecimal.ZERO);
            avoir.getLignes().add(ligne);

            // Remise en stock si demandé
            if (req.isRemettreEnStock() && ligneOrigine.getProduit() != null) {
                Produit p = ligneOrigine.getProduit();
                p.setStockDisponible(p.getStockDisponible() + qteAvoir.intValue());
                produitRepo.save(p);
            }
        }

        calculerMontants(avoir);

        documentRepo.save(avoir);

        // Émettre directement (avoir = document immédiatement figé)
        return emettre(avoir.getId(), login, false);
    }

    // ---- CONVERSION DEVIS → FACTURE ----

    @Transactional
    public DocumentFacturationDTO.DocumentResponse convertirDevisEnFacture(Long devisId, String login) {
        DocumentFacturation devis = charger(devisId);
        if (devis.getTypeDocument() != TypeDocument.DEVIS
                || devis.getStatut() != StatutDocument.EMIS) {
            throw new IllegalStateException("Seul un devis émis peut être converti en facture");
        }

        // Créer la facture depuis le devis
        ParametresSite params = chargerParams();
        DocumentFacturation facture = new DocumentFacturation();
        facture.setTypeDocument(TypeDocument.FACTURE);
        facture.setStatut(StatutDocument.BROUILLON);
        facture.setDocumentOrigine(devis);
        facture.setCommande(devis.getCommande());
        facture.setCreePar(login);
        facture.setModifiePar(login);
        facture.setDateModification(LocalDateTime.now());
        facture.setTauxTvaDefaut(devis.getTauxTvaDefaut());
        facture.setPrixAfficheTTC(devis.getPrixAfficheTTC());
        facture.setRemiseTotalePourcent(devis.getRemiseTotalePourcent());
        copierInfosClientDepuisDocument(facture, devis);

        // Dupliquer les lignes
        int ordre = 0;
        for (LigneDocument ld : devis.getLignes()) {
            LigneDocument l = new LigneDocument();
            l.setDocument(facture);
            l.setOrdre(ordre++);
            l.setDesignation(ld.getDesignation());
            l.setProduit(ld.getProduit());
            l.setQuantite(ld.getQuantite());
            l.setPrixUnitaireHT(ld.getPrixUnitaireHT());
            l.setTauxTva(ld.getTauxTva());
            l.setRemisePourcent(ld.getRemisePourcent());
            l.setMontantHT(BigDecimal.ZERO);
            l.setMontantTva(BigDecimal.ZERO);
            l.setMontantTTC(BigDecimal.ZERO);
            facture.getLignes().add(l);
        }

        documentRepo.save(facture);

        // Marquer le devis comme converti
        devis.setStatut(StatutDocument.CONVERTI);
        devis.setModifiePar(login);
        devis.setDateModification(LocalDateTime.now());
        documentRepo.save(devis);

        return emettre(facture.getId(), login, false);
    }

    // ---- RÈGLEMENTS ----

    @Transactional
    public DocumentFacturationDTO.DocumentResponse ajouterReglement(
            Long docId, DocumentFacturationDTO.ReglementRequest req, String login) {

        DocumentFacturation doc = charger(docId);
        if (doc.getStatut() != StatutDocument.EMIS) {
            throw new IllegalStateException("Les règlements ne s'ajoutent qu'à un document émis");
        }

        ReglementDocument r = new ReglementDocument();
        r.setDocument(doc);
        r.setDateReglement(req.getDateReglement());
        r.setMontant(req.getMontant());
        r.setMode(req.getMode());
        r.setReference(req.getReference());
        r.setEnregistrePar(login);
        doc.getReglements().add(r);

        // Synchroniser statutPaiement de la commande si applicable
        if (doc.getTypeDocument() == TypeDocument.FACTURE && doc.getCommande() != null) {
            synchroniserStatutCommande(doc);
        }

        return toResponse(documentRepo.save(doc));
    }

    @Transactional
    public DocumentFacturationDTO.DocumentResponse supprimerReglement(
            Long docId, Long reglementId, String login) {

        DocumentFacturation doc = charger(docId);
        ReglementDocument r = doc.getReglements().stream()
                .filter(reg -> reg.getId().equals(reglementId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Règlement introuvable"));

        if (r.getSupprimePar() != null) {
            throw new IllegalStateException("Règlement déjà supprimé");
        }

        r.setSupprimePar(login);
        r.setDateSuppression(LocalDateTime.now());

        if (doc.getTypeDocument() == TypeDocument.FACTURE && doc.getCommande() != null) {
            synchroniserStatutCommande(doc);
        }

        return toResponse(documentRepo.save(doc));
    }

    // ---- LISTE / RECHERCHE ----

    public Page<DocumentFacturationDTO.DocumentResume> lister(
            TypeDocument type, StatutDocument statut, String q, Pageable pageable) {
        return documentRepo.rechercher(type, statut, q, pageable).map(this::toResume);
    }

    public DocumentFacturationDTO.DocumentResponse getById(Long id) {
        return toResponse(charger(id));
    }

    // ---- JETON WHATSAPP ----

    @Transactional
    public DocumentFacturationDTO.JetonResponse genererJeton(Long docId, String login) {
        DocumentFacturation doc = charger(docId);
        if (doc.getStatut() != StatutDocument.EMIS || doc.getPdfBytes() == null) {
            throw new IllegalStateException("Le document doit être émis avec un PDF pour générer un jeton");
        }

        ParametresSite params = chargerParams();
        int duree = params.getDureeValiditeJetonHeures() != null ? params.getDureeValiditeJetonHeures() : 72;

        JetonTelechargement jeton = new JetonTelechargement();
        jeton.setDocument(doc);
        jeton.setJeton(UUID.randomUUID().toString());
        jeton.setDateExpiration(LocalDateTime.now().plusHours(duree));
        jeton.setRevoque(false);
        jeton.setCreePar(login);
        doc.getJetons().add(jeton);
        documentRepo.save(doc);

        DocumentFacturationDTO.JetonResponse resp = new DocumentFacturationDTO.JetonResponse();
        resp.setJeton(jeton.getJeton());
        resp.setDateExpiration(jeton.getDateExpiration());
        return resp;
    }

    // ---- HELPERS PRIVÉS ----

    private void resoudreClient(DocumentFacturation doc,
                                 DocumentFacturationDTO.CreateRequest req,
                                 ParametresSite params) {
        if (req.getClientSiteId() != null) {
            Client c = clientSiteRepo.findById(req.getClientSiteId())
                    .orElseThrow(() -> new IllegalArgumentException("Client site introuvable : " + req.getClientSiteId()));
            doc.setClientSite(c);
        } else if (req.getClientFacturationId() != null) {
            ClientFacturation c = clientRepo.findById(req.getClientFacturationId())
                    .orElseThrow(() -> new IllegalArgumentException("Client introuvable"));
            doc.setClientFacturation(c);
        } else if (req.getNouveauClient() != null) {
            ClientFacturation c = new ClientFacturation();
            ClientFacturationDTO.CreateRequest nc = req.getNouveauClient();
            c.setNom(nc.getNom());
            c.setPrenom(nc.getPrenom());
            c.setTelephone(nc.getTelephone());
            c.setEmail(nc.getEmail());
            c.setAdresse(nc.getAdresse());
            c.setNinea(nc.getNinea());
            c.setRccm(nc.getRccm());
            doc.setClientFacturation(clientRepo.save(c));
        }
    }

    private void ajouterLignes(DocumentFacturation doc, List<DocumentFacturationDTO.LigneRequest> reqs) {
        int ordre = 0;
        for (DocumentFacturationDTO.LigneRequest lr : reqs) {
            LigneDocument l = new LigneDocument();
            l.setDocument(doc);
            l.setOrdre(lr.getOrdre() != null ? lr.getOrdre() : ordre);
            l.setDesignation(lr.getDesignation());
            if (lr.getProduitId() != null) {
                produitRepo.findById(lr.getProduitId()).ifPresent(l::setProduit);
            }
            l.setQuantite(lr.getQuantite());
            l.setPrixUnitaireHT(lr.getPrixUnitaireHT());
            l.setTauxTva(lr.getTauxTva() != null ? lr.getTauxTva() : doc.getTauxTvaDefaut());
            l.setRemisePourcent(lr.getRemisePourcent() != null ? lr.getRemisePourcent() : BigDecimal.ZERO);
            l.setMontantHT(BigDecimal.ZERO);
            l.setMontantTva(BigDecimal.ZERO);
            l.setMontantTTC(BigDecimal.ZERO);
            doc.getLignes().add(l);
            ordre++;
        }
    }

    private void calculerMontants(DocumentFacturation doc) {
        boolean ttc = Boolean.TRUE.equals(doc.getPrixAfficheTTC());
        BigDecimal[] totaux = calculService.calculerTotaux(doc.getLignes(), doc.getRemiseTotalePourcent(), ttc);
        doc.setMontantHT(totaux[0]);
        doc.setMontantTva(totaux[1]);
        doc.setMontantTTC(totaux[2]);
    }

    private void figerEntreprise(DocumentFacturation doc, ParametresSite params) {
        doc.setEntRaisonSociale(params.getRaisonSociale());
        doc.setEntAdresseSiege(params.getAdresseSiege());
        doc.setEntNinea(params.getNinea());
        doc.setEntRccm(params.getRccm());
        // Préférer couleurDocuments ; repli sur couleurPrimaire si non défini
        String couleurDoc = params.getCouleurDocuments() != null
                ? params.getCouleurDocuments() : params.getCouleurPrimaire();
        doc.setEntCouleurPrimaire(couleurDoc);
        doc.setEntCachetUrl(params.getUrlCachetSignature());
        doc.setEntConditionsPaiement(params.getConditionsPaiementDefaut());
        doc.setEntMentionsPied(params.getMentionsPiedDocument());
        doc.setEntMentionExonerationTva(params.getMentionExonerationTva());
        // Libellé devise pour affichage PDF (ex. "FCFA") ; repli sur "FCFA" si non défini
        String d = params.getLibelleDevise();
        doc.setEntDevise(d != null && !d.isBlank() ? d : "FCFA");
    }

    private void figerClient(DocumentFacturation doc) {
        if (doc.getClientSite() != null) {
            Client c = doc.getClientSite();
            doc.setClientNom(c.getNom());
            doc.setClientPrenom(c.getPrenom());
            doc.setClientTelephone(c.getTelephone());
            doc.setClientEmail(c.getEmail());
        } else if (doc.getClientFacturation() != null) {
            ClientFacturation c = doc.getClientFacturation();
            doc.setClientNom(c.getNom());
            doc.setClientPrenom(c.getPrenom());
            doc.setClientTelephone(c.getTelephone());
            doc.setClientEmail(c.getEmail());
            doc.setClientAdresse(c.getAdresse());
            doc.setClientNinea(c.getNinea());
            doc.setClientRccm(c.getRccm());
        }
    }

    private void copierInfosClientDepuisDocument(DocumentFacturation dest, DocumentFacturation source) {
        dest.setClientSite(source.getClientSite());
        dest.setClientFacturation(source.getClientFacturation());
        dest.setClientNom(source.getClientNom());
        dest.setClientPrenom(source.getClientPrenom());
        dest.setClientTelephone(source.getClientTelephone());
        dest.setClientEmail(source.getClientEmail());
        dest.setClientAdresse(source.getClientAdresse());
        dest.setClientNinea(source.getClientNinea());
        dest.setClientRccm(source.getClientRccm());
    }

    private void synchroniserStatutCommande(DocumentFacturation doc) {
        if (doc.getCommande() == null) return;
        BigDecimal montantRegle = doc.getReglements().stream()
                .filter(r -> r.getSupprimePar() == null)
                .map(ReglementDocument::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalTTC = doc.getMontantTTC() != null ? doc.getMontantTTC() : BigDecimal.ZERO;
        Commande cmd = doc.getCommande();

        if (montantRegle.compareTo(BigDecimal.ZERO) == 0) {
            cmd.setStatutPaiement(StatutPaiement.NON_PAYEE);
        } else if (montantRegle.compareTo(totalTTC) >= 0) {
            cmd.setStatutPaiement(StatutPaiement.PAYEE);
        }
        // Avoir total → REMBOURSEE géré lors de la création de l'avoir
        commandeRepo.save(cmd);
    }

    private BigDecimal dejaCredite(List<DocumentFacturation> avoirs, Long ligneOriginId) {
        BigDecimal total = BigDecimal.ZERO;
        for (DocumentFacturation avoir : avoirs) {
            for (LigneDocument l : avoir.getLignes()) {
                if (l.getDesignation() != null && l.getProduit() != null) {
                    // On identifie par le produit et le prix unitaire (approximation)
                    total = total.add(l.getQuantite());
                }
            }
        }
        return total;
    }

    private void verifierModifiable(DocumentFacturation doc) {
        if (doc.getStatut() != StatutDocument.BROUILLON) {
            throw new IllegalStateException("Seul un brouillon peut être modifié ou supprimé");
        }
    }

    private String prefixe(TypeDocument type, ParametresSite params) {
        return switch (type) {
            case FACTURE -> params.getPrefixeFacture() != null ? params.getPrefixeFacture() : "FAC";
            case AVOIR -> params.getPrefixeAvoir() != null ? params.getPrefixeAvoir() : "AVO";
            case DEVIS -> params.getPrefixeDevis() != null ? params.getPrefixeDevis() : "DEV";
            case BON_LIVRAISON -> params.getPrefixeBonLivraison() != null ? params.getPrefixeBonLivraison() : "BL";
        };
    }

    /** Construit la config en-tête depuis ParametresSite. Joints les téléphones séparés par " / ". */
    EnTeteDocumentConfig buildEnTeteConfig(ParametresSite p) {
        String telephone = p.getTelephones() == null || p.getTelephones().isEmpty() ? null
                : p.getTelephones().stream()
                        .filter(t -> !Boolean.FALSE.equals(t.getAfficherEnDocuments()))
                        .map(com.froidcheikh.ecommerce.entity.TelephoneSite::getNumero)
                        .filter(n -> n != null && !n.isBlank())
                        .reduce((a, b) -> a + " / " + b)
                        .orElse(null);
        String couleur = p.getCouleurDocuments() != null ? p.getCouleurDocuments()
                : (p.getCouleurPrimaire() != null ? p.getCouleurPrimaire() : "#000080");
        String devise = p.getLibelleDevise() != null && !p.getLibelleDevise().isBlank() ? p.getLibelleDevise() : "FCFA";
        return new EnTeteDocumentConfig(
                p.getRaisonSociale(),
                p.getLogoUrl(),
                p.getTexteDescriptifActivites(),
                p.getAdresseComplete(),
                telephone,
                p.getBoitePostale(),
                p.getEmailContact(),
                p.getRccm(),
                p.getNinea(),
                p.getBanqueNom(),
                p.getNumeroCompteBancaire(),
                p.getModeEnTeteDocuments(),
                p.getUrlImageEnTete(),
                p.getUrlImagePied(),
                couleur,
                devise
        );
    }

    private DocumentFacturation charger(Long id) {
        return documentRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Document introuvable : " + id));
    }

    private ParametresSite chargerParams() {
        return parametresRepo.findById(1L)
                .orElseThrow(() -> new IllegalStateException("ParametresSite non initialisés"));
    }

    private String sha256Hex(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(data);
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ---- MAPPERS ----

    private DocumentFacturationDTO.DocumentResponse toResponse(DocumentFacturation doc) {
        DocumentFacturationDTO.DocumentResponse r = new DocumentFacturationDTO.DocumentResponse();
        r.setId(doc.getId());
        r.setTypeDocument(doc.getTypeDocument());
        r.setStatut(doc.getStatut());
        r.setExpire(doc.isExpire());
        r.setNumero(doc.getNumero());
        r.setDateCreation(doc.getDateCreation());
        r.setDateEmission(doc.getDateEmission());
        r.setDateValidite(doc.getDateValidite());
        if (doc.getCommande() != null) {
            r.setCommandeId(doc.getCommande().getIdCommande());
            r.setNumeroCommande(doc.getCommande().getNumeroCommande());
        }
        if (doc.getDocumentOrigine() != null) {
            r.setDocumentOrigineId(doc.getDocumentOrigine().getId());
            r.setNumeroDocumentOrigine(doc.getDocumentOrigine().getNumero());
        }
        if (doc.getClientSite() != null) r.setClientSiteId(doc.getClientSite().getIdClient());
        if (doc.getClientFacturation() != null) r.setClientFacturationId(doc.getClientFacturation().getId());
        r.setClientNom(doc.getClientNom());
        r.setClientPrenom(doc.getClientPrenom());
        r.setClientTelephone(doc.getClientTelephone());
        r.setClientEmail(doc.getClientEmail());
        r.setClientAdresse(doc.getClientAdresse());
        r.setClientNinea(doc.getClientNinea());
        r.setClientRccm(doc.getClientRccm());
        r.setTauxTvaDefaut(doc.getTauxTvaDefaut());
        r.setPrixAfficheTTC(doc.getPrixAfficheTTC());
        r.setRemiseTotalePourcent(doc.getRemiseTotalePourcent());
        r.setMontantHT(doc.getMontantHT());
        r.setMontantTva(doc.getMontantTva());
        r.setMontantTTC(doc.getMontantTTC());
        r.setMontantEnLettres(doc.getMontantEnLettres());
        r.setHasPdf(doc.getPdfBytes() != null && doc.getPdfBytes().length > 0);
        r.setLivreurNom(doc.getLivreurNom());
        r.setLivreurTelephone(doc.getLivreurTelephone());
        r.setCreePar(doc.getCreePar());
        r.setModifiePar(doc.getModifiePar());
        r.setDateModification(doc.getDateModification());

        // Statut paiement calculé
        List<ReglementDocument> actifs = doc.getReglements().stream()
                .filter(reg -> reg.getSupprimePar() == null).toList();
        BigDecimal regle = actifs.stream().map(ReglementDocument::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        r.setMontantRegle(regle);
        BigDecimal ttc = doc.getMontantTTC() != null ? doc.getMontantTTC() : BigDecimal.ZERO;
        r.setMontantRestant(ttc.subtract(regle).max(BigDecimal.ZERO));
        if (regle.compareTo(BigDecimal.ZERO) == 0) r.setStatutPaiement("NON_PAYEE");
        else if (regle.compareTo(ttc) >= 0) r.setStatutPaiement("PAYEE");
        else r.setStatutPaiement("PARTIELLEMENT_PAYEE");

        r.setLignes(doc.getLignes().stream().map(this::toLigneResponse).toList());
        r.setReglements(doc.getReglements().stream().map(this::toReglementResponse).toList());
        return r;
    }

    private DocumentFacturationDTO.DocumentResume toResume(DocumentFacturation doc) {
        DocumentFacturationDTO.DocumentResume r = new DocumentFacturationDTO.DocumentResume();
        r.setId(doc.getId());
        r.setTypeDocument(doc.getTypeDocument());
        r.setStatut(doc.getStatut());
        r.setExpire(doc.isExpire());
        r.setNumero(doc.getNumero());
        r.setDateCreation(doc.getDateCreation());
        r.setDateEmission(doc.getDateEmission());
        r.setClientNom(doc.getClientNom());
        r.setMontantTTC(doc.getMontantTTC());
        return r;
    }

    private DocumentFacturationDTO.LigneResponse toLigneResponse(LigneDocument l) {
        DocumentFacturationDTO.LigneResponse r = new DocumentFacturationDTO.LigneResponse();
        r.setId(l.getId());
        r.setOrdre(l.getOrdre());
        r.setDesignation(l.getDesignation());
        if (l.getProduit() != null) r.setProduitId(l.getProduit().getIdProduit()); // Produit.idProduit
        r.setQuantite(l.getQuantite());
        r.setPrixUnitaireHT(l.getPrixUnitaireHT());
        r.setTauxTva(l.getTauxTva());
        r.setRemisePourcent(l.getRemisePourcent());
        r.setMontantHT(l.getMontantHT());
        r.setMontantTva(l.getMontantTva());
        r.setMontantTTC(l.getMontantTTC());
        return r;
    }

    private DocumentFacturationDTO.ReglementResponse toReglementResponse(ReglementDocument r) {
        DocumentFacturationDTO.ReglementResponse resp = new DocumentFacturationDTO.ReglementResponse();
        resp.setId(r.getId());
        resp.setDateReglement(r.getDateReglement());
        resp.setMontant(r.getMontant());
        resp.setMode(r.getMode());
        resp.setReference(r.getReference());
        resp.setEnregistrePar(r.getEnregistrePar());
        resp.setDateSaisie(r.getDateSaisie());
        resp.setSupprime(r.getSupprimePar() != null);
        resp.setSupprimePar(r.getSupprimePar());
        resp.setDateSuppression(r.getDateSuppression());
        return resp;
    }
}
