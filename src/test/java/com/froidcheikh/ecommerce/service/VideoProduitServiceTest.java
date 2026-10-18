package com.froidcheikh.ecommerce.service;

import com.froidcheikh.ecommerce.dto.VideoProduitDTO;
import com.froidcheikh.ecommerce.entity.Produit;
import com.froidcheikh.ecommerce.entity.TypeVideo;
import com.froidcheikh.ecommerce.entity.VideoProduit;
import com.froidcheikh.ecommerce.repository.ProduitRepository;
import com.froidcheikh.ecommerce.repository.VideoProduitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VideoProduitServiceTest {

    @Mock private VideoProduitRepository videoRepository;
    @Mock private ProduitRepository produitRepository;
    @Mock private VideoExtractionService extractionService;
    @Mock private FileService fileService;
    @Mock private com.cloudinary.Cloudinary cloudinary;

    @InjectMocks
    private VideoProduitService service;

    private Produit produit;

    @BeforeEach
    void setUp() {
        produit = new Produit();
        produit.setIdProduit(1L);
        produit.setNomProduit("Test Produit");
        produit.setPrix(BigDecimal.valueOf(100));
    }

    // ── Ajout lien YouTube ────────────────────────────────────────────────────

    @Test
    void devraitAjouterLienYoutube() {
        when(produitRepository.findById(1L)).thenReturn(Optional.of(produit));
        when(videoRepository.countByProduitIdProduit(1L)).thenReturn(0L);
        when(extractionService.extraire(anyString())).thenReturn(
                new VideoExtractionService.ExtractionResult(
                        TypeVideo.YOUTUBE, "dQw4w9WgXcQ",
                        "https://img.youtube.com/vi/dQw4w9WgXcQ/hqdefault.jpg"));
        when(videoRepository.findByProduitIdProduitOrderByOrdreAsc(1L)).thenReturn(List.of());
        when(videoRepository.save(any())).thenAnswer(inv -> {
            VideoProduit v = inv.getArgument(0);
            v.setId(10L);
            return v;
        });

        VideoProduitDTO result = service.ajouterLien(1L, new VideoProduitDTO.AjoutLienRequest(
                "https://youtu.be/dQw4w9WgXcQ", "Super vidéo", null));

        assertThat(result.getType()).isEqualTo(TypeVideo.YOUTUBE);
        assertThat(result.getVideoId()).isEqualTo("dQw4w9WgXcQ");
        assertThat(result.getUrlApercu()).contains("hqdefault.jpg");
        assertThat(result.getTitre()).isEqualTo("Super vidéo");
    }

    @Test
    void devraitRefuserUrlNonReconnue() {
        when(produitRepository.findById(1L)).thenReturn(Optional.of(produit));
        when(videoRepository.countByProduitIdProduit(1L)).thenReturn(0L);
        when(extractionService.extraire(anyString())).thenReturn(null);

        assertThatThrownBy(() -> service.ajouterLien(1L, new VideoProduitDTO.AjoutLienRequest(
                "https://tiktok.com/video/123", null, null)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("400");
    }

    // ── Limite 3 vidéos ───────────────────────────────────────────────────────

    @Test
    void devraitRefuserSiLimiteAtteinte() {
        when(produitRepository.findById(1L)).thenReturn(Optional.of(produit));
        when(videoRepository.countByProduitIdProduit(1L)).thenReturn(3L);

        assertThatThrownBy(() -> service.ajouterLien(1L, new VideoProduitDTO.AjoutLienRequest(
                "https://youtu.be/dQw4w9WgXcQ", null, null)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("409");
    }

    // ── Confirmation upload Cloudinary ────────────────────────────────────────

    @Test
    void devraitRefuserFichierTropLourd() {
        when(produitRepository.findById(1L)).thenReturn(Optional.of(produit));
        when(videoRepository.countByProduitIdProduit(1L)).thenReturn(0L);

        VideoProduitDTO.ConfirmUploadRequest req = new VideoProduitDTO.ConfirmUploadRequest(
                "produits/videos/1/test", "https://res.cloudinary.com/demo/video/upload/v1/test.mp4",
                "test.mp4", 60L * 1024 * 1024, "mp4", null);

        assertThatThrownBy(() -> service.confirmerUpload(1L, req))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("400");
    }

    @Test
    void devraitRefuserFormatNonAutorise() {
        when(produitRepository.findById(1L)).thenReturn(Optional.of(produit));
        when(videoRepository.countByProduitIdProduit(1L)).thenReturn(0L);

        VideoProduitDTO.ConfirmUploadRequest req = new VideoProduitDTO.ConfirmUploadRequest(
                "produits/videos/1/test", "https://res.cloudinary.com/demo/video/upload/v1/test.avi",
                "test.avi", 5L * 1024 * 1024, "avi", null);

        assertThatThrownBy(() -> service.confirmerUpload(1L, req))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("400");
    }

    // ── Suppression vidéo Cloudinary ──────────────────────────────────────────

    @Test
    void devraitSupprimerVideoCloudinaryViaDeleteVideoByPublicId() {
        VideoProduit video = new VideoProduit();
        video.setId(5L);
        video.setProduit(produit);
        video.setType(TypeVideo.CLOUDINARY);
        video.setVideoId("produits/videos/1/ma-video");
        video.setUrl("https://res.cloudinary.com/demo/video/upload/v1/produits/videos/1/ma-video.mp4");

        when(videoRepository.findById(5L)).thenReturn(Optional.of(video));
        when(fileService.deleteVideoByPublicId(anyString())).thenReturn(true);

        service.supprimer(1L, 5L);

        verify(fileService).deleteVideoByPublicId("produits/videos/1/ma-video");
        verify(videoRepository).delete(video);
    }

    @Test
    void neDoitPasAppelerDeleteFilesPourVideoYoutube() {
        VideoProduit video = new VideoProduit();
        video.setId(6L);
        video.setProduit(produit);
        video.setType(TypeVideo.YOUTUBE);
        video.setVideoId("dQw4w9WgXcQ");
        video.setUrl("https://youtu.be/dQw4w9WgXcQ");

        when(videoRepository.findById(6L)).thenReturn(Optional.of(video));

        service.supprimer(1L, 6L);

        verify(fileService, never()).deleteVideoByPublicId(any());
        verify(fileService, never()).deleteFile(any());
        verify(videoRepository).delete(video);
    }
}
