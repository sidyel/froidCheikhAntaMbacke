package com.froidcheikh.ecommerce.controller;

import com.froidcheikh.ecommerce.dto.ContenuDTO;
import com.froidcheikh.ecommerce.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@CrossOrigin
public class ContenuController {

    private final ImageHeroService imageHeroService;
    private final PartenaireService partenaireService;
    private final PrestationService prestationService;
    private final MembreEquipeService membreEquipeService;
    private final RealisationService realisationService;

    // ===== IMAGE HERO =====

    @GetMapping("/api/contenu/hero")
    public List<ContenuDTO.ImageHeroDTO> getHeroImages() {
        return imageHeroService.getPublic();
    }

    @GetMapping("/api/admin/contenu/hero")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public List<ContenuDTO.ImageHeroDTO> getAllHeroImages() {
        return imageHeroService.getAll();
    }

    @PostMapping("/api/admin/contenu/hero")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ContenuDTO.ImageHeroDTO createHero(@RequestBody ContenuDTO.ImageHeroDTO dto) {
        return imageHeroService.create(dto);
    }

    @PutMapping("/api/admin/contenu/hero/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ContenuDTO.ImageHeroDTO updateHero(@PathVariable Long id, @RequestBody ContenuDTO.ImageHeroDTO dto) {
        return imageHeroService.update(id, dto);
    }

    @DeleteMapping("/api/admin/contenu/hero/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public void deleteHero(@PathVariable Long id) {
        imageHeroService.delete(id);
    }

    @PostMapping("/api/admin/contenu/hero/{id}/image")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ContenuDTO.ImageHeroDTO uploadHeroImage(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        return imageHeroService.uploadImage(id, file);
    }

    @PutMapping("/api/admin/contenu/hero/ordre")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public void updateHeroOrdre(@RequestBody ContenuDTO.OrdreUpdateRequest req) {
        imageHeroService.updateOrdre(req.getItems());
    }

    // ===== PARTENAIRES =====

    @GetMapping("/api/contenu/partenaires")
    public List<ContenuDTO.PartenaireDTO> getPartenaires() {
        return partenaireService.getPublic();
    }

    @GetMapping("/api/admin/contenu/partenaires")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public List<ContenuDTO.PartenaireDTO> getAllPartenaires() {
        return partenaireService.getAll();
    }

    @PostMapping("/api/admin/contenu/partenaires")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ContenuDTO.PartenaireDTO createPartenaire(@RequestBody ContenuDTO.PartenaireDTO dto) {
        return partenaireService.create(dto);
    }

    @PutMapping("/api/admin/contenu/partenaires/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ContenuDTO.PartenaireDTO updatePartenaire(@PathVariable Long id, @RequestBody ContenuDTO.PartenaireDTO dto) {
        return partenaireService.update(id, dto);
    }

    @DeleteMapping("/api/admin/contenu/partenaires/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public void deletePartenaire(@PathVariable Long id) {
        partenaireService.delete(id);
    }

    @PostMapping("/api/admin/contenu/partenaires/{id}/image")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ContenuDTO.PartenaireDTO uploadLogoPartenaire(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        return partenaireService.uploadLogo(id, file);
    }

    @PutMapping("/api/admin/contenu/partenaires/ordre")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public void updatePartenairesOrdre(@RequestBody ContenuDTO.OrdreUpdateRequest req) {
        partenaireService.updateOrdre(req.getItems());
    }

    // ===== PRESTATIONS =====

    @GetMapping("/api/contenu/prestations")
    public List<ContenuDTO.PrestationDTO> getPrestations() {
        return prestationService.getPublic();
    }

    @GetMapping("/api/admin/contenu/prestations")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public List<ContenuDTO.PrestationDTO> getAllPrestations() {
        return prestationService.getAll();
    }

