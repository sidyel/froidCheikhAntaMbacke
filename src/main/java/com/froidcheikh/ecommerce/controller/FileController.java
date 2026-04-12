package com.froidcheikh.ecommerce.controller;

import com.froidcheikh.ecommerce.service.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class FileController {

    private final FileService fileService;

    /**
     * Sert les fichiers uploadés (images, PDF, etc.)
     */
    /*@GetMapping("/uploads/**")
    public ResponseEntity<Resource> serveFile(@RequestParam String filePath) {
        try {
            // Obtenir le chemin complet du fichier
            Path file = fileService.getFilePath(filePath);

            // Vérifier que le fichier existe
            if (!fileService.fileExists(filePath)) {
                log.warn("Fichier non trouvé: {}", filePath);
                return ResponseEntity.notFound().build();
            }

            // Créer la ressource
            Resource resource = new UrlResource(file.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                log.warn("Fichier non lisible: {}", filePath);
                return ResponseEntity.notFound().build();
            }

            // Déterminer le type de contenu
            String contentType = determineContentType(file);

            // Construire la réponse avec les headers appropriés
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CACHE_CONTROL, "max-age=3600") // Cache 1 heure
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + file.getFileName().toString() + "\"")
                    .body(resource);

        } catch (MalformedURLException e) {
            log.error("URL malformée pour le fichier: {}", filePath, e);
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Erreur lors du service du fichier: {}", filePath, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
     */
    @GetMapping("/uploads")
    public ResponseEntity<Resource> serveFile(@RequestParam String path) {
        String[] parts = path.split("/", 2);
        if (parts.length == 2) {
            return serveFileByPath(parts[0], parts[1]);
        }
        return ResponseEntity.badRequest().build();
    }

    /**
     * Alternative avec path variable (plus propre pour les URLs)
     */
    @GetMapping("/uploads/{category}/{filename:.+}")
    public ResponseEntity<Resource> serveFileByPath(
            @PathVariable String category,
            @PathVariable String filename) {

        try {
            String filePath = category + "/" + filename;
            log.info("🔍 Serving file: {}", filePath);

            // Vérifier que le fichier existe
            if (!fileService.fileExists(filePath)) {
                log.warn("❌ Fichier non trouvé: {}", filePath);
                return ResponseEntity.notFound().build();
            }

            // Obtenir le chemin complet du fichier
            Path file = fileService.getFilePath(filePath);

            // Créer la ressource
            Resource resource = new UrlResource(file.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                log.warn("❌ Fichier non lisible: {}", filePath);
                return ResponseEntity.notFound().build();
            }

            // Déterminer le type de contenu
            String contentType = determineContentType(file);
            log.info("✅ Serving file: {} with content type: {}", filePath, contentType);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CACHE_CONTROL, "max-age=3600")
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                    .body(resource);

        } catch (Exception e) {
            log.error("❌ Erreur lors du service du fichier: {}/{}", category, filename, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Endpoint pour vérifier l'existence d'un fichier
     */
    /**
     * Endpoint pour vérifier l'existence d'un fichier
     */
    @GetMapping("/exists")
    public ResponseEntity<Boolean> fileExists(@RequestParam String filePath) {
        boolean exists = fileService.fileExists(filePath);
        log.info("🔍 File exists check: {} -> {}", filePath, exists);
        return ResponseEntity.ok(exists);
    }

    /**
     * Détermine le type de contenu basé sur l'extension du fichier
     */
    private String determineContentType(Path file) {
        try {
            String contentType = Files.probeContentType(file);
            if (contentType != null) {
                return contentType;
            }
        } catch (IOException e) {
            log.debug("Impossible de déterminer le type de contenu pour: {}", file);
        }

        String filename = file.getFileName().toString().toLowerCase();

        if (filename.endsWith(".jpg") || filename.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (filename.endsWith(".png")) {
            return "image/png";
        } else if (filename.endsWith(".gif")) {
            return "image/gif";
        } else if (filename.endsWith(".webp")) {
            return "image/webp";
        } else if (filename.endsWith(".pdf")) {
            return "application/pdf";
        }

        return "application/octet-stream";
    }
}