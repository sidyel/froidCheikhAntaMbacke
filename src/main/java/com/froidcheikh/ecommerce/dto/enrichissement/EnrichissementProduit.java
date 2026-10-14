package com.froidcheikh.ecommerce.dto.enrichissement;

import lombok.Data;

import java.util.List;

@Data
public class EnrichissementProduit {
    private Long idProduit;
    private String modeleFabricant;
    private String ean;
    private String description;
    /** true = écraser les champs déjà remplis (défaut false). */
    private boolean ecraser;
    private List<EnrichissementAttribut> attributs;
    private EnrichissementColonnesTechniques colonnesTechniques;
    private List<EnrichissementPrixConcurrent> prixConcurrents;
    private String remarques;
}