    @PostMapping("/api/admin/contenu/prestations")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ContenuDTO.PrestationDTO createPrestation(@RequestBody ContenuDTO.PrestationDTO dto) {
        return prestationService.create(dto);
    }

    @PutMapping("/api/admin/contenu/prestations/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ContenuDTO.PrestationDTO updatePrestation(@PathVariable Long id, @RequestBody ContenuDTO.PrestationDTO dto) {
        return prestationService.update(id, dto);
    }

    @DeleteMapping("/api/admin/contenu/prestations/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public void deletePrestation(@PathVariable Long id) {
        prestationService.delete(id);
    }

    @PostMapping("/api/admin/contenu/prestations/{id}/image")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ContenuDTO.PrestationDTO uploadImagePrestation(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        return prestationService.uploadImage(id, file);
    }

    @PutMapping("/api/admin/contenu/prestations/ordre")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public void updatePrestationsOrdre(@RequestBody ContenuDTO.OrdreUpdateRequest req) {
        prestationService.updateOrdre(req.getItems());
    }

    // ===== EQUIPE =====

    @GetMapping("/api/contenu/equipe")
    public List<ContenuDTO.MembreEquipeDTO> getEquipe() {
        return membreEquipeService.getPublic();
    }

    @GetMapping("/api/admin/contenu/equipe")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public List<ContenuDTO.MembreEquipeDTO> getAllEquipe() {
        return membreEquipeService.getAll();
    }

    @PostMapping("/api/admin/contenu/equipe")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ContenuDTO.MembreEquipeDTO createMembre(@RequestBody ContenuDTO.MembreEquipeDTO dto) {
        return membreEquipeService.create(dto);
    }

    @PutMapping("/api/admin/contenu/equipe/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ContenuDTO.MembreEquipeDTO updateMembre(@PathVariable Long id, @RequestBody ContenuDTO.MembreEquipeDTO dto) {
        return membreEquipeService.update(id, dto);
    }

    @DeleteMapping("/api/admin/contenu/equipe/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public void deleteMembre(@PathVariable Long id) {
        membreEquipeService.delete(id);
    }

    @PostMapping("/api/admin/contenu/equipe/{id}/image")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ContenuDTO.MembreEquipeDTO uploadPhotoMembre(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        return membreEquipeService.uploadPhoto(id, file);
    }

    @PutMapping("/api/admin/contenu/equipe/ordre")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public void updateEquipeOrdre(@RequestBody ContenuDTO.OrdreUpdateRequest req) {
        membreEquipeService.updateOrdre(req.getItems());
    }

    // ===== REALISATIONS =====

    @GetMapping("/api/contenu/realisations")
    public List<ContenuDTO.RealisationDTO> getRealisations() {
        return realisationService.getPublic();
    }

    @GetMapping("/api/admin/contenu/realisations")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public List<ContenuDTO.RealisationDTO> getAllRealisations() {
        return realisationService.getAll();
    }

    @PostMapping("/api/admin/contenu/realisations")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ContenuDTO.RealisationDTO createRealisation(@RequestBody ContenuDTO.RealisationDTO dto) {
        return realisationService.create(dto);
    }

    @PutMapping("/api/admin/contenu/realisations/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ContenuDTO.RealisationDTO updateRealisation(@PathVariable Long id, @RequestBody ContenuDTO.RealisationDTO dto) {
        return realisationService.update(id, dto);
    }

    @DeleteMapping("/api/admin/contenu/realisations/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public void deleteRealisation(@PathVariable Long id) {
        realisationService.delete(id);
    }

    @PostMapping("/api/admin/contenu/realisations/{id}/image")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ContenuDTO.RealisationDTO addImageRealisation(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        return realisationService.addImage(id, file);
    }

    @PutMapping("/api/admin/contenu/realisations/ordre")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public void updateRealisationsOrdre(@RequestBody ContenuDTO.OrdreUpdateRequest req) {
        realisationService.updateOrdre(req.getItems());
    }
}
