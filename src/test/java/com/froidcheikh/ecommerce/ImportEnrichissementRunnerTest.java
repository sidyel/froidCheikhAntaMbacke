package com.froidcheikh.ecommerce;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires pour les gardes du runner d'import.
 * Aucune connexion à la base de données requise.
 */
class ImportEnrichissementRunnerTest {

    // ── normaliserNom ────────────────────────────────────────────────────────

    @Test
    void normaliserNom_supprimeEspacesEtAccents() {
        assertEquals("garantie",        ImportEnrichissementRunner.normaliserNom("Garantie"));
        assertEquals("labelenergie",    ImportEnrichissementRunner.normaliserNom("Label énergie"));
        assertEquals("puissancebtu",    ImportEnrichissementRunner.normaliserNom("Puissance BTU"));
        assertEquals("consommationw",  ImportEnrichissementRunner.normaliserNom("Consommation (W)"));
        assertEquals("dimensions",      ImportEnrichissementRunner.normaliserNom("Dimensions"));
        assertEquals("poidsnet",        ImportEnrichissementRunner.normaliserNom("Poids net"));
        assertEquals("gazrefrigerant",  ImportEnrichissementRunner.normaliserNom("Gaz réfrigérant"));
    }

    @Test
    void normaliserNom_nullRetourneChainVide() {
        assertEquals("", ImportEnrichissementRunner.normaliserNom(null));
    }

    // ── estColonneProduit ─────────────────────────────────────────────────────

    @Test
    void estColonneProduit_reconnaîtLesColonnes() {
        assertTrue(ImportEnrichissementRunner.estColonneProduit("Garantie"),
                "\"Garantie\" doit être reconnue comme colonne Produit");
        assertTrue(ImportEnrichissementRunner.estColonneProduit("Label énergie"),
                "\"Label énergie\" doit être reconnue comme colonne Produit");
        assertTrue(ImportEnrichissementRunner.estColonneProduit("Puissance BTU"),
                "\"Puissance BTU\" est une colonne Produit (Integer), pas un AttributProduit");
        assertTrue(ImportEnrichissementRunner.estColonneProduit("Consommation (W)"),
                "\"Consommation (W)\" doit matcher consommationWatt");
        assertTrue(ImportEnrichissementRunner.estColonneProduit("Dimensions"));
        assertTrue(ImportEnrichissementRunner.estColonneProduit("Poids"));
        assertTrue(ImportEnrichissementRunner.estColonneProduit("Poids net"));
        assertTrue(ImportEnrichissementRunner.estColonneProduit("BTU"));
        assertTrue(ImportEnrichissementRunner.estColonneProduit("Classe énergétique"));
        assertTrue(ImportEnrichissementRunner.estColonneProduit("Étiquette énergie"));
    }

    @Test
    void estColonneProduit_laissePasserLesVraiesAttributs() {
        assertFalse(ImportEnrichissementRunner.estColonneProduit("Gaz réfrigérant"),
                "\"Gaz réfrigérant\" n'est pas une colonne Produit");
        assertFalse(ImportEnrichissementRunner.estColonneProduit("Classe climatique"),
                "\"Classe climatique\" n'est pas une colonne Produit");
        assertFalse(ImportEnrichissementRunner.estColonneProduit("Niveau sonore"),
                "\"Niveau sonore\" n'est pas une colonne Produit");
        assertFalse(ImportEnrichissementRunner.estColonneProduit("Consommation annuelle"),
                "\"Consommation annuelle\" ≠ watts, ne doit pas matcher consommationWatt");
        assertFalse(ImportEnrichissementRunner.estColonneProduit("Consommation journalière"),
                "\"Consommation journalière\" ≠ watts");
        assertFalse(ImportEnrichissementRunner.estColonneProduit("Tension"));
        assertFalse(ImportEnrichissementRunner.estColonneProduit("Fermeture à clé"));
        assertFalse(ImportEnrichissementRunner.estColonneProduit("Capacité de congélation"));
        assertFalse(ImportEnrichissementRunner.estColonneProduit("Puissance moteur"),
                "\"Puissance moteur\" (CV) est un vrai attribut, pas la colonne puissanceBTU");
    }
}
