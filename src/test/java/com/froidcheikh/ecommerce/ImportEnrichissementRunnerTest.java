package com.froidcheikh.ecommerce;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.froidcheikh.ecommerce.entity.Produit;
import com.froidcheikh.ecommerce.repository.AttributProduitRepository;
import com.froidcheikh.ecommerce.repository.ModeleAttributRepository;
import com.froidcheikh.ecommerce.repository.ProduitRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour les gardes du runner d'import.
 * Aucune connexion à la base de données requise.
 */
@ExtendWith(MockitoExtension.class)
class ImportEnrichissementRunnerTest {

    @Mock ProduitRepository produitRepository;
    @Mock AttributProduitRepository attributProduitRepository;
    @Mock ModeleAttributRepository modeleAttributRepository;
    @Mock ObjectMapper objectMapper;

    @InjectMocks ImportEnrichissementRunner runner;

    // ── normaliserNom ────────────────────────────────────────────────────────

    @Test
    void normaliserNom_supprimeEspacesEtAccents() {
        assertEquals("garantie",       ImportEnrichissementRunner.normaliserNom("Garantie"));
        assertEquals("labelenergie",   ImportEnrichissementRunner.normaliserNom("Label énergie"));
        assertEquals("puissancebtu",   ImportEnrichissementRunner.normaliserNom("Puissance BTU"));
        assertEquals("consommationw",  ImportEnrichissementRunner.normaliserNom("Consommation (W)"));
        assertEquals("dimensions",     ImportEnrichissementRunner.normaliserNom("Dimensions"));
        assertEquals("poidsnet",       ImportEnrichissementRunner.normaliserNom("Poids net"));
        assertEquals("gazrefrigerant", ImportEnrichissementRunner.normaliserNom("Gaz réfrigérant"));
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

    // ── Simulation : aucun save() ─────────────────────────────────────────────

    /**
     * En mode simulation (confirmer=false), le runner ne doit appeler aucun save()
     * sur ProduitRepository ni AttributProduitRepository, même si des valeurs valides
     * sont présentes dans le fichier JSON.
     *
     * Note : @Transactional n'est pas actif dans ce contexte Mockito (pas de Spring AOP),
     * donc setRollbackOnly() n'est pas testable ici. Ce test vérifie la couche logique :
     * les gardes if(confirmer) présents dans chaque branche d'écriture.
     */
    @Test
    void simulation_neDoitPasAppelerSave(@TempDir Path tempDir) throws Exception {
        // JSON minimal avec un produit valide et des champs "valide"
        String json = """
                [{
                  "idProduit": 42,
                  "modeleFabricant": {"valeur":"REF-42","source":"test","url":"http://test","confiance":"valide"},
                  "ean": null,
                  "description": null,
                  "ecraser": false,
                  "attributs": [
                    {"nom":"Gaz réfrigérant","valeur":"R410A","source":"test","url":"http://test","confiance":"valide"}
                  ],
                  "colonnesTechniques": {
                    "puissanceBTU": {"valeur":"9000","source":"test","url":"http://test","confiance":"valide"},
                    "garantie":     {"valeur":"12 mois","source":"test","url":"http://test","confiance":"valide"}
                  },
                  "prixConcurrents": [],
                  "remarques": null
                }]
                """;

        Path fichier = tempDir.resolve("lot_test.json");
        Files.writeString(fichier, json);

        Produit p = new Produit();
        p.setNomProduit("Produit de test 42");

        when(produitRepository.findById(42L)).thenReturn(Optional.of(p));
        // findByProduitIdProduit est appelé même en simulation (lecture des attributs existants)
        when(attributProduitRepository.findByProduitIdProduit(any())).thenReturn(List.of());
        // modeleAttributRepository.findByNomAttributIgnoreCase — PAS de stub :
        // il est dans if(confirmer), jamais appelé en simulation.

        // ObjectMapper réel (pas de mock) pour désérialiser le JSON
        ImportEnrichissementRunner runnerReel = new ImportEnrichissementRunner(
                produitRepository, attributProduitRepository, modeleAttributRepository,
                new ObjectMapper());
        ReflectionTestUtils.setField(runnerReel, "fichier", fichier.toString());
        ReflectionTestUtils.setField(runnerReel, "confirmer", false); // SIMULATION

        try {
            runnerReel.run();
        } catch (RuntimeException e) {
            // setRollbackOnly() lance NoTransactionException hors contexte Spring AOP — attendu en test unitaire
        }

        // Vérification principale : aucun save() appelé
        verify(produitRepository, never()).save(any());
        verify(attributProduitRepository, never()).save(any());
    }
}
