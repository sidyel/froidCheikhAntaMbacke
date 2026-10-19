package com.froidcheikh.ecommerce.service;

import com.froidcheikh.ecommerce.dto.CommandeDTO;
import com.froidcheikh.ecommerce.dto.LivraisonDTO;
import com.froidcheikh.ecommerce.entity.*;
import com.froidcheikh.ecommerce.exception.ResourceNotFoundException;
import com.froidcheikh.ecommerce.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CommandeService {

    private final CommandeRepository commandeRepository;
    private final ClientRepository clientRepository;
    private final ProduitRepository produitRepository;
    private final PaiementRepository paiementRepository;
    private final HistoriqueStatutCommandeRepository historiqueRepository;
    private final ProduitService produitService;
    private final CalculCommandeService calculCommandeService;
    private final PrixService prixService;
    private final ModelMapper modelMapper;
    private final MetaConversionsService metaConversionsService;

    public CommandeDTO getCommandeById(Long id) {
        Commande commande = commandeRepository.findByIdWithLignes(id)
                .orElseThrow(() -> new ResourceNotFoundException("Commande non trouvée avec l'ID : " + id));
        return convertToDTO(commande);
    }

    public CommandeDTO getCommandeByNumero(String numeroCommande) {
        Commande commande = commandeRepository.findByNumeroCommande(numeroCommande)
                .orElseThrow(() -> new ResourceNotFoundException("Commande non trouvée avec le numéro : " + numeroCommande));
        return convertToDTO(commande);
    }

    public Page<CommandeDTO> getCommandesClient(Long clientId, Pageable pageable) {
        return commandeRepository.findByClientIdClient(clientId, pageable)
                .map(this::convertToDTO);
    }

    public CommandeDTO creerCommandeClient(Long clientId, CommandeDTO commandeDTO,
                                            String ipClient, String userAgent) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client non trouvé avec l'ID : " + clientId));

        return creerCommande(commandeDTO, client, null, ipClient, userAgent);
    }

    public CommandeDTO creerCommandeInvite(CommandeDTO commandeDTO,
                                           String ipClient, String userAgent) {
        log.info("🔄 === DÉBUT CRÉATION COMMANDE INVITÉ - SERVICE ===");
        log.info("📧 Email reçu: {}", commandeDTO.getEmailInvite());
        log.info("💰 Montant DTO reçu: {}", commandeDTO.getMontantTotal());

        return creerCommande(commandeDTO, null, commandeDTO, ipClient, userAgent);
    }

    private CommandeDTO creerCommande(CommandeDTO commandeDTO, Client client, CommandeDTO inviteInfo,
                                      String ipClient, String userAgent) {
        try {
            log.info("🔧 === DÉBUT CRÉATION COMMANDE DANS creerCommande ===");

            // ✅ ÉTAPE 1 : Vérifier la disponibilité des produits
            for (CommandeDTO.LigneCommandeDTO ligneDTO : commandeDTO.getLignesCommande()) {
                if (!produitService.verifierStockDisponible(ligneDTO.getProduitId(), ligneDTO.getQuantite())) {
                    throw new RuntimeException("Stock insuffisant pour le produit ID : " + ligneDTO.getProduitId());
                }
            }

            // ✅ ÉTAPE 2 : Créer l'entité Commande
            Commande commande = new Commande();
            commande.setNumeroCommande(genererNumeroCommande());
            commande.setClient(client);
            commande.setStatutCommande(Commande.StatutCommande.EN_ATTENTE);
            commande.setModeLivraison(commandeDTO.getModeLivraison());
            commande.setCommentaire(commandeDTO.getCommentaire());

            log.info("📝 Commande de base créée - Numéro: {}", commande.getNumeroCommande());

            // ✅ ÉTAPE 3 : Données invité si applicable
            if (inviteInfo != null) {
                commande.setEmailInvite(inviteInfo.getEmailInvite());
                commande.setNomInvite(inviteInfo.getNomInvite());
                commande.setPrenomInvite(inviteInfo.getPrenomInvite());
                commande.setTelephoneInvite(inviteInfo.getTelephoneInvite());
                log.info("👤 Données invité assignées");
            }

            // ✅ ÉTAPE 4 : Adresse de livraison
            if (commandeDTO.getAdresseLivraison() != null) {
                Commande.AdresseLivraison adresseLivraison = new Commande.AdresseLivraison();
                adresseLivraison.setNom(commandeDTO.getAdresseLivraison().getNom());
                adresseLivraison.setPrenom(commandeDTO.getAdresseLivraison().getPrenom());
                adresseLivraison.setLigne1(commandeDTO.getAdresseLivraison().getLigne1());
                adresseLivraison.setLigne2(commandeDTO.getAdresseLivraison().getLigne2());
                adresseLivraison.setVille(commandeDTO.getAdresseLivraison().getVille());
                adresseLivraison.setCodePostal(commandeDTO.getAdresseLivraison().getCodePostal());
                adresseLivraison.setTelephone(commandeDTO.getAdresseLivraison().getTelephone());
                commande.setAdresseLivraison(adresseLivraison);
                log.info("🏠 Adresse de livraison assignée");
            }

            // ✅ ÉTAPE 5 : Calculer les frais de livraison via CalculCommandeService
            List<LivraisonDTO.EstimationRequest.LigneEstimation> lignesEstim = commandeDTO.getLignesCommande()
                    .stream()
                    .map(l -> {
                        LivraisonDTO.EstimationRequest.LigneEstimation le = new LivraisonDTO.EstimationRequest.LigneEstimation();
                        le.setProduitId(l.getProduitId());
                        le.setQuantite(l.getQuantite());
                        le.setAvecInstallation(l.isAvecInstallation());
                        return le;
                    }).toList();

            CalculCommandeService.CalculResultat calcul = calculCommandeService.calculer(
                    commandeDTO.getZoneId(), commandeDTO.getModeLivraison(), lignesEstim);

            commande.setFraisLivraison(calcul.fraisLivraisonTotal());
            commande.setZoneId(calcul.zoneId());
            commande.setZoneNom(calcul.zoneNom());
            commande.setZonePrixApplique(calcul.zonePrixApplique());
            commande.setZoneModeApplique(calcul.zoneModeApplique());
            commande.setSupplementExpressApplique(calcul.supplementExpress());
            commande.setSupplementGabaritApplique(calcul.supplementGabarit());
            commande.setLivraisonGratuiteAppliquee(calcul.livraisonGratuite());
            log.info("🚚 Frais livraison: {} (zone: {}, gratuit: {})",
                    calcul.fraisLivraisonTotal(), calcul.zoneNom(), calcul.livraisonGratuite());

            // ✅ ÉTAPE 6 : MONTANT TOTAL (déjà calculé par CalculCommandeService avec promos)
            commande.setMontantTotal(calcul.montantTotal());
            log.info("💰 Montant total calculé et assigné: {}", calcul.montantTotal());

            // ✅ ÉTAPE 7 : SAUVEGARDER LA COMMANDE AVEC LE MONTANT TOTAL
            log.info("💾 Sauvegarde de la commande...");
            commande = commandeRepository.save(commande);
            log.info("✅ Commande sauvegardée avec ID: {}", commande.getIdCommande());

            // ✅ ÉTAPE 8 : Créer les lignes de commande
            log.info("📋 Création des lignes de commande...");
            List<LigneCommande> lignesCommande = new ArrayList<>();

            for (CommandeDTO.LigneCommandeDTO ligneDTO : commandeDTO.getLignesCommande()) {
                Produit produit = produitRepository.findById(ligneDTO.getProduitId())
                        .orElseThrow(() -> new ResourceNotFoundException("Produit non trouvé avec l'ID : " + ligneDTO.getProduitId()));

                PrixService.PrixResultat prixResultat = prixService.calculerPrix(produit);
                BigDecimal prixApplique = prixResultat.prixEffectif();

                LigneCommande ligne = new LigneCommande();
                ligne.setCommande(commande);
                ligne.setProduit(produit);
                ligne.setQuantite(ligneDTO.getQuantite());
                ligne.setPrixUnitaireOrigine(produit.getPrix());
                ligne.setPrixUnitaire(prixApplique);
                ligne.setNomProduitCommande(produit.getNomProduit());
                ligne.setRefProduitCommande(produit.getRefProduit());
                if (prixResultat.hasPromo()) {
                    ligne.setPromotionId(prixResultat.promotionId());
                    ligne.setPromotionNom(prixResultat.nomPromotion());
                }

                // Installation optionnelle — prix figé au moment de la commande
                BigDecimal prixInstall = BigDecimal.ZERO;
                if (ligneDTO.isAvecInstallation() && produit.getPrixInstallation() != null) {
                    ligne.setAvecInstallation(true);
                    ligne.setPrixInstallationApplique(produit.getPrixInstallation());
                    prixInstall = produit.getPrixInstallation();
                }
                ligne.setSousTotal(prixApplique.multiply(BigDecimal.valueOf(ligneDTO.getQuantite())).add(prixInstall));

                lignesCommande.add(ligne);

                // Décrémenter le stock
                produitService.updateStock(produit.getIdProduit(),
                        produit.getStockDisponible() - ligneDTO.getQuantite());

                log.info("📦 Ligne de commande créée pour produit: {}", produit.getNomProduit());
            }

            commande.setLignesCommande(lignesCommande);

            // ✅ ÉTAPE 9 : Sauvegarder la commande finale
            commande = commandeRepository.save(commande);

            // ✅ ÉTAPE 10 : Envoyer l'événement Purchase à l'API Conversions Meta (async, si consentement)
            final Commande commandeFinale = commande;
            metaConversionsService.sendPurchaseEvent(commandeFinale, commandeDTO, ipClient, userAgent);

            log.info("🎉 === COMMANDE CRÉÉE AVEC SUCCÈS ===");
            log.info("🔢 ID: {}", commande.getIdCommande());
            log.info("📄 Numéro: {}", commande.getNumeroCommande());
            log.info("💰 Montant total: {}", commande.getMontantTotal());
            log.info("📦 Nombre de lignes: {}", commande.getLignesCommande().size());

            return convertToDTO(commande);

        } catch (Exception e) {
            log.error("❌ Erreur lors de la création de la commande: ", e);
            throw new RuntimeException("Erreur lors de la création de la commande: " + e.getMessage(), e);
        }
    }

    public CommandeDTO confirmerPaiement(Long commandeId, CommandeDTO.PaiementDTO paiementDTO) {
        Commande commande = commandeRepository.findById(commandeId)
                .orElseThrow(() -> new ResourceNotFoundException("Commande non trouvée avec l'ID : " + commandeId));

        // Créer le paiement
        Paiement paiement = new Paiement();
        paiement.setCommande(commande);
        paiement.setMethodePaiement(Paiement.MethodePaiement.valueOf(paiementDTO.getMethodePaiement()));
        paiement.setMontant(commande.getMontantTotal());
        paiement.setStatutPaiement(Paiement.StatutPaiement.CONFIRME);
        paiement.setReferencePaiement(genererReferencePaiement());
        paiement.setReferenceExterne(paiementDTO.getReferenceExterne());

        paiement = paiementRepository.save(paiement);

        // Mettre à jour le statut de paiement (indépendant du cycle de livraison)
        commande.setStatutPaiement(Commande.StatutPaiement.PAYEE);
        commande = commandeRepository.save(commande);

        log.info("Paiement confirmé pour la commande : {}", commandeId);
        return convertToDTO(commande);
    }

    private String genererNumeroCommande() {
        return "CMD-" + System.currentTimeMillis();
    }

    private String genererNumeroSuivi() {
        return "TRK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private String genererReferencePaiement() {
        return "PAY-" + System.currentTimeMillis();
    }

    private CommandeDTO convertToDTO(Commande commande) {
        CommandeDTO dto = modelMapper.map(commande, CommandeDTO.class);

        if (commande.getLignesCommande() != null) {
            dto.setLignesCommande(commande.getLignesCommande().stream()
                    .map(ligne -> {
                        CommandeDTO.LigneCommandeDTO ligneDTO = new CommandeDTO.LigneCommandeDTO();
                        ligneDTO.setIdLigneCommande(ligne.getIdLigneCommande());
                        ligneDTO.setProduitId(ligne.getProduit().getIdProduit());
                        ligneDTO.setQuantite(ligne.getQuantite());
                        ligneDTO.setPrixUnitaire(ligne.getPrixUnitaire());
                        ligneDTO.setSousTotal(ligne.getSousTotal());
                        ligneDTO.setNomProduitCommande(ligne.getNomProduitCommande());
                        ligneDTO.setRefProduitCommande(ligne.getRefProduitCommande());
                        ligneDTO.setPrixUnitaireOrigine(ligne.getPrixUnitaireOrigine());
                        ligneDTO.setPromotionId(ligne.getPromotionId());
                        ligneDTO.setPromotionNom(ligne.getPromotionNom());
                        return ligneDTO;
                    })
                    .collect(Collectors.toList()));
        }

        if (commande.getPaiement() != null) {
            Paiement paiement = commande.getPaiement();
            CommandeDTO.PaiementDTO paiementDTO = new CommandeDTO.PaiementDTO();
            paiementDTO.setIdPaiement(paiement.getIdPaiement());
            paiementDTO.setMethodePaiement(paiement.getMethodePaiement().name());
            paiementDTO.setStatutPaiement(paiement.getStatutPaiement().name());
            paiementDTO.setMontant(paiement.getMontant());
            paiementDTO.setDatePaiement(paiement.getDatePaiement());
            paiementDTO.setReferencePaiement(paiement.getReferencePaiement());
            paiementDTO.setReferenceExterne(paiement.getReferenceExterne());
            dto.setPaiement(paiementDTO);
        }

        // Historique des statuts
        List<CommandeDTO.HistoriqueStatutDTO> historique = historiqueRepository
                .findByCommandeIdCommandeOrderByDateChangementAsc(commande.getIdCommande()).stream()
                .map(h -> new CommandeDTO.HistoriqueStatutDTO(
                        h.getId(), h.getAncienStatut(), h.getNouveauStatut(),
                        h.getDateChangement(), h.getAuteur(), h.getCommentaire()))
                .collect(Collectors.toList());
        dto.setHistoriqueStatuts(historique);

        return dto;
    }

    // Autres méthodes du service restent identiques...
    public Page<CommandeDTO> getAllCommandes(Pageable pageable) {
        return commandeRepository.findAll(pageable)
                .map(this::convertToDTO);
    }

    public Page<CommandeDTO> getCommandesByStatut(Commande.StatutCommande statutCommande, Pageable pageable) {
        return null;
    }





    /**
     * Récupérer toutes les commandes avec pagination et filtrage par statut
     */
    public Page<CommandeDTO> getAllCommandesAdmin(Pageable pageable, Commande.StatutCommande statut) {
        log.info("📋 Récupération commandes admin - Page: {}, Taille: {}, Statut: {}",
                pageable.getPageNumber(), pageable.getPageSize(), statut);

        Page<Commande> commandes;
        if (statut != null) {
            commandes = commandeRepository.findByStatutCommande(statut, pageable);
        } else {
            commandes = commandeRepository.findAllByOrderByDateCommandeDesc(pageable);
        }

        log.info("✅ {} commandes trouvées", commandes.getTotalElements());
        return commandes.map(this::convertToDTO);
    }

    /**
     * Mettre à jour le statut d'une commande (admin).
     * @param commentaire commentaire optionnel affiché dans l'historique
     * @param auteur      email de l'admin ou "SYSTEME"
     */
    public CommandeDTO updateStatutCommande(Long commandeId, Commande.StatutCommande nouveauStatut,
                                             String commentaire, String auteur) {
        log.info("🔄 Mise à jour statut commande - ID: {}, Nouveau statut: {}", commandeId, nouveauStatut);

        Commande commande = commandeRepository.findById(commandeId)
                .orElseThrow(() -> new ResourceNotFoundException("Commande non trouvée avec l'ID : " + commandeId));

        Commande.StatutCommande ancienStatut = commande.getStatutCommande();

        if (!isStatutChangeAllowed(ancienStatut, nouveauStatut)) {
            throw new IllegalStateException(
                    String.format("Changement de statut non autorisé : %s -> %s", ancienStatut, nouveauStatut));
        }

        commande.setStatutCommande(nouveauStatut);
        commande.setDateModification(LocalDateTime.now());

        if (nouveauStatut == Commande.StatutCommande.ANNULEE) {
            restaurerStock(commande);
        }

        commande = commandeRepository.save(commande);
        historiqueRepository.save(new HistoriqueStatutCommande(commande, ancienStatut, nouveauStatut,
                auteur != null ? auteur : "SYSTEME", commentaire));

        log.info("✅ Statut commande mis à jour - ID: {}, Statut: {}", commandeId, nouveauStatut);
        return convertToDTO(commande);
    }

    /** Surcharge sans commentaire — rétrocompatibilité interne. */
    public CommandeDTO updateStatutCommande(Long commandeId, Commande.StatutCommande nouveauStatut) {
        return updateStatutCommande(commandeId, nouveauStatut, null, "SYSTEME");
    }

    /**
     * Annulation par le client — uniquement SA commande, uniquement en EN_ATTENTE.
     */
    public CommandeDTO annulerCommandeClient(Long commandeId, Long clientId, String motif) {
        log.info("❌ Annulation client - commandeId: {}, clientId: {}", commandeId, clientId);

        Commande commande = commandeRepository.findById(commandeId)
                .orElseThrow(() -> new ResourceNotFoundException("Commande non trouvée : " + commandeId));

        if (commande.getClient() == null || !commande.getClient().getIdClient().equals(clientId)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Vous n'avez pas accès à cette commande");
        }

        if (commande.getStatutCommande() != Commande.StatutCommande.EN_ATTENTE) {
            throw new IllegalStateException(
                    "Seules les commandes en attente peuvent être annulées par le client");
        }

        commande.setStatutCommande(Commande.StatutCommande.ANNULEE);
        commande.setCommentaire(motif != null && !motif.isBlank() ? motif : commande.getCommentaire());
        commande.setDateModification(LocalDateTime.now());
        restaurerStock(commande);
        commande = commandeRepository.save(commande);
        historiqueRepository.save(new HistoriqueStatutCommande(commande, Commande.StatutCommande.EN_ATTENTE,
                Commande.StatutCommande.ANNULEE, "CLIENT", motif));
        log.info("✅ Commande {} annulée par le client {}", commandeId, clientId);
        return convertToDTO(commande);
    }

    /**
     * Annuler une commande avec motif
     */
    public CommandeDTO annulerCommande(Long commandeId, String motif) {
        log.info("❌ Annulation commande - ID: {}, Motif: {}", commandeId, motif);

        Commande commande = commandeRepository.findById(commandeId)
                .orElseThrow(() -> new ResourceNotFoundException("Commande non trouvée avec l'ID : " + commandeId));

        // Vérifier que la commande peut être annulée
        if (commande.getStatutCommande() == Commande.StatutCommande.LIVREE) {
            throw new IllegalStateException("Une commande livrée ne peut pas être annulée");
        }

        if (commande.getStatutCommande() == Commande.StatutCommande.ANNULEE) {
            throw new IllegalStateException("Cette commande est déjà annulée");
        }

        Commande.StatutCommande ancienStatut = commande.getStatutCommande();
        commande.setStatutCommande(Commande.StatutCommande.ANNULEE);
        if (motif != null && !motif.isBlank()) commande.setCommentaire(motif);
        commande.setDateModification(LocalDateTime.now());
        restaurerStock(commande);
        commande = commandeRepository.save(commande);
        historiqueRepository.save(new HistoriqueStatutCommande(commande, ancienStatut,
                Commande.StatutCommande.ANNULEE, "ADMIN", motif));
        log.info("✅ Commande annulée - ID: {}", commandeId);
        return convertToDTO(commande);
    }

    /**
     * Récupère le statut d'une commande pour un invité (numéro + téléphone normalisé).
     * Retourne un DTO limité : statut, date, articles (nom+qté), total, historique simplifié.
     * Réponse identique (404) si commande inexistante ou téléphone incorrect.
     */
    public CommandeDTO getSuiviCommande(String numeroCommande, String telephone) {
        Commande commande = commandeRepository.findByNumeroCommande(numeroCommande)
                .orElseThrow(() -> new ResourceNotFoundException("Commande non trouvée"));

        String telNormalise = normaliserTelephone(telephone);
        String telCommande = commande.getClient() != null
                ? normaliserTelephone(commande.getAdresseLivraison() != null
                        ? commande.getAdresseLivraison().getTelephone() : "")
                : normaliserTelephone(commande.getTelephoneInvite());

        if (!telNormalise.equals(telCommande)) {
            throw new ResourceNotFoundException("Commande non trouvée");
        }

        return convertToSuiviDTO(commande);
    }

    private String normaliserTelephone(String telephone) {
        if (telephone == null) return "";
        return telephone.replaceAll("[^0-9]", "");
    }

    private CommandeDTO convertToSuiviDTO(Commande commande) {
        CommandeDTO dto = new CommandeDTO();
        dto.setIdCommande(commande.getIdCommande());
        dto.setNumeroCommande(commande.getNumeroCommande());
        dto.setDateCommande(commande.getDateCommande());
        dto.setStatutCommande(commande.getStatutCommande());
        dto.setStatutPaiement(commande.getStatutPaiement());
        dto.setMontantTotal(commande.getMontantTotal());
        dto.setModeLivraison(commande.getModeLivraison());
        dto.setZoneNom(commande.getZoneNom());
        if (commande.getLignesCommande() != null) {
            dto.setLignesCommande(commande.getLignesCommande().stream()
                    .map(l -> {
                        CommandeDTO.LigneCommandeDTO ld = new CommandeDTO.LigneCommandeDTO();
                        ld.setNomProduitCommande(l.getNomProduitCommande());
                        ld.setQuantite(l.getQuantite());
                        return ld;
                    }).collect(Collectors.toList()));
        }
        List<CommandeDTO.HistoriqueStatutDTO> historique = historiqueRepository
                .findByCommandeIdCommandeOrderByDateChangementAsc(commande.getIdCommande()).stream()
                .map(h -> {
                    CommandeDTO.HistoriqueStatutDTO hd = new CommandeDTO.HistoriqueStatutDTO();
                    hd.setNouveauStatut(h.getNouveauStatut());
                    hd.setDateChangement(h.getDateChangement());
                    return hd;
                }).collect(Collectors.toList());
        dto.setHistoriqueStatuts(historique);
        return dto;
    }

    /**
     * Supprimer définitivement une commande (uniquement si annulée)
     */
    public void supprimerCommande(Long commandeId) {
        log.info("🗑️ Suppression commande - ID: {}", commandeId);

        Commande commande = commandeRepository.findById(commandeId)
                .orElseThrow(() -> new ResourceNotFoundException("Commande non trouvée avec l'ID : " + commandeId));

        // Vérifier que la commande peut être supprimée
        if (commande.getStatutCommande() != Commande.StatutCommande.ANNULEE) {
            throw new IllegalStateException("Seules les commandes annulées peuvent être supprimées définitivement");
        }

        commandeRepository.delete(commande);
        log.info("✅ Commande supprimée - ID: {}", commandeId);
    }

    /** Compte les commandes par statut — pour le dashboard admin. */
    public Map<String, Long> getCommandeStats() {
        Map<String, Long> stats = new LinkedHashMap<>();
        for (Commande.StatutCommande s : Commande.StatutCommande.values()) {
            stats.put(s.name(), commandeRepository.countByStatutCommande(s));
        }
        return stats;
    }

    // ============== MÉTHODES UTILITAIRES ==============

    /**
     * Vérifier si un changement de statut est autorisé
     */
    private boolean isStatutChangeAllowed(Commande.StatutCommande ancienStatut, Commande.StatutCommande nouveauStatut) {
        switch (ancienStatut) {
            case EN_ATTENTE:
                return nouveauStatut == Commande.StatutCommande.CONFIRMEE ||
                        nouveauStatut == Commande.StatutCommande.ANNULEE;
            case CONFIRMEE:
                // Seul l'admin peut annuler depuis CONFIRMEE
                return nouveauStatut == Commande.StatutCommande.LIVREE ||
                        nouveauStatut == Commande.StatutCommande.ANNULEE;
            case LIVREE:
                return false;
            case ANNULEE:
                return false;
            default:
                return false;
        }
    }

    /**
     * Restaurer le stock des produits lors d'une annulation
     */
    private void restaurerStock(Commande commande) {
        if (commande.getLignesCommande() != null) {
            for (LigneCommande ligne : commande.getLignesCommande()) {
                try {
                    Produit produit = ligne.getProduit();
                    int nouvelleQuantite = produit.getStockDisponible() + ligne.getQuantite();
                    produitService.updateStock(produit.getIdProduit(), nouvelleQuantite);

                    log.info("🔄 Stock restauré - Produit: {} (+{})",
                            produit.getNomProduit(), ligne.getQuantite());
                } catch (Exception e) {
                    log.error("❌ Erreur lors de la restauration du stock pour le produit ID: {}",
                            ligne.getProduit().getIdProduit(), e);
                }
            }
        }
    }

    /**
     * Conversion DTO avec toutes les données nécessaires pour l'admin
     */
    private CommandeDTO convertToDTOWithFullDetails(Commande commande) {
        CommandeDTO dto = convertToDTO(commande);

        // Ajouter des informations spécifiques pour l'admin
        if (commande.getClient() != null) {
            dto.setClientId(commande.getClient().getIdClient());
        }

        // S'assurer que tous les champs sont bien mappés
       // dto.setMotifAnnulation(commande.getMotifAnnulation());
        dto.setDateModification(commande.getDateModification());

        return dto;
    }
}