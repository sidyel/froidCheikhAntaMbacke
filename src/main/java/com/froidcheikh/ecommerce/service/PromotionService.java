package com.froidcheikh.ecommerce.service;

import com.froidcheikh.ecommerce.dto.PromotionDTO;
import com.froidcheikh.ecommerce.entity.*;
import com.froidcheikh.ecommerce.exception.ResourceNotFoundException;
import com.froidcheikh.ecommerce.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class PromotionService {

    private final PromotionRepository promotionRepository;
    private final ProduitRepository produitRepository;
    private final CategorieRepository categorieRepository;
    private final MarqueRepository marqueRepository;
    private final PrixService prixService;
    private final CatalogueFeedService catalogueFeedService;

    @CacheEvict(value = "promotionsActives", allEntries = true)
    public PromotionDTO.Response creer(PromotionDTO.Request dto) {
        valider(dto);
        Promotion p = new Promotion();
        applyDto(p, dto);
        PromotionDTO.Response r = toResponse(promotionRepository.save(p));
        catalogueFeedService.evictFeedCache();
        return r;
    }

    @CacheEvict(value = "promotionsActives", allEntries = true)
    public PromotionDTO.Response modifier(Long id, PromotionDTO.Request dto) {
        Promotion p = promotionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Promotion non trouvée : " + id));
        valider(dto);
        applyDto(p, dto);
        PromotionDTO.Response r = toResponse(promotionRepository.save(p));
        catalogueFeedService.evictFeedCache();
        return r;
    }

    @CacheEvict(value = "promotionsActives", allEntries = true)
    public void supprimer(Long id) {
        Promotion p = promotionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Promotion non trouvée : " + id));
        if (promotionRepository.isUsedInCommande(id)) {
            p.setActif(false);
            promotionRepository.save(p);
        } else {
            promotionRepository.delete(p);
        }
        catalogueFeedService.evictFeedCache();
    }

    @Transactional(readOnly = true)
    public PromotionDTO.Response getById(Long id) {
        return toResponse(promotionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Promotion non trouvée : " + id)));
    }

    @Transactional(readOnly = true)
    public Page<PromotionDTO.Response> listerActives(Pageable pageable) {
        return promotionRepository.findActives(prixService.nowInSiteZone(), pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<PromotionDTO.Response> listerProgrammees(Pageable pageable) {
        return promotionRepository.findProgrammees(prixService.nowInSiteZone(), pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<PromotionDTO.Response> listerExpirees(Pageable pageable) {
        return promotionRepository.findExpirees(prixService.nowInSiteZone(), pageable).map(this::toResponse);
    }

    // Aperçu : liste paginée des produits concernés avec leur prix avant/après
    @Transactional(readOnly = true)
    public PromotionDTO.ApercuPage apercu(PromotionDTO.Request dto, int page, int size) {
        valider(dto);

        List<Long> produitIdsCibles = computeTargetProduitIds(dto);
        if (produitIdsCibles.isEmpty()) {
            PromotionDTO.ApercuPage empty = new PromotionDTO.ApercuPage();
            empty.setProduits(List.of());
            empty.setTotalElements(0);
            empty.setTotalPages(0);
            empty.setPage(page);
            empty.setSize(size);
            return empty;
        }

        int from = page * size;
        int to = Math.min(from + size, produitIdsCibles.size());
        List<Long> pageIds = (from < produitIdsCibles.size()) ? produitIdsCibles.subList(from, to) : List.of();

        List<PromotionDTO.ApercuProduit> produits = produitRepository.findByIdIn(pageIds).stream()
                .map(p -> {
                    BigDecimal prixApres = prixService.calculerPrixFictif(p.getPrix(), dto.getTypePromotion(), dto.getValeur());
                    boolean plancher = prixApres.compareTo(BigDecimal.ZERO) <= 0;

                    PromotionDTO.ApercuProduit ap = new PromotionDTO.ApercuProduit();
                    ap.setIdProduit(p.getIdProduit());
                    ap.setNomProduit(p.getNomProduit());
                    ap.setPrixOrigine(p.getPrix());
                    ap.setPrixApresPromo(plancher ? p.getPrix() : prixApres); // prix inchangé si plancher
                    ap.setPourcentageReduction(plancher ? null : prixService.calculerPourcentagePublic(p.getPrix(), prixApres));
                    ap.setPrixPlancher(plancher);
                    return ap;
                })
                .collect(Collectors.toList());

        int totalPages = (int) Math.ceil((double) produitIdsCibles.size() / size);
        PromotionDTO.ApercuPage result = new PromotionDTO.ApercuPage();
        result.setProduits(produits);
        result.setTotalElements(produitIdsCibles.size());
        result.setTotalPages(totalPages);
        result.setPage(page);
        result.setSize(size);
        return result;
    }

    // ---- Privé ----

    private List<Long> computeTargetProduitIds(PromotionDTO.Request dto) {
        if (dto.getCible() == null) return List.of();
        return switch (dto.getCible()) {
            case TOUS -> produitRepository.findAllAvailableIds();
            case PRODUITS -> dto.getProduitIds() != null ? dto.getProduitIds() : List.of();
            case CATEGORIES -> {
                if (dto.getCategorieIds() == null || dto.getCategorieIds().isEmpty()) yield List.of();
                // expand sous-catégories
                java.util.Set<Long> expanded = new java.util.HashSet<>(dto.getCategorieIds());
                java.util.Queue<Long> queue = new java.util.ArrayDeque<>(dto.getCategorieIds());
                while (!queue.isEmpty()) {
                    Long pid = queue.poll();
                    for (Long cid : categorieRepository.findIdsByParentId(pid)) {
                        if (expanded.add(cid)) queue.add(cid);
                    }
                }
                yield produitRepository.findIdsByCategorieIds(expanded);
            }
            case MARQUES -> {
                if (dto.getMarqueIds() == null || dto.getMarqueIds().isEmpty()) yield List.of();
                yield produitRepository.findIdsByMarqueIds(dto.getMarqueIds());
            }
        };
    }

    private void valider(PromotionDTO.Request dto) {
        if (dto.getTypePromotion() == Promotion.TypePromotion.POURCENTAGE
                && dto.getValeur().compareTo(BigDecimal.valueOf(100)) >= 0) {
            throw new IllegalArgumentException("Un pourcentage de réduction doit être strictement inférieur à 100");
        }
        if (dto.getDateFin() != null && !dto.getDateFin().isAfter(dto.getDateDebut())) {
            throw new IllegalArgumentException("La date de fin doit être postérieure à la date de début");
        }
    }

    private void applyDto(Promotion p, PromotionDTO.Request dto) {
        p.setNom(dto.getNom());
        p.setTypePromotion(dto.getTypePromotion());
        p.setValeur(dto.getValeur());
        p.setDateDebut(dto.getDateDebut());
        p.setDateFin(dto.getDateFin());
        p.setActif(dto.getActif() != null ? dto.getActif() : true);
        p.setPriorite(dto.getPriorite() != null ? dto.getPriorite() : 0);
        p.setCible(dto.getCible());

        p.getProduitsCibles().clear();
        if (dto.getProduitIds() != null) {
            produitRepository.findAllById(dto.getProduitIds()).forEach(p.getProduitsCibles()::add);
        }
        p.getCategoriesCibles().clear();
        if (dto.getCategorieIds() != null) {
            categorieRepository.findAllById(dto.getCategorieIds()).forEach(p.getCategoriesCibles()::add);
        }
        p.getMarquesCibles().clear();
        if (dto.getMarqueIds() != null) {
            marqueRepository.findAllById(dto.getMarqueIds()).forEach(p.getMarquesCibles()::add);
        }
    }

    private PromotionDTO.Response toResponse(Promotion p) {
        LocalDateTime now = prixService.nowInSiteZone();

        PromotionDTO.Response r = new PromotionDTO.Response();
        r.setIdPromotion(p.getIdPromotion());
        r.setNom(p.getNom());
        r.setTypePromotion(p.getTypePromotion());
        r.setValeur(p.getValeur());
        r.setDateDebut(p.getDateDebut());
        r.setDateFin(p.getDateFin());
        r.setActif(p.getActif());
        r.setPriorite(p.getPriorite());
        r.setCible(p.getCible());
        r.setProduitIds(p.getProduitsCibles().stream().map(Produit::getIdProduit).collect(Collectors.toList()));
        r.setCategorieIds(p.getCategoriesCibles().stream().map(Categorie::getIdCategorie).collect(Collectors.toList()));
        r.setMarqueIds(p.getMarquesCibles().stream().map(Marque::getIdMarque).collect(Collectors.toList()));
        r.setDateCreation(p.getDateCreation());
        r.setDateModification(p.getDateModification());
        r.setStatut(calculerStatut(p, now));
        return r;
    }

    private String calculerStatut(Promotion p, LocalDateTime now) {
        if (!Boolean.TRUE.equals(p.getActif())) return "INACTIVE";
        if (p.getDateFin() != null && p.getDateFin().isBefore(now)) return "EXPIREE";
        if (p.getDateDebut().isAfter(now)) return "PROGRAMMEE";
        return "ACTIVE";
    }
}
