package com.froidcheikh.ecommerce.controller;

import com.froidcheikh.ecommerce.dto.MarqueDTO;
import com.froidcheikh.ecommerce.service.FileService;
import com.froidcheikh.ecommerce.service.MarqueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/marques")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class MarqueController {

    private final MarqueService marqueService;
    private final FileService fileService;

    @GetMapping
    public ResponseEntity<List<MarqueDTO>> getAllMarques() {
        List<MarqueDTO> marques = marqueService.getAllMarques();
        return ResponseEntity.ok(marques);
    }

    @GetMapping("/available")
    public ResponseEntity<List<MarqueDTO>> getMarquesWithProducts() {
        List<MarqueDTO> marques = marqueService.getMarquesWithAvailableProducts();
        return ResponseEntity.ok(marques);
    }

    @GetMapping("/{id}")
    public ResponseEntity<MarqueDTO> getMarqueById(@PathVariable Long id) {
        MarqueDTO marque = marqueService.getMarqueById(id);
        return ResponseEntity.ok(marque);
    }

    // Endpoints administrateur
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<MarqueDTO> createMarque(@Valid @RequestBody MarqueDTO marqueDTO) {
        MarqueDTO marque = marqueService.createMarque(marqueDTO);
        return ResponseEntity.ok(marque);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<MarqueDTO> updateMarque(
            @PathVariable Long id,
            @Valid @RequestBody MarqueDTO marqueDTO) {
        MarqueDTO marque = marqueService.updateMarque(id, marqueDTO);
        return ResponseEntity.ok(marque);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Void> deleteMarque(@PathVariable Long id) {
        marqueService.deleteMarque(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/logo")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<String> uploadMarqueLogo(
            @PathVariable Long id,
            @RequestParam("logo") MultipartFile file) {

        try {
            // Vérifier que la marque existe
            MarqueDTO marque = marqueService.getMarqueById(id);

            if (!fileService.isValidImageFile(file)) {
                throw new RuntimeException("Format d'image non valide");
            }

            // Supprimer l'ancien logo si il existe
            if (marque.getLogo() != null) {
                fileService.deleteFile(marque.getLogo());
            }

            // Upload du nouveau logo
            String logoPath = fileService.uploadFile(file, "marques/logos");

            // Mettre à jour la marque
            marque.setLogo(logoPath);
            marqueService.updateMarque(id, marque);

            log.info("Logo uploadé pour la marque {}: {}", id, logoPath);
            return ResponseEntity.ok(logoPath);

        } catch (Exception e) {
            log.error("Erreur lors de l'upload du logo de marque " + id, e);
            throw new RuntimeException("Erreur lors de l'upload du logo : " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}/logo")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Void> deleteMarqueLogo(@PathVariable Long id) {
        try {
            MarqueDTO marque = marqueService.getMarqueById(id);

            if (marque.getLogo() != null) {
                fileService.deleteFile(marque.getLogo());
                marque.setLogo(null);
                marqueService.updateMarque(id, marque);

                log.info("Logo supprimé pour la marque {}", id);
            }

            return ResponseEntity.ok().build();

        } catch (Exception e) {
            log.error("Erreur lors de la suppression du logo de marque", e);
            throw new RuntimeException("Erreur lors de la suppression du logo : " + e.getMessage());
        }
    }
}