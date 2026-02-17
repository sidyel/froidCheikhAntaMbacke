package com.froidcheikh.ecommerce.controller;

import com.froidcheikh.ecommerce.dto.ProduitDTO;
import com.froidcheikh.ecommerce.service.ProduitService;
import com.froidcheikh.ecommerce.service.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/produits")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class ProduitController {

    private final ProduitService produitService;
    private final FileService fileService;

    @GetMapping
    public ResponseEntity<Page<ProduitDTO>> getAllProduits(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "dateAjout") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("desc") ?
                Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<ProduitDTO> produits = produitService.getAllProduits(pageable);
        return ResponseEntity.ok(produits);
    }

    @GetMapping("/admin/produits")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'GESTIONNAIRE')")
    public ResponseEntity<Page<ProduitDTO>> getAllProduitsAdmin(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long categorieId,
            @RequestParam(required = false) Long marqueId,
            @RequestParam(required = false) String stockFilter,
            @RequestParam(defaultValue = "dateAjout") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("desc") ?
                Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        // Récupérer TOUS les produits (disponibles ET non disponibles)
        Page<ProduitDTO> produits = produitService.getAllProduitsAdmin(
                search, categorieId, marqueId, stockFilter, pageable);

        return ResponseEntity.ok(produits);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProduitDTO> getProduitById(@PathVariable Long id) {
        ProduitDTO produit = produitService.getProduitById(id);
        return ResponseEntity.ok(produit);
    }

    @GetMapping("/ref/{refProduit}")
    public ResponseEntity<ProduitDTO> getProduitByRef(@PathVariable String refProduit) {
        ProduitDTO produit = produitService.getProduitByRef(refProduit);
        return ResponseEntity.ok(produit);
    }

    @GetMapping("/categorie/{categorieId}")
    public ResponseEntity<Page<ProduitDTO>> getProduitsByCategorie(
            @PathVariable Long categorieId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<ProduitDTO> produits = produitService.getProduitsByCategorie(categorieId, pageable);
        return ResponseEntity.ok(produits);
    }

    @GetMapping("/marque/{marqueId}")
    public ResponseEntity<Page<ProduitDTO>> getProduitsByMarque(
            @PathVariable Long marqueId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<ProduitDTO> produits = produitService.getProduitsByMarque(marqueId, pageable);
        return ResponseEntity.ok(produits);
    }

    @GetMapping("/search")
    public ResponseEntity<Page<ProduitDTO>> searchProduits(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<ProduitDTO> produits = produitService.searchProduits(q, pageable);
        return ResponseEntity.ok(produits);
    }

    @GetMapping("/filter")
    public ResponseEntity<Page<ProduitDTO>> getProduitsWithFilters(
            @RequestParam(required = false) String nom,
            @RequestParam(required = false) BigDecimal prixMin,
            @RequestParam(required = false) BigDecimal prixMax,
            @RequestParam(required = false) Long categorieId,
            @RequestParam(required = false) Long marqueId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "prix") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("desc") ?
                Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<ProduitDTO> produits = produitService.getProduitsWithFilters(
                nom, prixMin, prixMax, categorieId, marqueId, pageable);
        return ResponseEntity.ok(produits);
    }

    @GetMapping("/latest")
    public ResponseEntity<Page<ProduitDTO>> getLatestProduits(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "8") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<ProduitDTO> produits = produitService.getLatestProduits(pageable);
        return ResponseEntity.ok(produits);
    }

    @PostMapping("/batch")
    public ResponseEntity<List<ProduitDTO>> getProduitsById(@RequestBody List<Long> ids) {
        List<ProduitDTO> produits = produitService.getProduitsById(ids);
        return ResponseEntity.ok(produits);
    }

    // Endpoints administrateur
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'GESTIONNAIRE')")
    public ResponseEntity<ProduitDTO> createProduit(@Valid @RequestBody ProduitDTO produitDTO) {
        ProduitDTO produit = produitService.createProduit(produitDTO);
        return ResponseEntity.ok(produit);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'GESTIONNAIRE')")
    public ResponseEntity<ProduitDTO> updateProduit(
            @PathVariable Long id,
            @Valid @RequestBody ProduitDTO produitDTO) {
        ProduitDTO produit = produitService.updateProduit(id, produitDTO);
        return ResponseEntity.ok(produit);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Void> deleteProduit(@PathVariable Long id) {
        produitService.deleteProduit(id);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}/stock")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'GESTIONNAIRE')")
    public ResponseEntity<Void> updateStock(
            @PathVariable Long id,
            @RequestParam Integer quantite) {
        produitService.updateStock(id, quantite);
        return ResponseEntity.ok().build();
    }

  /*  @PostMapping("/{id}/images")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'GESTIONNAIRE')")
    public ResponseEntity<List<String>> uploadImages(
            @PathVariable Long id,
            @RequestParam("images") MultipartFile[] files) {

        try {
            List<String> imageUrls = new ArrayList<>();

            for (MultipartFile file : files) {
                if (!fileService.isValidImageFile(file)) {
                    throw new RuntimeException("Format d'image non valide : " + file.getOriginalFilename());
                }

                String imageUrl = fileService.uploadFile(file, "produits/images");
                imageUrls.add(imageUrl);
            }

            return ResponseEntity.ok(imageUrls);
        } catch (Exception e) {
            log.error("Erreur lors de l'upload des images", e);
            throw new RuntimeException("Erreur lors de l'upload des images : " + e.getMessage());
        }
    }

    @PostMapping("/{id}/fiche-technique")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'GESTIONNAIRE')")
    public ResponseEntity<String> uploadFicheTechnique(
            @PathVariable Long id,
            @RequestParam("ficheTechnique") MultipartFile file) {

        try {
            if (!fileService.isValidPdfFile(file)) {
                throw new RuntimeException("Le fichier doit être au format PDF");
            }

            String pdfUrl = fileService.uploadFile(file, "produits/fiches-techniques");
            return ResponseEntity.ok(pdfUrl);
        } catch (Exception e) {
            log.error("Erreur lors de l'upload de la fiche technique", e);
            throw new RuntimeException("Erreur lors de l'upload de la fiche technique : " + e.getMessage());
        }
    }*/

    @PostMapping("/{id}/images")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'GESTIONNAIRE')")
    public ResponseEntity<List<String>> uploadImages(
            @PathVariable Long id,
            @RequestParam("images") MultipartFile[] files) {

        try {
            log.info("🔄 Upload d'images pour le produit ID: {}", id);

            // Vérifier que le produit existe
            ProduitDTO produit = produitService.getProduitById(id);
            log.info("Produit trouvé: {}", produit.getNomProduit());

            List<String> imagePaths = new ArrayList<>();

            for (MultipartFile file : files) {
                log.info("Traitement du fichier: {} ({})", file.getOriginalFilename(), file.getSize());

                if (!fileService.isValidImageFile(file)) {
                    throw new RuntimeException("Format d'image non valide : " + file.getOriginalFilename());
                }

                // Upload du fichier et récupération du chemin relatif
                String imagePath = fileService.uploadFile(file, "produits/images");
                imagePaths.add(imagePath);
                log.info("✅ Fichier uploadé: {}", imagePath);
            }

            // CORRECTION: Ajouter les chemins d'images au produit EXISTANT
            List<String> currentImages = produit.getListeImages() != null
                    ? new ArrayList<>(produit.getListeImages())
                    : new ArrayList<>();

            currentImages.addAll(imagePaths);
            produit.setListeImages(currentImages);

            // IMPORTANT: Mettre à jour le produit avec les nouvelles images
            produitService.updateProduit(id, produit);
            log.info("✅ Produit mis à jour avec {} images total", currentImages.size());

            return ResponseEntity.ok(imagePaths);

        } catch (Exception e) {
            log.error("❌ Erreur lors de l'upload des images pour le produit {}: {}", id, e.getMessage(), e);
            throw new RuntimeException("Erreur lors de l'upload des images : " + e.getMessage());
        }
    }


    @PostMapping("/{id}/fiche-technique")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'GESTIONNAIRE')")
    public ResponseEntity<String> uploadFicheTechnique(
            @PathVariable Long id,
            @RequestParam("ficheTechnique") MultipartFile file) {

        try {
            // Vérifier que le produit existe
            ProduitDTO produit = produitService.getProduitById(id);

            if (!fileService.isValidPdfFile(file)) {
                throw new RuntimeException("Le fichier doit être au format PDF");
            }

            // Upload du fichier PDF
            String pdfPath = fileService.uploadFile(file, "produits/fiches-techniques");

            // Mettre à jour le produit avec la fiche technique
            produit.setFicheTechniquePDF(pdfPath);
            produitService.updateProduit(id, produit);

            log.info("Fiche technique uploadée pour le produit {}: {}", id, pdfPath);
            return ResponseEntity.ok(pdfPath);

        } catch (Exception e) {
            log.error("Erreur lors de l'upload de la fiche technique pour le produit " + id, e);
            throw new RuntimeException("Erreur lors de l'upload de la fiche technique : " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}/images/{imageIndex}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'GESTIONNAIRE')")
    public ResponseEntity<Void> deleteImage(
            @PathVariable Long id,
            @PathVariable int imageIndex) {

        try {
            ProduitDTO produit = produitService.getProduitById(id);

            if (produit.getListeImages() != null && imageIndex >= 0 && imageIndex < produit.getListeImages().size()) {
                String imageToDelete = produit.getListeImages().get(imageIndex);

                // Supprimer le fichier physique
                fileService.deleteFile(imageToDelete);

                // Mettre à jour la liste des images
                List<String> updatedImages = new ArrayList<>(produit.getListeImages());
                updatedImages.remove(imageIndex);
                produit.setListeImages(updatedImages);

                produitService.updateProduit(id, produit);

                log.info("Image supprimée du produit {}: {}", id, imageToDelete);
            }

            return ResponseEntity.ok().build();

        } catch (Exception e) {
            log.error("Erreur lors de la suppression de l'image", e);
            throw new RuntimeException("Erreur lors de la suppression de l'image : " + e.getMessage());
        }
    }
}