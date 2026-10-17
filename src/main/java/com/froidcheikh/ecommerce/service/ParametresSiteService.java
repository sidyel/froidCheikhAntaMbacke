package com.froidcheikh.ecommerce.service;

import com.froidcheikh.ecommerce.dto.ParametresSiteDTO;
import com.froidcheikh.ecommerce.entity.ParametresSite;
import com.froidcheikh.ecommerce.entity.HoraireJour;
import com.froidcheikh.ecommerce.entity.TelephoneSite;
import com.froidcheikh.ecommerce.repository.ParametresSiteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ParametresSiteService {

    private final ParametresSiteRepository repository;

    @Cacheable("siteConfig")
    public ParametresSiteDTO.PublicDTO getPublicConfig() {
        return repository.findAll().stream()
                .findFirst()
                .map(this::toPublicDTO)
                .orElseGet(this::defaultPublicDTO);
    }

    public ParametresSiteDTO.AdminDTO getAdminConfig() {
        return repository.findAll().stream()
                .findFirst()
                .map(this::toAdminDTO)
                .orElseGet(ParametresSiteDTO.AdminDTO::new);
    }

    @Transactional
    @CacheEvict(value = "siteConfig", allEntries = true)
    public ParametresSiteDTO.AdminDTO update(ParametresSiteDTO.AdminDTO dto, String modifiePar) {
        ParametresSite entity = repository.findAll().stream()
                .findFirst()
                .orElseGet(ParametresSite::new);

        applyDTO(entity, dto);
        entity.setModifiePar(modifiePar);
        entity = repository.save(entity);
        return toAdminDTO(entity);
    }

    @Transactional
    @CacheEvict(value = "siteConfig", allEntries = true)
    public String updateCachetUrl(String url) {
        ParametresSite entity = repository.findAll().stream().findFirst().orElseThrow();
        entity.setUrlCachetSignature(url);
        repository.save(entity);
        return url;
    }

    @Transactional
    @CacheEvict(value = "siteConfig", allEntries = true)
    public String updateImageEnTeteUrl(String url) {
        ParametresSite entity = repository.findAll().stream().findFirst().orElseThrow();
        entity.setUrlImageEnTete(url);
        repository.save(entity);
        return url;
    }

    @Transactional
    @CacheEvict(value = "siteConfig", allEntries = true)
    public String updateImagePiedUrl(String url) {
        ParametresSite entity = repository.findAll().stream().findFirst().orElseThrow();
        entity.setUrlImagePied(url);
        repository.save(entity);
        return url;
    }

    @Transactional
    @CacheEvict(value = "siteConfig", allEntries = true)
    public void initDefault(String nomSite, String sloganSite, String emailContact, Integer anneesExperience) {
        if (!repository.findAll().isEmpty()) return;

        ParametresSite p = new ParametresSite();
        p.setNomSite(nomSite);
        p.setSloganSite(sloganSite);
        p.setEmailContact(emailContact);
        p.setAnneesExperience(anneesExperience);
        p.setCouleurDocuments("#000080");
        p.setSiteUrl("http://localhost:4000");
        p.setTexteDescriptifActivites("Réparation et Vente de tous Appareils de Froid : Electricité, Plomberie, Climatisation Centrale, Chambre Froide, Split Système, Climatiseur et Frigos");
        p.setDevise("XOF");
        p.setLibelleDevise("FCFA");
        p.setRaisonSociale("FROID CHEIKH ANTA MBACKE - BABACAR DIONE");
        p.setAdresseSiege("Ouest Foire Cité Aelmas Villa N°168 Rue 577 Yoff, Dakar, Sénégal");
        p.setBoitePostale("B.P. 7605 Dakar");
        p.setRccm("92-A-5673");
        p.setNinea("0151340-2B1");
        p.setBanqueNom("CBAO");
        p.setNumeroCompteBancaire("36017179-49");
        p.getTelephones().add(new com.froidcheikh.ecommerce.entity.TelephoneSite("33 820 16 33", "Fixe", false, true));
        p.getTelephones().add(new com.froidcheikh.ecommerce.entity.TelephoneSite("77 335 20 00", "Mobile", true, true));
        p.getTelephones().add(new com.froidcheikh.ecommerce.entity.TelephoneSite("76 888 04 42", "Mobile", false, false));
        repository.save(p);
    }

    /** Migration douce : renseigne les champs de facturation si absents sur la ligne existante. */
    @org.springframework.transaction.annotation.Transactional
    public void initInfosFacturationSiAbsentes() {
        repository.findAll().stream().findFirst().ifPresent(p -> {
            boolean modifie = false;
            if (p.getRaisonSociale() == null || p.getRaisonSociale().isBlank()) {
                p.setRaisonSociale("FROID CHEIKH ANTA MBACKE - BABACAR DIONE"); modifie = true;
            }
            if (p.getTexteDescriptifActivites() == null || p.getTexteDescriptifActivites().isBlank()) {
                p.setTexteDescriptifActivites("Réparation et Vente de tous Appareils de Froid : Electricité, Plomberie, Climatisation Centrale, Chambre Froide, Split Système, Climatiseur et Frigos"); modifie = true;
            }
            if (p.getAdresseSiege() == null || p.getAdresseSiege().isBlank()) {
                p.setAdresseSiege("Ouest Foire Cité Aelmas Villa N°168 Rue 577 Yoff, Dakar, Sénégal"); modifie = true;
            }
            if (p.getBoitePostale() == null || p.getBoitePostale().isBlank()) {
                p.setBoitePostale("B.P. 7605 Dakar"); modifie = true;
            }
            if (p.getRccm() == null || p.getRccm().isBlank()) {
                p.setRccm("92-A-5673"); modifie = true;
            }
            if (p.getNinea() == null || p.getNinea().isBlank()) {
                p.setNinea("0151340-2B1"); modifie = true;
            }
            if (p.getBanqueNom() == null || p.getBanqueNom().isBlank()) {
                p.setBanqueNom("CBAO"); modifie = true;
            }
            if (p.getNumeroCompteBancaire() == null || p.getNumeroCompteBancaire().isBlank()) {
                p.setNumeroCompteBancaire("36017179-49"); modifie = true;
            }
            if (modifie) repository.save(p);
        });
    }

    /** Migration douce : ajoute les téléphones par défaut si la liste est vide. */
    @org.springframework.transaction.annotation.Transactional
    public void initTelephonesSiAbsents() {
        repository.findAll().stream().findFirst().ifPresent(p -> {
            if (p.getTelephones() == null || p.getTelephones().isEmpty()) {
                p.getTelephones().add(new com.froidcheikh.ecommerce.entity.TelephoneSite("33 820 16 33", "Fixe", false, true));
                p.getTelephones().add(new com.froidcheikh.ecommerce.entity.TelephoneSite("77 335 20 00", "Mobile", true, true));
                p.getTelephones().add(new com.froidcheikh.ecommerce.entity.TelephoneSite("76 888 04 42", "Mobile", false, false));
                repository.save(p);
            }
        });
    }

    /** Migration douce : remet devise à son code ISO si une ancienne migration l'avait mis à "FCFA". */
    @org.springframework.transaction.annotation.Transactional
    public void migrerDevise(String ancienne, String nouvelle) {
        repository.findAll().stream().findFirst().ifPresent(p -> {
            if (ancienne.equals(p.getDevise())) {
                p.setDevise(nouvelle);
                repository.save(p);
            }
        });
    }

    /**
     * Migration ponctuelle idempotente : corrige texteDescriptifActivites seulement si sa valeur
     * est EXACTEMENT l'ancienne valeur connue. Sans effet si le champ a été modifié depuis l'admin.
     */
    @org.springframework.transaction.annotation.Transactional
    public void migrerTexteDescriptifActivites(String ancienne, String nouvelle) {
        repository.findAll().stream().findFirst().ifPresent(p -> {
            if (ancienne.equals(p.getTexteDescriptifActivites())) {
                p.setTexteDescriptifActivites(nouvelle);
                repository.save(p);
            }
        });
    }

    /** Migration ponctuelle idempotente : corrige boitePostale si la valeur est exactement l'ancienne. */
    @org.springframework.transaction.annotation.Transactional
    public void migrerBoitePostale(String ancienne, String nouvelle) {
        repository.findAll().stream().findFirst().ifPresent(p -> {
            if (ancienne.equals(p.getBoitePostale())) {
                p.setBoitePostale(nouvelle);
                repository.save(p);
            }
        });
    }

    /**
     * Migration ponctuelle idempotente : supprime le numéro de test 77 330 00 06 et installe
     * les trois numéros réels. N'agit que si 77 330 00 06 est présent dans la liste.
     */
    @org.springframework.transaction.annotation.Transactional
    public void migrerTelephones() {
        repository.findAll().stream().findFirst().ifPresent(p -> {
            boolean aNumeroTest = p.getTelephones().stream()
                    .anyMatch(t -> "77 330 00 06".equals(t.getNumero()));
            if (!aNumeroTest) return;

            p.getTelephones().removeIf(t -> "77 330 00 06".equals(t.getNumero()));

            java.util.Set<String> existants = p.getTelephones().stream()
                    .map(com.froidcheikh.ecommerce.entity.TelephoneSite::getNumero)
                    .collect(java.util.stream.Collectors.toSet());

            if (!existants.contains("33 820 16 33"))
                p.getTelephones().add(new com.froidcheikh.ecommerce.entity.TelephoneSite("33 820 16 33", "Fixe", false, true));
            if (!existants.contains("77 335 20 00"))
                p.getTelephones().add(new com.froidcheikh.ecommerce.entity.TelephoneSite("77 335 20 00", "Mobile", true, true));
            if (!existants.contains("76 888 04 42"))
                p.getTelephones().add(new com.froidcheikh.ecommerce.entity.TelephoneSite("76 888 04 42", "Mobile", false, false));

            repository.save(p);
        });
    }

    /** Migration douce : renseigne libelleDevise si null sur la ligne existante. */
    @org.springframework.transaction.annotation.Transactional
    public void initLibelleDeviseSiAbsent(String libelle) {
        repository.findAll().stream().findFirst().ifPresent(p -> {
            if (p.getLibelleDevise() == null || p.getLibelleDevise().isBlank()) {
                p.setLibelleDevise(libelle);
                repository.save(p);
            }
        });
    }

    /**
     * Migration douce : renseigne couleurDocuments si null sur la ligne existante.
     * Appelé par DataInitializer au démarrage pour les bases déjà initialisées.
     */
    @org.springframework.transaction.annotation.Transactional
    public void initCouleurDocumentsSiAbsente(String couleur) {
        repository.findAll().stream().findFirst().ifPresent(p -> {
            if (p.getCouleurDocuments() == null) {
                p.setCouleurDocuments(couleur);
                repository.save(p);
            }
        });
    }

    /**
     * Migration ponctuelle idempotente (oct. 2026) : vide mentionExonerationTva si elle vaut
     * exactement le défaut Java de l'entité. Sans effet si l'admin a saisi une valeur différente.
     * conditionsPaiementDefaut : aucune migration (le texte n'existait que dans les anciennes
     * données de test de PdfExempleGenerateur, jamais dans ParametresSite).
     */
    private static final String OLD_MENTION_EXONERATION_TVA = "TVA non applicable";

    @org.springframework.transaction.annotation.Transactional
    public void viderMentionsDefaut() {
        repository.findAll().stream().findFirst().ifPresent(p -> {
            boolean modifie = false;
            if (OLD_MENTION_EXONERATION_TVA.equals(p.getMentionExonerationTva())) {
                p.setMentionExonerationTva(null);
                modifie = true;
            }
            if (modifie) repository.save(p);
        });
    }

    @org.springframework.transaction.annotation.Transactional
    public void initTextsLegauxSiAbsents() {
        repository.findAll().stream().findFirst().ifPresent(p -> {
            boolean modifie = false;

            if (p.getCgv() == null || p.getCgv().isBlank()) {
                p.setCgv(CGV_DEFAULT);
                modifie = true;
            }
            if (p.getMentionsLegales() == null || p.getMentionsLegales().isBlank()) {
                p.setMentionsLegales(MENTIONS_LEGALES_DEFAULT);
                modifie = true;
            }
            if (p.getPolitiqueConfidentialite() == null || p.getPolitiqueConfidentialite().isBlank()) {
                p.setPolitiqueConfidentialite(POLITIQUE_CONF_DEFAULT);
                modifie = true;
            }
            if (modifie) {
                repository.save(p);
            }
        });
    }

    private static final String CGV_DEFAULT = """
# Conditions Générales de Vente

---

## Article 1 — Objet et champ d'application

Les présentes Conditions Générales de Vente (CGV) régissent les relations contractuelles entre Froid Cheikh Anta Mbacké (ci-après « le Vendeur ») et tout acheteur particulier ou professionnel (ci-après « le Client ») passant commande sur le site froidcheikhantambacke.sn ou en boutique.

Toute commande implique l'acceptation pleine et entière des présentes CGV, qui prévalent sur tout autre document.

---

## Article 2 — Produits

Les produits proposés sont des équipements de climatisation, réfrigération et électroménager. Les caractéristiques essentielles de chaque produit sont présentées sur la fiche produit correspondante. Les photographies sont fournies à titre illustratif et ne sont pas contractuelles.

---

## Article 3 — Prix

Les prix sont indiqués en Francs CFA (XOF), toutes taxes comprises. Le Vendeur se réserve le droit de modifier ses prix à tout moment ; les commandes sont facturées au tarif en vigueur au moment de la validation.

---

## Article 4 — Commande et paiement

La commande est ferme et définitive après confirmation par le Client. Le paiement s'effectue à la livraison (espèces ou mobile money) ou selon les modalités convenues avec le Vendeur. Aucune commande n'est traitée sans confirmation de paiement ou accord préalable.

---

## Article 5 — Livraison

Les délais de livraison sont indiqués à titre indicatif lors de la commande. Les frais de livraison sont calculés selon la zone géographique et communiqués avant validation. Tout retard de livraison dû à un cas de force majeure ne saurait engager la responsabilité du Vendeur.

---

## Article 6 — Garantie

Les appareils vendus bénéficient d'une garantie constructeur (généralement 1 an pièces et main-d'œuvre), dont les conditions sont précisées sur le bon de garantie remis à la livraison. La garantie ne couvre pas les dommages résultant d'une mauvaise utilisation, d'une installation non conforme ou d'une intervention non autorisée.

---

## Article 7 — Retours et réclamations

Toute réclamation doit être formulée dans les 48 heures suivant la livraison. Un produit ne peut être retourné qu'après accord préalable du Vendeur. Les frais de retour sont à la charge du Client, sauf en cas de défaut avéré ou d'erreur de livraison.

---

## Article 8 — Responsabilité

Le Vendeur ne saurait être tenu responsable des dommages indirects résultant de l'utilisation des produits. Sa responsabilité est limitée au montant de la commande concernée.

---

## Article 9 — Droit applicable et litiges

Les présentes CGV sont soumises au droit sénégalais. En cas de litige, les parties rechercheront une solution amiable. À défaut, les tribunaux compétents de Dakar seront saisis.

---

## Article 10 — Contact

Pour toute question relative à une commande, contactez-nous à l'adresse email ou par téléphone indiqués sur le site.
""";

    private static final String MENTIONS_LEGALES_DEFAULT = """
# Mentions Légales

---

## Éditeur du site

Dénomination sociale : Froid Cheikh Anta Mbacké
Activité : Vente, installation et maintenance d'équipements de climatisation et réfrigération
Adresse : Dakar, Sénégal
Email : froidcheikhantambacke.dione65@gmail.com
NINEA : (renseigner dans les paramètres du site)
RCCM : (renseigner dans les paramètres du site)

---

## Responsable de publication

Le responsable de la publication est le gérant de Froid Cheikh Anta Mbacké.

---

## Hébergement

Le site est hébergé par un prestataire tiers. Les coordonnées de l'hébergeur sont disponibles sur demande.

---

## Propriété intellectuelle

L'ensemble du contenu de ce site (textes, images, logos, structure) est la propriété exclusive de Froid Cheikh Anta Mbacké ou de ses partenaires. Toute reproduction, même partielle, est strictement interdite sans autorisation écrite préalable.

---

## Données personnelles

Le traitement des données personnelles collectées sur ce site est déclaré auprès de la Commission de Protection des Données Personnelles (CDP) conformément à la loi n° 2008-12 du 25 janvier 2008 sur la protection des données à caractère personnel au Sénégal.

Pour exercer vos droits, consultez notre Politique de Confidentialité.

---

## Cookies

Ce site utilise des cookies à des fins fonctionnelles et statistiques. Vous pouvez gérer vos préférences en cliquant sur « Gérer mes cookies » en bas de page.

---

## Limitation de responsabilité

Froid Cheikh Anta Mbacké s'efforce d'assurer l'exactitude des informations diffusées sur ce site. Cependant, il ne peut garantir leur exhaustivité et décline toute responsabilité pour les interruptions ou erreurs qui pourraient y apparaître.

---

## Droit applicable

Les présentes mentions légales sont soumises au droit sénégalais. Tout litige relève de la compétence exclusive des tribunaux de Dakar.
""";

    private static final String POLITIQUE_CONF_DEFAULT = """
# Politique de Confidentialité

---

## 1. Responsable du traitement

Froid Cheikh Anta Mbacké, entreprise sénégalaise dont le siège social est à Dakar, est responsable du traitement de vos données personnelles collectées via le site froidcheikhantambacke.sn.

Ce traitement est déclaré auprès de la Commission de Protection des Données Personnelles (CDP) conformément à la loi n° 2008-12 du 25 janvier 2008 sur la protection des données à caractère personnel en République du Sénégal.

---

## 2. Données collectées

Nous collectons uniquement les données nécessaires au traitement de vos commandes et à l'amélioration de nos services :

Données d'identification : nom, prénom, adresse email, numéro de téléphone.
Données de livraison : adresse de livraison, zone géographique.
Données de navigation : adresse IP, pages visitées, cookies (avec votre consentement).

---

## 3. Finalités du traitement

Vos données sont utilisées pour : traiter et suivre vos commandes, vous contacter concernant votre commande, améliorer notre site et nos services, respecter nos obligations légales et comptables.

Nous n'utilisons jamais vos données à des fins de démarchage non consenti.

---

## 4. Partage des données

Vos données ne sont jamais vendues à des tiers. Elles peuvent être transmises à nos partenaires logistiques uniquement dans le cadre de la livraison de votre commande, dans le strict respect de la confidentialité.

---

## 5. Conservation des données

Vos données de commande sont conservées pendant 5 ans conformément aux obligations comptables et fiscales sénégalaises. Les données de compte sont conservées tant que votre compte est actif.

---

## 6. Vos droits (loi CDP)

Conformément à la loi n° 2008-12 du 25 janvier 2008, vous disposez des droits suivants : droit d'accès, de rectification, de suppression et d'opposition à l'utilisation de vos données personnelles.

Pour exercer ces droits, contactez-nous à l'adresse email indiquée sur le site en précisant votre demande. Nous répondrons dans un délai de 30 jours.

---

## 7. Cookies

Notre site utilise des cookies fonctionnels (nécessaires au bon fonctionnement du site) et des cookies analytiques (avec votre consentement). Vous pouvez gérer vos préférences à tout moment via le bouton « Gérer mes cookies » en bas de page.

---

## 8. Sécurité

Nous mettons en œuvre les mesures techniques et organisationnelles appropriées pour protéger vos données contre tout accès non autorisé, perte ou altération.

---

## 9. Contact

Pour toute question relative à cette politique ou pour exercer vos droits, contactez-nous via le formulaire de contact ou à l'adresse email du site.
""";

    public boolean exists() {
        return !repository.findAll().isEmpty();
    }

    @Cacheable(value = "siteConfig", key = "'zoneId'")
    public ZoneId getSiteZoneId() {
        return repository.findAll().stream().findFirst()
                .map(p -> {
                    String tz = p.getFuseauHoraire();
                    if (tz == null || tz.isBlank()) return ZoneId.of("Africa/Dakar");
                    try { return ZoneId.of(tz); }
                    catch (Exception e) { return ZoneId.of("Africa/Dakar"); }
                })
                .orElse(ZoneId.of("Africa/Dakar"));
    }

    // ---- mapping helpers ----

    private ParametresSiteDTO.PublicDTO toPublicDTO(ParametresSite p) {
        ParametresSiteDTO.PublicDTO dto = new ParametresSiteDTO.PublicDTO();
        dto.setNomSite(p.getNomSite());
        dto.setSloganSite(p.getSloganSite());
        dto.setDescriptionSite(p.getDescriptionSite());
        dto.setLogoUrl(p.getLogoUrl());
        dto.setFaviconUrl(p.getFaviconUrl());
        dto.setEmailContact(p.getEmailContact());
        dto.setTelephones(mapTelephones(p.getTelephones()));
        dto.setAdresseComplete(p.getAdresseComplete());
        dto.setVille(p.getVille());
        dto.setPays(p.getPays());
        dto.setFacebookUrl(p.getFacebookUrl());
        dto.setInstagramUrl(p.getInstagramUrl());
        dto.setTwitterUrl(p.getTwitterUrl());
        dto.setLinkedinUrl(p.getLinkedinUrl());
        dto.setYoutubeUrl(p.getYoutubeUrl());
        dto.setTiktokUrl(p.getTiktokUrl());
        dto.setCouleurPrimaire(p.getCouleurPrimaire());
        dto.setCouleurSecondaire(p.getCouleurSecondaire());
        dto.setCouleurAccent(p.getCouleurAccent());
        dto.setPolice(p.getPolice());
        dto.setDevise(p.getDevise());
        dto.setSeuilLivraisonGratuite(p.getSeuilLivraisonGratuite());
        dto.setMetaTitle(p.getMetaTitle());
        dto.setMetaDescription(p.getMetaDescription());
        dto.setMetaKeywords(p.getMetaKeywords());
        dto.setSiteUrl(p.getSiteUrl());
        dto.setMission(p.getMission());
        dto.setVision(p.getVision());
        dto.setAnneesExperience(p.getAnneesExperience());
        dto.setDescriptionLivraison(p.getDescriptionLivraison());
        dto.setAdresseCourte(p.getAdresseCourte());
        dto.setHorairesOuverture(mapHoraires(p.getHorairesOuverture()));
        dto.setAfficherHeroSlider(p.getAfficherHeroSlider());
        dto.setAfficherPartenaires(p.getAfficherPartenaires());
        dto.setAfficherPrestations(p.getAfficherPrestations());
        dto.setAfficherEquipe(p.getAfficherEquipe());
        dto.setAfficherRealisations(p.getAfficherRealisations());
        // WhatsApp : numéro de la boutique calculé + templates
        dto.setIndicatifPaysDefaut(p.getIndicatifPaysDefaut());
        dto.setWhatsappMsgNouvelleCommande(p.getWhatsappMsgNouvelleCommande());
        dto.setWhatsappMsgConfirmee(p.getWhatsappMsgConfirmee());
        dto.setWhatsappMsgLivree(p.getWhatsappMsgLivree());
        dto.setWhatsappMsgAnnulee(p.getWhatsappMsgAnnulee());
        p.getTelephones().stream()
                .filter(t -> Boolean.TRUE.equals(t.getWhatsapp()))
                .findFirst()
                .ifPresent(t -> dto.setTelephoneWhatsappBoutique(t.getNumero()));
        // Publicité et suivi
        dto.setMetaPixelId(p.getMetaPixelId());
        dto.setGoogleAnalyticsId(p.getGoogleAnalyticsId());
        // Textes légaux
        dto.setCgv(p.getCgv());
        dto.setMentionsLegales(p.getMentionsLegales());
        dto.setPolitiqueConfidentialite(p.getPolitiqueConfidentialite());
        return dto;
    }

    private ParametresSiteDTO.AdminDTO toAdminDTO(ParametresSite p) {
        ParametresSiteDTO.AdminDTO dto = new ParametresSiteDTO.AdminDTO();
        dto.setNomSite(p.getNomSite());
        dto.setSloganSite(p.getSloganSite());
        dto.setDescriptionSite(p.getDescriptionSite());
        dto.setLogoUrl(p.getLogoUrl());
        dto.setFaviconUrl(p.getFaviconUrl());
        dto.setEmailContact(p.getEmailContact());
        dto.setEmailSupport(p.getEmailSupport());
        dto.setTelephones(mapTelephones(p.getTelephones()));
        dto.setAdresseComplete(p.getAdresseComplete());
        dto.setVille(p.getVille());
        dto.setPays(p.getPays());
        dto.setFacebookUrl(p.getFacebookUrl());
        dto.setInstagramUrl(p.getInstagramUrl());
        dto.setTwitterUrl(p.getTwitterUrl());
        dto.setLinkedinUrl(p.getLinkedinUrl());
        dto.setYoutubeUrl(p.getYoutubeUrl());
        dto.setTiktokUrl(p.getTiktokUrl());
        dto.setCouleurPrimaire(p.getCouleurPrimaire());
        dto.setCouleurSecondaire(p.getCouleurSecondaire());
        dto.setCouleurAccent(p.getCouleurAccent());
        dto.setPolice(p.getPolice());
        dto.setMentionsLegales(p.getMentionsLegales());
        dto.setPolitiqueConfidentialite(p.getPolitiqueConfidentialite());
        dto.setCgv(p.getCgv());
        dto.setRaisonSociale(p.getRaisonSociale());
        dto.setFormeJuridique(p.getFormeJuridique());
        dto.setNinea(p.getNinea());
        dto.setRccm(p.getRccm());
        dto.setNumeroCfe(p.getNumeroCfe());
        dto.setCapitalSocial(p.getCapitalSocial());
        dto.setDevise(p.getDevise());
        dto.setLibelleDevise(p.getLibelleDevise());
        dto.setSeuilLivraisonGratuite(p.getSeuilLivraisonGratuite());
        dto.setMetaTitle(p.getMetaTitle());
        dto.setMetaDescription(p.getMetaDescription());
        dto.setMetaKeywords(p.getMetaKeywords());
        dto.setSiteUrl(p.getSiteUrl());
        dto.setDateModification(p.getDateModification());
        dto.setModifiePar(p.getModifiePar());
        dto.setMission(p.getMission());
        dto.setVision(p.getVision());
        dto.setAnneesExperience(p.getAnneesExperience());
        dto.setDescriptionLivraison(p.getDescriptionLivraison());
        dto.setAdresseCourte(p.getAdresseCourte());
        dto.setHorairesOuverture(mapHoraires(p.getHorairesOuverture()));
        dto.setAfficherHeroSlider(p.getAfficherHeroSlider());
        dto.setAfficherPartenaires(p.getAfficherPartenaires());
        dto.setAfficherPrestations(p.getAfficherPrestations());
        dto.setAfficherEquipe(p.getAfficherEquipe());
        dto.setAfficherRealisations(p.getAfficherRealisations());
        // WhatsApp
        dto.setIndicatifPaysDefaut(p.getIndicatifPaysDefaut());
        dto.setWhatsappMsgNouvelleCommande(p.getWhatsappMsgNouvelleCommande());
        dto.setWhatsappMsgConfirmee(p.getWhatsappMsgConfirmee());
        dto.setWhatsappMsgLivree(p.getWhatsappMsgLivree());
        dto.setWhatsappMsgAnnulee(p.getWhatsappMsgAnnulee());
        // En-tête documents PDF
        dto.setCouleurDocuments(p.getCouleurDocuments());
        dto.setTexteDescriptifActivites(p.getTexteDescriptifActivites());
        dto.setBoitePostale(p.getBoitePostale());
        dto.setBanqueNom(p.getBanqueNom());
        dto.setNumeroCompteBancaire(p.getNumeroCompteBancaire());
        dto.setModeEnTeteDocuments(p.getModeEnTeteDocuments());
        dto.setUrlImageEnTete(p.getUrlImageEnTete());
        dto.setUrlImagePied(p.getUrlImagePied());
        // Publicité et suivi
        dto.setMetaPixelId(p.getMetaPixelId());
        dto.setGoogleAnalyticsId(p.getGoogleAnalyticsId());
        dto.setMetaConversionsTestCode(p.getMetaConversionsTestCode());
        return dto;
    }

    private void applyDTO(ParametresSite p, ParametresSiteDTO.AdminDTO dto) {
        if (dto.getNomSite() != null) p.setNomSite(dto.getNomSite());
        if (dto.getSloganSite() != null) p.setSloganSite(dto.getSloganSite());
        if (dto.getDescriptionSite() != null) p.setDescriptionSite(dto.getDescriptionSite());
        if (dto.getLogoUrl() != null) p.setLogoUrl(dto.getLogoUrl());
        if (dto.getFaviconUrl() != null) p.setFaviconUrl(dto.getFaviconUrl());
        if (dto.getEmailContact() != null) p.setEmailContact(dto.getEmailContact());
        if (dto.getEmailSupport() != null) p.setEmailSupport(dto.getEmailSupport());
        if (dto.getTelephones() != null) {
            p.getTelephones().clear();
            dto.getTelephones().forEach(t -> {
                TelephoneSite tel = new TelephoneSite();
                tel.setNumero(t.getNumero());
                tel.setLibelle(t.getLibelle());
                tel.setWhatsapp(Boolean.TRUE.equals(t.getWhatsapp()));
                tel.setAfficherEnDocuments(t.getAfficherEnDocuments() == null || t.getAfficherEnDocuments());
                p.getTelephones().add(tel);
            });
        }
        if (dto.getAdresseComplete() != null) p.setAdresseComplete(dto.getAdresseComplete());
        if (dto.getVille() != null) p.setVille(dto.getVille());
        if (dto.getPays() != null) p.setPays(dto.getPays());
        if (dto.getFacebookUrl() != null) p.setFacebookUrl(dto.getFacebookUrl());
        if (dto.getInstagramUrl() != null) p.setInstagramUrl(dto.getInstagramUrl());
        if (dto.getTwitterUrl() != null) p.setTwitterUrl(dto.getTwitterUrl());
        if (dto.getLinkedinUrl() != null) p.setLinkedinUrl(dto.getLinkedinUrl());
        if (dto.getYoutubeUrl() != null) p.setYoutubeUrl(dto.getYoutubeUrl());
        if (dto.getTiktokUrl() != null) p.setTiktokUrl(dto.getTiktokUrl());
        if (dto.getCouleurPrimaire() != null) p.setCouleurPrimaire(dto.getCouleurPrimaire());
        if (dto.getCouleurSecondaire() != null) p.setCouleurSecondaire(dto.getCouleurSecondaire());
        if (dto.getCouleurAccent() != null) p.setCouleurAccent(dto.getCouleurAccent());
        if (dto.getPolice() != null) p.setPolice(dto.getPolice());
        if (dto.getMentionsLegales() != null) p.setMentionsLegales(dto.getMentionsLegales());
        if (dto.getPolitiqueConfidentialite() != null) p.setPolitiqueConfidentialite(dto.getPolitiqueConfidentialite());
        if (dto.getCgv() != null) p.setCgv(dto.getCgv());
        if (dto.getRaisonSociale() != null) p.setRaisonSociale(dto.getRaisonSociale());
        if (dto.getFormeJuridique() != null) p.setFormeJuridique(dto.getFormeJuridique());
        if (dto.getNinea() != null) p.setNinea(dto.getNinea());
        if (dto.getRccm() != null) p.setRccm(dto.getRccm());
        if (dto.getNumeroCfe() != null) p.setNumeroCfe(dto.getNumeroCfe());
        if (dto.getCapitalSocial() != null) p.setCapitalSocial(dto.getCapitalSocial());
        if (dto.getDevise() != null) p.setDevise(dto.getDevise());
        if (dto.getLibelleDevise() != null) p.setLibelleDevise(dto.getLibelleDevise());
        if (dto.getSeuilLivraisonGratuite() != null) p.setSeuilLivraisonGratuite(dto.getSeuilLivraisonGratuite());
        if (dto.getMetaTitle() != null) p.setMetaTitle(dto.getMetaTitle());
        if (dto.getMetaDescription() != null) p.setMetaDescription(dto.getMetaDescription());
        if (dto.getMetaKeywords() != null) p.setMetaKeywords(dto.getMetaKeywords());
        if (dto.getSiteUrl() != null) p.setSiteUrl(dto.getSiteUrl());
        if (dto.getMission() != null) p.setMission(dto.getMission());
        if (dto.getVision() != null) p.setVision(dto.getVision());
        if (dto.getAnneesExperience() != null) p.setAnneesExperience(dto.getAnneesExperience());
        if (dto.getDescriptionLivraison() != null) p.setDescriptionLivraison(dto.getDescriptionLivraison());
        if (dto.getAdresseCourte() != null) p.setAdresseCourte(dto.getAdresseCourte());
        if (dto.getAfficherHeroSlider() != null) p.setAfficherHeroSlider(dto.getAfficherHeroSlider());
        if (dto.getAfficherPartenaires() != null) p.setAfficherPartenaires(dto.getAfficherPartenaires());
        if (dto.getAfficherPrestations() != null) p.setAfficherPrestations(dto.getAfficherPrestations());
        if (dto.getAfficherEquipe() != null) p.setAfficherEquipe(dto.getAfficherEquipe());
        if (dto.getAfficherRealisations() != null) p.setAfficherRealisations(dto.getAfficherRealisations());
        // WhatsApp
        if (dto.getIndicatifPaysDefaut() != null) p.setIndicatifPaysDefaut(dto.getIndicatifPaysDefaut());
        if (dto.getWhatsappMsgNouvelleCommande() != null) p.setWhatsappMsgNouvelleCommande(dto.getWhatsappMsgNouvelleCommande());
        if (dto.getWhatsappMsgConfirmee() != null) p.setWhatsappMsgConfirmee(dto.getWhatsappMsgConfirmee());
        if (dto.getWhatsappMsgLivree() != null) p.setWhatsappMsgLivree(dto.getWhatsappMsgLivree());
        if (dto.getWhatsappMsgAnnulee() != null) p.setWhatsappMsgAnnulee(dto.getWhatsappMsgAnnulee());
        // En-tête documents PDF
        if (dto.getCouleurDocuments() != null) p.setCouleurDocuments(dto.getCouleurDocuments());
        if (dto.getTexteDescriptifActivites() != null) p.setTexteDescriptifActivites(dto.getTexteDescriptifActivites());
        if (dto.getBoitePostale() != null) p.setBoitePostale(dto.getBoitePostale());
        if (dto.getBanqueNom() != null) p.setBanqueNom(dto.getBanqueNom());
        if (dto.getNumeroCompteBancaire() != null) p.setNumeroCompteBancaire(dto.getNumeroCompteBancaire());
        if (dto.getModeEnTeteDocuments() != null) p.setModeEnTeteDocuments(dto.getModeEnTeteDocuments());
        if (dto.getUrlImageEnTete() != null) p.setUrlImageEnTete(dto.getUrlImageEnTete());
        if (dto.getUrlImagePied() != null) p.setUrlImagePied(dto.getUrlImagePied());
        // Publicité et suivi (les valeurs vides sont autorisées pour désactiver)
        if (dto.getMetaPixelId() != null) p.setMetaPixelId(dto.getMetaPixelId().isBlank() ? null : dto.getMetaPixelId().trim());
        if (dto.getGoogleAnalyticsId() != null) p.setGoogleAnalyticsId(dto.getGoogleAnalyticsId().isBlank() ? null : dto.getGoogleAnalyticsId().trim());
        if (dto.getMetaConversionsTestCode() != null) p.setMetaConversionsTestCode(dto.getMetaConversionsTestCode().isBlank() ? null : dto.getMetaConversionsTestCode().trim());
        if (dto.getHorairesOuverture() != null) {
            p.getHorairesOuverture().clear();
            dto.getHorairesOuverture().forEach(h -> {
                HoraireJour horaire = new HoraireJour();
                horaire.setJour(h.getJour());
                horaire.setHeureDebut(h.getHeureDebut());
                horaire.setHeureFin(h.getHeureFin());
                horaire.setFerme(Boolean.TRUE.equals(h.getFerme()));
                p.getHorairesOuverture().add(horaire);
            });
        }
    }

    private List<ParametresSiteDTO.TelephoneSiteDTO> mapTelephones(List<TelephoneSite> telephones) {
        if (telephones == null) return List.of();
        return telephones.stream().map(t -> {
            ParametresSiteDTO.TelephoneSiteDTO dto = new ParametresSiteDTO.TelephoneSiteDTO();
            dto.setNumero(t.getNumero());
            dto.setLibelle(t.getLibelle());
            dto.setWhatsapp(t.getWhatsapp());
            dto.setAfficherEnDocuments(t.getAfficherEnDocuments());
            return dto;
        }).collect(Collectors.toList());
    }

    private List<ParametresSiteDTO.HoraireJourDTO> mapHoraires(List<HoraireJour> horaires) {
        if (horaires == null) return List.of();
        return horaires.stream().map(h -> {
            ParametresSiteDTO.HoraireJourDTO dto = new ParametresSiteDTO.HoraireJourDTO();
            dto.setJour(h.getJour());
            dto.setHeureDebut(h.getHeureDebut());
            dto.setHeureFin(h.getHeureFin());
            dto.setFerme(h.getFerme());
            return dto;
        }).collect(Collectors.toList());
    }

    private ParametresSiteDTO.PublicDTO defaultPublicDTO() {
        ParametresSiteDTO.PublicDTO dto = new ParametresSiteDTO.PublicDTO();
        dto.setCouleurPrimaire("#2563eb");
        dto.setCouleurSecondaire("#16a34a");
        dto.setCouleurAccent("#dc2626");
        dto.setPolice("Inter");
        dto.setDevise("XOF");
        return dto;
    }
}
