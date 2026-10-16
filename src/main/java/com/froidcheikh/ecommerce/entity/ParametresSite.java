package com.froidcheikh.ecommerce.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "parametres_site")
@Data
@NoArgsConstructor
public class ParametresSite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ---- IDENTITE ----
    @Column(name = "nom_site")
    private String nomSite;

    @Column(name = "slogan_site")
    private String sloganSite;

    @Column(name = "description_site", columnDefinition = "TEXT")
    private String descriptionSite;

    @Column(name = "logo_url")
    private String logoUrl;

    @Column(name = "favicon_url")
    private String faviconUrl;

    @Column(name = "mission", columnDefinition = "TEXT")
    private String mission;

    @Column(name = "vision", columnDefinition = "TEXT")
    private String vision;

    @Column(name = "annees_experience")
    private Integer anneesExperience;

    @Column(name = "description_livraison")
    private String descriptionLivraison;

    // ---- CONTACT ----
    @Column(name = "email_contact")
    private String emailContact;

    @Column(name = "email_support")
    private String emailSupport;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "site_telephones", joinColumns = @JoinColumn(name = "parametres_site_id"))
    private List<TelephoneSite> telephones = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "site_horaires", joinColumns = @JoinColumn(name = "parametres_site_id"))
    private List<HoraireJour> horairesOuverture = new ArrayList<>();

    @Column(name = "adresse_complete")
    private String adresseComplete;

    @Column(name = "adresse_courte")
    private String adresseCourte;

    @Column(name = "ville")
    private String ville;

    @Column(name = "pays")
    private String pays;

    // ---- RESEAUX SOCIAUX ----
    @Column(name = "facebook_url")
    private String facebookUrl;

    @Column(name = "instagram_url")
    private String instagramUrl;

    @Column(name = "twitter_url")
    private String twitterUrl;

    @Column(name = "linkedin_url")
    private String linkedinUrl;

    @Column(name = "youtube_url")
    private String youtubeUrl;

    @Column(name = "tiktok_url")
    private String tiktokUrl;

    // ---- APPARENCE ----
    /** Couleur primaire au format #RRGGBB (défaut : bleu) */
    @Column(name = "couleur_primaire", length = 7)
    private String couleurPrimaire = "#2563eb";

    /** Couleur secondaire au format #RRGGBB (défaut : vert) */
    @Column(name = "couleur_secondaire", length = 7)
    private String couleurSecondaire = "#16a34a";

    /** Couleur d'accentuation au format #RRGGBB (défaut : rouge) */
    @Column(name = "couleur_accent", length = 7)
    private String couleurAccent = "#dc2626";

    @Column(name = "police")
    private String police = "Inter";

    // ---- TEXTES LEGAUX ----
    @Column(name = "mentions_legales", columnDefinition = "TEXT")
    private String mentionsLegales;

    @Column(name = "politique_confidentialite", columnDefinition = "TEXT")
    private String politiqueConfidentialite;

    @Column(name = "cgv", columnDefinition = "TEXT")
    private String cgv;

    // ---- ENTREPRISE / FACTURATION ----
    @Column(name = "raison_sociale")
    private String raisonSociale;

    @Column(name = "forme_juridique")
    private String formeJuridique;

    @Column(name = "ninea", length = 20)
    private String ninea;

    @Column(name = "rccm", length = 30)
    private String rccm;

    @Column(name = "numero_cfe", length = 30)
    private String numeroCfe;

    @Column(name = "capital_social")
    private String capitalSocial;

    @Column(name = "adresse_siege", columnDefinition = "TEXT")
    private String adresseSiege;

    /** Taux de TVA par défaut en %. 0 = non assujetti (le PDF masque les colonnes TVA). */
    @Column(name = "taux_tva_defaut", precision = 5, scale = 2)
    private java.math.BigDecimal tauxTvaDefaut = java.math.BigDecimal.ZERO;

    /** true = les prix du site sont TTC ; false = HT (non assujetti ou HT affiché). */
    @Column(name = "prix_affiche_ttc")
    private Boolean prixAfficheTTC = false;

    @Column(name = "prefixe_facture", length = 10)
    private String prefixeFacture = "FAC";

    @Column(name = "prefixe_avoir", length = 10)
    private String prefixeAvoir = "AVO";

    @Column(name = "prefixe_devis", length = 10)
    private String prefixeDevis = "DEV";

    @Column(name = "prefixe_bon_livraison", length = 10)
    private String prefixeBonLivraison = "BL";

    @Column(name = "conditions_paiement_defaut", columnDefinition = "TEXT")
    private String conditionsPaiementDefaut;

    @Column(name = "mentions_pied_document", columnDefinition = "TEXT")
    private String mentionsPiedDocument;

    @Column(name = "url_cachet_signature")
    private String urlCachetSignature;

    /** Mention affichée sur les PDFs quand tauxTvaDefaut = 0 (ex : "TVA non applicable"). */
    @Column(name = "mention_exoneration_tva", length = 200)
    private String mentionExonerationTva = "TVA non applicable";

    /** Durée de validité des jetons de téléchargement WhatsApp, en heures. */
    @Column(name = "duree_validite_jeton_heures")
    private Integer dureeValiditeJetonHeures = 72;

    /**
     * Couleur utilisée pour les documents PDF (en-tête tableau, traits, numéro de document).
     * Si null, la couleurPrimaire du site est utilisée en repli.
     * Format #RRGGBB.
     */
    @Column(name = "couleur_documents", length = 7)
    private String couleurDocuments;

    // ---- EN-TÊTE DOCUMENTS PDF ----
    /** Texte libre décrivant les activités de l'entreprise (affiché dans l'en-tête composé). */
    @Column(name = "texte_descriptif_activites", columnDefinition = "TEXT")
    private String texteDescriptifActivites;

    @Column(name = "boite_postale", length = 30)
    private String boitePostale;

    @Column(name = "banque_nom")
    private String banqueNom;

    @Column(name = "numero_compte_bancaire")
    private String numeroCompteBancaire;

    /** Mode de rendu de l'en-tête des PDFs de facturation. */
    @Enumerated(EnumType.STRING)
    @Column(name = "mode_en_tete_documents", length = 10)
    private ModeEnTeteDocuments modeEnTeteDocuments = ModeEnTeteDocuments.COMPOSE;

    /** URL de l'image pleine largeur utilisée en mode IMAGE pour l'en-tête (Cloudinary). */
    @Column(name = "url_image_en_tete")
    private String urlImageEnTete;

    /** URL de l'image pleine largeur utilisée en mode IMAGE pour le pied de page (Cloudinary). */
    @Column(name = "url_image_pied")
    private String urlImagePied;

    // ---- LOCALISATION ----
    @Column(name = "fuseau_horaire", length = 50)
    private String fuseauHoraire = "Africa/Dakar";

    // ---- LIVRAISON ----
    /** Code ISO 4217 (ex. "XOF") — utilisé pour les paiements, catalogues Meta/Google et Angular. */
    @Column(name = "devise", length = 10)
    private String devise = "XOF";

    /** Libellé affiché aux clients (ex. "FCFA") — utilisé dans les PDFs et sur le site. */
    @Column(name = "libelle_devise", length = 20)
    private String libelleDevise = "FCFA";

    /** Montant minimum du panier pour bénéficier de la livraison gratuite (null = désactivé). */
    @Column(name = "seuil_livraison_gratuite")
    private Integer seuilLivraisonGratuite;

    /** Supplément facturé en plus du prix standard lors d'une LIVRAISON_EXPRESS, sauf si prixExpress est explicitement fixé sur la zone. */
    @Column(name = "supplement_express", precision = 10, scale = 2)
    private java.math.BigDecimal supplementExpress = java.math.BigDecimal.valueOf(1500);

    /** Supplément unique par commande si au moins un produit d'une catégorie à gabarit lourd est dans le panier. 0 = désactivé. */
    @Column(name = "supplement_gabarit_lourd", precision = 10, scale = 2)
    private java.math.BigDecimal supplementGabaritLourd = java.math.BigDecimal.ZERO;

    // ---- SEO ----
    @Column(name = "meta_title")
    private String metaTitle;

    @Column(name = "meta_description")
    private String metaDescription;

    @Column(name = "meta_keywords")
    private String metaKeywords;

    /** URL publique du site (ex. https://www.froidcheikhantambacke.sn) — utilisée pour les URLs canoniques, le sitemap et les balises OG. */
    @Column(name = "site_url")
    private String siteUrl;

    // ---- SECTIONS VISIBILITE ----
    @Column(name = "afficher_hero_slider")
    private Boolean afficherHeroSlider = true;

    @Column(name = "afficher_partenaires")
    private Boolean afficherPartenaires = true;

    @Column(name = "afficher_prestations")
    private Boolean afficherPrestations = true;

    @Column(name = "afficher_equipe")
    private Boolean afficherEquipe = true;

    @Column(name = "afficher_realisations")
    private Boolean afficherRealisations = true;

    // ---- WHATSAPP MESSAGES ----

    /** Indicatif pays par défaut pour la normalisation des numéros (ex. "221" pour le Sénégal). */
    @Column(name = "indicatif_pays_defaut", length = 5)
    private String indicatifPaysDefaut = "221";

    /** Message client affiché sur la page de confirmation (bouton → numéro boutique). */
    @Column(name = "whatsapp_msg_nouvelle_commande", columnDefinition = "TEXT")
    private String whatsappMsgNouvelleCommande =
            "Bonjour ! Je viens de passer la commande {numeroCommande}.\nArticles : {articles}\nTotal : {total} {devise}\nZone : {zoneNom} — {modeLivraison}";

    /** Message admin → client : commande confirmée. */
    @Column(name = "whatsapp_msg_confirmee", columnDefinition = "TEXT")
    private String whatsappMsgConfirmee =
            "Bonjour {nomClient}, votre commande {numeroCommande} est confirmée. Montant : {total} {devise}. Merci de votre confiance !";

    /** Message admin → client : commande livrée. */
    @Column(name = "whatsapp_msg_livree", columnDefinition = "TEXT")
    private String whatsappMsgLivree =
            "Bonjour {nomClient}, votre commande {numeroCommande} a bien été livrée. Bonne utilisation !";

    /** Message admin → client : commande annulée. */
    @Column(name = "whatsapp_msg_annulee", columnDefinition = "TEXT")
    private String whatsappMsgAnnulee =
            "Bonjour {nomClient}, votre commande {numeroCommande} a été annulée. Motif : {motif}. Contactez-nous pour plus d'informations.";

    // ---- METADATA ----
    @CreationTimestamp
    @Column(name = "date_creation", updatable = false)
    private LocalDateTime dateCreation;

    @UpdateTimestamp
    @Column(name = "date_modification")
    private LocalDateTime dateModification;

    @Column(name = "modifie_par")
    private String modifiePar;
}
