package com.froidcheikh.ecommerce.controller;

import com.froidcheikh.ecommerce.dto.ModeleAttributDTO;
import com.froidcheikh.ecommerce.service.ModeleAttributService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/modeles-attributs")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ModeleAttributController {

    private final ModeleAttributService modeleAttributService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'GESTIONNAIRE')")
    public ResponseEntity<List<ModeleAttributDTO>> findAll() {
        return ResponseEntity.ok(modeleAttributService.findAll());
    }

    @GetMapping("/categorie/{categorieId}")
    public ResponseEntity<List<ModeleAttributDTO>> findByCategorie(@PathVariable Long categorieId) {
        return ResponseEntity.ok(modeleAttributService.findByCategorie(categorieId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'GESTIONNAIRE')")
    public ResponseEntity<ModeleAttributDTO> findById(@PathVariable Long id) {
        return ResponseEntity.ok(modeleAttributService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ModeleAttributDTO> create(@Valid @RequestBody ModeleAttributDTO dto) {
        return ResponseEntity.ok(modeleAttributService.create(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ModeleAttributDTO> update(@PathVariable Long id,
                                                    @Valid @RequestBody ModeleAttributDTO dto) {
        return ResponseEntity.ok(modeleAttributService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        modeleAttributService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
