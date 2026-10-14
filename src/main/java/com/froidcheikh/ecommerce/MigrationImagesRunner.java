package com.froidcheikh.ecommerce;

import com.froidcheikh.ecommerce.entity.ImageProduit;
import com.froidcheikh.ecommerce.entity.Produit;
import com.froidcheikh.ecommerce.repository.ImageProduitRepository;
import com.froidcheikh.ecommerce.repository.ProduitRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Migre les URLs de produit_images vers image_produit.
 * Idempotent : saute les produits qui ont déjà des ImageProduit.
 * NE SUPPRIME PAS l'ancienne table produit_images.
 * Exécution : mvn spring-boot:run -Dspring-boot.run.profiles=migration
 */
@Component
@Profile("migration")
@RequiredArgsConstructor
@Slf4j
public class MigrationImagesRunner implements CommandLineRunner {

    private static final Pattern PUBLIC_ID_PATTERN =
            Pattern.compile("upload/(?:v\\d+/)?(.+?)(?:\\.[a-zA-Z0-9]+)?$");

    private final ProduitRepository produitRepository;
    private final ImageProduitRepository imageProduitRepository;

    @Override
    public void run(String... args) {
        log.info("══════════════════════════════════════════");
        log.info("  Migration produit_images → image_produit");
        log.info("══════════════════════════════════════════");

        List<Produit> produits = produitRepository.findAll();
        int totalMigre = 0;
        int totalSaute = 0;
        int totalErreurPublicId = 0;

        for (Produit produit : produits) {
            long existantes = imageProduitRepository.countByProduitId(produit.getIdProduit());
            if (existantes > 0) {
                log.info("⏭  Produit {} ({}) — {} image(s) déjà migrée(s), ignoré",
                        produit.getIdProduit(), produit.getNomProduit(), existantes);
                totalSaute++;
                continue;
            }

            List<String> urls = produit.getListeImages();
            if (urls == null || urls.isEmpty()) continue;

            for (int i = 0; i < urls.size(); i++) {
                String url = urls.get(i);
                String publicId = extractPublicId(url);
                if (publicId == null) {
                    log.warn("⚠  publicId non extrait pour l'URL : {}", url);
                    totalErreurPublicId++;
                }
                ImageProduit img = new ImageProduit();
                img.setUrl(url);
                img.setPublicId(publicId);
                img.setOrdre(i);
                img.setPrincipale(i == 0);
                img.setProduit(produit);
                imageProduitRepository.save(img);
                totalMigre++;
            }
            log.info("✅ Produit {} ({}) — {} image(s) migrée(s)",
                    produit.getIdProduit(), produit.getNomProduit(), urls.size());
        }

        log.info("══════════════════════════════════════════");
        log.info("  Résultat : {} migrées, {} produits ignorés (déjà migrés), {} URLs sans publicId",
                totalMigre, totalSaute, totalErreurPublicId);
        log.info("  La table produit_images est conservée — supprimez-la après vérification.");
        log.info("══════════════════════════════════════════");
    }

    private String extractPublicId(String url) {
        if (url == null) return null;
        Matcher m = PUBLIC_ID_PATTERN.matcher(url);
        return m.find() ? m.group(1) : null;
    }
}
