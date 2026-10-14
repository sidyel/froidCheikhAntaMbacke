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

    // ---- LIVRAISON ----
    @Column(name = "devise", length = 10)
    private String devise = "XOF";

    @Column(name = "seuil_livraison_gratuite")
    private Integer seuilLivraisonGratuite;

    // ---- SEO ----
    @Column(name = "meta_title")
    private String metaTitle;

    @Column(name = "meta_description")
    private String metaDescription;

    @Column(name = "meta_keywords")
    private String metaKeywords;

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
