package com.froidcheikh.ecommerce.service;

import com.froidcheikh.ecommerce.dto.ContenuDTO;
import com.froidcheikh.ecommerce.entity.MembreEquipe;
import com.froidcheikh.ecommerce.repository.MembreEquipeRepository;
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
public class MembreEquipeService {

    private final MembreEquipeRepository repository;
    private final FileService fileService;

    @Cacheable("equipe")
    public List<ContenuDTO.MembreEquipeDTO> getPublic() {
        return repository.findAllByActifTrueOrderByOrdreAsc().stream()
                .map(this::toDTO).toList();
    }

    public List<ContenuDTO.MembreEquipeDTO> getAll() {
        return repository.findAll(Sort.by("ordre")).stream()
                .map(this::toDTO).toList();
    }

    @Transactional
    @CacheEvict(value = "equipe", allEntries = true)
    public ContenuDTO.MembreEquipeDTO create(ContenuDTO.MembreEquipeDTO dto) {
        MembreEquipe m = new MembreEquipe();
        applyDTO(m, dto);
        return toDTO(repository.save(m));
    }

    @Transactional
    @CacheEvict(value = "equipe", allEntries = true)
    public ContenuDTO.MembreEquipeDTO update(Long id, ContenuDTO.MembreEquipeDTO dto) {
        MembreEquipe m = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Membre non trouvé : " + id));
        applyDTO(m, dto);
        return toDTO(repository.save(m));
    }

    @Transactional
    @CacheEvict(value = "equipe", allEntries = true)
    public void delete(Long id) {
        MembreEquipe m = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Membre non trouvé : " + id));
        if (m.getUrlPhoto() != null && m.getUrlPhoto().contains("cloudinary")) {
            try { fileService.deleteFile(m.getUrlPhoto()); } catch (Exception ignored) {}
        }
        repository.delete(m);
    }

    @Transactional
    @CacheEvict(value = "equipe", allEntries = true)
    public ContenuDTO.MembreEquipeDTO uploadPhoto(Long id, MultipartFile file) {
        MembreEquipe m = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Membre non trouvé : " + id));
        if (m.getUrlPhoto() != null && m.getUrlPhoto().contains("cloudinary")) {
            try { fileService.deleteFile(m.getUrlPhoto()); } catch (Exception ignored) {}
        }
        m.setUrlPhoto(fileService.uploadFile(file, "contenu/equipe"));
        return toDTO(repository.save(m));
    }

    @Transactional
    @CacheEvict(value = "equipe", allEntries = true)
    public void updateOrdre(List<ContenuDTO.OrdreUpdateRequest.OrdreItem> items) {
        items.forEach(item -> repository.findById(item.getId()).ifPresent(m -> {
            m.setOrdre(item.getOrdre());
            repository.save(m);
        }));
    }

    private void applyDTO(MembreEquipe m, ContenuDTO.MembreEquipeDTO dto) {
        if (dto.getNom() != null) m.setNom(dto.getNom());
        if (dto.getPoste() != null) m.setPoste(dto.getPoste());
        if (dto.getExperience() != null) m.setExperience(dto.getExperience());
        if (dto.getBio() != null) m.setBio(dto.getBio());
        if (dto.getUrlPhoto() != null) m.setUrlPhoto(dto.getUrlPhoto());
        if (dto.getCompetences() != null) {
            m.getCompetences().clear();
            m.getCompetences().addAll(dto.getCompetences());
        }
        if (dto.getUrlLinkedin() != null) m.setUrlLinkedin(dto.getUrlLinkedin());
        if (dto.getEmailMembre() != null) m.setEmailMembre(dto.getEmailMembre());
        if (dto.getOrdre() != null) m.setOrdre(dto.getOrdre());
        if (dto.getActif() != null) m.setActif(dto.getActif());
    }

    private ContenuDTO.MembreEquipeDTO toDTO(MembreEquipe m) {
        ContenuDTO.MembreEquipeDTO dto = new ContenuDTO.MembreEquipeDTO();
        dto.setId(m.getId());
        dto.setNom(m.getNom());
        dto.setPoste(m.getPoste());
        dto.setExperience(m.getExperience());
        dto.setBio(m.getBio());
        dto.setUrlPhoto(m.getUrlPhoto());
        dto.setCompetences(new ArrayList<>(m.getCompetences()));
        dto.setUrlLinkedin(m.getUrlLinkedin());
        dto.setEmailMembre(m.getEmailMembre());
        dto.setOrdre(m.getOrdre());
        dto.setActif(m.getActif());
        return dto;
    }
}
