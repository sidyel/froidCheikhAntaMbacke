package com.froidcheikh.ecommerce.controller;

import com.froidcheikh.ecommerce.dto.LivraisonDTO;
import com.froidcheikh.ecommerce.dto.ZoneLivraisonDTO;
import com.froidcheikh.ecommerce.entity.Commande;
import com.froidcheikh.ecommerce.service.CalculCommandeService;
import com.froidcheikh.ecommerce.service.ZoneLivraisonService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class LivraisonController {

    private final ZoneLivraisonService zoneLivraisonService;
    private final CalculCommandeService calculCommandeService;

    // ─── PUBLIC ─────────────────────────────────────────────────────────────

    @GetMapping("/api/livraison/zones")
    public ResponseEntity<List<ZoneLivraisonDTO.PublicDTO>> getZonesPubliques() {
        return ResponseEntity.ok(zoneLivraisonService.getZonesActives());
    }

    @PostMapping("/api/livraison/estimation")
    public ResponseEntity<LivraisonDTO.EstimationResponse> estimer(
            @Valid @RequestBody LivraisonDTO.EstimationRequest request) {

        CalculCommandeService.CalculResultat r = calculCommandeService.calculer(
                request.getZoneId(), request.getModeLivraison(), request.getLignes());

        LivraisonDTO.EstimationResponse response = new LivraisonDTO.EstimationResponse();
        response.setMontantArticles(r.montantArticles());
        response.setFraisZone(r.fraisZone());
        response.setSupplementExpress(r.supplementExpress());
        response.setSupplementGabarit(r.supplementGabarit());
        response.setFraisLivraisonTotal(r.fraisLivraisonTotal());
        response.setMontantInstallation(r.montantInstallation());
        response.setMontantTotal(r.montantTotal());
        response.setLivraisonGratuite(r.livraisonGratuite());
        response.setDelaiEstime(r.delaiEstime());
        response.setModeLivraisonLabel(labelMode(request.getModeLivraison()));
        return ResponseEntity.ok(response);
    }

    // ─── ADMIN — CRUD ────────────────────────────────────────────────────────

    @GetMapping("/api/admin/livraison/zones")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','GESTIONNAIRE')")
    public ResponseEntity<List<ZoneLivraisonDTO.AdminDTO>> getAllZonesAdmin() {
        return ResponseEntity.ok(zoneLivraisonService.getAllZones());
    }

    @PostMapping("/api/admin/livraison/zones")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ZoneLivraisonDTO.AdminDTO> creerZone(
            @Valid @RequestBody ZoneLivraisonDTO.CreateDTO dto) {
        return ResponseEntity.ok(zoneLivraisonService.creer(dto));
    }

    @PutMapping("/api/admin/livraison/zones/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ZoneLivraisonDTO.AdminDTO> modifierZone(
            @PathVariable Long id,
            @Valid @RequestBody ZoneLivraisonDTO.CreateDTO dto) {
        return ResponseEntity.ok(zoneLivraisonService.modifier(id, dto));
    }

    @PatchMapping("/api/admin/livraison/zones/{id}/toggle-actif")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ZoneLivraisonDTO.AdminDTO> toggleActif(@PathVariable Long id) {
        return ResponseEntity.ok(zoneLivraisonService.toggleActif(id));
    }

    /** Désactivation sans suppression physique. */
    @DeleteMapping("/api/admin/livraison/zones/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<Void> desactiverZone(@PathVariable Long id) {
        zoneLivraisonService.desactiver(id);
        return ResponseEntity.noContent().build();
    }

    // ─── ADMIN — Import CSV ──────────────────────────────────────────────────

    @PostMapping("/api/admin/livraison/zones/import-csv/preview")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<LivraisonDTO.CsvPreviewResponse> previewCsv(
            @RequestParam("fichier") MultipartFile fichier) {
        return ResponseEntity.ok(zoneLivraisonService.previsualiserCsv(fichier));
    }

    @PostMapping("/api/admin/livraison/zones/import-csv/confirmer")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> confirmerCsv(@RequestParam("token") String token) {
        int count = zoneLivraisonService.confirmerImportCsv(token);
        return ResponseEntity.ok(Map.of("zonesImportees", count));
    }

    // ─── Helper ──────────────────────────────────────────────────────────────

    private String labelMode(Commande.ModeLivraison mode) {
        return switch (mode) {
            case RETRAIT_MAGASIN -> "Retrait en magasin";
            case LIVRAISON_EXPRESS -> "Livraison express";
            default -> "Livraison standard";
        };
    }
}
