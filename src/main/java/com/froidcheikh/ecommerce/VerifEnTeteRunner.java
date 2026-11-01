package com.froidcheikh.ecommerce;

import com.froidcheikh.ecommerce.service.DocumentFacturationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Path;
import java.util.Map;

/**
 * Profil de diagnostic en-tête PDF. Jamais au démarrage normal.
 *
 * Usage :
 *   mvn spring-boot:run -Dspring-boot.run.profiles=verif-entete \
 *       -Dspring-boot.run.jvmArguments="-Dentete.sortie=C:/chemin/entete.png"
 *
 * Lit les paramètres de facturation, génère un PDF aperçu,
 * convertit la première page en PNG (150 DPI).
 */
@Component
@Profile("verif-entete")
@RequiredArgsConstructor
@Slf4j
public class VerifEnTeteRunner implements CommandLineRunner {

    private final JdbcTemplate jdbc;
    private final DocumentFacturationService documentService;

    @Value("${entete.sortie:#{systemProperties['java.io.tmpdir']}/entete-test.png}")
    private String sortiePng;

    @Override
    public void run(String... args) throws Exception {
        // ── 1. Afficher les paramètres de facturation ────────────────────────
        log.info("=== PARAMETRES FACTURATION (parametres_site) ===");
        Map<String, Object> row = jdbc.queryForMap(
                "SELECT mode_en_tete_documents, " +
                "       couleur_documents, " +
                "       logo_url, " +
                "       url_logo_documents, " +
                "       url_image_en_tete, " +
                "       url_image_pied, " +
                "       texte_descriptif_activites " +
                "FROM parametres_site LIMIT 1");

        log.info("  modeEnTeteDocuments  : {}", row.get("mode_en_tete_documents"));
        log.info("  couleurDocuments     : {}", row.get("couleur_documents"));
        log.info("  urlLogo (logo_url)   : {}", row.get("logo_url"));
        log.info("  urlLogoDocuments     : {}", row.get("url_logo_documents"));
        log.info("  urlImageEnTete       : {}", row.get("url_image_en_tete"));
        log.info("  urlImagePied         : {}", row.get("url_image_pied"));
        log.info("  texteDescriptif      : {}",
                row.get("texte_descriptif_activites") != null ? "(renseigné)" : "(null)");

        // ── 2. Générer le PDF aperçu ─────────────────────────────────────────
        log.info("=== GENERATION PDF APERCU EN-TETE ===");
        byte[] pdf;
        try {
            pdf = documentService.genererApercuEnTete();
            log.info("  PDF généré : {} octets", pdf.length);
        } catch (Exception e) {
            log.error("  Erreur génération PDF : {}", e.getMessage(), e);
            return;
        }

        // ── 3. Sauvegarder le PDF (optionnel, pour débogage) ─────────────────
        Path pdfPath = Path.of(sortiePng).getParent().resolve("entete-test.pdf");
        try (FileOutputStream fos = new FileOutputStream(pdfPath.toFile())) {
            fos.write(pdf);
            log.info("  PDF sauvegardé : {}", pdfPath);
        } catch (Exception e) {
            log.warn("  Impossible de sauvegarder le PDF : {}", e.getMessage());
        }

        // ── 4. Convertir la première page en PNG (PDFBox) ───────────────────
        log.info("=== CONVERSION PAGE 1 → PNG ===");
        try (PDDocument pdDoc = Loader.loadPDF(pdf)) {
            PDFRenderer renderer = new PDFRenderer(pdDoc);
            BufferedImage image = renderer.renderImageWithDPI(0, 150);
            File pngFile = new File(sortiePng);
            pngFile.getParentFile().mkdirs();
            ImageIO.write(image, "PNG", pngFile);
            log.info("  PNG sauvegardé : {}", pngFile.getAbsolutePath());
        } catch (Exception e) {
            log.error("  Erreur conversion PNG : {}", e.getMessage(), e);
        }

        log.info("=== FIN verif-entete ===");
    }
}
