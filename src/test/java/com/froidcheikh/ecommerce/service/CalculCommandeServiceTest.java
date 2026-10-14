package com.froidcheikh.ecommerce.service;

import com.froidcheikh.ecommerce.dto.LivraisonDTO;
import com.froidcheikh.ecommerce.entity.Categorie;
import com.froidcheikh.ecommerce.entity.Commande;
import com.froidcheikh.ecommerce.entity.ParametresSite;
import com.froidcheikh.ecommerce.entity.Produit;
import com.froidcheikh.ecommerce.entity.ZoneLivraison;
import com.froidcheikh.ecommerce.repository.ParametresSiteRepository;
import com.froidcheikh.ecommerce.repository.ProduitRepository;
import com.froidcheikh.ecommerce.repository.ZoneLivraisonRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CalculCommandeServiceTest {

    @Mock private ZoneLivraisonRepository zoneRepo;
    @Mock private ProduitRepository produitRepo;
    @Mock private ParametresSiteRepository configRepo;

    @InjectMocks
    private CalculCommandeService service;

    private ParametresSite config;
    private ZoneLivraison zoneDakar;
    private Produit produitNormal;
    private Produit produitLourd;
    private LivraisonDTO.EstimationRequest.LigneEstimation ligne(long produitId, int qte) {
        LivraisonDTO.EstimationRequest.LigneEstimation l = new LivraisonDTO.EstimationRequest.LigneEstimation();
        l.setProduitId(produitId);
        l.setQuantite(qte);
        return l;
    }

    @BeforeEach
    void setUp() {
        config = new ParametresSite();
        config.setSeuilLivraisonGratuite(100_000);
        config.setSupplementExpress(BigDecimal.valueOf(1500));
        config.setSupplementGabaritLourd(BigDecimal.valueOf(3000));

        Categorie catNormale = new Categorie();
        catNormale.setGabaritLourd(false);

        Categorie catLourde = new Categorie();
        catLourde.setGabaritLourd(true);

        produitNormal = new Produit();
        produitNormal.setIdProduit(1L);
        produitNormal.setPrix(BigDecimal.valueOf(50_000));
        produitNormal.setCategorie(catNormale);

        produitLourd = new Produit();
        produitLourd.setIdProduit(2L);
        produitLourd.setPrix(BigDecimal.valueOf(80_000));
        produitLourd.setCategorie(catLourde);

        zoneDakar = new ZoneLivraison();
        zoneDakar.setId(1L);
        zoneDakar.setVille("dakar");
        zoneDakar.setVilleLabel("Dakar");
        zoneDakar.setQuartier("plateau");
        zoneDakar.setQuartierLabel("Plateau");
        zoneDakar.setPrixStandard(BigDecimal.ZERO);
        zoneDakar.setPrixExpress(null);
        zoneDakar.setExpressDisponible(true);
        zoneDakar.setActif(true);
        zoneDakar.setEligibleLivraisonGratuite(true);
        zoneDakar.setDelaiEstime("24h");
    }

    // ── 1. RETRAIT_MAGASIN ──────────────────────────────────────────────────

    @Test
    void retraitMagasin_fraisZero() {
        when(produitRepo.findById(1L)).thenReturn(Optional.of(produitNormal));

        CalculCommandeService.CalculResultat r = service.calculer(
                null, Commande.ModeLivraison.RETRAIT_MAGASIN, List.of(ligne(1L, 2)));

        assertThat(r.fraisLivraisonTotal()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(r.montantTotal()).isEqualByComparingTo(BigDecimal.valueOf(100_000));
        assertThat(r.livraisonGratuite()).isFalse();
        assertThat(r.zoneId()).isNull();
    }

    // ── 2. Livraison standard ───────────────────────────────────────────────

    @Test
    void livraisonStandard_fraisZoneAppliques() {
        ZoneLivraison zone = zoneThies();
        when(produitRepo.findById(1L)).thenReturn(Optional.of(produitNormal));
        when(zoneRepo.findById(10L)).thenReturn(Optional.of(zone));
        when(configRepo.findById(1L)).thenReturn(Optional.of(config));

        CalculCommandeService.CalculResultat r = service.calculer(
                10L, Commande.ModeLivraison.LIVRAISON_DOMICILE, List.of(ligne(1L, 1)));

        assertThat(r.fraisZone()).isEqualByComparingTo(BigDecimal.valueOf(5000));
        assertThat(r.supplementExpress()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(r.fraisLivraisonTotal()).isEqualByComparingTo(BigDecimal.valueOf(5000));
        assertThat(r.montantTotal()).isEqualByComparingTo(BigDecimal.valueOf(55_000));
    }

    // ── 3. Livraison express (supplement global) ────────────────────────────

    @Test
    void livraisonExpress_supplementGlobal() {
        ZoneLivraison zone = zoneThies();
        when(produitRepo.findById(1L)).thenReturn(Optional.of(produitNormal));
        when(zoneRepo.findById(10L)).thenReturn(Optional.of(zone));
        when(configRepo.findById(1L)).thenReturn(Optional.of(config));

        CalculCommandeService.CalculResultat r = service.calculer(
                10L, Commande.ModeLivraison.LIVRAISON_EXPRESS, List.of(ligne(1L, 1)));

        assertThat(r.fraisZone()).isEqualByComparingTo(BigDecimal.valueOf(5000));
        assertThat(r.supplementExpress()).isEqualByComparingTo(BigDecimal.valueOf(1500));
        assertThat(r.fraisLivraisonTotal()).isEqualByComparingTo(BigDecimal.valueOf(6500));
    }

    // ── 4. Express avec prixExpress explicite ───────────────────────────────

    @Test
    void livraisonExpress_prixExpresseExplicite() {
        ZoneLivraison zone = zoneThies();
        zone.setPrixExpress(BigDecimal.valueOf(8000));
        when(produitRepo.findById(1L)).thenReturn(Optional.of(produitNormal));
        when(zoneRepo.findById(10L)).thenReturn(Optional.of(zone));
        when(configRepo.findById(1L)).thenReturn(Optional.of(config));

        CalculCommandeService.CalculResultat r = service.calculer(
                10L, Commande.ModeLivraison.LIVRAISON_EXPRESS, List.of(ligne(1L, 1)));

        // supplement = prixExpress - prixStandard = 8000 - 5000 = 3000
        assertThat(r.supplementExpress()).isEqualByComparingTo(BigDecimal.valueOf(3000));
        assertThat(r.fraisLivraisonTotal()).isEqualByComparingTo(BigDecimal.valueOf(8000));
    }

    // ── 5. Express indisponible ─────────────────────────────────────────────

    @Test
    void livraisonExpress_indisponible_throwsException() {
        ZoneLivraison zone = zoneThies();
        zone.setExpressDisponible(false);
        when(produitRepo.findById(1L)).thenReturn(Optional.of(produitNormal));
        when(zoneRepo.findById(10L)).thenReturn(Optional.of(zone));

        assertThatThrownBy(() -> service.calculer(
                10L, Commande.ModeLivraison.LIVRAISON_EXPRESS, List.of(ligne(1L, 1))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("express");
    }

    // ── 6. Zone inactive ────────────────────────────────────────────────────

    @Test
    void zoneInactive_throwsException() {
        ZoneLivraison zone = zoneThies();
        zone.setActif(false);
        when(produitRepo.findById(1L)).thenReturn(Optional.of(produitNormal));
        when(zoneRepo.findById(10L)).thenReturn(Optional.of(zone));

        assertThatThrownBy(() -> service.calculer(
                10L, Commande.ModeLivraison.LIVRAISON_DOMICILE, List.of(ligne(1L, 1))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("disponible");
    }

    // ── 7. Seuil gratuit atteint (zone éligible) ────────────────────────────

    @Test
    void seuilGratuit_atteint_zoneEligible_fraisZeroMaisSupplementsConserves() {
        // Plateau Dakar : fraisBase=0, eligibleGratuit=true
        // Montant 200 000 > seuil 100 000 → livraison gratuite
        when(produitRepo.findById(1L)).thenReturn(Optional.of(produitNormal)); // 50 000 FCFA
        when(zoneRepo.findById(1L)).thenReturn(Optional.of(zoneDakar));
        when(configRepo.findById(1L)).thenReturn(Optional.of(config));

        CalculCommandeService.CalculResultat r = service.calculer(
                1L, Commande.ModeLivraison.LIVRAISON_DOMICILE, List.of(ligne(1L, 4))); // 200 000

        assertThat(r.livraisonGratuite()).isTrue();
        assertThat(r.fraisZone()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(r.fraisLivraisonTotal()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // ── 8. Seuil gratuit atteint mais zone NON éligible ─────────────────────

    @Test
    void seuilGratuit_atteint_zoneNonEligible_fraisConserves() {
        ZoneLivraison zone = zoneThies(); // eligibleLivraisonGratuite=false
        when(produitRepo.findById(1L)).thenReturn(Optional.of(produitNormal));
        when(zoneRepo.findById(10L)).thenReturn(Optional.of(zone));
        when(configRepo.findById(1L)).thenReturn(Optional.of(config));

        // 4 × 50 000 = 200 000 > seuil 100 000 — mais zone non éligible
        CalculCommandeService.CalculResultat r = service.calculer(
                10L, Commande.ModeLivraison.LIVRAISON_DOMICILE, List.of(ligne(1L, 4)));

        assertThat(r.livraisonGratuite()).isFalse();
        assertThat(r.fraisZone()).isEqualByComparingTo(BigDecimal.valueOf(5000));
    }

    // ── 9. Supplément gabarit lourd ─────────────────────────────────────────

    @Test
    void gabaritLourd_supplementAjoute() {
        ZoneLivraison zone = zoneThies();
        when(produitRepo.findById(2L)).thenReturn(Optional.of(produitLourd));
        when(zoneRepo.findById(10L)).thenReturn(Optional.of(zone));
        when(configRepo.findById(1L)).thenReturn(Optional.of(config));

        CalculCommandeService.CalculResultat r = service.calculer(
                10L, Commande.ModeLivraison.LIVRAISON_DOMICILE, List.of(ligne(2L, 1)));

        assertThat(r.supplementGabarit()).isEqualByComparingTo(BigDecimal.valueOf(3000));
        assertThat(r.fraisLivraisonTotal()).isEqualByComparingTo(BigDecimal.valueOf(8000)); // 5000+3000
    }

    // ── 10. zoneId null pour livraison → exception ──────────────────────────

    @Test
    void livraisonSansZone_throwsException() {
        when(produitRepo.findById(1L)).thenReturn(Optional.of(produitNormal));

        assertThatThrownBy(() -> service.calculer(
                null, Commande.ModeLivraison.LIVRAISON_DOMICILE, List.of(ligne(1L, 1))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("zoneId");
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private ZoneLivraison zoneThies() {
        ZoneLivraison z = new ZoneLivraison();
        z.setId(10L);
        z.setVille("thies");
        z.setVilleLabel("Thiès");
        z.setQuartier("thies-centre");
        z.setQuartierLabel("Thiès Centre");
        z.setPrixStandard(BigDecimal.valueOf(5000));
        z.setPrixExpress(null);
        z.setExpressDisponible(true);
        z.setActif(true);
        z.setEligibleLivraisonGratuite(false);
        z.setDelaiEstime("2-3 jours");
        return z;
    }
}
