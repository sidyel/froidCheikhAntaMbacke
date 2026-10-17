package com.froidcheikh.ecommerce.controller;

import com.froidcheikh.ecommerce.dto.DocumentFacturationDTO;
import com.froidcheikh.ecommerce.entity.CompteurDocument.TypeDocument;
import com.froidcheikh.ecommerce.service.DocumentFacturationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/commandes")
@RequiredArgsConstructor
public class AdminCommandeFacturationController {

    private final DocumentFacturationService documentService;

    @PostMapping("/{commandeId}/facture")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','GESTIONNAIRE')")
    public ResponseEntity<DocumentFacturationDTO.DocumentResponse> creerFacture(
            @PathVariable Long commandeId,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(
                documentService.creerDepuisCommande(commandeId, TypeDocument.FACTURE, user.getUsername()));
    }

    @PostMapping("/{commandeId}/bon-livraison")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','GESTIONNAIRE')")
    public ResponseEntity<DocumentFacturationDTO.DocumentResponse> creerBonLivraison(
            @PathVariable Long commandeId,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(
                documentService.creerDepuisCommande(commandeId, TypeDocument.BON_LIVRAISON, user.getUsername()));
    }
}
