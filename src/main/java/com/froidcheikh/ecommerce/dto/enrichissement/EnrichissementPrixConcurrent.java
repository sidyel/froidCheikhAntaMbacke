package com.froidcheikh.ecommerce.dto.enrichissement;

import lombok.Data;

/** Informatif uniquement — jamais importé en base. */
@Data
public class EnrichissementPrixConcurrent {
    private String site;
    private String prix;
    private String date;
    private String url;
}
