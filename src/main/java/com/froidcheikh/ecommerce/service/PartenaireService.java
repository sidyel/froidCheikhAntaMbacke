package com.froidcheikh.ecommerce.service;

import com.froidcheikh.ecommerce.dto.ContenuDTO;
import com.froidcheikh.ecommerce.entity.Partenaire;
import com.froidcheikh.ecommerce.repository.PartenaireRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PartenaireService {

    private final PartenaireRepository repository;
    private final FileService fileService;

    @Cacheable("partenaires")
    public List<ContenuDTO.PartenaireDTO> getPublic() {
        return repository.findAllByActifTrueOrderByOrdreAsc().stream()
                .map(this::toDTO).toList();
    }

    public List<ContenuDTO.PartenaireDTO> getAll() {
        return repository.findAll(Sort.by("ordre")).stream()
                .map(this::toDTO).toList();
    }

    @Transactional
    @CacheEvict(value = "partenaires", allEntries = true)
    public ContenuDTO.PartenaireDTO create(ContenuDTO.PartenaireDTO dto) {
        Partenaire p = new Partenaire();
        applyDTO(p, dto);
        return toDTO(repository.save(p));
    }

    @Transactional
    @CacheEvict(value = "partenaires", allEntries = true)
    public ContenuDTO.PartenaireDTO update(Long id, ContenuDTO.PartenaireDTO dto) {
        Partenaire p = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Partenaire non trouvé : " + id));
        applyDTO(p, dto);
        return toDTO(repository.save(p));
    }

    @Transactional
    @CacheEvict(value = "partenaires", allEntries = true)
    public void delete(Long id) {
        Partenaire p = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Partenaire non trouvé : " + id));
        if (p.getUrlLogo() != null && p.getUrlLogo().contains("cloudinary")) {
            try { fileService.deleteFile(p.getUrlLogo()); } catch (Exception ignored) {}
        }
        repository.delete(p);
    }

    @Transactional
    @CacheEvict(value = "partenaires", allEntries = true)
    public ContenuDTO.PartenaireDTO uploadLogo(Long id, MultipartFile file) {
        Partenaire p = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Partenaire non trouvé : " + id));
        if (p.getUrlLogo() != null && p.getUrlLogo().contains("cloudinary")) {
            try { fileService.deleteFile(p.getUrlLogo()); } catch (Exception ignored) {}
        }
        p.setUrlLogo(fileService.uploadFile(file, "contenu/partenaires"));
        return toDTO(repository.save(p));
    }

    @Transactional
    @CacheEvict(value = "partenaires", allEntries = true)
    public void updateOrdre(List<ContenuDTO.OrdreUpdateRequest.OrdreItem> items) {
        items.forEach(item -> repository.findById(item.getId()).ifPresent(p -> {
            p.setOrdre(item.getOrdre());
            repository.save(p);
        }));
    }

    private void applyDTO(Partenaire p, ContenuDTO.PartenaireDTO dto) {
        if (dto.getNom() != null) p.setNom(dto.getNom());
        if (dto.getDescription() != null) p.setDescription(dto.getDescription());
        if (dto.getUrlLogo() != null) p.setUrlLogo(dto.getUrlLogo());
        if (dto.getUrlSite() != null) p.setUrlSite(dto.getUrlSite());
        if (dto.getOrdre() != null) p.setOrdre(dto.getOrdre());
        if (dto.getActif() != null) p.setActif(dto.getActif());
    }

    private ContenuDTO.PartenaireDTO toDTO(Partenaire p) {
        ContenuDTO.PartenaireDTO dto = new ContenuDTO.PartenaireDTO();
        dto.setId(p.getId());
        dto.setNom(p.getNom());
        dto.setDescription(p.getDescription());
        dto.setUrlLogo(p.getUrlLogo());
        dto.setUrlSite(p.getUrlSite());
        dto.setOrdre(p.getOrdre());
        dto.setActif(p.getActif());
        return dto;
    }
}
