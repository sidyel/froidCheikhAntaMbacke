package com.froidcheikh.ecommerce.service;

import com.froidcheikh.ecommerce.dto.MarqueDTO;
import com.froidcheikh.ecommerce.entity.Marque;
import com.froidcheikh.ecommerce.exception.ResourceNotFoundException;
import com.froidcheikh.ecommerce.repository.MarqueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class MarqueService {

    private final MarqueRepository marqueRepository;
    private final ModelMapper modelMapper;

    public List<MarqueDTO> getAllMarques() {
        return marqueRepository.findAllOrderByName()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<MarqueDTO> getMarquesWithAvailableProducts() {
        return marqueRepository.findMarquesWithAvailableProducts()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public MarqueDTO getMarqueById(Long id) {
        Marque marque = marqueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Marque non trouvée avec l'ID : " + id));
        return convertToDTO(marque);
    }

    public MarqueDTO createMarque(MarqueDTO marqueDTO) {
        Marque marque = convertToEntity(marqueDTO);
        marque = marqueRepository.save(marque);
        log.info("Marque créée avec l'ID : {}", marque.getIdMarque());
        return convertToDTO(marque);
    }

    public MarqueDTO updateMarque(Long id, MarqueDTO marqueDTO) {
        Marque marque = marqueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Marque non trouvée avec l'ID : " + id));

        marque.setNomMarque(marqueDTO.getNomMarque());
        marque.setLogo(marqueDTO.getLogo());
        marque.setDescription(marqueDTO.getDescription());

        marque = marqueRepository.save(marque);
        log.info("Marque mise à jour avec l'ID : {}", marque.getIdMarque());

        return convertToDTO(marque);
    }

    public void deleteMarque(Long id) {
        if (!marqueRepository.existsById(id)) {
            throw new ResourceNotFoundException("Marque non trouvée avec l'ID : " + id);
        }
        marqueRepository.deleteById(id);
        log.info("Marque supprimée avec l'ID : {}", id);
    }

    private MarqueDTO convertToDTO(Marque marque) {
        MarqueDTO dto = modelMapper.map(marque, MarqueDTO.class);

        if (marque.getProduits() != null) {
            dto.setNombreProduits((long) marque.getProduits().size());
        }

        return dto;
    }

    private Marque convertToEntity(MarqueDTO dto) {
        return modelMapper.map(dto, Marque.class);
    }
}