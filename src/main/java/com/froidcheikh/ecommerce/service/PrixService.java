package com.froidcheikh.ecommerce.service;

import com.froidcheikh.ecommerce.entity.Promotion;
import com.froidcheikh.ecommerce.entity.Produit;
import com.froidcheikh.ecommerce.repository.CategorieRepository;
import com.froidcheikh.ecommerce.repository.ProduitRepository;
import com.froidcheikh.ecommerce.repository.PromotionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PrixService {

    private final PromotionRepository promotionRepository;
    private final ParametresSiteService parametresSiteService;
    private final CategorieRepository categorieRepository;
    private final ProduitRepository produitRepository;
    private final Clock clock;

    // Auto-injection lazy pour contourner la self-invocation AOP (@Cacheable)
    @Autowired @Lazy
    private PrixService self;

    // ---- Records publics ----

    public record PrixResultat(
        BigDecimal prixOrigine,
        BigDecimal prixEffectif,         // >= 0, arrondi HALF_UP ; == prixOrigine si aucune promo
        Integer    pourcentageReduction, // null si aucune promo
        LocalDateTime dateFinPromo,      // null si pas de promo ou dateFin=null
        Long       promotionId,
        String     nomPromotion
    ) {
        public boolean hasPromo() { return promotionId != null; }
    }

    // ---- Record interne — uniquement primitifs + Sets de Longs, jamais d'entités JPA ----
    record PromotionCache(
        Long idPromotion,
        String nom,
        Promotion.TypePromotion typePromotion,
        BigDecimal valeur,
        LocalDateTime dateDebut,
        LocalDateTime dateFin,
        Integer priorite,
        Promotion.CiblePromotion cible,
        Set<Long> produitIds,
        Set<Long> categorieIdsExpanded,  // inclut toutes les sous-catégories
        Set<Long> marqueIds
    ) {}

    private record CandidatResultat(
        BigDecimal prixEffectif,
        int priorite,
        LocalDateTime dateFin,
        Long promotionId,
        String nomPromotion
    ) {}

    // ---- Cache ----

    @Cacheable("promotionsActives")
    @Transactional(readOnly = true)
    public List<PromotionCache> chargerPromotionsActives() {
        LocalDateTime now = nowInSiteZone();
        return promotionRepository.findCandidates(now).stream()
                .map(this::toCache)
                .collect(Collectors.toList());
    }

    @CacheEvict(value = "promotionsActives", allEntries = true)
    public void evictCache() {}

    // ---- API publique ----

    public PrixResultat calculerPrix(Produit produit) {
        List<PromotionCache> candidates = self.chargerPromotionsActives();
        LocalDateTime now = nowInSiteZone();
        BigDecimal prixOrigine = produit.getPrix();

        Optional<CandidatResultat> best = candidates.stream()
                .filter(p -> !p.dateDebut().isAfter(now))
                .filter(p -> p.dateFin() == null || p.dateFin().isAfter(now))
                .filter(p -> appliqueSurProduit(p, produit))
                .map(p -> calculerAvecPromo(prixOrigine, p))
                .filter(r -> r.prixEffectif().compareTo(BigDecimal.ZERO) > 0)
                .filter(r -> r.prixEffectif().compareTo(prixOrigine) < 0)
                .sorted(Comparator.comparing(CandidatResultat::prixEffectif)
                        .thenComparing(Comparator.comparing(CandidatResultat::priorite).reversed())
                        .thenComparing(CandidatResultat::promotionId))
                .findFirst();

        return best.map(b -> new PrixResultat(
                prixOrigine, b.prixEffectif(),
                calculerPourcentage(prixOrigine, b.prixEffectif()),
                b.dateFin(), b.promotionId(), b.nomPromotion()
        )).orElse(new PrixResultat(prixOrigine, prixOrigine, null, null, null, null));
    }

    public Map<Long, PrixResultat> calculerPrixBatch(List<Produit> produits) {
        List<PromotionCache> candidates = self.chargerPromotionsActives();
        LocalDateTime now = nowInSiteZone();

        Map<Long, PrixResultat> result = new HashMap<>();
        for (Produit produit : produits) {
            BigDecimal prixOrigine = produit.getPrix();
            Optional<CandidatResultat> best = candidates.stream()
                    .filter(p -> !p.dateDebut().isAfter(now))
                    .filter(p -> p.dateFin() == null || p.dateFin().isAfter(now))
                    .filter(p -> appliqueSurProduit(p, produit))
                    .map(p -> calculerAvecPromo(prixOrigine, p))
                    .filter(r -> r.prixEffectif().compareTo(BigDecimal.ZERO) > 0)
                    .filter(r -> r.prixEffectif().compareTo(prixOrigine) < 0)
                    .sorted(Comparator.comparing(CandidatResultat::prixEffectif)
                            .thenComparing(Comparator.comparing(CandidatResultat::priorite).reversed())
                            .thenComparing(CandidatResultat::promotionId))
                    .findFirst();
            result.put(produit.getIdProduit(), best
                    .map(b -> new PrixResultat(prixOrigine, b.prixEffectif(),
                            calculerPourcentage(prixOrigine, b.prixEffectif()),
                            b.dateFin(), b.promotionId(), b.nomPromotion()))
                    .orElse(new PrixResultat(prixOrigine, prixOrigine, null, null, null, null)));
        }
        return result;
    }

    // Calcul fictif pour l'aperçu admin (promo non encore enregistrée)
    public BigDecimal calculerPrixFictif(BigDecimal prix, Promotion.TypePromotion type, BigDecimal valeur) {
        BigDecimal result;
        if (type == Promotion.TypePromotion.POURCENTAGE) {
            result = prix.multiply(
                    BigDecimal.ONE.subtract(valeur.divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP))
            ).setScale(0, RoundingMode.HALF_UP);
        } else {
            result = prix.subtract(valeur).setScale(0, RoundingMode.HALF_UP);
        }
        return result.max(BigDecimal.ZERO);
    }

    // IDs des produits actuellement en promo active (pour le filtre catalogue)
    public Set<Long> getActivePromoProductIds() {
        List<PromotionCache> actives = self.chargerPromotionsActives();
        LocalDateTime now = nowInSiteZone();

        List<PromotionCache> courants = actives.stream()
                .filter(p -> !p.dateDebut().isAfter(now))
                .filter(p -> p.dateFin() == null || p.dateFin().isAfter(now))
                .collect(Collectors.toList());

        if (courants.isEmpty()) return Set.of();

        if (courants.stream().anyMatch(p -> p.cible() == Promotion.CiblePromotion.TOUS)) {
            return new HashSet<>(produitRepository.findAllAvailableIds());
        }

        Set<Long> result = new HashSet<>();

        courants.stream().filter(p -> p.cible() == Promotion.CiblePromotion.PRODUITS)
                .flatMap(p -> p.produitIds().stream()).forEach(result::add);

        Set<Long> catIds = courants.stream()
                .filter(p -> p.cible() == Promotion.CiblePromotion.CATEGORIES)
                .flatMap(p -> p.categorieIdsExpanded().stream()).collect(Collectors.toSet());
        if (!catIds.isEmpty()) result.addAll(produitRepository.findIdsByCategorieIds(catIds));

        Set<Long> marqueIds = courants.stream()
                .filter(p -> p.cible() == Promotion.CiblePromotion.MARQUES)
                .flatMap(p -> p.marqueIds().stream()).collect(Collectors.toSet());
        if (!marqueIds.isEmpty()) result.addAll(produitRepository.findIdsByMarqueIds(marqueIds));

        return result;
    }

    public LocalDateTime nowInSiteZone() {
        ZoneId zone = parametresSiteService.getSiteZoneId();
        return ZonedDateTime.now(clock).withZoneSameInstant(zone).toLocalDateTime();
    }

    // ---- Méthodes privées ----

    private CandidatResultat calculerAvecPromo(BigDecimal prixOrigine, PromotionCache promo) {
        BigDecimal prixCalc;
        if (promo.typePromotion() == Promotion.TypePromotion.POURCENTAGE) {
            prixCalc = prixOrigine.multiply(
                    BigDecimal.ONE.subtract(promo.valeur().divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP))
            ).setScale(0, RoundingMode.HALF_UP);
        } else {
            prixCalc = prixOrigine.subtract(promo.valeur()).setScale(0, RoundingMode.HALF_UP);
        }
        // Plancher : si <= 0, la promo ne s'applique pas (filtrée en aval)
        return new CandidatResultat(prixCalc.max(BigDecimal.ZERO),
                promo.priorite(), promo.dateFin(), promo.idPromotion(), promo.nom());
    }

    private boolean appliqueSurProduit(PromotionCache promo, Produit produit) {
        return switch (promo.cible()) {
            case TOUS -> true;
            case PRODUITS -> promo.produitIds().contains(produit.getIdProduit());
            case CATEGORIES -> produit.getCategorie() != null
                    && promo.categorieIdsExpanded().contains(produit.getCategorie().getIdCategorie());
            case MARQUES -> produit.getMarque() != null
                    && promo.marqueIds().contains(produit.getMarque().getIdMarque());
        };
    }

    public Integer calculerPourcentagePublic(BigDecimal prixOrigine, BigDecimal prixEffectif) {
        return calculerPourcentage(prixOrigine, prixEffectif);
    }

    private Integer calculerPourcentage(BigDecimal prixOrigine, BigDecimal prixEffectif) {
        if (prixOrigine.compareTo(BigDecimal.ZERO) == 0) return null;
        return prixOrigine.subtract(prixEffectif)
                .multiply(BigDecimal.valueOf(100))
                .divide(prixOrigine, 0, RoundingMode.HALF_UP)
                .intValue();
    }

    @Transactional(readOnly = true)
    private PromotionCache toCache(Promotion p) {
        Set<Long> produitIds = p.getProduitsCibles().stream()
                .map(Produit::getIdProduit).collect(Collectors.toSet());
        Set<Long> catIds = p.getCategoriesCibles().stream()
                .map(c -> c.getIdCategorie()).collect(Collectors.toSet());
        Set<Long> marqueIds = p.getMarquesCibles().stream()
                .map(m -> m.getIdMarque()).collect(Collectors.toSet());
        return new PromotionCache(
                p.getIdPromotion(), p.getNom(), p.getTypePromotion(), p.getValeur(),
                p.getDateDebut(), p.getDateFin(), p.getPriorite(), p.getCible(),
                produitIds, expandCategories(catIds), marqueIds
        );
    }

    private Set<Long> expandCategories(Set<Long> directIds) {
        if (directIds.isEmpty()) return directIds;
        Set<Long> all = new HashSet<>(directIds);
        Queue<Long> toProcess = new ArrayDeque<>(directIds);
        while (!toProcess.isEmpty()) {
            Long parentId = toProcess.poll();
            for (Long childId : categorieRepository.findIdsByParentId(parentId)) {
                if (all.add(childId)) toProcess.add(childId);
            }
        }
        return all;
    }
}
