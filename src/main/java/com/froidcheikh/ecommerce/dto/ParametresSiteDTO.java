package com.froidcheikh.ecommerce.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

public final class ParametresSiteDTO {
    private ParametresSiteDTO() {}

    @Data
    @NoArgsConstructor
    public static class HoraireJourDTO {
        private String jour;
        private String heureDebut;
        private String heureFin;
        private Boolean ferme;
    }

    @Data
    @NoArgsConstructor
    public static class TelephoneSiteDTO {
        private String numero;
        private String libelle;
        private Boolean whatsapp;
        private Boolean afficherEnDocuments;
    }

    /**
     * Projection publique — pas de données sensibles (textes légaux, facturation).
     * Retournée par GET /api/config.
     */
    @Data
    @NoArgsConstructor
    public static class PublicDTO {
        // IDENTITE
        private String nomSite;
        private String sloganSite;
        private String descriptionSite;
        private String logoUrl;
        private String faviconUrl;

        // IDENTITE - champs supplémentaires
        private String mission;
        private String vision;
        private Integer anneesExperience;
        private String descriptionLivraison;

        // CONTACT
        private String emailContact;
        private List<TelephoneSiteDTO> telephones;
        private List<HoraireJourDTO> horairesOuverture;
        private String adresseComplete;
        private String adresseCourte;
        private String ville;
        private String pays;

        // RESEAUX SOCIAUX
        private String facebookUrl;
        private String instagramUrl;
        private String twitterUrl;
        private String linkedinUrl;
        private String youtubeUrl;
        private String tiktokUrl;

        // APPARENCE
        private String couleurPrimaire;
        private String couleurSecondaire;
        private String couleurAccent;
        private String police;

        // LIVRAISON
        private String devise;
        private Integer seuilLivraisonGratuite;

        // SEO
        private String metaTitle;
        private String metaDescription;
        private String metaKeywords;
        private String siteUrl;

        // SECTIONS VISIBILITE
        private Boolean afficherHeroSlider;
        private Boolean afficherPartenaires;
        private Boolean afficherPrestations;
        private Boolean afficherEquipe;
        private Boolean afficherRealisations;

        // WHATSAPP — numéro de la boutique (premier téléphone whatsapp=true) et templates
        private String telephoneWhatsappBoutique; // calculé par ConfigService, pas mappé depuis ParametresSite
        private String indicatifPaysDefaut;
        private String whatsappMsgNouvelleCommande;
        private String whatsappMsgConfirmee;
        private String whatsappMsgLivree;
        private String whatsappMsgAnnulee;

        // PUBLICITE ET SUIVI
        private String metaPixelId;
        private String googleAnalyticsId;

        // TEXTES LEGAUX
        private String cgv;
        private String mentionsLegales;
        private String politiqueConfidentialite;
    }

    /**
     * DTO admin — tous les champs + validation + metadata.
     * Utilisé par PUT /api/admin/config.
     */
    @Data
    @NoArgsConstructor
    public static class AdminDTO {
        // IDENTITE
        private String nomSite;
        private String sloganSite;
        private String descriptionSite;
        private String logoUrl;
        private String faviconUrl;

        // IDENTITE - champs supplémentaires
        private String mission;
        private String vision;
        private Integer anneesExperience;
        private String descriptionLivraison;

        // CONTACT
        @Email(message = "Format d'email invalide")
        private String emailContact;

        @Email(message = "Format d'email invalide")
        private String emailSupport;

        private List<TelephoneSiteDTO> telephones;
        private List<HoraireJourDTO> horairesOuverture;
        private String adresseComplete;
        private String adresseCourte;
        private String ville;
        private String pays;

        // RESEAUX SOCIAUX
        private String facebookUrl;
        private String instagramUrl;
        private String twitterUrl;
        private String linkedinUrl;
        private String youtubeUrl;
        private String tiktokUrl;

        // APPARENCE
        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Couleur primaire invalide (format #RRGGBB attendu)")
        private String couleurPrimaire;

        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Couleur secondaire invalide (format #RRGGBB attendu)")
        private String couleurSecondaire;

        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Couleur accent invalide (format #RRGGBB attendu)")
        private String couleurAccent;

        private String police;

        // TEXTES LEGAUX
        private String mentionsLegales;
        private String politiqueConfidentialite;
        private String cgv;

        // ENTREPRISE / FACTURATION
        private String raisonSociale;
        private String formeJuridique;
        private String ninea;
        private String rccm;
        private String numeroCfe;
        private String capitalSocial;
        private String adresseSiege;
        private java.math.BigDecimal tauxTvaDefaut;
        private Boolean prixAfficheTTC;
        private String prefixeFacture;
        private String prefixeAvoir;
        private String prefixeDevis;
        private String prefixeBonLivraison;
        private String conditionsPaiementDefaut;
        private String mentionsPiedDocument;
        private String urlCachetSignature;
        private String mentionExonerationTva;
        private Integer dureeValiditeJetonHeures;
        private String couleurDocuments;

        // EN-TÊTE DOCUMENTS PDF
        private String texteDescriptifActivites;
        private String boitePostale;
        private String banqueNom;
        private String numeroCompteBancaire;
        private com.froidcheikh.ecommerce.entity.ModeEnTeteDocuments modeEnTeteDocuments;
        private String urlImageEnTete;
        private String urlImagePied;

        // LIVRAISON
        private String devise;
        private String libelleDevise;
        private Integer seuilLivraisonGratuite;

        // SEO
        private String metaTitle;
        private String metaDescription;
        private String metaKeywords;
        private String siteUrl;

        // SECTIONS VISIBILITE
        private Boolean afficherHeroSlider;
        private Boolean afficherPartenaires;
        private Boolean afficherPrestations;
        private Boolean afficherEquipe;
        private Boolean afficherRealisations;

        // WHATSAPP
        private String indicatifPaysDefaut;
        private String whatsappMsgNouvelleCommande;
        private String whatsappMsgConfirmee;
        private String whatsappMsgLivree;
        private String whatsappMsgAnnulee;

        // PUBLICITE ET SUIVI
        private String metaPixelId;
        private String googleAnalyticsId;
        private String metaConversionsTestCode;

        // METADATA (lecture seule)
        private LocalDateTime dateModification;
        private String modifiePar;
    }
}
