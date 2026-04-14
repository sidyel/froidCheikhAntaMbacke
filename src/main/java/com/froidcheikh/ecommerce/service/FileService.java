package com.froidcheikh.ecommerce.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
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
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class FileService {

    private final Cloudinary cloudinary;

    private static final List<String> ALLOWED_IMAGE_TYPES = Arrays.asList(
            "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp"
    );

    /**
     * Upload un fichier vers Cloudinary
     * Retourne l'URL publique
     */
    public String uploadFile(MultipartFile file, String folder) {
        try {
            Map<?, ?> result = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", folder,
                            "resource_type", "auto",
                            "type", "upload",        // ← ajouter
                            "access_mode", "public"  // ← ajouter
                    )
            );
            String url = (String) result.get("secure_url");
            log.info("✅ Fichier uploadé sur Cloudinary: {}", url);
            return url;
        } catch (IOException e) {
            log.error("❌ Erreur upload Cloudinary", e);
            throw new RuntimeException("Impossible d'uploader le fichier : " + e.getMessage());
        }
    }

    /**
     * Supprime un fichier de Cloudinary via son URL
     */
    public boolean deleteFile(String fileUrl) {
        try {
            // Extraire le public_id depuis l'URL Cloudinary
            String publicId = extractPublicId(fileUrl);
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            log.info("✅ Fichier supprimé de Cloudinary: {}", publicId);
            return true;
        } catch (IOException e) {
            log.error("❌ Erreur suppression Cloudinary", e);
            return false;
        }
    }

    public boolean isValidImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) return false;
        return ALLOWED_IMAGE_TYPES.contains(file.getContentType());
    }

    public boolean isValidPdfFile(MultipartFile file) {
        if (file == null || file.isEmpty()) return false;
        return "application/pdf".equals(file.getContentType());
    }

    /**
     * Extrait le public_id depuis une URL Cloudinary
     * Ex: https://res.cloudinary.com/dle0qfobt/image/upload/v123/produits/images/fichier.jpg
     *  -> produits/images/fichier
     */
    private String extractPublicId(String url) {
        if (url == null) return "";
        String[] parts = url.split("/upload/");
        if (parts.length < 2) return url;
        String afterUpload = parts[1].replaceFirst("v\\d+/", ""); // enlève v123456/
        int dotIndex = afterUpload.lastIndexOf('.');
        return dotIndex > 0 ? afterUpload.substring(0, dotIndex) : afterUpload;
    }

    // Garder pour compatibilité si utilisé ailleurs
    public boolean fileExists(String filePath) {
        return filePath != null && !filePath.isEmpty();
    }
}