package com.froidcheikh.ecommerce.controller;

import com.froidcheikh.ecommerce.dto.CategorieDTO;
import com.froidcheikh.ecommerce.service.CategorieService;
import com.froidcheikh.ecommerce.service.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class CategorieController {

    private final CategorieService categorieService;
    private final FileService fileService;


    @GetMapping
    public ResponseEntity<List<CategorieDTO>> getAllCategories() {
        List<CategorieDTO> categories = categorieService.getAllCategories();
        return ResponseEntity.ok(categories);
    }

    @GetMapping("/tree")
    public ResponseEntity<List<CategorieDTO>> getCategoriesTree() {
        List<CategorieDTO> categories = categorieService.getCategoriesTree();
        return ResponseEntity.ok(categories);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategorieDTO> getCategorieById(@PathVariable Long id) {
        CategorieDTO categorie = categorieService.getCategorieById(id);
        return ResponseEntity.ok(categorie);
    }

    @GetMapping("/{id}/sous-categories")
    public ResponseEntity<List<CategorieDTO>> getSousCategories(@PathVariable Long id) {
        List<CategorieDTO> sousCategories = categorieService.getSousCategories(id);
        return ResponseEntity.ok(sousCategories);
    }

    // Endpoints administrateur
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<CategorieDTO> createCategorie(@Valid @RequestBody CategorieDTO categorieDTO) {
        CategorieDTO categorie = categorieService.createCategorie(categorieDTO);
        return ResponseEntity.ok(categorie);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<CategorieDTO> updateCategorie(
            @PathVariable Long id,
            @Valid @RequestBody CategorieDTO categorieDTO) {
        CategorieDTO categorie = categorieService.updateCategorie(id, categorieDTO);
        return ResponseEntity.ok(categorie);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Void> deleteCategorie(@PathVariable Long id) {
        categorieService.deleteCategorie(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/image")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<String> uploadCategorieImage(
            @PathVariable Long id,
            @RequestParam("image") MultipartFile file) {

        try {
            // Vérifier que la catégorie existe
            CategorieDTO categorie = categorieService.getCategorieById(id);

            if (!fileService.isValidImageFile(file)) {
                throw new RuntimeException("Format d'image non valide");
            }

            // Supprimer l'ancienne image si elle existe
            if (categorie.getImageCategorie() != null) {
                fileService.deleteFile(categorie.getImageCategorie());
            }

            // Upload de la nouvelle image
            String imagePath = fileService.uploadFile(file, "categories");

            // Mettre à jour la catégorie
            categorie.setImageCategorie(imagePath);
            categorieService.updateCategorie(id, categorie);

            log.info("Image uploadée pour la catégorie {}: {}", id, imagePath);
            return ResponseEntity.ok(imagePath);

        } catch (Exception e) {
            log.error("Erreur lors de l'upload de l'image de catégorie " + id, e);
            throw new RuntimeException("Erreur lors de l'upload de l'image : " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}/image")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Void> deleteCategorieImage(@PathVariable Long id) {
        try {
            CategorieDTO categorie = categorieService.getCategorieById(id);

            if (categorie.getImageCategorie() != null) {
                fileService.deleteFile(categorie.getImageCategorie());
                categorie.setImageCategorie(null);
                categorieService.updateCategorie(id, categorie);

                log.info("Image supprimée pour la catégorie {}", id);
            }

            return ResponseEntity.ok().build();

        } catch (Exception e) {
            log.error("Erreur lors de la suppression de l'image de catégorie", e);
            throw new RuntimeException("Erreur lors de la suppression de l'image : " + e.getMessage());
        }
    }
}