package com.froidcheikh.ecommerce.service;

import com.froidcheikh.ecommerce.dto.CategorieDTO;
import com.froidcheikh.ecommerce.entity.Categorie;
import com.froidcheikh.ecommerce.exception.ResourceNotFoundException;
import com.froidcheikh.ecommerce.repository.CategorieRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CategorieService {

    private final CategorieRepository categorieRepository;
    private final ModelMapper modelMapper;

    /**
     * Liste publique des catégories — mise en cache.
     * Utilise findAllWithParentEager (1 seule query SQL avec LEFT JOIN FETCH) pour éviter
     * le problème N+1 qui était causé par l'accès lazy à parent et produits.
     */
    @Cacheable("categories")
    @Transactional(readOnly = true)
    public List<CategorieDTO> getAllCategories() {
        return categorieRepository.findAllWithParentEager()
                .stream()
                .map(this::convertToDTONoLazy)
                .collect(Collectors.toList());
    }

    public List<CategorieDTO> getCategoriesTree() {
        return categorieRepository.findRootCategoriesWithSubCategories()
                .stream()
                .map(this::convertToDTOWithSubCategories)
                .collect(Collectors.toList());
    }

    public CategorieDTO getCategorieById(Long id) {
        Categorie categorie = categorieRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Catégorie non trouvée avec l'ID : " + id));
        return convertToDTO(categorie);
    }

    public List<CategorieDTO> getSousCategories(Long parentId) {
        return categorieRepository.findByParentIdCategorie(parentId)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Caching(evict = {
        @CacheEvict(value = "promotionsActives", allEntries = true),
        @CacheEvict(value = "categories", allEntries = true)
    })
    public CategorieDTO createCategorie(CategorieDTO categorieDTO) {
        Categorie categorie = convertToEntity(categorieDTO);
        categorie = categorieRepository.save(categorie);
        log.info("Catégorie créée avec l'ID : {}", categorie.getIdCategorie());
        return convertToDTO(categorie);
    }

    @Caching(evict = {
        @CacheEvict(value = "promotionsActives", allEntries = true),
        @CacheEvict(value = "categories", allEntries = true)
    })
    public CategorieDTO updateCategorie(Long id, CategorieDTO categorieDTO) {
        Categorie categorie = categorieRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Catégorie non trouvée avec l'ID : " + id));

        categorie.setNomCategorie(categorieDTO.getNomCategorie());
        categorie.setDescriptionCategorie(categorieDTO.getDescriptionCategorie());
        categorie.setImageCategorie(categorieDTO.getImageCategorie());

        if (categorieDTO.getParentId() != null) {
            Categorie parent = categorieRepository.findById(categorieDTO.getParentId()).orElse(null);
            categorie.setParent(parent);
        }

        categorie = categorieRepository.save(categorie);
        log.info("Catégorie mise à jour avec l'ID : {}", categorie.getIdCategorie());

        return convertToDTO(categorie);
    }

    @Caching(evict = {
        @CacheEvict(value = "promotionsActives", allEntries = true),
        @CacheEvict(value = "categories", allEntries = true)
    })
    public void deleteCategorie(Long id) {
        if (!categorieRepository.existsById(id)) {
            throw new ResourceNotFoundException("Catégorie non trouvée avec l'ID : " + id);
        }
        categorieRepository.deleteById(id);
        log.info("Catégorie supprimée avec l'ID : {}", id);
    }

    /**
     * Mapping pour la liste publique — n'accède PAS aux collections lazy (produits, sousCategories).
     * Compatible avec findAllWithParentEager (parent déjà chargé via JOIN FETCH).
     */
    private CategorieDTO convertToDTONoLazy(Categorie categorie) {
        CategorieDTO dto = new CategorieDTO();
        dto.setIdCategorie(categorie.getIdCategorie());
        dto.setNomCategorie(categorie.getNomCategorie());
        dto.setDescriptionCategorie(categorie.getDescriptionCategorie());
        dto.setImageCategorie(categorie.getImageCategorie());
        if (categorie.getParent() != null) {
            dto.setParentId(categorie.getParent().getIdCategorie());
            dto.setNomParent(categorie.getParent().getNomCategorie());
        }
        return dto;
    }

    private CategorieDTO convertToDTO(Categorie categorie) {
        CategorieDTO dto = modelMapper.map(categorie, CategorieDTO.class);

        if (categorie.getParent() != null) {
            dto.setParentId(categorie.getParent().getIdCategorie());
            dto.setNomParent(categorie.getParent().getNomCategorie());
        }

        if (categorie.getProduits() != null) {
            dto.setNombreProduits((long) categorie.getProduits().size());
        }

        return dto;
    }

    private CategorieDTO convertToDTOWithSubCategories(Categorie categorie) {
        CategorieDTO dto = convertToDTO(categorie);

        if (categorie.getSousCategories() != null) {
            dto.setSousCategories(categorie.getSousCategories()
                    .stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList()));
        }

        return dto;
    }

    private Categorie convertToEntity(CategorieDTO dto) {
        Categorie categorie = modelMapper.map(dto, Categorie.class);

        if (dto.getParentId() != null) {
            Categorie parent = categorieRepository.findById(dto.getParentId()).orElse(null);
            categorie.setParent(parent);
        }

        return categorie;
    }
}