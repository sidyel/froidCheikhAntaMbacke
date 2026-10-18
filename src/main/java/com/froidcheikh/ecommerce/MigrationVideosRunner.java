package com.froidcheikh.ecommerce;

import com.froidcheikh.ecommerce.entity.Produit;
import com.froidcheikh.ecommerce.entity.TypeVideo;
import com.froidcheikh.ecommerce.entity.VideoProduit;
import com.froidcheikh.ecommerce.repository.ProduitRepository;
import com.froidcheikh.ecommerce.repository.VideoProduitRepository;
import com.froidcheikh.ecommerce.service.VideoExtractionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Migre les URLs de produit_videos (List<String>) vers video_produit (VideoProduit).
 * Idempotent : saute les produits qui ont déjà des VideoProduit en base.
 * NE SUPPRIME PAS l'ancienne table produit_videos.
 *
 * Exécution :
 *   mvn spring-boot:run -Dspring-boot.run.profiles=migration-videos
 */
@Component
@Profile("migration-videos")
@RequiredArgsConstructor
@Slf4j
public class MigrationVideosRunner implements CommandLineRunner {

    private final ProduitRepository produitRepository;
    private final VideoProduitRepository videoProduitRepository;
    private final VideoExtractionService extractionService;
    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("=== Début migration produit_videos → video_produit ===");

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT produit_id, video_url FROM produit_videos ORDER BY produit_id");

        if (rows.isEmpty()) {
            log.info("Aucune ligne dans produit_videos — migration non nécessaire.");
            return;
        }

        int migrees = 0;
        int ignorees = 0;

        for (Map<String, Object> row : rows) {
            Long produitId = ((Number) row.get("produit_id")).longValue();
            String videoUrl = (String) row.get("video_url");

            if (videoUrl == null || videoUrl.isBlank()) {
                ignorees++;
                continue;
            }

            // Idempotence : si ce produit a déjà des VideoProduit, on ne migre pas
            long existantes = videoProduitRepository.countByProduitIdProduit(produitId);
            if (existantes > 0) {
                log.debug("Produit {} : {} video(s) déjà présente(s), ligne ignorée.", produitId, existantes);
                ignorees++;
                continue;
            }

            Produit produit = produitRepository.findById(produitId).orElse(null);
            if (produit == null) {
                log.warn("Produit {} introuvable — ligne ignorée.", produitId);
                ignorees++;
                continue;
            }

            VideoProduit video = new VideoProduit();
            video.setProduit(produit);
            video.setUrl(videoUrl);
            video.setOrdre(0);
            video.setActive(true);

            VideoExtractionService.ExtractionResult extraction = extractionService.extraire(videoUrl);
            if (extraction != null) {
                video.setType(extraction.type());
                video.setVideoId(extraction.videoId());
                video.setUrlApercu(extraction.urlApercu());
            } else if (videoUrl.contains("res.cloudinary.com")) {
                video.setType(TypeVideo.CLOUDINARY);
                // publicId non disponible depuis l'URL seule pour les anciennes entrées
                video.setUrlApercu(extractionService.buildCloudinaryApercu(videoUrl));
            } else {
                // Type inconnu — on stocke l'URL brute en CLOUDINARY par défaut
                video.setType(TypeVideo.CLOUDINARY);
                log.warn("Type non reconnu pour l'URL : {} (produit {})", videoUrl, produitId);
            }

            videoProduitRepository.save(video);
            migrees++;
            log.info("Produit {} — vidéo migrée ({}) : {}", produitId, video.getType(), videoUrl);
        }

        log.info("=== Migration terminée : {} migrée(s), {} ignorée(s) ===", migrees, ignorees);
    }
}
