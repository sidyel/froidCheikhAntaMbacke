package com.froidcheikh.ecommerce.dto;

import com.froidcheikh.ecommerce.entity.Promotion;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class PromotionDTO {
    private PromotionDTO() {}

    @Data
    @NoArgsConstructor
    public static class Request {
        @NotBlank
        @Size(max = 150)
        private String nom;

        @NotNull
        private Promotion.TypePromotion typePromotion;

        @NotNull
        @DecimalMin("0.01")
        private BigDecimal valeur;

        @NotNull
        private LocalDateTime dateDebut;

        private LocalDateTime dateFin;

        private Boolean actif = true;

        private Integer priorite = 0;

        @NotNull
        private Promotion.CiblePromotion cible;

        private List<Long> produitIds;
        private List<Long> categorieIds;
        private List<Long> marqueIds;
    }

    @Data
    @NoArgsConstructor
    public static class Response {
        private Long idPromotion;
        private String nom;
        private Promotion.TypePromotion typePromotion;
        private BigDecimal valeur;
        private LocalDateTime dateDebut;
        private LocalDateTime dateFin;
        private Boolean actif;
        private Integer priorite;
        private Promotion.CiblePromotion cible;
        private List<Long> produitIds;
        private List<Long> categorieIds;
        private List<Long> marqueIds;
        private LocalDateTime dateCreation;
        private LocalDateTime dateModification;
        private String statut; // "ACTIVE", "PROGRAMMEE", "EXPIREE", "INACTIVE"
    }

    @Data
    @NoArgsConstructor
    public static class ApercuProduit {
        private Long idProduit;
        private String nomProduit;
        private BigDecimal prixOrigine;
        private BigDecimal prixApresPromo;
        private Integer pourcentageReduction;
        private boolean prixPlancher; // true si la promo ferait tomber le prix à 0 ou moins
    }

    @Data
    @NoArgsConstructor
    public static class ApercuPage {
        private List<ApercuProduit> produits;
        private long totalElements;
        private int totalPages;
        private int page;
        private int size;
    }
}
