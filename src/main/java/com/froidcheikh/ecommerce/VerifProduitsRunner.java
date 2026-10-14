package com.froidcheikh.ecommerce;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Lit directement la base (JdbcTemplate) pour les produits ciblés et affiche
 * puissanceBTU, modeleFabricant, garantie et la liste des attributs.
 *
 * Exécution :
 *   mvn spring-boot:run -Dspring-boot.run.profiles=verif-produits
 *       -Dspring-boot.run.jvmArguments="-Dverif.ids=3,8,19,105,29"
 */
@Component
@Profile("verif-produits")
@RequiredArgsConstructor
@Slf4j
public class VerifProduitsRunner implements CommandLineRunner {

    private final JdbcTemplate jdbc;

    @Override
    public void run(String... args) {
        String idsParam = System.getProperty("verif.ids", "3,8,19,105,29");
        String[] parts = idsParam.split(",");
        String placeholders = String.join(",", java.util.Collections.nCopies(parts.length, "?"));
        Object[] ids = java.util.Arrays.stream(parts)
                .map(String::trim).map(Long::parseLong).toArray();

        log.info("══════════════════════════════════════════════════════");
        log.info("  VÉRIFICATION BASE — produits : {}", idsParam);
        log.info("══════════════════════════════════════════════════════");

        // Colonnes techniques
        String sqlProduits = "SELECT id_produit, nom_produit, modele_fabricant, puissance_btu, " +
                "consommation_watt, label_energie, dimensions, poids, garantie, date_modification " +
                "FROM produits WHERE id_produit IN (" + placeholders + ") ORDER BY id_produit";

        List<String> lignes = jdbc.query(sqlProduits, ids, (rs, rowNum) ->
                String.format("  ID %d | %-48s | modele=%-14s | puissanceBTU=%-6s | " +
                                "consomm=%-6s | label=%-5s | dim=%-20s | poids=%-5s | garantie=%-10s | modif=%s",
                        rs.getLong("id_produit"),
                        rs.getString("nom_produit"),
                        rs.getString("modele_fabricant"),
                        rs.getString("puissance_btu"),
                        rs.getString("consommation_watt"),
                        rs.getString("label_energie"),
                        rs.getString("dimensions"),
                        rs.getString("poids"),
                        rs.getString("garantie"),
                        rs.getString("date_modification"))
        );
        lignes.forEach(log::info);

        // Attributs
        String sqlAttributs = "SELECT ap.produit_id, ap.nom_attribut, ap.valeur_attribut " +
                "FROM attributs_produit ap WHERE ap.produit_id IN (" + placeholders + ") " +
                "ORDER BY ap.produit_id, ap.nom_attribut";

        log.info("─── Attributs ───────────────────────────────────────");
        List<String> attrs = jdbc.query(sqlAttributs, ids, (rs, rowNum) ->
                String.format("  Produit %d | %s = %s",
                        rs.getLong("produit_id"),
                        rs.getString("nom_attribut"),
                        rs.getString("valeur_attribut"))
        );
        if (attrs.isEmpty()) {
            log.info("  (aucun attribut pour ces produits)");
        } else {
            attrs.forEach(log::info);
        }

        log.info("══════════════════════════════════════════════════════");
    }
}
