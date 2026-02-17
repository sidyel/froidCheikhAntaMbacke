package com.froidcheikh.ecommerce.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import jakarta.validation.constraints.*;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategorieDTO {

    private Long idCategorie;

    @NotBlank(message = "Le nom de la catégorie est obligatoire")
    @Size(min = 2, max = 100)
    private String nomCategorie;

    private String descriptionCategorie;
    private String imageCategorie;

    // Relation parent-enfant
    private Long parentId;
    private String nomParent;
    private List<CategorieDTO> sousCategories;

    // Statistiques
    private Long nombreProduits;
}