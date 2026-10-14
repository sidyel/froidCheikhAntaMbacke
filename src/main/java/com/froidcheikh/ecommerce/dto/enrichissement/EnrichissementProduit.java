package com.froidcheikh.ecommerce.dto.enrichissement;

import lombok.Data;

import java.util.List;

@Data
public class EnrichissementProduit {
    private Long idProduit;
    /** Utiliser {"valeur":"...","source":"...","url":"...","confiance":"valide|a_verifier"} */
    private EnrichissementValeur modeleFabricant;
    private EnrichissementValeur ean;
    private String description;
    /** true = écraser les champs déjà remplis (défaut false). */
    private boolean ecraser;
    private List<EnrichissementAttribut> attributs;
    private EnrichissementColonnesTechniques colonnesTechniques;
    private List<EnrichissementPrixConcurrent> prixConcurrents;
    private String remarques;
}
