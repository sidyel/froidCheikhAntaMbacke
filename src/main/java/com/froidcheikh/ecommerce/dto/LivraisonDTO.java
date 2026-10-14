package com.froidcheikh.ecommerce.dto;

import com.froidcheikh.ecommerce.entity.Commande;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

public final class LivraisonDTO {
    private LivraisonDTO() {}

    @Data
    @NoArgsConstructor
    public static class EstimationRequest {
        /** Null pour RETRAIT_MAGASIN. */
        private Long zoneId;

        @NotNull(message = "Le mode de livraison est obligatoire")
        private Commande.ModeLivraison modeLivraison;

        @NotEmpty(message = "Les lignes de commande sont requises")
        @Valid
        private List<LigneEstimation> lignes;

        @Data
        @NoArgsConstructor
        public static class LigneEstimation {
            @NotNull
            private Long produitId;
            @NotNull
            @Min(1)
            private Integer quantite;
        }
    }

    @Data
    @NoArgsConstructor
    public static class EstimationResponse {
        private BigDecimal montantArticles;
        private BigDecimal fraisZone;
        private BigDecimal supplementExpress;
        private BigDecimal supplementGabarit;
        private BigDecimal fraisLivraisonTotal;
        private BigDecimal montantTotal;
        private boolean livraisonGratuite;
        private String delaiEstime;
        private String modeLivraisonLabel;
    }

    /** Réponse prévisualisation import CSV. */
    @Data
    @NoArgsConstructor
    public static class CsvPreviewResponse {
        private List<CsvLigne> lignesACreer;
        private List<CsvLigne> lignesAModifier;
        private List<CsvErreur> lignesEnErreur;
        private int totalTraitees;
        private String token;

        @Data
        @NoArgsConstructor
        public static class CsvLigne {
            private int numeroLigne;
            private String ville;
            private String villeLabel;
            private String quartier;
            private String quartierLabel;
            private BigDecimal prixStandard;
            private BigDecimal prixExpress;
            private String delaiEstime;
            private Boolean expressDisponible;
            private Boolean eligibleLivraisonGratuite;
        }

        @Data
        @NoArgsConstructor
        public static class CsvErreur {
            private int numeroLigne;
            private String contenu;
            private String motif;
        }
    }
}
