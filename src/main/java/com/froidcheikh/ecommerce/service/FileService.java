package com.froidcheikh.ecommerce.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class FileService {

    @Value("${cloudinary.cloud-name}")
    private String cloudName;

    @Value("${cloudinary.api-key}")
    private String apiKey;

    @Value("${cloudinary.api-secret}")
    private String apiSecret;

    private Cloudinary cloudinary;

    private static final List<String> ALLOWED_IMAGE_TYPES = Arrays.asList(
            "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp"
    );

    @PostConstruct
    public void init() {
        cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key",    apiKey,
                "api_secret", apiSecret,
                "secure",     true
        ));
        log.info("✅ Cloudinary initialisé: cloud={}", cloudName);
    }

    /**
     * Upload un fichier vers Cloudinary
     * Retourne l'URL publique permanente
     */
    public String uploadFile(MultipartFile file, String subDirectory) {
        try {
            validateFile(file);

            Map<?, ?> result = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder",          "froid-cheikh/" + subDirectory,
                            "resource_type",   "auto",
                            "use_filename",    true,
                            "unique_filename", true
                    )
            );

            String url = (String) result.get("secure_url");
            log.info("✅ Fichier uploadé sur Cloudinary: {}", url);
            return url; // URL permanente HTTPS

        } catch (IOException e) {
            log.error("❌ Erreur upload Cloudinary: {}", e.getMessage());
            throw new RuntimeException("Impossible d'uploader le fichier: " + e.getMessage());
        }
    }

    /**
     * Supprime un fichier sur Cloudinary via son URL
     */
    public boolean deleteFile(String fileUrl) {
        try {
            // Extraire le public_id depuis l'URL Cloudinary
            String publicId = extractPublicId(fileUrl);
            Map<?, ?> result = cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            boolean deleted = "ok".equals(result.get("result"));
            if (deleted) {
                log.info("✅ Fichier supprimé sur Cloudinary: {}", publicId);
            } else {
                log.warn("⚠️ Fichier non trouvé sur Cloudinary: {}", publicId);
            }
            return deleted;
        } catch (IOException e) {
            log.error("❌ Erreur suppression Cloudinary: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Upload plusieurs fichiers
     */
    public List<String> uploadMultipleFiles(MultipartFile[] files, String subDirectory) {
        return Arrays.stream(files)
                .map(file -> uploadFile(file, subDirectory))
                .toList();
    }

    public boolean isValidImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) return false;
        return ALLOWED_IMAGE_TYPES.contains(file.getContentType());
    }

    public boolean isValidPdfFile(MultipartFile file) {
        if (file == null || file.isEmpty()) return false;
        return "application/pdf".equals(file.getContentType());
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Le fichier est vide ou null");
        }
        if (file.getSize() > 10 * 1024 * 1024) { // 10MB
            throw new IllegalArgumentException("Fichier trop volumineux (max 10MB)");
        }
    }

    /**
     * Extrait le public_id Cloudinary depuis une URL
     * Ex: https://res.cloudinary.com/moncloud/image/upload/v123/froid-cheikh/produits/images/abc.jpg
     *  → froid-cheikh/produits/images/abc
     */
    private String extractPublicId(String url) {
        try {
            String withoutExtension = url.substring(0, url.lastIndexOf('.'));
            int uploadIndex = withoutExtension.indexOf("/upload/");
            String afterUpload = withoutExtension.substring(uploadIndex + 8);
            // Supprimer la version (v1234567/)
            if (afterUpload.matches("v\\d+/.*")) {
                afterUpload = afterUpload.substring(afterUpload.indexOf('/') + 1);
            }
            return afterUpload;
        } catch (Exception e) {
            log.error("Impossible d'extraire le public_id de l'URL: {}", url);
            return url;
        }
    }
}




