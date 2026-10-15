package com.froidcheikh.ecommerce.entity;

import com.froidcheikh.ecommerce.entity.CompteurDocument.TypeDocument;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "documents_facturation")
@Data
@NoArgsConstructor
public class DocumentFacturation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_document", nullable = false, length = 20)
    private TypeDocument typeDocument;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 20)
    private StatutDocument statut = StatutDocument.BROUILLON;

    /** Null en brouillon — attribué à l'émission uniquement. */
    @Column(name = "numero", unique = true)
    private String numero;

    @CreationTimestamp
    @Column(name = "date_creation", updatable = false)
    private LocalDateTime dateCreation;

    @Column(name = "date_emission")
    private LocalDateTime dateEmission;

    /** DEVIS uniquement — date de fin de validité. */
    @Column(name = "date_validite")
    private LocalDate dateValidite;

    /** Commande source (facture de commande ou BL de commande). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "commande_id")
    private Commande commande;

    /** AVOIR → FACTURE source ; FACTURE → DEVIS source. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_origine_id")
    private DocumentFacturation documentOrigine;

    /** Client hors site (vente comptoir / professionnel). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_facturation_id")
    private ClientFacturation clientFacturation;

    /** Client du site. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_site_id")
    private Client clientSite;

    // ---- COPIE FIGÉE CLIENT (remplie à l'émission) ----
    @Column(name = "client_nom")
    private String clientNom;
    @Column(name = "client_prenom")
    private String clientPrenom;
    @Column(name = "client_telephone")
    private String clientTelephone;
    @Column(name = "client_email")
    private String clientEmail;
    @Column(name = "client_adresse", columnDefinition = "TEXT")
    private String clientAdresse;
    @Column(name = "client_ninea", length = 20)
    private String clientNinea;
    @Column(name = "client_rccm", length = 30)
    private String clientRccm;

    // ---- COPIE FIGÉE ENTREPRISE (remplie à l'émission) ----
    @Column(name = "ent_raison_sociale")
    private String entRaisonSociale;
    @Column(name = "ent_adresse_siege", columnDefinition = "TEXT")
    private String entAdresseSiege;
    @Column(name = "ent_ninea", length = 20)
    private String entNinea;
    @Column(name = "ent_rccm", length = 30)
    private String entRccm;
    @Column(name = "ent_couleur_primaire", length = 7)
    private String entCouleurPrimaire;
    @Column(name = "ent_logo_url")
    private String entLogoUrl;
    @Column(name = "ent_cachet_url")
    private String entCachetUrl;
    @Column(name = "ent_conditions_paiement", columnDefinition = "TEXT")
    private String entConditionsPaiement;
    @Column(name = "ent_mentions_pied", columnDefinition = "TEXT")
    private String entMentionsPied;
    @Column(name = "ent_mention_exoneration_tva", length = 200)
    private String entMentionExonerationTva;

    /** Taux TVA par défaut du document (copié de ParametresSite à l'émission). */
    @Column(name = "taux_tva_defaut", precision = 5, scale = 2)
    private BigDecimal tauxTvaDefaut = BigDecimal.ZERO;

    /** true = prix TTC, false = prix HT. */
    @Column(name = "prix_affiche_ttc")
    private Boolean prixAfficheTTC = false;

    /** Remise globale sur le total (%). */
    @Column(name = "remise_totale_pourcent", precision = 5, scale = 2)
    private BigDecimal remiseTotalePourcent = BigDecimal.ZERO;

    // ---- MONTANTS FIGÉS À L'ÉMISSION ----
    @Column(name = "montant_ht", precision = 12, scale = 2)
    private BigDecimal montantHT;

    @Column(name = "montant_tva", precision = 12, scale = 2)
    private BigDecimal montantTva;

    @Column(name = "montant_ttc", precision = 12, scale = 2)
    private BigDecimal montantTTC;

    @Column(name = "montant_en_lettres", columnDefinition = "TEXT")
    private String montantEnLettres;

    /** PDF figé (bytea) — null pour les brouillons. */
    @Column(name = "pdf_bytes", columnDefinition = "bytea")
    @Basic(fetch = FetchType.LAZY)
    private byte[] pdfBytes;

    /** Empreinte SHA-256 du PDF figé. */
    @Column(name = "pdf_sha256", length = 64)
    private String pdfSha256;

    // ---- BON DE LIVRAISON ----
    @Column(name = "livreur_nom")
    private String livreurNom;
    @Column(name = "livreur_telephone")
    private String livreurTelephone;

    // ---- TRAÇABILITÉ ----
    @Column(name = "cree_par")
    private String creePar;

    @Column(name = "modifie_par")
    private String modifiePar;

    @Column(name = "date_modification")
    private LocalDateTime dateModification;

    // ---- RELATIONS ----
    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("ordre ASC")
    private List<LigneDocument> lignes = new ArrayList<>();

    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("dateReglement ASC")
    private List<ReglementDocument> reglements = new ArrayList<>();

    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<JetonTelechargement> jetons = new ArrayList<>();

    public enum StatutDocument {
        BROUILLON,
        EMIS,
        CONVERTI   // DEVIS converti en facture
    }

    /** Un devis est expiré si sa date de validité est passée et qu'il est encore BROUILLON ou EMIS. */
    @Transient
    public boolean isExpire() {
        return typeDocument == TypeDocument.DEVIS
                && dateValidite != null
                && dateValidite.isBefore(LocalDate.now())
                && statut != StatutDocument.CONVERTI;
    }
}
