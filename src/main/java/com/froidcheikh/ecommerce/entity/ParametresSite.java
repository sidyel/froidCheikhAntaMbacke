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

    // ---- LOCALISATION ----
    @Column(name = "fuseau_horaire", length = 50)
    private String fuseauHoraire = "Africa/Dakar";

    // ---- LIVRAISON ----
    @Column(name = "devise", length = 10)
    private String devise = "XOF";

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
