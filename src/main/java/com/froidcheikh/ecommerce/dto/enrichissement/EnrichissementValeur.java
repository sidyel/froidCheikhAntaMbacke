package com.froidcheikh.ecommerce.dto.enrichissement;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Valeur typée d'une colonne technique avec sa traçabilité. */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class EnrichissementValeur {
    private String valeur;
    private String source;
    private String url;
    /** "valide" ou "a_verifier" */
    private String confiance;
    /**
     * true + ecraser:true au niveau produit → vide la colonne (la met à null).
     * Utilisé pour corriger des valeurs incorrectes sans fournir de nouvelle valeur.
     */
    private Boolean effacer;
}
