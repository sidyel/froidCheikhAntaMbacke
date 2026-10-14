package com.froidcheikh.ecommerce.service;

import com.froidcheikh.ecommerce.dto.ProduitDTO.AttributProduitDTO;
import com.froidcheikh.ecommerce.entity.AttributProduit;
import com.froidcheikh.ecommerce.entity.ModeleAttribut;
import com.froidcheikh.ecommerce.entity.Produit;
import com.froidcheikh.ecommerce.entity.TypeAttribut;
import com.froidcheikh.ecommerce.exception.ResourceNotFoundException;
import com.froidcheikh.ecommerce.repository.AttributProduitRepository;
import com.froidcheikh.ecommerce.repository.ModeleAttributRepository;
import com.froidcheikh.ecommerce.repository.ProduitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AttributProduitService {

    private final AttributProduitRepository attributProduitRepository;
    private final ProduitRepository produitRepository;
    private final ModeleAttributRepository modeleAttributRepository;

    public List<AttributProduitDTO> findByProduit(Long produitId) {
        getProduit(produitId);
        return attributProduitRepository.findByProduitIdProduit(produitId)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional
    public AttributProduitDTO create(Long produitId, AttributProduitDTO dto) {
        Produit produit = getProduit(produitId);
        AttributProduit attr = new AttributProduit();
        attr.setProduit(produit);
        applyDTO(dto, attr);
        return toDTO(attributProduitRepository.save(attr));
    }

    @Transactional
    public AttributProduitDTO update(Long produitId, Long attrId, AttributProduitDTO dto) {
        AttributProduit attr = getAttr(produitId, attrId);
        applyDTO(dto, attr);
        return toDTO(attributProduitRepository.save(attr));
    }

    @Transactional
    public void delete(Long produitId, Long attrId) {
        AttributProduit attr = getAttr(produitId, attrId);
        attributProduitRepository.delete(attr);
    }

    private void applyDTO(AttributProduitDTO dto, AttributProduit attr) {
        attr.setNomAttribut(dto.getNomAttribut());

        ModeleAttribut modele = null;
        if (dto.getModeleAttributId() != null) {
            modele = modeleAttributRepository.findById(dto.getModeleAttributId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "ModeleAttribut non trouvé : " + dto.getModeleAttributId()));
            attr.setNomAttribut(modele.getNomAttribut()); // nom homogène depuis le modèle
        }
        attr.setModeleAttribut(modele);

        String valeur = dto.getValeurAttribut();
        if (modele != null) {
            valeur = validerValeur(valeur, modele);
        }
        attr.setValeurAttribut(valeur);
    }

    private String validerValeur(String valeur, ModeleAttribut modele) {
        if (valeur == null || valeur.isBlank()) {
            throw new IllegalArgumentException("La valeur de l'attribut ne peut pas être vide");
        }
        TypeAttribut type = modele.getTypeValeur();
        switch (type) {
            case NUMBER -> {
                try { new java.math.BigDecimal(valeur.replace(",", ".")); }
                catch (NumberFormatException e) {
                    throw new IllegalArgumentException(
                            "Valeur numérique invalide pour '" + modele.getNomAttribut() + "' : " + valeur);
                }
            }
            case BOOLEAN -> {
                if (!"true".equalsIgnoreCase(valeur) && !"false".equalsIgnoreCase(valeur)) {
                    throw new IllegalArgumentException(
                            "Valeur booléenne invalide pour '" + modele.getNomAttribut() + "' : attendu true/false");
                }
            }
            case SELECT -> {
                String json = modele.getValeursAutorisees();
                if (json != null && !json.contains("\"" + valeur + "\"")) {
                    throw new IllegalArgumentException(
                            "Valeur '" + valeur + "' non autorisée pour '" + modele.getNomAttribut() + "'");
                }
            }
            default -> { /* TEXT : pas de contrainte */ }
        }
        return valeur;
    }

    private Produit getProduit(Long id) {
        return produitRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produit non trouvé : " + id));
    }

    private AttributProduit getAttr(Long produitId, Long attrId) {
        AttributProduit attr = attributProduitRepository.findById(attrId)
                .orElseThrow(() -> new ResourceNotFoundException("Attribut non trouvé : " + attrId));
        if (attr.getProduit() == null || !attr.getProduit().getIdProduit().equals(produitId)) {
            throw new ResourceNotFoundException(
                    "L'attribut " + attrId + " n'appartient pas au produit " + produitId);
        }
        return attr;
    }

    private AttributProduitDTO toDTO(AttributProduit attr) {
        AttributProduitDTO dto = new AttributProduitDTO();
        dto.setIdAttribut(attr.getIdAttribut());
        dto.setNomAttribut(attr.getNomAttribut());
        dto.setValeurAttribut(attr.getValeurAttribut());
        if (attr.getModeleAttribut() != null) {
            dto.setModeleAttributId(attr.getModeleAttribut().getIdModele());
        }
        return dto;
    }
}
