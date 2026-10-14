package com.froidcheikh.ecommerce.controller;

import com.froidcheikh.ecommerce.dto.ProduitDTO.AttributProduitDTO;
import com.froidcheikh.ecommerce.service.AttributProduitService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/produits/{produitId}/attributs")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AttributProduitController {

    private final AttributProduitService attributProduitService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'GESTIONNAIRE')")
    public ResponseEntity<List<AttributProduitDTO>> findAll(@PathVariable Long produitId) {
        return ResponseEntity.ok(attributProduitService.findByProduit(produitId));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'GESTIONNAIRE')")
    public ResponseEntity<AttributProduitDTO> create(@PathVariable Long produitId,
                                                     @RequestBody AttributProduitDTO dto) {
        return ResponseEntity.ok(attributProduitService.create(produitId, dto));
    }

    @PutMapping("/{attrId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'GESTIONNAIRE')")
    public ResponseEntity<AttributProduitDTO> update(@PathVariable Long produitId,
                                                     @PathVariable Long attrId,
                                                     @RequestBody AttributProduitDTO dto) {
        return ResponseEntity.ok(attributProduitService.update(produitId, attrId, dto));
    }

    @DeleteMapping("/{attrId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'GESTIONNAIRE')")
    public ResponseEntity<Void> delete(@PathVariable Long produitId,
                                       @PathVariable Long attrId) {
        attributProduitService.delete(produitId, attrId);
        return ResponseEntity.noContent().build();
    }
}
