package com.froidcheikh.ecommerce.controller;

import com.froidcheikh.ecommerce.dto.PromotionDTO;
import com.froidcheikh.ecommerce.service.PromotionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/promotions")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
public class PromotionController {

    private final PromotionService promotionService;

    @GetMapping("/actives")
    public ResponseEntity<Page<PromotionDTO.Response>> listerActives(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(promotionService.listerActives(
                PageRequest.of(page, size, Sort.by("priorite").descending())));
    }

    @GetMapping("/programmees")
    public ResponseEntity<Page<PromotionDTO.Response>> listerProgrammees(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(promotionService.listerProgrammees(
                PageRequest.of(page, size, Sort.by("dateDebut").ascending())));
    }

    @GetMapping("/expirees")
    public ResponseEntity<Page<PromotionDTO.Response>> listerExpirees(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(promotionService.listerExpirees(
                PageRequest.of(page, size, Sort.by("dateFin").descending())));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PromotionDTO.Response> getById(@PathVariable Long id) {
        return ResponseEntity.ok(promotionService.getById(id));
    }

    @PostMapping
    public ResponseEntity<PromotionDTO.Response> creer(@Valid @RequestBody PromotionDTO.Request dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(promotionService.creer(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PromotionDTO.Response> modifier(
            @PathVariable Long id,
            @Valid @RequestBody PromotionDTO.Request dto) {
        return ResponseEntity.ok(promotionService.modifier(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> supprimer(@PathVariable Long id) {
        promotionService.supprimer(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/apercu")
    public ResponseEntity<PromotionDTO.ApercuPage> apercu(
            @Valid @RequestBody PromotionDTO.Request dto,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(promotionService.apercu(dto, page, size));
    }
}
