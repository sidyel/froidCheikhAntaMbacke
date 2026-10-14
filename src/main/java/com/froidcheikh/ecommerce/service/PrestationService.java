package com.froidcheikh.ecommerce.service;

import com.froidcheikh.ecommerce.dto.ContenuDTO;
import com.froidcheikh.ecommerce.entity.Prestation;
import com.froidcheikh.ecommerce.repository.PrestationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PrestationService {

    private final PrestationRepository repository;
    private final FileService fileService;

    @Cacheable("prestations")
    public List<ContenuDTO.PrestationDTO> getPublic() {
        return repository.findAllByActifTrueOrderByOrdreAsc().stream()
                .map(this::toDTO).toList();
    }

    public List<ContenuDTO.PrestationDTO> getAll() {
        return repository.findAll(Sort.by("ordre")).stream()
                .map(this::toDTO).toList();
    }

    @Transactional
    @CacheEvict(value = "prestations", allEntries = true)
    public ContenuDTO.PrestationDTO create(ContenuDTO.PrestationDTO dto) {
        Prestation p = new Prestation();
        applyDTO(p, dto);
        return toDTO(repository.save(p));
    }

    @Transactional
    @CacheEvict(value = "prestations", allEntries = true)
    public ContenuDTO.PrestationDTO update(Long id, ContenuDTO.PrestationDTO dto) {
        Prestation p = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Prestation non trouvée : " + id));
        applyDTO(p, dto);
        return toDTO(repository.save(p));
    }

    @Transactional
    @CacheEvict(value = "prestations", allEntries = true)
    public void delete(Long id) {
        Prestation p = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Prestation non trouvée : " + id));
        if (p.getUrlImage() != null && p.getUrlImage().contains("cloudinary")) {
            try { fileService.deleteFile(p.getUrlImage()); } catch (Exception ignored) {}
        }
        repository.delete(p);
    }

    @Transactional
    @CacheEvict(value = "prestations", allEntries = true)
    public ContenuDTO.PrestationDTO uploadImage(Long id, MultipartFile file) {
        Prestation p = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Prestation non trouvée : " + id));
        if (p.getUrlImage() != null && p.getUrlImage().contains("cloudinary")) {
            try { fileService.deleteFile(p.getUrlImage()); } catch (Exception ignored) {}
        }
        p.setUrlImage(fileService.uploadFile(file, "contenu/prestations"));
        return toDTO(repository.save(p));
    }

    @Transactional
    @CacheEvict(value = "prestations", allEntries = true)
    public void updateOrdre(List<ContenuDTO.OrdreUpdateRequest.OrdreItem> items) {
        items.forEach(item -> repository.findById(item.getId()).ifPresent(p -> {
            p.setOrdre(item.getOrdre());
            repository.save(p);
        }));
    }

    private void applyDTO(Prestation p, ContenuDTO.PrestationDTO dto) {
        if (dto.getTitre() != null) p.setTitre(dto.getTitre());
        if (dto.getDescription() != null) p.setDescription(dto.getDescription());
        if (dto.getUrlImage() != null) p.setUrlImage(dto.getUrlImage());
        if (dto.getCaracteristiques() != null) {
            p.getCaracteristiques().clear();
            p.getCaracteristiques().addAll(dto.getCaracteristiques());
        }
        if (dto.getOrdre() != null) p.setOrdre(dto.getOrdre());
        if (dto.getActif() != null) p.setActif(dto.getActif());
    }

    private ContenuDTO.PrestationDTO toDTO(Prestation p) {
        ContenuDTO.PrestationDTO dto = new ContenuDTO.PrestationDTO();
        dto.setId(p.getId());
        dto.setTitre(p.getTitre());
        dto.setDescription(p.getDescription());
        dto.setUrlImage(p.getUrlImage());
        dto.setCaracteristiques(new ArrayList<>(p.getCaracteristiques()));
        dto.setOrdre(p.getOrdre());
        dto.setActif(p.getActif());
        return dto;
    }
}
