package com.froidcheikh.ecommerce.dto;

import com.froidcheikh.ecommerce.entity.TypeAttribut;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ModeleAttributDTO {

    private Long idModele;

    @NotBlank(message = "Le nom de l'attribut modèle est obligatoire")
    private String nomAttribut;

    private String description;

    @NotNull(message = "Le type de valeur est obligatoire")
    private TypeAttribut typeValeur;

    private String valeursAutorisees;

    private String unite;

    private boolean obligatoire;

    private int ordre;

    private Set<Long> categorieIds;
}
