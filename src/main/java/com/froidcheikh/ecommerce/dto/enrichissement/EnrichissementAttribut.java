package com.froidcheikh.ecommerce.dto.enrichissement;

import lombok.Data;

@Data
public class EnrichissementAttribut {
    private String nom;
    private String valeur;
    private String source;
    private String url;
    /** "valide" ou "a_verifier" */
    private String confiance;
}
