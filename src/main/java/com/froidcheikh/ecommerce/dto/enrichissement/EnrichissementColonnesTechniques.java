package com.froidcheikh.ecommerce.dto.enrichissement;

import lombok.Data;

@Data
public class EnrichissementColonnesTechniques {
    private EnrichissementValeur labelEnergie;
    private EnrichissementValeur consommationWatt;
    private EnrichissementValeur dimensions;
    private EnrichissementValeur poids;
    private EnrichissementValeur garantie;
    /** Stocké comme AttributProduit "Puissance BTU" (non colonne Produit). */
    private EnrichissementValeur puissanceBTU;
}
