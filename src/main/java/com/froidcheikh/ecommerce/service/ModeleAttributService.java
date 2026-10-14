package com.froidcheikh.ecommerce.service;

import com.froidcheikh.ecommerce.dto.ModeleAttributDTO;
import com.froidcheikh.ecommerce.entity.Categorie;
import com.froidcheikh.ecommerce.entity.ModeleAttribut;
import com.froidcheikh.ecommerce.exception.ResourceNotFoundException;
import com.froidcheikh.ecommerce.repository.CategorieRepository;
import com.froidcheikh.ecommerce.repository.ModeleAttributRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ModeleAttributService {

    private final ModeleAttributRepository modeleAttributRepository;
    private final CategorieRepository categorieRepository;

    public List<ModeleAttributDTO> findAll() {
        return modeleAttributRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public List<ModeleAttributDTO> findByCategorie(Long categorieId) {
        return modeleAttributRepository
                .findByCategoriesIdCategorieOrderByOrdreAsc(categorieId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public ModeleAttributDTO findById(Long id) {
        return toDTO(getEntity(id));
    }

    @Transactional
    public ModeleAttributDTO create(ModeleAttributDTO dto) {
        ModeleAttribut entity = new ModeleAttribut();
        applyDTO(dto, entity);
        return toDTO(modeleAttributRepository.save(entity));
    }

    @Transactional
    public ModeleAttributDTO update(Long id, ModeleAttributDTO dto) {
        ModeleAttribut entity = getEntity(id);
        applyDTO(dto, entity);
        return toDTO(modeleAttributRepository.save(entity));
    }

    @Transactional
    public void delete(Long id) {
        getEntity(id);
        modeleAttributRepository.deleteById(id);
    }

    private ModeleAttribut getEntity(Long id) {
        return modeleAttributRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ModeleAttribut non trouvé : " + id));
    }

    private void applyDTO(ModeleAttributDTO dto, ModeleAttribut entity) {
        entity.setNomAttribut(dto.getNomAttribut());
        entity.setDescription(dto.getDescription());
        entity.setTypeValeur(dto.getTypeValeur());
        entity.setValeursAutorisees(dto.getValeursAutorisees());
        entity.setUnite(dto.getUnite());
        entity.setObligatoire(dto.isObligatoire());
        entity.setOrdre(dto.getOrdre());

        Set<Categorie> categories = new HashSet<>();
        if (dto.getCategorieIds() != null) {
            for (Long catId : dto.getCategorieIds()) {
                categorieRepository.findById(catId).ifPresent(categories::add);
            }
        }
        entity.setCategories(categories);
    }

    public ModeleAttributDTO toDTO(ModeleAttribut entity) {
        ModeleAttributDTO dto = new ModeleAttributDTO();
        dto.setIdModele(entity.getIdModele());
        dto.setNomAttribut(entity.getNomAttribut());
        dto.setDescription(entity.getDescription());
        dto.setTypeValeur(entity.getTypeValeur());
        dto.setValeursAutorisees(entity.getValeursAutorisees());
        dto.setUnite(entity.getUnite());
        dto.setObligatoire(entity.isObligatoire());
        dto.setOrdre(entity.getOrdre());
        if (entity.getCategories() != null) {
            dto.setCategorieIds(entity.getCategories().stream()
                    .map(Categorie::getIdCategorie)
                    .collect(Collectors.toSet()));
        }
        return dto;
    }
}
