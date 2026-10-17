package com.froidcheikh.ecommerce.controller;

import com.froidcheikh.ecommerce.dto.DocumentFacturationDTO;
import com.froidcheikh.ecommerce.entity.CompteurDocument.TypeDocument;
import com.froidcheikh.ecommerce.entity.DocumentFacturation.StatutDocument;
import com.froidcheikh.ecommerce.service.DocumentFacturationService;
import com.froidcheikh.ecommerce.service.JetonTelechargementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/facturation")
@RequiredArgsConstructor
public class DocumentFacturationController {

    private final DocumentFacturationService documentService;
    private final JetonTelechargementService jetonService;

    // ---- LISTE ----

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','GESTIONNAIRE')")
    public Page<DocumentFacturationDTO.DocumentResume> lister(
            @RequestParam(required = false) TypeDocument type,
            @RequestParam(required = false) StatutDocument statut,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return documentService.lister(type, statut, q,
                PageRequest.of(page, size, Sort.by("dateCreation").descending()));
    }

    // ---- DÉTAIL ----

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','GESTIONNAIRE')")
    public DocumentFacturationDTO.DocumentResponse getById(@PathVariable Long id) {
        return documentService.getById(id);
    }

    // ---- BROUILLON ----

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','GESTIONNAIRE')")
    public ResponseEntity<DocumentFacturationDTO.DocumentResponse> creer(
            @Valid @RequestBody DocumentFacturationDTO.CreateRequest req,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(documentService.creerBrouillon(req, user.getUsername()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','GESTIONNAIRE')")
    public DocumentFacturationDTO.DocumentResponse modifier(
            @PathVariable Long id,
            @Valid @RequestBody DocumentFacturationDTO.CreateRequest req,
            @AuthenticationPrincipal UserDetails user) {
        return documentService.modifierBrouillon(id, req, user.getUsername());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<Void> supprimer(@PathVariable Long id) {
        documentService.supprimerBrouillon(id);
        return ResponseEntity.noContent().build();
    }

    // ---- ÉMISSION ----

    @PostMapping("/{id}/emettre")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','GESTIONNAIRE')")
    public DocumentFacturationDTO.DocumentResponse emettre(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails user) {
        boolean estGestionnaire = user.getAuthorities().stream()
                .anyMatch(a -> "ROLE_GESTIONNAIRE".equals(a.getAuthority()));
        return documentService.emettre(id, user.getUsername(), estGestionnaire);
    }

    // ---- PDF ----

    @GetMapping("/{id}/pdf")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','GESTIONNAIRE')")
    public ResponseEntity<byte[]> telechargerPdf(@PathVariable Long id) {
        byte[] pdf = documentService.telechargerPdf(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"document-" + id + ".pdf\"")
                .body(pdf);
    }

    @GetMapping("/{id}/pdf/brouillon")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','GESTIONNAIRE')")
    public ResponseEntity<byte[]> previsualiser(@PathVariable Long id) {
        byte[] pdf = documentService.previsualiserBrouillon(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"brouillon-" + id + ".pdf\"")
                .body(pdf);
    }

    // ---- AVOIR ----

    @PostMapping("/avoirs")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public DocumentFacturationDTO.DocumentResponse creerAvoir(
            @Valid @RequestBody DocumentFacturationDTO.AvoirRequest req,
            @AuthenticationPrincipal UserDetails user) {
        return documentService.creerAvoir(req, user.getUsername());
    }

    /** Avoir total sur toutes les lignes de la facture (appelé depuis la fiche document). */
    @PostMapping("/{id}/avoir")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public DocumentFacturationDTO.DocumentResponse creerAvoirTotal(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails user) {
        return documentService.creerAvoirTotal(id, user.getUsername());
    }

    // ---- CONVERSION DEVIS ----

    @PostMapping("/{id}/convertir")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public DocumentFacturationDTO.DocumentResponse convertir(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails user) {
        return documentService.convertirDevisEnFacture(id, user.getUsername());
    }

    // ---- RÈGLEMENTS ----

    @PostMapping("/{id}/reglements")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public DocumentFacturationDTO.DocumentResponse ajouterReglement(
            @PathVariable Long id,
            @Valid @RequestBody DocumentFacturationDTO.ReglementRequest req,
            @AuthenticationPrincipal UserDetails user) {
        return documentService.ajouterReglement(id, req, user.getUsername());
    }

    @DeleteMapping("/{id}/reglements/{reglementId}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public DocumentFacturationDTO.DocumentResponse supprimerReglement(
            @PathVariable Long id,
            @PathVariable Long reglementId,
            @AuthenticationPrincipal UserDetails user) {
        return documentService.supprimerReglement(id, reglementId, user.getUsername());
    }

    // ---- JETON WHATSAPP ----

    @PostMapping("/{id}/jeton")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','GESTIONNAIRE')")
    public DocumentFacturationDTO.JetonResponse genererJeton(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails user) {
        return documentService.genererJeton(id, user.getUsername());
    }

    @DeleteMapping("/jetons/{jeton}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<Void> revoquerJeton(
            @PathVariable String jeton,
            @AuthenticationPrincipal UserDetails user) {
        jetonService.revoquer(jeton, user.getUsername());
        return ResponseEntity.noContent().build();
    }

    /** Aperçu PDF de l'en-tête/pied de page avec les paramètres actuels. Aucun document persisté. */
    @GetMapping("/apercu-entete")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<byte[]> apercuEnTete() {
        byte[] pdf = documentService.genererApercuEnTete();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"apercu-entete.pdf\"")
                .body(pdf);
    }
}
