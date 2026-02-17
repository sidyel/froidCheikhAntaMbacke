package com.froidcheikh.ecommerce.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class FileService {

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;


    @Value("${app.upload.max-file-size:5242880}") // 5MB par défaut
    private long maxFileSize;

    private static final List<String> ALLOWED_IMAGE_TYPES = Arrays.asList(
            "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp"
    );

    private static final List<String> ALLOWED_PDF_TYPES = Arrays.asList(
            "application/pdf"
    );

    private static final List<String> ALLOWED_IMAGE_EXTENSIONS = Arrays.asList(
            "jpg", "jpeg", "png", "gif", "webp"
    );

    @PostConstruct
    public void init() {
        try {
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
                log.info("📁 Répertoire d'upload créé: {}", uploadPath.toAbsolutePath());
            }
        } catch (IOException e) {
            log.error("❌ Impossible de créer le répertoire d'upload", e);
            throw new RuntimeException("Impossible d'initialiser le stockage de fichiers", e);
        }
    }

    /**
     * Upload un fichier dans le répertoire spécifié
     */
    public String uploadFile(MultipartFile file, String subDirectory) {
        try {
            // Validation du fichier
            validateFile(file);

            // Création du répertoire de destination
            Path uploadPath = createUploadDirectory(subDirectory);

            // Génération du nom de fichier unique
            String fileName = generateUniqueFileName(file.getOriginalFilename());

            // Chemin complet du fichier
            Path filePath = uploadPath.resolve(fileName);

            // Copie du fichier
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // Retourne le chemin relatif pour la base de données
            String relativePath = subDirectory + "/" + fileName;
            log.info("Fichier uploadé avec succès: {}", relativePath);

            return relativePath;

        } catch (IOException e) {
            log.error("Erreur lors de l'upload du fichier: {}", e.getMessage());
            throw new RuntimeException("Impossible d'uploader le fichier: " + e.getMessage());
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

    /**
     * Supprime un fichier
     */
    public boolean deleteFile(String filePath) {
        try {
            Path path = Paths.get(uploadDir).resolve(filePath);
            boolean deleted = Files.deleteIfExists(path);
            if (deleted) {
                log.info("Fichier supprimé: {}", filePath);
            } else {
                log.warn("Fichier non trouvé pour suppression: {}", filePath);
            }
            return deleted;
        } catch (IOException e) {
            log.error("Erreur lors de la suppression du fichier {}: {}", filePath, e.getMessage());
            return false;
        }
    }

    /**
     * Vérifie si un fichier est une image valide
     */
    public boolean isValidImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return false;
        }

        String contentType = file.getContentType();
        if (!ALLOWED_IMAGE_TYPES.contains(contentType)) {
            log.warn("Type de fichier non autorisé: {}", contentType);
            return false;
        }

        String extension = getFileExtension(file.getOriginalFilename());
        return ALLOWED_IMAGE_EXTENSIONS.contains(extension.toLowerCase());
    }

    /**
     * Vérifie si un fichier est un PDF valide
     */
    public boolean isValidPdfFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return false;
        }

        String contentType = file.getContentType();
        return ALLOWED_PDF_TYPES.contains(contentType);
    }

    /**
     * Valide un fichier (taille, type, etc.)
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Le fichier est vide ou null");
        }

        if (file.getSize() > maxFileSize) {
            throw new IllegalArgumentException(
                    String.format("Le fichier est trop volumineux. Taille maximum autorisée: %d bytes", maxFileSize)
            );
        }

        String originalFilename = file.getOriginalFilename();
        if (!StringUtils.hasText(originalFilename)) {
            throw new IllegalArgumentException("Le nom du fichier est invalide");
        }
    }

    /**
     * Crée le répertoire d'upload si nécessaire
     */
    private Path createUploadDirectory(String subDirectory) throws IOException {
        Path uploadPath = Paths.get(uploadDir).resolve(subDirectory);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
            log.info("Répertoire créé: {}", uploadPath);
        }
        return uploadPath;
    }

    /**
     * Génère un nom de fichier unique
     */
    private String generateUniqueFileName(String originalFilename) {
        String extension = getFileExtension(originalFilename);
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String uuid = UUID.randomUUID().toString().substring(0, 8);

        return String.format("%s_%s.%s", timestamp, uuid, extension);
    }

    /**
     * Extrait l'extension d'un fichier
     */
    private String getFileExtension(String filename) {
        if (!StringUtils.hasText(filename)) {
            return "";
        }
        int lastDotIndex = filename.lastIndexOf('.');
        return lastDotIndex >= 0 ? filename.substring(lastDotIndex + 1) : "";
    }

    /**
     * Obtient le chemin absolu d'un fichier
     */
    public Path getFilePath(String relativePath) {
        return Paths.get(uploadDir).resolve(relativePath);
    }

    /**
     * Vérifie si un fichier existe
     */
    public boolean fileExists(String relativePath) {
        Path filePath = getFilePath(relativePath);
        return Files.exists(filePath);
    }
}