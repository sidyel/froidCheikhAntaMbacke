package com.froidcheikh.ecommerce.service;

import com.froidcheikh.ecommerce.dto.ProduitDTO;
import com.froidcheikh.ecommerce.entity.AttributProduit;
import com.froidcheikh.ecommerce.entity.ImageProduit;
import com.froidcheikh.ecommerce.entity.Produit;
import com.froidcheikh.ecommerce.exception.ResourceNotFoundException;
import com.froidcheikh.ecommerce.repository.ImageProduitRepository;
import com.froidcheikh.ecommerce.repository.ProduitRepository;
import com.froidcheikh.ecommerce.repository.CategorieRepository;
import com.froidcheikh.ecommerce.repository.MarqueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
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
    private final PrixService prixService;
    private final ImageProduitRepository imageProduitRepository;
    private final FileService fileService;

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
            Boolean enPromotion,
            Pageable pageable) {

        if (Boolean.TRUE.equals(enPromotion)) {
            java.util.Set<Long> promoIds = prixService.getActivePromoProductIds();
            if (promoIds.isEmpty()) return org.springframework.data.domain.Page.empty(pageable);
            return produitRepository.findWithFiltersEnPromotion(
                    promoIds, nomProduit, prixMin, prixMax, categorieId, marqueId, pageable)
                    .map(this::convertToDTO);
        }
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
    public List<ProduitDTO.ImageProduitDTO> uploadImages(Long produitId, MultipartFile[] files) {
        Produit produit = produitRepository.findById(produitId)
                .orElseThrow(() -> new ResourceNotFoundException("Produit non trouvé avec l'ID : " + produitId));

        long nextOrdre = produit.getImages() == null ? 0L : produit.getImages().stream()
                .mapToLong(ImageProduit::getOrdre).max().orElse(-1L) + 1;
        boolean firstImage = produit.getImages() == null || produit.getImages().isEmpty();

        List<ProduitDTO.ImageProduitDTO> result = new ArrayList<>();
        for (int i = 0; i < files.length; i++) {
            MultipartFile file = files[i];
            if (!fileService.isValidImageFile(file)) {
                throw new RuntimeException("Format d'image non valide : " + file.getOriginalFilename());
            }
            FileService.UploadResult upload = fileService.uploadFileWithPublicId(file, "produits/images");
            ImageProduit img = new ImageProduit();
            img.setUrl(upload.url());
            img.setPublicId(upload.publicId());
            img.setOrdre((int) (nextOrdre + i));
            img.setPrincipale(firstImage && i == 0);
            img.setProduit(produit);
            imageProduitRepository.save(img);
            result.add(toImageDTO(img));
            log.info("📷 Image uploadée produit {}: {}", produitId, upload.url());
        }
        return result;
    }

    public void supprimerImage(Long produitId, Long imageId) {
        ImageProduit img = imageProduitRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("Image non trouvée : " + imageId));
        if (!img.getProduit().getIdProduit().equals(produitId)) {
            throw new ResourceNotFoundException("Image " + imageId + " n'appartient pas au produit " + produitId);
        }
        boolean etaitPrincipale = img.isPrincipale();
        fileService.deleteFile(img.getUrl());
        imageProduitRepository.delete(img);
        log.info("🗑️ Image {} supprimée du produit {}", imageId, produitId);

        if (etaitPrincipale) {
            imageProduitRepository.findByProduitIdProduitOrderByOrdreAsc(produitId)
                    .stream().findFirst().ifPresent(suivante -> {
                        suivante.setPrincipale(true);
                        imageProduitRepository.save(suivante);
                    });
        }
    }

    public ProduitDTO.ImageProduitDTO definirImagePrincipale(Long produitId, Long imageId) {
        ImageProduit cible = imageProduitRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("Image non trouvée : " + imageId));
        if (!cible.getProduit().getIdProduit().equals(produitId)) {
            throw new ResourceNotFoundException("Image " + imageId + " n'appartient pas au produit " + produitId);
        }
        imageProduitRepository.findByProduitIdProduitAndPrincipaleTrue(produitId)
                .ifPresent(ancienne -> {
                    ancienne.setPrincipale(false);
                    imageProduitRepository.save(ancienne);
                });
        cible.setPrincipale(true);
        cible = imageProduitRepository.save(cible);
        return toImageDTO(cible);
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

    @CacheEvict(value = "promotionsActives", allEntries = true)
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
        produit.setModeleFabricant(produitDTO.getModeleFabricant());
        produit.setEan(produitDTO.getEan());

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

        // Mise à jour des attributs (null = ne pas toucher, liste vide = tout supprimer)
        if (produitDTO.getAttributs() != null) {
            final Produit ref = produit;
            ref.getAttributs().clear();
            produitDTO.getAttributs().forEach(attrDTO -> {
                AttributProduit attr = new AttributProduit();
                attr.setNomAttribut(attrDTO.getNomAttribut());
                attr.setValeurAttribut(attrDTO.getValeurAttribut());
                attr.setProduit(ref);
                ref.getAttributs().add(attr);
            });
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

    public String uploadFicheTechnique(Long produitId, MultipartFile file) {
        Produit produit = produitRepository.findById(produitId)
                .orElseThrow(() -> new ResourceNotFoundException("Produit non trouvé avec l'ID : " + produitId));
        if (!fileService.isValidPdfFile(file)) {
            throw new RuntimeException("Le fichier doit être au format PDF");
        }
        String pdfPath = fileService.uploadFile(file, "produits/fiches-techniques");
        produit.setFicheTechniquePDF(pdfPath);
        produitRepository.save(produit);
        log.info("📄 Fiche technique uploadée pour le produit {}: {}", produitId, pdfPath);
        return pdfPath;
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

        // Images : list<ImageProduitDTO> pour l'admin + listeImages calculé pour rétrocompat public
        if (produit.getImages() != null && !produit.getImages().isEmpty()) {
            List<ImageProduit> sorted = produit.getImages().stream()
                    .sorted(Comparator.comparingInt(ImageProduit::getOrdre))
                    .collect(Collectors.toList());
            dto.setImages(sorted.stream().map(this::toImageDTO).collect(Collectors.toList()));
            dto.setListeImages(sorted.stream().map(ImageProduit::getUrl).collect(Collectors.toList()));
        }

        PrixService.PrixResultat prix = prixService.calculerPrix(produit);
        if (prix.hasPromo()) {
            dto.setPrixPromo(prix.prixEffectif());
            dto.setPourcentageReduction(prix.pourcentageReduction());
            dto.setDateFinPromo(prix.dateFinPromo());
            dto.setNomPromotion(prix.nomPromotion());
        }

        dto.setScoreCompletude(calculerScore(produit));

        return dto;
    }

    public int calculerScore(Produit produit) {
        int score = 0;
        if (produit.getImages() != null && !produit.getImages().isEmpty()) score += 25;
        if (produit.getDescriptionProduit() != null
                && produit.getDescriptionProduit().length() >= 150) score += 25;
        if (produit.getAttributs() != null && produit.getAttributs().size() >= 3) score += 20;
        if (produit.getMarque() != null) score += 10;
        if (produit.getCategorie() != null) score += 10;
        if ((produit.getEan() != null && !produit.getEan().isBlank())
                || (produit.getModeleFabricant() != null && !produit.getModeleFabricant().isBlank())) score += 10;
        return score;
    }

    private ProduitDTO.ImageProduitDTO toImageDTO(ImageProduit img) {
        return new ProduitDTO.ImageProduitDTO(
                img.getIdImage(), img.getUrl(), img.getPublicId(),
                img.getTexteAlternatif(), img.getOrdre(), img.isPrincipale());
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