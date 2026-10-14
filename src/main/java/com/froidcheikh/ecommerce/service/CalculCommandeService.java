package com.froidcheikh.ecommerce.service;

import com.froidcheikh.ecommerce.dto.LivraisonDTO;
import com.froidcheikh.ecommerce.entity.Categorie;
import com.froidcheikh.ecommerce.entity.Commande;
import com.froidcheikh.ecommerce.entity.ParametresSite;
import com.froidcheikh.ecommerce.entity.Produit;
import com.froidcheikh.ecommerce.entity.ZoneLivraison;
import com.froidcheikh.ecommerce.exception.ResourceNotFoundException;
import com.froidcheikh.ecommerce.repository.ParametresSiteRepository;
import com.froidcheikh.ecommerce.repository.ProduitRepository;
import com.froidcheikh.ecommerce.repository.ZoneLivraisonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Source unique de vérité pour le calcul des frais de livraison.
 * Utilisé à la fois par /api/livraison/estimation et par la création de commande,
 * garantissant que le montant affiché = le montant facturé.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CalculCommandeService {

    private final ZoneLivraisonRepository zoneLivraisonRepository;
    private final ProduitRepository produitRepository;
    private final ParametresSiteRepository parametresSiteRepository;
    private final PrixService prixService;

    public record CalculResultat(
            BigDecimal montantArticles,
            BigDecimal fraisZone,
            BigDecimal supplementExpress,
            BigDecimal supplementGabarit,
            BigDecimal fraisLivraisonTotal,
            BigDecimal montantTotal,
            boolean livraisonGratuite,
            Long zoneId,
            String zoneNom,
            BigDecimal zonePrixApplique,
            String zoneModeApplique,
            String delaiEstime
    ) {}

    /**
     * Calcule les frais de livraison et le total.
     *
     * @param zoneId       null pour RETRAIT_MAGASIN
     * @param modeLivraison mode de livraison
     * @param lignes       lignes du panier (produitId, quantite)
     */
    public CalculResultat calculer(Long zoneId,
                                   Commande.ModeLivraison modeLivraison,
                                   List<LivraisonDTO.EstimationRequest.LigneEstimation> lignes) {

        // ─── Montant articles ───────────────────────────────────────────────
        BigDecimal montantArticles = BigDecimal.ZERO;
        boolean hasGabaritLourd = false;

        for (LivraisonDTO.EstimationRequest.LigneEstimation ligne : lignes) {
            Produit produit = produitRepository.findById(ligne.getProduitId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Produit introuvable : " + ligne.getProduitId()));

            montantArticles = montantArticles.add(
                    prixService.calculerPrix(produit).prixEffectif().multiply(BigDecimal.valueOf(ligne.getQuantite())));

            if (produit.getCategorie() != null
                    && Boolean.TRUE.equals(produit.getCategorie().getGabaritLourd())) {
                hasGabaritLourd = true;
            }
        }

        // ─── RETRAIT MAGASIN ────────────────────────────────────────────────
        if (modeLivraison == Commande.ModeLivraison.RETRAIT_MAGASIN) {
            BigDecimal total = montantArticles;
            return new CalculResultat(montantArticles, BigDecimal.ZERO, BigDecimal.ZERO,
                    BigDecimal.ZERO, BigDecimal.ZERO, total, false,
                    null, null, BigDecimal.ZERO, "RETRAIT_MAGASIN", null);
        }

        // ─── Zone obligatoire pour livraison ────────────────────────────────
        if (zoneId == null) {
            throw new IllegalArgumentException("zoneId est requis pour ce mode de livraison");
        }
        ZoneLivraison zone = zoneLivraisonRepository.findById(zoneId)
                .orElseThrow(() -> new ResourceNotFoundException("Zone de livraison introuvable : " + zoneId));

        if (!Boolean.TRUE.equals(zone.getActif())) {
            throw new IllegalStateException("La zone de livraison n'est plus disponible : " + zone.getQuartierLabel());
        }
        if (modeLivraison == Commande.ModeLivraison.LIVRAISON_EXPRESS
                && !Boolean.TRUE.equals(zone.getExpressDisponible())) {
            throw new IllegalStateException("La livraison express n'est pas disponible pour cette zone : " + zone.getQuartierLabel());
        }

        // ─── Configuration globale ──────────────────────────────────────────
        ParametresSite config = parametresSiteRepository.findById(1L).orElse(null);
        BigDecimal supplementExpressGlobal = config != null && config.getSupplementExpress() != null
                ? config.getSupplementExpress() : BigDecimal.valueOf(1500);
        BigDecimal supplementGabaritConfig = config != null && config.getSupplementGabaritLourd() != null
                ? config.getSupplementGabaritLourd() : BigDecimal.ZERO;
        Integer seuilGratuite = config != null ? config.getSeuilLivraisonGratuite() : null;

        // ─── Frais de base (prixStandard) ───────────────────────────────────
        BigDecimal fraisBase = zone.getPrixStandard();

        // ─── Supplément express ─────────────────────────────────────────────
        BigDecimal suppExpress = BigDecimal.ZERO;
        if (modeLivraison == Commande.ModeLivraison.LIVRAISON_EXPRESS) {
            if (zone.getPrixExpress() != null) {
                // prixExpress est le total express : le supplement = prixExpress - prixStandard
                suppExpress = zone.getPrixExpress().subtract(zone.getPrixStandard());
                if (suppExpress.compareTo(BigDecimal.ZERO) < 0) suppExpress = BigDecimal.ZERO;
            } else {
                suppExpress = supplementExpressGlobal;
            }
        }

        // ─── Livraison gratuite (sur fraisBase uniquement) ──────────────────
        boolean gratuit = Boolean.TRUE.equals(zone.getEligibleLivraisonGratuite())
                && seuilGratuite != null
                && montantArticles.compareTo(BigDecimal.valueOf(seuilGratuite)) >= 0;
        if (gratuit) {
            fraisBase = BigDecimal.ZERO;
        }

        // ─── Supplément gabarit lourd ────────────────────────────────────────
        BigDecimal suppGabarit = hasGabaritLourd ? supplementGabaritConfig : BigDecimal.ZERO;

        BigDecimal fraisTotal = fraisBase.add(suppExpress).add(suppGabarit);
        BigDecimal montantTotal = montantArticles.add(fraisTotal);

        String zoneNom = zone.getQuartierLabel() + " — " + zone.getVilleLabel();
        BigDecimal zonePrixApplique = fraisBase.add(suppExpress);

        return new CalculResultat(montantArticles, fraisBase, suppExpress, suppGabarit,
                fraisTotal, montantTotal, gratuit,
                zone.getId(), zoneNom, zonePrixApplique,
                modeLivraison.name(), zone.getDelaiEstime());
    }
}
