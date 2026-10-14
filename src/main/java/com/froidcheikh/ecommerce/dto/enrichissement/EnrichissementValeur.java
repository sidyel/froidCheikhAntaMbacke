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
}
