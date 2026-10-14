package com.froidcheikh.ecommerce.controller;

import com.froidcheikh.ecommerce.dto.ModeleAttributDTO;
import com.froidcheikh.ecommerce.service.ModeleAttributService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Route publique — utilisée par le formulaire admin côté frontend (sans auth)
 * et par les futurs comparateurs produits.
 */
@RestController
@RequestMapping("/api/modeles-attributs")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ModeleAttributPublicController {

    private final ModeleAttributService modeleAttributService;

    @GetMapping("/categorie/{categorieId}")
    public ResponseEntity<List<ModeleAttributDTO>> findByCategorie(@PathVariable Long categorieId) {
        return ResponseEntity.ok(modeleAttributService.findByCategorie(categorieId));
    }
}
