package com.froidcheikh.ecommerce.dto;

import com.froidcheikh.ecommerce.entity.TypeVideo;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VideoProduitDTO {

    private Long id;
    private TypeVideo type;
    private String videoId;
    private String url;
    private String urlApercu;
    private String titre;
    private int ordre;
    private boolean active;
    private LocalDateTime dateCreation;

    // ── Requêtes entrantes ────────────────────────────────────────────────────

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AjoutLienRequest {
        private String url;
        private String titre;
        /** Image d'aperçu personnalisée pour Facebook (URL Cloudinary déjà uploadée). */
        private String urlApercuPersonnalise;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrdreRequest {
        private int ordre;
    }

    /** Réponse à la demande de signature pour l'upload direct Cloudinary. */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SignatureResponse {
        private String apiKey;
        private String cloudName;
        private String signature;
        private long timestamp;
        private String folder;
        private int maxFileSize;
        private String allowedFormats;
    }

    /** Corps envoyé par le frontend après un upload direct Cloudinary réussi. */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConfirmUploadRequest {
        private String publicId;
        private String secureUrl;
        private String originalFilename;
        private long bytes;
        private String format;
        private String titre;
    }
}
