package com.froidcheikh.ecommerce.controller;

import com.froidcheikh.ecommerce.dto.ParametresSiteDTO;
import com.froidcheikh.ecommerce.service.ParametresSiteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class ConfigController {

    private final ParametresSiteService parametresSiteService;

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
}