//package com.froidcheikh.ecommerce.service;
//
//import jakarta.annotation.PostConstruct;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Service;
//import org.springframework.util.StringUtils;
//import org.springframework.web.multipart.MultipartFile;
//
//import java.io.IOException;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.nio.file.Paths;
//import java.nio.file.StandardCopyOption;
//import java.time.LocalDateTime;
//import java.time.format.DateTimeFormatter;
//import java.util.Arrays;
//import java.util.List;
//import java.util.UUID;
//
//@Service
//@Slf4j
//public class FileService {
//
//    @Value("${app.upload.dir:uploads}")
//    private String uploadDir;
//
//
//    @Value("${app.upload.max-file-size:5242880}") // 5MB par défaut
//    private long maxFileSize;
//
//    private static final List<String> ALLOWED_IMAGE_TYPES = Arrays.asList(
//            "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp"
//    );
//
//    private static final List<String> ALLOWED_PDF_TYPES = Arrays.asList(
//            "application/pdf"
//    );
//
//    private static final List<String> ALLOWED_IMAGE_EXTENSIONS = Arrays.asList(
//            "jpg", "jpeg", "png", "gif", "webp"
//    );
//
//    @PostConstruct
//    public void init() {
//        try {
//            Path uploadPath = Paths.get(uploadDir);
//            if (!Files.exists(uploadPath)) {
//                Files.createDirectories(uploadPath);
//                log.info("📁 Répertoire d'upload créé: {}", uploadPath.toAbsolutePath());
//            }
//        } catch (IOException e) {
//            log.error("❌ Impossible de créer le répertoire d'upload", e);
//            throw new RuntimeException("Impossible d'initialiser le stockage de fichiers", e);
//        }
//    }
//
//    /**
//     * Upload un fichier dans le répertoire spécifié
//     */
//    public String uploadFile(MultipartFile file, String subDirectory) {
//        try {
//            // Validation du fichier
//            validateFile(file);
//
//            // Création du répertoire de destination
//            Path uploadPath = createUploadDirectory(subDirectory);
//
//            // Génération du nom de fichier unique
//            String fileName = generateUniqueFileName(file.getOriginalFilename());
//
//            // Chemin complet du fichier
//            Path filePath = uploadPath.resolve(fileName);
//
//            // Copie du fichier
//            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
//
//            // Retourne le chemin relatif pour la base de données
//            String relativePath = subDirectory + "/" + fileName;
//            log.info("Fichier uploadé avec succès: {}", relativePath);
//
//            return relativePath;
//
//        } catch (IOException e) {
//            log.error("Erreur lors de l'upload du fichier: {}", e.getMessage());
//            throw new RuntimeException("Impossible d'uploader le fichier: " + e.getMessage());
//        }
//    }
//
//    /**
//     * Upload plusieurs fichiers
//     */
//    public List<String> uploadMultipleFiles(MultipartFile[] files, String subDirectory) {
//        return Arrays.stream(files)
//                .map(file -> uploadFile(file, subDirectory))
//                .toList();
//    }
//
//    /**
//     * Supprime un fichier
//     */
//    public boolean deleteFile(String filePath) {
//        try {
//            Path path = Paths.get(uploadDir).resolve(filePath);
//            boolean deleted = Files.deleteIfExists(path);
//            if (deleted) {
//                log.info("Fichier supprimé: {}", filePath);
//            } else {
//                log.warn("Fichier non trouvé pour suppression: {}", filePath);
//            }
//            return deleted;
//        } catch (IOException e) {
//            log.error("Erreur lors de la suppression du fichier {}: {}", filePath, e.getMessage());
//            return false;
//        }
//    }
//
//    /**
//     * Vérifie si un fichier est une image valide
//     */
//    public boolean isValidImageFile(MultipartFile file) {
//        if (file == null || file.isEmpty()) {
//            return false;
//        }
//
//        String contentType = file.getContentType();
//        if (!ALLOWED_IMAGE_TYPES.contains(contentType)) {
//            log.warn("Type de fichier non autorisé: {}", contentType);
//            return false;
//        }
//
//        String extension = getFileExtension(file.getOriginalFilename());
//        return ALLOWED_IMAGE_EXTENSIONS.contains(extension.toLowerCase());
//    }
//
//    /**
//     * Vérifie si un fichier est un PDF valide
//     */
//    public boolean isValidPdfFile(MultipartFile file) {
//        if (file == null || file.isEmpty()) {
//            return false;
//        }
//
//        String contentType = file.getContentType();
//        return ALLOWED_PDF_TYPES.contains(contentType);
//    }
//
//    /**
//     * Valide un fichier (taille, type, etc.)
//     */
//    private void validateFile(MultipartFile file) {
//        if (file == null || file.isEmpty()) {
//            throw new IllegalArgumentException("Le fichier est vide ou null");
//        }
//
//        if (file.getSize() > maxFileSize) {
//            throw new IllegalArgumentException(
//                    String.format("Le fichier est trop volumineux. Taille maximum autorisée: %d bytes", maxFileSize)
//            );
//        }
//
//        String originalFilename = file.getOriginalFilename();
//        if (!StringUtils.hasText(originalFilename)) {
//            throw new IllegalArgumentException("Le nom du fichier est invalide");
//        }
//    }
//
//    /**
//     * Crée le répertoire d'upload si nécessaire
//     */
//    private Path createUploadDirectory(String subDirectory) throws IOException {
//        Path uploadPath = Paths.get(uploadDir).resolve(subDirectory);
//        if (!Files.exists(uploadPath)) {
//            Files.createDirectories(uploadPath);
//            log.info("Répertoire créé: {}", uploadPath);
//        }
//        return uploadPath;
//    }
//
//    /**
//     * Génère un nom de fichier unique
//     */
//    private String generateUniqueFileName(String originalFilename) {
//        String extension = getFileExtension(originalFilename);
//        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
//        String uuid = UUID.randomUUID().toString().substring(0, 8);
//
//        return String.format("%s_%s.%s", timestamp, uuid, extension);
//    }
//
//    /**
//     * Extrait l'extension d'un fichier
//     */
//    private String getFileExtension(String filename) {
//        if (!StringUtils.hasText(filename)) {
//            return "";
//        }
//        int lastDotIndex = filename.lastIndexOf('.');
//        return lastDotIndex >= 0 ? filename.substring(lastDotIndex + 1) : "";
//    }
//
//    /**
//     * Obtient le chemin absolu d'un fichier
//     */
//    public Path getFilePath(String relativePath) {
//        return Paths.get(uploadDir).resolve(relativePath);
//    }
//
//    /**
//     * Vérifie si un fichier existe
//     */
//    public boolean fileExists(String relativePath) {
//        Path filePath = getFilePath(relativePath);
//        return Files.exists(filePath);
//    }
//}