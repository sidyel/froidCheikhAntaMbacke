package com.froidcheikh.ecommerce.controller;

import com.froidcheikh.ecommerce.dto.ProduitDTO;
import com.froidcheikh.ecommerce.service.ProduitService;
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
import java.util.List;

@RestController
@RequestMapping("/api/produits")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class ProduitController {

    private final ProduitService produitService;

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
            @RequestParam(required = false) Boolean enPromotion,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "prix") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("desc") ?
                Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<ProduitDTO> produits = produitService.getProduitsWithFilters(
                nom, prixMin, prixMax, categorieId, marqueId, enPromotion, pageable);
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

    @PostMapping("/{id}/images")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'GESTIONNAIRE')")
    public ResponseEntity<List<ProduitDTO.ImageProduitDTO>> uploadImages(
            @PathVariable Long id,
            @RequestParam("images") MultipartFile[] files) {
        log.info("🔄 Upload d'images pour le produit ID: {}", id);
        List<ProduitDTO.ImageProduitDTO> result = produitService.uploadImages(id, files);
        return ResponseEntity.ok(result);
    }


    @PostMapping("/{id}/fiche-technique")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'GESTIONNAIRE')")
    public ResponseEntity<String> uploadFicheTechnique(
            @PathVariable Long id,
            @RequestParam("ficheTechnique") MultipartFile file) {

        String pdfPath = produitService.uploadFicheTechnique(id, file);
        return ResponseEntity.ok(pdfPath);
    }

    @DeleteMapping("/{id}/images/{imageId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'GESTIONNAIRE')")
    public ResponseEntity<Void> deleteImage(
            @PathVariable Long id,
            @PathVariable Long imageId) {
        produitService.supprimerImage(id, imageId);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}/images/{imageId}/principale")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'GESTIONNAIRE')")
    public ResponseEntity<ProduitDTO.ImageProduitDTO> definirImagePrincipale(
            @PathVariable Long id,
            @PathVariable Long imageId) {
        return ResponseEntity.ok(produitService.definirImagePrincipale(id, imageId));
    }
}