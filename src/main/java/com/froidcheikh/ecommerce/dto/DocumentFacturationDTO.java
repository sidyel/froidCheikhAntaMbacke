package com.froidcheikh.ecommerce.dto;

import com.froidcheikh.ecommerce.entity.CompteurDocument.TypeDocument;
import com.froidcheikh.ecommerce.entity.DocumentFacturation.StatutDocument;
import com.froidcheikh.ecommerce.entity.ReglementDocument.ModeReglement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class DocumentFacturationDTO {
    private DocumentFacturationDTO() {}

    // ---- REQUÊTES ----

    @Data @NoArgsConstructor
    public static class LigneRequest {
        @NotBlank
        private String designation;
        private Long produitId;
        @NotNull @DecimalMin("0.001")
        private BigDecimal quantite;
        @NotNull @DecimalMin("0")
        private BigDecimal prixUnitaireHT;
        @DecimalMin("0") @DecimalMax("100")
        private BigDecimal tauxTva;       // null = hérite du document
        @DecimalMin("0") @DecimalMax("100")
        private BigDecimal remisePourcent = BigDecimal.ZERO;
        private Integer ordre = 0;
    }

    @Data @NoArgsConstructor
    public static class CreateRequest {
        @NotNull
        private TypeDocument typeDocument;
        private Long commandeId;          // facture depuis commande
        private Long clientSiteId;        // client du site
        private Long clientFacturationId; // client hors site existant
        private ClientFacturationDTO.CreateRequest nouveauClient; // créer hors site à la volée
        @DecimalMin("0") @DecimalMax("100")
        private BigDecimal remiseTotalePourcent = BigDecimal.ZERO;
        private LocalDate dateValidite;   // DEVIS uniquement
        private String livreurNom;
        private String livreurTelephone;
        @Valid
        private List<LigneRequest> lignes;
    }

    @Data @NoArgsConstructor
    public static class AvoirRequest {
        @NotNull
        private Long factureOriginId;
        @Valid @NotEmpty
        private List<LigneAvoirRequest> lignes;
        private boolean remettreEnStock = false;
    }

    @Data @NoArgsConstructor
    public static class LigneAvoirRequest {
        @NotNull
        private Long ligneOriginId;
        @NotNull @DecimalMin("0.001")
        private BigDecimal quantiteAvoir;
    }

    @Data @NoArgsConstructor
    public static class ReglementRequest {
        @NotNull
        private LocalDate dateReglement;
        @NotNull @DecimalMin("0.01")
        private BigDecimal montant;
        @NotNull
        private ModeReglement mode;
        private String reference;
    }

    // ---- RÉPONSES ----

    @Data @NoArgsConstructor
    public static class LigneResponse {
        private Long id;
        private Integer ordre;
        private String designation;
        private Long produitId;
        private BigDecimal quantite;
        private BigDecimal prixUnitaireHT;
        private BigDecimal tauxTva;
        private BigDecimal remisePourcent;
        private BigDecimal montantHT;
        private BigDecimal montantTva;
        private BigDecimal montantTTC;
    }

    @Data @NoArgsConstructor
    public static class ReglementResponse {
        private Long id;
        private LocalDate dateReglement;
        private BigDecimal montant;
        private ModeReglement mode;
        private String reference;
        private String enregistrePar;
        private LocalDateTime dateSaisie;
        private boolean supprime;
        private String supprimePar;
        private LocalDateTime dateSuppression;
    }

    @Data @NoArgsConstructor
    public static class DocumentResponse {
        private Long id;
        private TypeDocument typeDocument;
        private StatutDocument statut;
        private boolean expire;
        private String numero;
        private LocalDateTime dateCreation;
        private LocalDateTime dateEmission;
        private LocalDate dateValidite;
        private Long commandeId;
        private String numeroCommande;
        private Long documentOrigineId;
        private String numeroDocumentOrigine;
        // Client
        private Long clientSiteId;
        private Long clientFacturationId;
        private String clientNom;
        private String clientPrenom;
        private String clientTelephone;
        private String clientEmail;
        private String clientAdresse;
        private String clientNinea;
        private String clientRccm;
        // Calculs
        private BigDecimal tauxTvaDefaut;
        private Boolean prixAfficheTTC;
        private BigDecimal remiseTotalePourcent;
        private BigDecimal montantHT;
        private BigDecimal montantTva;
        private BigDecimal montantTTC;
        private String montantEnLettres;
        private boolean hasPdf;
        // BL
        private String livreurNom;
        private String livreurTelephone;
        // Statut paiement calculé
        private String statutPaiement; // NON_PAYEE / PARTIELLEMENT_PAYEE / PAYEE
        private BigDecimal montantRegle;
        private BigDecimal montantRestant;
        // Lignes & règlements
        private List<LigneResponse> lignes;
        private List<ReglementResponse> reglements;
        // Traçabilité
        private String creePar;
        private String modifiePar;
        private LocalDateTime dateModification;
    }

    @Data @NoArgsConstructor
    public static class DocumentResume {
        private Long id;
        private TypeDocument typeDocument;
        private StatutDocument statut;
        private boolean expire;
        private String numero;
        private LocalDateTime dateCreation;
        private LocalDateTime dateEmission;
        private String clientNom;
        private BigDecimal montantTTC;
        private String statutPaiement;
    }

    @Data @NoArgsConstructor
    public static class JetonResponse {
        private String jeton;
        private LocalDateTime dateExpiration;
        private String urlTelechargement;
    }
}
