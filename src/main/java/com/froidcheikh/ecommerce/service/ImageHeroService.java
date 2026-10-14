package com.froidcheikh.ecommerce.service;

import com.froidcheikh.ecommerce.dto.ContenuDTO;
import com.froidcheikh.ecommerce.entity.ImageHero;
import com.froidcheikh.ecommerce.repository.ImageHeroRepository;
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
public class ImageHeroService {

    private final ImageHeroRepository repository;
    private final FileService fileService;

    @Cacheable("heroImages")
    public List<ContenuDTO.ImageHeroDTO> getPublic() {
        return repository.findAllByActifTrueOrderByOrdreAsc().stream()
                .map(this::toDTO).toList();
    }

    public List<ContenuDTO.ImageHeroDTO> getAll() {
        return repository.findAll(Sort.by("ordre")).stream()
                .map(this::toDTO).toList();
    }

    @Transactional
    @CacheEvict(value = "heroImages", allEntries = true)
    public ContenuDTO.ImageHeroDTO create(ContenuDTO.ImageHeroDTO dto) {
        ImageHero h = new ImageHero();
        applyDTO(h, dto);
        return toDTO(repository.save(h));
    }

    @Transactional
    @CacheEvict(value = "heroImages", allEntries = true)
    public ContenuDTO.ImageHeroDTO update(Long id, ContenuDTO.ImageHeroDTO dto) {
        ImageHero h = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("ImageHero non trouvée : " + id));
        applyDTO(h, dto);
        return toDTO(repository.save(h));
    }

    @Transactional
    @CacheEvict(value = "heroImages", allEntries = true)
    public void delete(Long id) {
        ImageHero h = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("ImageHero non trouvée : " + id));
        if (h.getUrlImage() != null && h.getUrlImage().contains("cloudinary")) {
            try { fileService.deleteFile(h.getUrlImage()); } catch (Exception ignored) {}
        }
        repository.delete(h);
    }

    @Transactional
    @CacheEvict(value = "heroImages", allEntries = true)
    public ContenuDTO.ImageHeroDTO uploadImage(Long id, MultipartFile file) {
        ImageHero h = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("ImageHero non trouvée : " + id));
        if (h.getUrlImage() != null && h.getUrlImage().contains("cloudinary")) {
            try { fileService.deleteFile(h.getUrlImage()); } catch (Exception ignored) {}
        }
        h.setUrlImage(fileService.uploadFile(file, "contenu/hero"));
        return toDTO(repository.save(h));
    }

    @Transactional
    @CacheEvict(value = "heroImages", allEntries = true)
    public void updateOrdre(List<ContenuDTO.OrdreUpdateRequest.OrdreItem> items) {
        items.forEach(item -> repository.findById(item.getId()).ifPresent(h -> {
            h.setOrdre(item.getOrdre());
            repository.save(h);
        }));
    }

    private void applyDTO(ImageHero h, ContenuDTO.ImageHeroDTO dto) {
        if (dto.getUrlImage() != null) h.setUrlImage(dto.getUrlImage());
        if (dto.getAltText() != null) h.setAltText(dto.getAltText());
        if (dto.getTitre() != null) h.setTitre(dto.getTitre());
        if (dto.getSousTitre() != null) h.setSousTitre(dto.getSousTitre());
        if (dto.getLienCta() != null) h.setLienCta(dto.getLienCta());
        if (dto.getTexteCta() != null) h.setTexteCta(dto.getTexteCta());
        if (dto.getOrdre() != null) h.setOrdre(dto.getOrdre());
        if (dto.getActif() != null) h.setActif(dto.getActif());
    }

    private ContenuDTO.ImageHeroDTO toDTO(ImageHero h) {
        ContenuDTO.ImageHeroDTO dto = new ContenuDTO.ImageHeroDTO();
        dto.setId(h.getId());
        dto.setUrlImage(h.getUrlImage());
        dto.setAltText(h.getAltText());
        dto.setTitre(h.getTitre());
        dto.setSousTitre(h.getSousTitre());
        dto.setLienCta(h.getLienCta());
        dto.setTexteCta(h.getTexteCta());
        dto.setOrdre(h.getOrdre());
        dto.setActif(h.getActif());
        return dto;
    }
}
