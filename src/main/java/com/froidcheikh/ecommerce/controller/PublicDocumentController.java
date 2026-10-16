package com.froidcheikh.ecommerce.controller;

import com.froidcheikh.ecommerce.service.JetonTelechargementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Endpoint public pour le téléchargement de documents via jeton WhatsApp.
 * Pas de JWT requis — le jeton UUID est la preuve d'accès.
 */
@RestController
@RequestMapping("/api/public/documents")
@RequiredArgsConstructor
public class PublicDocumentController {

    private final JetonTelechargementService jetonService;

    @GetMapping("/telecharger/{jeton}")
    public ResponseEntity<byte[]> telecharger(@PathVariable String jeton) {
        byte[] pdf = jetonService.telechargerParJeton(jeton);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"document.pdf\"")
                .body(pdf);
    }
}
