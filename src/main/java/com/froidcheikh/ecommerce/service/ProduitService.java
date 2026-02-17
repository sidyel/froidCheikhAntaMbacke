package com.froidcheikh.ecommerce.service;

import com.froidcheikh.ecommerce.dto.ProduitDTO;
import com.froidcheikh.ecommerce.entity.Produit;
import com.froidcheikh.ecommerce.exception.ResourceNotFoundException;
import com.froidcheikh.ecommerce.repository.ProduitRepository;
import com.froidcheikh.ecommerce.repository.CategorieRepository;
import com.froidcheikh.ecommerce.repository.MarqueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ProduitService {

    private final ProduitRepository produitRepository;
    private final CategorieRepository categorieRepository;
    private final MarqueRepository marqueRepository;
    private final ModelMapper modelMapper;

    public Page<ProduitDTO> getAllProduits(Pageable pageable) {
        return produitRepository.findByDisponibiliteTrue(pageable)
                .map(this::convertToDTO);
    }

    public ProduitDTO getProduitById(Long id) {
        Produit produit = produitRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produit non trouvé avec l'ID : " + id));
        return convertToDTO(produit);
    }

    // Nouvelle méthode pour l'admin qui récupère TOUS les produits
    public Page<ProduitDTO> getAllProduitsAdmin(
            String search,
            Long categorieId,
            Long marqueId,
            String stockFilter,
            Pageable pageable) {

        // Cette méthode ne filtre PAS par disponibilite = true
        return produitRepository.findAllWithAdminFilters(
                        search, categorieId, marqueId, stockFilter, pageable)
                .map(this::convertToDTO);
    }

    public ProduitDTO getProduitByRef(String refProduit) {
        Produit produit = produitRepository.findByRefProduit(refProduit)
                .orElseThrow(() -> new ResourceNotFoundException("Produit non trouvé avec la référence : " + refProduit));
        return convertToDTO(produit);
    }

    public Page<ProduitDTO> getProduitsByCategorie(Long categorieId, Pageable pageable) {
        return produitRepository.findByCategorieIdCategorie(categorieId, pageable)
                .map(this::convertToDTO);
    }

    public Page<ProduitDTO> getProduitsByMarque(Long marqueId, Pageable pageable) {
        return produitRepository.findByMarqueIdMarque(marqueId, pageable)
                .map(this::convertToDTO);
    }

    public Page<ProduitDTO> searchProduits(String searchTerm, Pageable pageable) {
        return produitRepository.findBySearchTerm(searchTerm, pageable)
                .map(this::convertToDTO);
    }

    public Page<ProduitDTO> getProduitsWithFilters(
            String nomProduit,
            BigDecimal prixMin,
            BigDecimal prixMax,
            Long categorieId,
            Long marqueId,
            Pageable pageable) {

        return produitRepository.findWithFilters(nomProduit, prixMin, prixMax, categorieId, marqueId, pageable)
                .map(this::convertToDTO);
    }

    public Page<ProduitDTO> getLatestProduits(Pageable pageable) {
        return produitRepository.findLatestProducts(pageable)
                .map(this::convertToDTO);
    }

    public List<ProduitDTO> getProduitsById(List<Long> ids) {
        return produitRepository.findByIdIn(ids)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    public ProduitDTO ajouterImages(Long produitId, List<String> nouvellesImages) {
        Produit produit = produitRepository.findById(produitId)
                .orElseThrow(() -> new ResourceNotFoundException("Produit non trouvé avec l'ID : " + produitId));

        List<String> imagesExistantes = produit.getListeImages() != null
                ? new ArrayList<>(produit.getListeImages())
                : new ArrayList<>();

        imagesExistantes.addAll(nouvellesImages);
        produit.setListeImages(imagesExistantes);

        produit = produitRepository.save(produit);
        log.info("📷 Images ajoutées au produit {}: {} (Total: {})",
                produitId, nouvellesImages, imagesExistantes.size());

        return convertToDTO(produit);
    }

    // Méthode pour supprimer une image spécifique
    public ProduitDTO supprimerImage(Long produitId, String imagePath) {
        Produit produit = produitRepository.findById(produitId)
                .orElseThrow(() -> new ResourceNotFoundException("Produit non trouvé avec l'ID : " + produitId));

        if (produit.getListeImages() != null) {
            List<String> images = new ArrayList<>(produit.getListeImages());
            if (images.remove(imagePath)) {
                produit.setListeImages(images);
                produit = produitRepository.save(produit);
                log.info("🗑️ Image supprimée du produit {}: {}", produitId, imagePath);
            }
        }

        return convertToDTO(produit);
    }

    public ProduitDTO createProduit(ProduitDTO produitDTO) {
        Produit produit = convertToEntity(produitDTO);

        // Générer automatiquement refProduit et codeProduit si non fournis
        if (produit.getRefProduit() == null) {
            produit.setRefProduit(generateRefProduit());
        }
        if (produit.getCodeProduit() == null) {
            produit.setCodeProduit(generateCodeProduit());
        }

        produit = produitRepository.save(produit);
        log.info("Produit créé avec l'ID : {}", produit.getIdProduit());

        return convertToDTO(produit);
    }

    public ProduitDTO updateProduit(Long id, ProduitDTO produitDTO) {
        Produit produit = produitRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produit non trouvé avec l'ID : " + id));

        // Mise à jour des champs de base
        produit.setNomProduit(produitDTO.getNomProduit());
        produit.setDescriptionProduit(produitDTO.getDescriptionProduit());
        produit.setPrix(produitDTO.getPrix());
        produit.setStockDisponible(produitDTO.getStockDisponible());
        produit.setGarantie(produitDTO.getGarantie());
        produit.setLabelEnergie(produitDTO.getLabelEnergie());
        produit.setPuissanceBTU(produitDTO.getPuissanceBTU());
        produit.setConsommationWatt(produitDTO.getConsommationWatt());
        produit.setDimensions(produitDTO.getDimensions());
        produit.setPoids(produitDTO.getPoids());
        produit.setDisponibilite(produitDTO.getDisponibilite());

        // IMPORTANT: Mise à jour des images
        if (produitDTO.getListeImages() != null) {
            produit.setListeImages(new ArrayList<>(produitDTO.getListeImages()));
            log.info("🖼️ Images mises à jour pour le produit {}: {}", id, produitDTO.getListeImages());
        }

        // IMPORTANT: Mise à jour de la fiche technique
        if (produitDTO.getFicheTechniquePDF() != null) {
            produit.setFicheTechniquePDF(produitDTO.getFicheTechniquePDF());
            log.info("📄 Fiche technique mise à jour pour le produit {}: {}", id, produitDTO.getFicheTechniquePDF());
        }

        // Mise à jour des relations
        if (produitDTO.getCategorie() != null) {
            produit.setCategorie(categorieRepository.findById(produitDTO.getCategorie().getIdCategorie()).orElse(null));
        }
        if (produitDTO.getMarque() != null) {
            produit.setMarque(marqueRepository.findById(produitDTO.getMarque().getIdMarque()).orElse(null));
        }

        // Sauvegarde
        produit = produitRepository.save(produit);
        log.info("✅ Produit mis à jour avec l'ID : {}", produit.getIdProduit());

        return convertToDTO(produit);
    }

    public void deleteProduit(Long id) {
        if (!produitRepository.existsById(id)) {
            throw new ResourceNotFoundException("Produit non trouvé avec l'ID : " + id);
        }
        produitRepository.deleteById(id);
        log.info("Produit supprimé avec l'ID : {}", id);
    }

    public void updateStock(Long produitId, Integer nouvelleQuantite) {
        Produit produit = produitRepository.findById(produitId)
                .orElseThrow(() -> new ResourceNotFoundException("Produit non trouvé avec l'ID : " + produitId));

        produit.setStockDisponible(nouvelleQuantite);
        produit.setDisponibilite(nouvelleQuantite > 0);
        produitRepository.save(produit);

        log.info("Stock mis à jour pour le produit {} : {}", produitId, nouvelleQuantite);
    }

    public boolean verifierStockDisponible(Long produitId, Integer quantiteDemandee) {
        Produit produit = produitRepository.findById(produitId)
                .orElseThrow(() -> new ResourceNotFoundException("Produit non trouvé avec l'ID : " + produitId));

        return produit.getStockDisponible() >= quantiteDemandee && produit.getDisponibilite();
    }

    public List<ProduitDTO> getProduitsStockFaible(Integer seuil) {
        return produitRepository.findByStockDisponibleLessThan(seuil)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    private String generateRefProduit() {
        return "REF-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private String generateCodeProduit() {
        return "PRD-" + System.currentTimeMillis();
    }

    private ProduitDTO convertToDTO(Produit produit) {
        ProduitDTO dto = modelMapper.map(produit, ProduitDTO.class);

        if (produit.getCategorie() != null) {
            dto.setCategorie(new ProduitDTO.CategorieDTO(
                    produit.getCategorie().getIdCategorie(),
                    produit.getCategorie().getNomCategorie()
            ));
        }

        if (produit.getMarque() != null) {
            dto.setMarque(new ProduitDTO.MarqueDTO(
                    produit.getMarque().getIdMarque(),
                    produit.getMarque().getNomMarque(),
                    produit.getMarque().getLogo()
            ));
        }

        return dto;
    }

    private Produit convertToEntity(ProduitDTO dto) {
        Produit produit = modelMapper.map(dto, Produit.class);

        if (dto.getCategorie() != null) {
            produit.setCategorie(categorieRepository.findById(dto.getCategorie().getIdCategorie()).orElse(null));
        }

        if (dto.getMarque() != null) {
            produit.setMarque(marqueRepository.findById(dto.getMarque().getIdMarque()).orElse(null));
        }

        return produit;
    }
}