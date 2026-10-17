package com.froidcheikh.ecommerce.controller;

import com.froidcheikh.ecommerce.dto.ParametresSiteDTO;
import com.froidcheikh.ecommerce.service.FileService;
import com.froidcheikh.ecommerce.service.ParametresSiteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class ConfigController {

    private final ParametresSiteService parametresSiteService;
    private final FileService fileService;

    /** Endpoint public — chargé par APP_INITIALIZER Angular au démarrage. */
    @GetMapping("/api/config")
    public ResponseEntity<ParametresSiteDTO.PublicDTO> getPublicConfig() {
        return ResponseEntity.ok(parametresSiteService.getPublicConfig());
    }

    @GetMapping("/api/admin/config")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ParametresSiteDTO.AdminDTO> getAdminConfig() {
        return ResponseEntity.ok(parametresSiteService.getAdminConfig());
    }

    @PutMapping("/api/admin/config")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ParametresSiteDTO.AdminDTO> updateConfig(
            @Valid @RequestBody ParametresSiteDTO.AdminDTO dto,
            Authentication authentication) {
        String modifiePar = authentication != null ? authentication.getName() : "system";
        return ResponseEntity.ok(parametresSiteService.update(dto, modifiePar));
    }

    @PostMapping("/api/admin/config/cachet")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<Map<String, String>> uploadCachet(@RequestParam("file") MultipartFile file) {
        String url = fileService.uploadFile(file, "site/cachets");
        parametresSiteService.updateCachetUrl(url);
        return ResponseEntity.ok(Map.of("url", url));
    }

    @PostMapping("/api/admin/config/entete-image")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<Map<String, String>> uploadImageEnTete(@RequestParam("file") MultipartFile file) {
        String url = fileService.uploadFile(file, "site/entetes");
        parametresSiteService.updateImageEnTeteUrl(url);
        return ResponseEntity.ok(Map.of("url", url));
    }

    @PostMapping("/api/admin/config/pied-image")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<Map<String, String>> uploadImagePied(@RequestParam("file") MultipartFile file) {
        String url = fileService.uploadFile(file, "site/pieds");
        parametresSiteService.updateImagePiedUrl(url);
        return ResponseEntity.ok(Map.of("url", url));
    }

    @DeleteMapping("/api/admin/config/cachet")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<Void> supprimerCachet() {
        parametresSiteService.updateCachetUrl(null);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/api/admin/config/entete-image")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<Void> supprimerImageEnTete() {
        parametresSiteService.updateImageEnTeteUrl(null);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/api/admin/config/pied-image")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<Void> supprimerImagePied() {
        parametresSiteService.updateImagePiedUrl(null);
        return ResponseEntity.noContent().build();
    }
}
