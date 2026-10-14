package com.froidcheikh.ecommerce.dto.enrichissement;

import lombok.Data;

/** Valeur typée d'une colonne technique avec sa traçabilité. */
@Data
public class EnrichissementValeur {
    private String valeur;
    private String source;
    private String url;
    /** "valide" ou "a_verifier" */
    private String confiance;
}
