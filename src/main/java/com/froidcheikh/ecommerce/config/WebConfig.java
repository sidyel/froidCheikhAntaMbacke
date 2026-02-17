package com.froidcheikh.ecommerce.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;
import java.nio.file.Paths;

@Configuration
@Slf4j
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String uploadPath = Paths.get(uploadDir).toAbsolutePath().toString();
        File uploadFolder = new File(uploadPath);

        if (!uploadFolder.exists()) {
            boolean created = uploadFolder.mkdirs();
            log.info("Dossier uploads créé: {}", created);
        }

        String resourceLocation = "file:" + uploadPath + File.separator;

        // Configuration pour servir les fichiers via /uploads/**
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(resourceLocation)
                .setCachePeriod(3600);

        // IMPORTANT: Ajouter aussi la configuration pour /api/files/uploads/**
        registry.addResourceHandler("/api/files/uploads/**")
                .addResourceLocations(resourceLocation)
                .setCachePeriod(3600);

        log.info("=== CONFIGURATION FICHIERS STATIQUES ===");
        log.info("URL Pattern 1: /uploads/**");
        log.info("URL Pattern 2: /api/files/uploads/**");
        log.info("Upload Directory: {}", uploadPath);
        log.info("Resource Location: {}", resourceLocation);
        log.info("Directory exists: {}", uploadFolder.exists());
        log.info("=========================================");
    }
  /*  public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Obtenir le chemin absolu du dossier uploads
        String uploadPath = Paths.get(uploadDir).toAbsolutePath().toString();
        File uploadFolder = new File(uploadPath);

        // Créer le dossier s'il n'existe pas
        if (!uploadFolder.exists()) {
            boolean created = uploadFolder.mkdirs();
            log.info("Dossier uploads créé: {}", created);
        }

        // IMPORTANT: S'assurer que le chemin se termine par un séparateur
        String resourceLocation = "file:" + uploadPath + File.separator;

        // Configurer le mapping pour servir les fichiers statiques
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(resourceLocation)
                .setCachePeriod(3600); // Cache 1 heure

        // Logs détaillés pour le debug
        log.info("=== CONFIGURATION FICHIERS STATIQUES ===");
        log.info("URL Pattern: /uploads/**");
        log.info("Upload Directory: {}", uploadPath);
        log.info("Resource Location: {}", resourceLocation);
        log.info("Directory exists: {}", uploadFolder.exists());
        log.info("Directory readable: {}", uploadFolder.canRead());
        log.info("Directory writable: {}", uploadFolder.canWrite());

        // Vérifier le contenu du dossier uploads
        if (uploadFolder.exists()) {
            File[] contents = uploadFolder.listFiles();
            if (contents != null && contents.length > 0) {
                log.info("Contenu du dossier uploads ({} éléments):", contents.length);
                for (File item : contents) {
                    if (item.isDirectory()) {
                        log.info("  📁 {} (dossier)", item.getName());
                        // Vérifier le contenu des sous-dossiers
                        File[] subFiles = item.listFiles();
                        if (subFiles != null) {
                            log.info("     Contient {} fichiers", subFiles.length);
                            for (File subFile : subFiles) {
                                log.info("       📄 {} ({} bytes)", subFile.getName(), subFile.length());
                            }
                        }
                    } else {
                        log.info("  📄 {} ({} bytes)", item.getName(), item.length());
                    }
                }
            } else {
                log.warn("Dossier uploads vide!");
            }
        } else {
            log.error("❌ Dossier uploads n'existe pas: {}", uploadPath);
        }

        log.info("=========================================");
    }

   */
}