package com.froidcheikh.ecommerce.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

public final class ZoneLivraisonDTO {
    private ZoneLivraisonDTO() {}

    /** Projection publique retournée par GET /api/livraison/zones. */
    @Data
    @NoArgsConstructor
    public static class PublicDTO {
        private Long id;
        private String villeLabel;
        private String quartierLabel;
        private BigDecimal prixStandard;
        /** Null quand express non disponible pour cette zone. */
        private BigDecimal prixExpress;
        private Boolean expressDisponible;
        private String delaiEstime;
        private Boolean eligibleLivraisonGratuite;
        private Integer ordre;
    }

    /** Vue admin complète (tous les champs). */
    @Data
    @NoArgsConstructor
    public static class AdminDTO {
        private Long id;
        private String ville;
        private String villeLabel;
        private String quartier;
        private String quartierLabel;
        private BigDecimal prixStandard;
        private BigDecimal prixExpress;
        private Boolean expressDisponible;
        private String delaiEstime;
        private Boolean actif;
        private Boolean eligibleLivraisonGratuite;
        private Integer ordre;
    }

    /** Corps de création / modification (ADMIN). */
    @Data
    @NoArgsConstructor
    public static class CreateDTO {
        @NotBlank
        private String ville;
        @NotBlank
        private String villeLabel;
        @NotBlank
        private String quartier;
        @NotBlank
        private String quartierLabel;
        @NotNull
        @DecimalMin("0.0")
        private BigDecimal prixStandard;
        @DecimalMin("0.0")
        private BigDecimal prixExpress;
        private Boolean expressDisponible = Boolean.TRUE;
        private String delaiEstime;
        private Boolean actif = Boolean.TRUE;
        private Boolean eligibleLivraisonGratuite = Boolean.FALSE;
        private Integer ordre = 0;
    }
}
