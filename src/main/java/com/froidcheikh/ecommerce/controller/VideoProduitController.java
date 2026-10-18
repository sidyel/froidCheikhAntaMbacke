package com.froidcheikh.ecommerce.controller;

import com.froidcheikh.ecommerce.dto.VideoProduitDTO;
import com.froidcheikh.ecommerce.service.VideoProduitService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class VideoProduitController {

    private final VideoProduitService videoProduitService;

    // ── Public ────────────────────────────────────────────────────────────────

    @GetMapping("/api/produits/{produitId}/videos")
    public ResponseEntity<List<VideoProduitDTO>> listerActives(@PathVariable Long produitId) {
        return ResponseEntity.ok(videoProduitService.listerActives(produitId));
    }

    // ── Admin ─────────────────────────────────────────────────────────────────

    @GetMapping("/api/admin/produits/{produitId}/videos")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'GESTIONNAIRE')")
    public ResponseEntity<List<VideoProduitDTO>> listerToutes(@PathVariable Long produitId) {
        return ResponseEntity.ok(videoProduitService.listerToutes(produitId));
    }

    @PostMapping("/api/admin/produits/{produitId}/videos/lien")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'GESTIONNAIRE')")
    public ResponseEntity<VideoProduitDTO> ajouterLien(
            @PathVariable Long produitId,
            @RequestBody VideoProduitDTO.AjoutLienRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(videoProduitService.ajouterLien(produitId, req));
    }

    @GetMapping("/api/admin/produits/{produitId}/videos/signature")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'GESTIONNAIRE')")
    public ResponseEntity<VideoProduitDTO.SignatureResponse> genererSignature(
            @PathVariable Long produitId) {
        return ResponseEntity.ok(videoProduitService.genererSignature(produitId));
    }

    @PostMapping("/api/admin/produits/{produitId}/videos/confirmer-upload")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'GESTIONNAIRE')")
    public ResponseEntity<VideoProduitDTO> confirmerUpload(
            @PathVariable Long produitId,
            @RequestBody VideoProduitDTO.ConfirmUploadRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(videoProduitService.confirmerUpload(produitId, req));
    }

    @PutMapping("/api/admin/produits/{produitId}/videos/{videoId}/ordre")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'GESTIONNAIRE')")
    public ResponseEntity<VideoProduitDTO> modifierOrdre(
            @PathVariable Long produitId,
            @PathVariable Long videoId,
            @RequestBody VideoProduitDTO.OrdreRequest req) {
        return ResponseEntity.ok(videoProduitService.modifierOrdre(produitId, videoId, req.getOrdre()));
    }

    @PutMapping("/api/admin/produits/{produitId}/videos/{videoId}/activer")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'GESTIONNAIRE')")
    public ResponseEntity<VideoProduitDTO> toggleActif(
            @PathVariable Long produitId,
            @PathVariable Long videoId) {
        return ResponseEntity.ok(videoProduitService.toggleActif(produitId, videoId));
    }

    @DeleteMapping("/api/admin/produits/{produitId}/videos/{videoId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'GESTIONNAIRE')")
    public ResponseEntity<Void> supprimer(
            @PathVariable Long produitId,
            @PathVariable Long videoId) {
        videoProduitService.supprimer(produitId, videoId);
        return ResponseEntity.noContent().build();
    }
}
