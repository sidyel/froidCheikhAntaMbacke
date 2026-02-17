package com.froidcheikh.ecommerce.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import jakarta.validation.constraints.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MarqueDTO {

    private Long idMarque;

    @NotBlank(message = "Le nom de la marque est obligatoire")
    @Size(min = 2, max = 100)
    private String nomMarque;

    private String logo;
    private String description;

    // Statistiques
    private Long nombreProduits;
}