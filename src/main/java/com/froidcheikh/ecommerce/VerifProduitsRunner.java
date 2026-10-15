package com.froidcheikh.ecommerce;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * Outil de vérification de la base — profil Spring dédié (jamais au démarrage normal).
 *
 * Mode par défaut (verif.mode=ids) :
 *   mvn spring-boot:run -Dspring-boot.run.profiles=verif-produits \
 *       -Dspring-boot.run.jvmArguments="-Dverif.ids=3,8,19,105,29"
 *
 * Mode recherche dimensions suspectes (capacités dans la colonne dimensions) :
 *   mvn spring-boot:run -Dspring-boot.run.profiles=verif-produits \
 *       -Dspring-boot.run.jvmArguments="-Dverif.mode=search-dimensions"
 */
@Component
@Profile("verif-produits")
@RequiredArgsConstructor
@Slf4j
public class VerifProduitsRunner implements CommandLineRunner {

    private final JdbcTemplate jdbc;

    @Value("${verif.mode:ids}")
    private String mode;

    @Override
    public void run(String... args) {
        if ("search-dimensions".equals(mode)) {
            searchDimensions();
        } else if ("check-attribut".equals(mode)) {
            String idsParam = System.getProperty("verif.ids", "");
            String nomAttribut = System.getProperty("verif.attribut", "Capacité totale");
            checkAttribut(idsParam, nomAttribut);
        } else {
            String idsParam = System.getProperty("verif.ids", "3,8,19,105,29");
            verifierParIds(idsParam);
        }
    }

    // ── Mode : vérification par IDs ────────────────────────────────────────────

    private void verifierParIds(String idsParam) {
        String[] parts = idsParam.split(",");
        String placeholders = String.join(",", Collections.nCopies(parts.length, "?"));
        Object[] ids = java.util.Arrays.stream(parts)
                .map(String::trim).map(Long::parseLong).toArray();

        log.info("══════════════════════════════════════════════════════");
        log.info("  VÉRIFICATION BASE — produits : {}", idsParam);
        log.info("══════════════════════════════════════════════════════");

        String sqlProduits = "SELECT id_produit, nom_produit, modele_fabricant, puissance_btu, " +
                "consommation_watt, label_energie, dimensions, poids, garantie, date_modification " +
                "FROM produits WHERE id_produit IN (" + placeholders + ") ORDER BY id_produit";

        List<String> lignes = jdbc.query(sqlProduits, ids, (rs, rowNum) ->
                String.format("  ID %-5d | %-48s | modele=%-14s | btu=%-6s | " +
                                "consomm=%-6s | label=%-20s | dim=%-25s | poids=%-5s | garantie=%-10s | modif=%s",
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

        String sqlAttributs = "SELECT ap.produit_id, ap.nom_attribut, ap.valeur_attribut " +
                "FROM attributs_produit ap WHERE ap.produit_id IN (" + placeholders + ") " +
                "ORDER BY ap.produit_id, ap.nom_attribut";

        log.info("─── Attributs ───────────────────────────────────────");
        List<String> attrs = jdbc.query(sqlAttributs, ids, (rs, rowNum) ->
                String.format("  Produit %-5d | %s = %s",
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

    // ── Mode : vérification d'un attribut sur une liste de produits ───────────

    private void checkAttribut(String idsParam, String nomAttribut) {
        log.info("══════════════════════════════════════════════════════");
        log.info("  CHECK ATTRIBUT '{}' — produits : {}",
                nomAttribut, idsParam.isBlank() ? "19,20,21,22,23,24,26,27,28,29,30,31,32,101,103,104,105" : idsParam);
        log.info("══════════════════════════════════════════════════════");

        String ids = idsParam.isBlank()
                ? "19,20,21,22,23,24,26,27,28,29,30,31,32,101,103,104,105"
                : idsParam;

        // Produits ciblés qui ont déjà l'attribut demandé
        String sqlExistants = "SELECT p.id_produit, p.nom_produit, ap.nom_attribut, ap.valeur_attribut " +
                "FROM produits p " +
                "JOIN attributs_produit ap ON ap.produit_id = p.id_produit " +
                "WHERE ap.nom_attribut ILIKE ? " +
                "AND p.id_produit IN (" + ids + ") " +
                "ORDER BY p.id_produit";

        List<String> existants = jdbc.query(sqlExistants, new Object[]{nomAttribut}, (rs, rowNum) ->
                String.format("  ID %-5d | %-48s | %s = %s",
                        rs.getLong("id_produit"),
                        rs.getString("nom_produit"),
                        rs.getString("nom_attribut"),
                        rs.getString("valeur_attribut"))
        );

        if (existants.isEmpty()) {
            log.info("  ✓ Aucun des produits ciblés n'a déjà l'attribut '{}' — pas de doublon.", nomAttribut);
        } else {
            log.warn("  ⚠  {} produit(s) ont DÉJÀ l'attribut '{}' :", existants.size(), nomAttribut);
            existants.forEach(log::warn);
        }

        // Vérification modeleFabricant des produits 3 et 8 (lot 1)
        log.info("─── Lot 1 — modeleFabricant produits 3 et 8 ────────");
        String sqlLot1 = "SELECT id_produit, nom_produit, modele_fabricant, date_modification " +
                "FROM produits WHERE id_produit IN (3, 8) ORDER BY id_produit";
        List<String> lot1 = jdbc.query(sqlLot1, (rs, rowNum) ->
                String.format("  ID %-5d | %-48s | modele_fabricant = '%s' | modif = %s",
                        rs.getLong("id_produit"),
                        rs.getString("nom_produit"),
                        rs.getString("modele_fabricant"),
                        rs.getString("date_modification"))
        );
        lot1.forEach(log::info);
        log.info("══════════════════════════════════════════════════════");
    }

    // ── Mode : recherche de dimensions suspectes (capacités) ──────────────────

    private void searchDimensions() {
        log.info("══════════════════════════════════════════════════════");
        log.info("  RECHERCHE — dimensions suspectes (capacités) dans toute la base");
        log.info("══════════════════════════════════════════════════════");

        // Détecte : "195 L", "133 litres", "197 L", "42l", "500ml", etc.
        // Signale aussi tout ce qui n'a pas le pattern "N × N × N" standard.
        String sql = """
                SELECT id_produit, nom_produit, dimensions,
                  CASE
                    WHEN dimensions ~* '^\\d+([,.]\\d+)?\\s*(L|litres?|liters?|cl|ml)\\s*$'
                      THEN 'CAPACITE'
                    WHEN dimensions ILIKE '%litre%' OR dimensions ILIKE '%liter%'
                      OR dimensions ILIKE '% L'
                      THEN 'CAPACITE_PROBABLE'
                    ELSE 'A_VERIFIER'
                  END AS statut
                FROM produits
                WHERE dimensions IS NOT NULL
                  AND dimensions <> ''
                ORDER BY statut, id_produit
                """;

        List<String> lignes = jdbc.query(sql, (rs, rowNum) ->
                String.format("  [%-20s] ID %-5d | %-48s | dimensions = \"%s\"",
                        rs.getString("statut"),
                        rs.getLong("id_produit"),
                        rs.getString("nom_produit"),
                        rs.getString("dimensions"))
        );

        if (lignes.isEmpty()) {
            log.info("  Aucun produit avec dimensions non nulle trouvé.");
        } else {
            log.info("  {} produit(s) trouvé(s) avec colonne dimensions remplie :", lignes.size());
            lignes.forEach(log::info);
        }
        log.info("══════════════════════════════════════════════════════");
    }
}
