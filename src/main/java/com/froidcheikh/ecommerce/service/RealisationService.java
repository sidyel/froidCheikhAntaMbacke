package com.froidcheikh.ecommerce.service;

import com.froidcheikh.ecommerce.dto.ContenuDTO;
import com.froidcheikh.ecommerce.entity.Realisation;
import com.froidcheikh.ecommerce.repository.RealisationRepository;
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
public class RealisationService {

    private final RealisationRepository repository;
    private final FileService fileService;

    @Cacheable("realisations")
    public List<ContenuDTO.RealisationDTO> getPublic() {
        return repository.findAllByActifTrueOrderByOrdreAsc().stream()
                .map(this::toDTO).toList();
    }

    public List<ContenuDTO.RealisationDTO> getAll() {
        return repository.findAll(Sort.by("ordre")).stream()
                .map(this::toDTO).toList();
    }

    @Transactional
    @CacheEvict(value = "realisations", allEntries = true)
    public ContenuDTO.RealisationDTO create(ContenuDTO.RealisationDTO dto) {
        Realisation r = new Realisation();
        applyDTO(r, dto);
        return toDTO(repository.save(r));
    }

    @Transactional
    @CacheEvict(value = "realisations", allEntries = true)
    public ContenuDTO.RealisationDTO update(Long id, ContenuDTO.RealisationDTO dto) {
        Realisation r = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Réalisation non trouvée : " + id));
        applyDTO(r, dto);
        return toDTO(repository.save(r));
    }

    @Transactional
    @CacheEvict(value = "realisations", allEntries = true)
    public void delete(Long id) {
        Realisation r = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Réalisation non trouvée : " + id));
        r.getImages().stream()
                .filter(url -> url.contains("cloudinary"))
                .forEach(url -> { try { fileService.deleteFile(url); } catch (Exception ignored) {} });
        repository.delete(r);
    }

    @Transactional
    @CacheEvict(value = "realisations", allEntries = true)
    public ContenuDTO.RealisationDTO addImage(Long id, MultipartFile file) {
        Realisation r = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Réalisation non trouvée : " + id));
        r.getImages().add(fileService.uploadFile(file, "contenu/realisations"));
        return toDTO(repository.save(r));
    }

    @Transactional
    @CacheEvict(value = "realisations", allEntries = true)
    public void updateOrdre(List<ContenuDTO.OrdreUpdateRequest.OrdreItem> items) {
        items.forEach(item -> repository.findById(item.getId()).ifPresent(r -> {
            r.setOrdre(item.getOrdre());
            repository.save(r);
        }));
    }

    private void applyDTO(Realisation r, ContenuDTO.RealisationDTO dto) {
        if (dto.getTitre() != null) r.setTitre(dto.getTitre());
        if (dto.getCategorie() != null) r.setCategorie(dto.getCategorie());
        if (dto.getLocalisation() != null) r.setLocalisation(dto.getLocalisation());
        if (dto.getDescription() != null) r.setDescription(dto.getDescription());
        if (dto.getClientNom() != null) r.setClientNom(dto.getClientNom());
        if (dto.getAnnee() != null) r.setAnnee(dto.getAnnee());
        if (dto.getDuree() != null) r.setDuree(dto.getDuree());
        if (dto.getSurface() != null) r.setSurface(dto.getSurface());
        if (dto.getImages() != null) {
            r.getImages().clear();
            r.getImages().addAll(dto.getImages());
        }
        if (dto.getTags() != null) {
            r.getTags().clear();
            r.getTags().addAll(dto.getTags());
        }
        if (dto.getOrdre() != null) r.setOrdre(dto.getOrdre());
        if (dto.getActif() != null) r.setActif(dto.getActif());
    }

    private ContenuDTO.RealisationDTO toDTO(Realisation r) {
        ContenuDTO.RealisationDTO dto = new ContenuDTO.RealisationDTO();
        dto.setId(r.getId());
        dto.setTitre(r.getTitre());
        dto.setCategorie(r.getCategorie());
        dto.setLocalisation(r.getLocalisation());
        dto.setDescription(r.getDescription());
        dto.setClientNom(r.getClientNom());
        dto.setAnnee(r.getAnnee());
        dto.setDuree(r.getDuree());
        dto.setSurface(r.getSurface());
        dto.setImages(new ArrayList<>(r.getImages()));
        dto.setTags(new ArrayList<>(r.getTags()));
        dto.setOrdre(r.getOrdre());
        dto.setActif(r.getActif());
        return dto;
    }
}
