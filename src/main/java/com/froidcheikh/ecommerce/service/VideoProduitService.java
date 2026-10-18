package com.froidcheikh.ecommerce.service;

import com.cloudinary.Cloudinary;
import com.froidcheikh.ecommerce.dto.VideoProduitDTO;
import com.froidcheikh.ecommerce.entity.Produit;
import com.froidcheikh.ecommerce.entity.TypeVideo;
import com.froidcheikh.ecommerce.entity.VideoProduit;
import com.froidcheikh.ecommerce.repository.ProduitRepository;
import com.froidcheikh.ecommerce.repository.VideoProduitRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class VideoProduitService {

    private static final int MAX_VIDEOS_PAR_PRODUIT = 3;
    private static final List<String> ALLOWED_FORMATS = List.of("mp4", "mov", "webm");

    /** Taille maximale en Mo pour l'upload direct Cloudinary (validation signature). */
    @Value("${app.video.max-size-mb:50}")
    private int maxSizeMb;

    private final VideoProduitRepository videoRepository;
    private final ProduitRepository produitRepository;
    private final VideoExtractionService extractionService;
    private final FileService fileService;
    private final Cloudinary cloudinary;

    // ── Lecture ───────────────────────────────────────────────────────────────

    public List<VideoProduitDTO> listerActives(Long produitId) {
        return videoRepository
                .findByProduitIdProduitAndActiveOrderByOrdreAsc(produitId, true)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    public List<VideoProduitDTO> listerToutes(Long produitId) {
        return videoRepository
                .findByProduitIdProduitOrderByOrdreAsc(produitId)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    // ── Ajout par lien YouTube / Facebook ────────────────────────────────────

    @Transactional
    public VideoProduitDTO ajouterLien(Long produitId, VideoProduitDTO.AjoutLienRequest req) {
        Produit produit = getProduit(produitId);
        verifierLimite(produitId);

        VideoExtractionService.ExtractionResult extraction = extractionService.extraire(req.getUrl());
        if (extraction == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "URL non reconnue : seuls les liens YouTube et Facebook sont acceptés.");
        }

        String urlApercu = req.getUrlApercuPersonnalise() != null
                ? req.getUrlApercuPersonnalise()
                : extraction.urlApercu();

        VideoProduit video = new VideoProduit();
        video.setProduit(produit);
        video.setType(extraction.type());
        video.setVideoId(extraction.videoId());
        video.setUrl(req.getUrl().trim());
        video.setUrlApercu(urlApercu);
        video.setTitre(req.getTitre());
        video.setOrdre(prochainOrdre(produitId));
        video.setActive(true);

        return toDto(videoRepository.save(video));
    }

    // ── Signature upload direct Cloudinary ───────────────────────────────────

    /**
     * Génère une signature à durée courte pour l'upload direct navigateur → Cloudinary.
     * Le fichier ne transite jamais par notre serveur.
     */
    public VideoProduitDTO.SignatureResponse genererSignature(Long produitId) {
        // Vérifie que le produit existe et que la limite n'est pas atteinte
        getProduit(produitId);
        verifierLimite(produitId);

        try {
            long timestamp = System.currentTimeMillis() / 1000L;
            String folder = "produits/videos/" + produitId;

            Map<String, Object> params = new HashMap<>();
            params.put("timestamp", timestamp);
            params.put("folder", folder);
            // Limite les formats et la taille côté Cloudinary
            params.put("allowed_formats", String.join(",", ALLOWED_FORMATS));
            params.put("max_file_size", (long) maxSizeMb * 1024 * 1024);

            String signature = cloudinary.apiSignRequest(params,
                    (String) cloudinary.config.apiSecret);

            return new VideoProduitDTO.SignatureResponse(
                    (String) cloudinary.config.apiKey,
                    (String) cloudinary.config.cloudName,
                    signature,
                    timestamp,
                    folder,
                    maxSizeMb,
                    String.join(",", ALLOWED_FORMATS)
            );
        } catch (Exception e) {
            log.error("Erreur génération signature Cloudinary", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Impossible de générer la signature d'upload.");
        }
    }

    // ── Confirmation après upload direct ─────────────────────────────────────

    @Transactional
    public VideoProduitDTO confirmerUpload(Long produitId, VideoProduitDTO.ConfirmUploadRequest req) {
        Produit produit = getProduit(produitId);
        verifierLimite(produitId);

        // Vérifications de sécurité sur le résultat Cloudinary
        if (req.getPublicId() == null || req.getSecureUrl() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "publicId et secureUrl requis.");
        }
        if (req.getBytes() > (long) maxSizeMb * 1024 * 1024) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Fichier trop lourd (max " + maxSizeMb + " Mo).");
        }
        String fmt = req.getFormat() != null ? req.getFormat().toLowerCase() : "";
        if (!ALLOWED_FORMATS.contains(fmt)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Format non autorisé : " + fmt + ". Formats acceptés : " + ALLOWED_FORMATS);
        }

        String urlApercu = extractionService.buildCloudinaryApercu(req.getSecureUrl());

        VideoProduit video = new VideoProduit();
        video.setProduit(produit);
        video.setType(TypeVideo.CLOUDINARY);
        video.setVideoId(req.getPublicId());
        video.setUrl(req.getSecureUrl());
        video.setUrlApercu(urlApercu);
        video.setTitre(req.getTitre() != null ? req.getTitre() : req.getOriginalFilename());
        video.setOrdre(prochainOrdre(produitId));
        video.setActive(true);

        return toDto(videoRepository.save(video));
    }

    // ── Modification ──────────────────────────────────────────────────────────

    @Transactional
    public VideoProduitDTO modifierOrdre(Long produitId, Long videoId, int nouvelOrdre) {
        VideoProduit video = getVideo(produitId, videoId);
        video.setOrdre(nouvelOrdre);
        return toDto(videoRepository.save(video));
    }

    @Transactional
    public VideoProduitDTO toggleActif(Long produitId, Long videoId) {
        VideoProduit video = getVideo(produitId, videoId);
        video.setActive(!video.isActive());
        return toDto(videoRepository.save(video));
    }

    // ── Suppression ───────────────────────────────────────────────────────────

    @Transactional
    public void supprimer(Long produitId, Long videoId) {
        VideoProduit video = getVideo(produitId, videoId);
        if (video.getType() == TypeVideo.CLOUDINARY && video.getVideoId() != null) {
            fileService.deleteVideoByPublicId(video.getVideoId());
        }
        videoRepository.delete(video);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Produit getProduit(Long produitId) {
        return produitRepository.findById(produitId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Produit " + produitId + " introuvable."));
    }

    private VideoProduit getVideo(Long produitId, Long videoId) {
        VideoProduit video = videoRepository.findById(videoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Vidéo " + videoId + " introuvable."));
        if (!video.getProduit().getIdProduit().equals(produitId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Vidéo introuvable pour ce produit.");
        }
        return video;
    }

    private void verifierLimite(Long produitId) {
        long count = videoRepository.countByProduitIdProduit(produitId);
        if (count >= MAX_VIDEOS_PAR_PRODUIT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Limite atteinte : " + MAX_VIDEOS_PAR_PRODUIT + " vidéos maximum par produit.");
        }
    }

    private int prochainOrdre(Long produitId) {
        List<VideoProduit> existing = videoRepository.findByProduitIdProduitOrderByOrdreAsc(produitId);
        return existing.isEmpty() ? 0 : existing.get(existing.size() - 1).getOrdre() + 1;
    }

    public VideoProduitDTO toDto(VideoProduit v) {
        return new VideoProduitDTO(
                v.getId(),
                v.getType(),
                v.getVideoId(),
                v.getUrl(),
                v.getUrlApercu(),
                v.getTitre(),
                v.getOrdre(),
                v.isActive(),
                v.getDateCreation()
        );
    }
}
