package com.froidcheikh.ecommerce.dto;

import com.froidcheikh.ecommerce.entity.Produit;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProduitDTO {

    private Long idProduit;

    @NotBlank(message = "Le nom du produit est obligatoire")
    @Size(min = 2, max = 255)
    private String nomProduit;

    private String descriptionProduit;

    @NotNull(message = "Le prix est obligatoire")
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal prix;

    @Min(0)
    private Integer stockDisponible;

    private String refProduit;
    private String codeProduit;
    private String garantie;
    private Produit.LabelEnergie labelEnergie;
    private Integer puissanceBTU;
    private Integer consommationWatt;
    private String dimensions;
    private Double poids;
    private String ficheTechniquePDF;
    private List<String> listeImages;
    private List<String> videosOptionnelles;
    private Boolean disponibilite;
    private LocalDateTime dateAjout;

    // Relations
    private CategorieDTO categorie;
    private MarqueDTO marque;
    private List<AttributProduitDTO> attributs;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategorieDTO {
        private Long idCategorie;
        private String nomCategorie;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MarqueDTO {
        private Long idMarque;
        private String nomMarque;
        private String logo;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AttributProduitDTO {
        private Long idAttribut;
        private String nomAttribut;
        private String valeurAttribut;
    }
}