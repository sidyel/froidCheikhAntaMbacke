package com.froidcheikh.ecommerce.service;

import com.froidcheikh.ecommerce.entity.Categorie;
import com.froidcheikh.ecommerce.entity.Marque;
import com.froidcheikh.ecommerce.entity.Produit;
import com.froidcheikh.ecommerce.entity.Promotion;
import com.froidcheikh.ecommerce.repository.CategorieRepository;
import com.froidcheikh.ecommerce.repository.ProduitRepository;
import com.froidcheikh.ecommerce.repository.PromotionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PrixServiceTest {

    @Mock private PromotionRepository promotionRepository;
    @Mock private ParametresSiteService parametresSiteService;
    @Mock private CategorieRepository categorieRepository;
    @Mock private ProduitRepository produitRepository;

    // Africa/Dakar = UTC+0 — instant UTC == LDT Dakar
    private static final ZoneId ZONE = ZoneId.of("Africa/Dakar");
    private static final Instant NOW_I = Instant.parse("2026-06-15T10:00:00Z");
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 6, 15, 10, 0);

    private PrixService service;

    @BeforeEach
    void setUp() throws Exception {
        service = newService(Clock.fixed(NOW_I, ZoneId.of("UTC")));
        lenient().when(parametresSiteService.getSiteZoneId()).thenReturn(ZONE);
        lenient().when(promotionRepository.findCandidates(any())).thenReturn(List.of());
        lenient().when(categorieRepository.findIdsByParentId(any())).thenReturn(List.of());
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private PrixService newService(Clock clock) throws Exception {
        PrixService svc = new PrixService(promotionRepository, parametresSiteService,
                categorieRepository, produitRepository, clock);
        Field self = PrixService.class.getDeclaredField("self");
        self.setAccessible(true);
        self.set(svc, svc);
        return svc;
    }

    private Produit produit(long id, BigDecimal prix) {
        Produit p = new Produit();
        p.setIdProduit(id);
        p.setPrix(prix);
        return p;
    }

    private Promotion promo(long id, String nom, Promotion.TypePromotion type, BigDecimal valeur,
                             Promotion.CiblePromotion cible, int priorite,
                             LocalDateTime debut, LocalDateTime fin) {
        Promotion p = new Promotion();
        p.setIdPromotion(id);
        p.setNom(nom);
        p.setTypePromotion(type);
        p.setValeur(valeur);
        p.setCible(cible);
        p.setPriorite(priorite);
        p.setDateDebut(debut);
        p.setDateFin(fin);
        p.setActif(true);
        return p;
    }

    private Promotion promoTous(long id, Promotion.TypePromotion type, BigDecimal valeur) {
        return promo(id, "Promo " + id, type, valeur, Promotion.CiblePromotion.TOUS, 0,
                NOW.minusDays(1), NOW.plusDays(30));
    }

    // ── 1. Pourcentage basique ────────────────────────────────────────────────

    @Test
    void pourcentage_basique() {
        Produit clim = produit(1L, BigDecimal.valueOf(250_000));
        when(promotionRepository.findCandidates(any()))
                .thenReturn(List.of(promoTous(10L, Promotion.TypePromotion.POURCENTAGE, BigDecimal.valueOf(20))));

        PrixService.PrixResultat r = service.calculerPrix(clim);

        assertThat(r.hasPromo()).isTrue();
        assertThat(r.prixEffectif()).isEqualByComparingTo(BigDecimal.valueOf(200_000));
        assertThat(r.pourcentageReduction()).isEqualTo(20);
        assertThat(r.promotionId()).isEqualTo(10L);
    }

    // ── 2. Montant fixe basique ───────────────────────────────────────────────

    @Test
    void montantFixe_basique() {
        Produit frigo = produit(2L, BigDecimal.valueOf(100_000));
        when(promotionRepository.findCandidates(any()))
                .thenReturn(List.of(promoTous(20L, Promotion.TypePromotion.MONTANT_FIXE, BigDecimal.valueOf(15_000))));

        PrixService.PrixResultat r = service.calculerPrix(frigo);

        assertThat(r.prixEffectif()).isEqualByComparingTo(BigDecimal.valueOf(85_000));
        assertThat(r.hasPromo()).isTrue();
    }

    // ── 3. Plusieurs promos : meilleur prix retenu ────────────────────────────

    @Test
    void plusieursPromos_meilleeurPrix() {
        // Réfrigérateur 180 000 FCFA : promo A -10%, promo B -15%
        Produit frigo = produit(3L, BigDecimal.valueOf(180_000));
        Promotion a = promo(1L, "Promo -10", Promotion.TypePromotion.POURCENTAGE, BigDecimal.TEN,
                Promotion.CiblePromotion.TOUS, 0, NOW.minusDays(1), NOW.plusDays(7));
        Promotion b = promo(2L, "Promo -15", Promotion.TypePromotion.POURCENTAGE, BigDecimal.valueOf(15),
                Promotion.CiblePromotion.TOUS, 0, NOW.minusDays(1), NOW.plusDays(7));
        when(promotionRepository.findCandidates(any())).thenReturn(List.of(a, b));

        PrixService.PrixResultat r = service.calculerPrix(frigo);

        // 180 000 * 0.85 = 153 000
        assertThat(r.prixEffectif()).isEqualByComparingTo(BigDecimal.valueOf(153_000));
        assertThat(r.promotionId()).isEqualTo(2L);
    }

    // ── 4. Égalité de prix : départagée par priorité (plus haute gagne) ──────

    @Test
    void egalitePrix_departageeParPriorite() {
        Produit p = produit(4L, BigDecimal.valueOf(100_000));
        Promotion basse = promo(1L, "Prio 1", Promotion.TypePromotion.MONTANT_FIXE, BigDecimal.valueOf(10_000),
                Promotion.CiblePromotion.TOUS, 1, NOW.minusDays(1), NOW.plusDays(7));
        Promotion haute = promo(2L, "Prio 5", Promotion.TypePromotion.MONTANT_FIXE, BigDecimal.valueOf(10_000),
                Promotion.CiblePromotion.TOUS, 5, NOW.minusDays(1), NOW.plusDays(7));
        when(promotionRepository.findCandidates(any())).thenReturn(List.of(basse, haute));

        PrixService.PrixResultat r = service.calculerPrix(p);

        assertThat(r.promotionId()).isEqualTo(2L); // priorité 5 > 1
    }

    // ── 5. Égalité totale : id le plus petit gagne ────────────────────────────

    @Test
    void egaliteTotale_departageeParIdLePlusPetit() {
        Produit p = produit(5L, BigDecimal.valueOf(100_000));
        Promotion gros = promo(5L, "Id 5", Promotion.TypePromotion.MONTANT_FIXE, BigDecimal.valueOf(10_000),
                Promotion.CiblePromotion.TOUS, 0, NOW.minusDays(1), NOW.plusDays(7));
        Promotion petit = promo(3L, "Id 3", Promotion.TypePromotion.MONTANT_FIXE, BigDecimal.valueOf(10_000),
                Promotion.CiblePromotion.TOUS, 0, NOW.minusDays(1), NOW.plusDays(7));
        when(promotionRepository.findCandidates(any())).thenReturn(List.of(gros, petit));

        PrixService.PrixResultat r = service.calculerPrix(p);

        assertThat(r.promotionId()).isEqualTo(3L); // id 3 < id 5
    }

    // ── 6. Promo pas encore commencée : ignorée ───────────────────────────────

    @Test
    void promoPassNotStarted_nonAppliquee() {
        Produit p = produit(6L, BigDecimal.valueOf(50_000));
        Promotion future = promo(30L, "Future", Promotion.TypePromotion.POURCENTAGE, BigDecimal.TEN,
                Promotion.CiblePromotion.TOUS, 0, NOW.plusHours(1), NOW.plusDays(7));
        when(promotionRepository.findCandidates(any())).thenReturn(List.of(future));

        PrixService.PrixResultat r = service.calculerPrix(p);

        assertThat(r.hasPromo()).isFalse();
        assertThat(r.prixEffectif()).isEqualByComparingTo(BigDecimal.valueOf(50_000));
    }

    // ── 7. Promo expirée : ignorée ────────────────────────────────────────────

    @Test
    void promoExpiree_nonAppliquee() {
        Produit p = produit(7L, BigDecimal.valueOf(50_000));
        Promotion expiree = promo(40L, "Expirée", Promotion.TypePromotion.POURCENTAGE, BigDecimal.TEN,
                Promotion.CiblePromotion.TOUS, 0, NOW.minusDays(10), NOW.minusHours(1));
        when(promotionRepository.findCandidates(any())).thenReturn(List.of(expiree));

        PrixService.PrixResultat r = service.calculerPrix(p);

        assertThat(r.hasPromo()).isFalse();
    }

    // ── 8. Promo inactive : le repo ne la renvoie pas (actif=false filtré) ───

    @Test
    void promoInactive_nonAppliquee() {
        // findCandidates filtre actif=true dans la base ; ici on simule l'absence de retour
        when(promotionRepository.findCandidates(any())).thenReturn(List.of());

        PrixService.PrixResultat r = service.calculerPrix(produit(8L, BigDecimal.valueOf(50_000)));

        assertThat(r.hasPromo()).isFalse();
    }

    // ── 9. Cible CATEGORIES : produit de bonne catégorie ─────────────────────

    @Test
    void cibleCategorie_produitBonneCategorie() {
        Categorie cat = new Categorie();
        cat.setIdCategorie(100L);

        Produit p = produit(9L, BigDecimal.valueOf(80_000));
        p.setCategorie(cat);

        Promotion promo = promo(50L, "Cat promo", Promotion.TypePromotion.POURCENTAGE, BigDecimal.TEN,
                Promotion.CiblePromotion.CATEGORIES, 0, NOW.minusDays(1), NOW.plusDays(7));
        promo.getCategoriesCibles().add(cat);

        when(promotionRepository.findCandidates(any())).thenReturn(List.of(promo));
        when(categorieRepository.findIdsByParentId(100L)).thenReturn(List.of());

        PrixService.PrixResultat r = service.calculerPrix(p);

        // 80 000 * 0.9 = 72 000
        assertThat(r.hasPromo()).isTrue();
        assertThat(r.prixEffectif()).isEqualByComparingTo(BigDecimal.valueOf(72_000));
    }

    // ── 10. Cible sous-catégorie niveau 2 : expansion BFS ────────────────────

    @Test
    void cibleSousCategorie_niveau2() {
        Categorie parent = new Categorie();
        parent.setIdCategorie(10L);
        Categorie enfant = new Categorie();
        enfant.setIdCategorie(20L);
        Categorie petitEnfant = new Categorie();
        petitEnfant.setIdCategorie(30L);

        Produit p = produit(10L, BigDecimal.valueOf(60_000));
        p.setCategorie(petitEnfant); // produit dans la sous-sous-catégorie

        Promotion promo = promo(60L, "BFS promo", Promotion.TypePromotion.POURCENTAGE, BigDecimal.valueOf(5),
                Promotion.CiblePromotion.CATEGORIES, 0, NOW.minusDays(1), NOW.plusDays(7));
        promo.getCategoriesCibles().add(parent);

        when(promotionRepository.findCandidates(any())).thenReturn(List.of(promo));
        when(categorieRepository.findIdsByParentId(10L)).thenReturn(List.of(20L));
        when(categorieRepository.findIdsByParentId(20L)).thenReturn(List.of(30L));
        when(categorieRepository.findIdsByParentId(30L)).thenReturn(List.of());

        PrixService.PrixResultat r = service.calculerPrix(p);

        // 60 000 * 0.95 = 57 000
        assertThat(r.hasPromo()).isTrue();
        assertThat(r.prixEffectif()).isEqualByComparingTo(BigDecimal.valueOf(57_000));
    }

    // ── 11. Cible MARQUES : produit hors marque → ignoré ─────────────────────

    @Test
    void cibleMarque_produitHorsMarque_ignore() {
        Marque m1 = new Marque();
        m1.setIdMarque(1L);
        Marque m2 = new Marque();
        m2.setIdMarque(2L);

        Produit p = produit(11L, BigDecimal.valueOf(70_000));
        p.setMarque(m2); // produit appartient à la marque 2

        Promotion promo = promo(70L, "Marque 1 only", Promotion.TypePromotion.POURCENTAGE, BigDecimal.TEN,
                Promotion.CiblePromotion.MARQUES, 0, NOW.minusDays(1), NOW.plusDays(7));
        promo.getMarquesCibles().add(m1); // promo cible uniquement la marque 1

        when(promotionRepository.findCandidates(any())).thenReturn(List.of(promo));

        PrixService.PrixResultat r = service.calculerPrix(p);

        assertThat(r.hasPromo()).isFalse();
        assertThat(r.prixEffectif()).isEqualByComparingTo(BigDecimal.valueOf(70_000));
    }

    // ── 12. Prix plancher : promo non appliquée si résultat ≤ 0 ─────────────

    @Test
    void plancher_promoNonAppliquee() {
        // Produit 5 000 FCFA, remise fixe 8 000 FCFA → calcul brut = -3 000 → planché à 0 → filtré
        Produit p = produit(12L, BigDecimal.valueOf(5_000));
        when(promotionRepository.findCandidates(any()))
                .thenReturn(List.of(promoTous(80L, Promotion.TypePromotion.MONTANT_FIXE, BigDecimal.valueOf(8_000))));

        PrixService.PrixResultat r = service.calculerPrix(p);

        assertThat(r.hasPromo()).isFalse();
        assertThat(r.prixEffectif()).isEqualByComparingTo(BigDecimal.valueOf(5_000));
    }

    // ── 13. Passage de l'heure de fin : avant=promo active, après=ignorée ────
    // (le nom de promo contient des accents UTF-8 pour vérifier l'encodage)

    @Test
    void passageHeureFin_avantPuisApres() throws Exception {
        LocalDateTime dateFin = NOW.plusMinutes(30);
        Promotion promoEte = promo(90L, "Soldes été — spécial frigo",
                Promotion.TypePromotion.POURCENTAGE, BigDecimal.TEN,
                Promotion.CiblePromotion.TOUS, 0, NOW.minusDays(1), dateFin);

        Produit p = produit(13L, BigDecimal.valueOf(100_000));
        when(promotionRepository.findCandidates(any())).thenReturn(List.of(promoEte));

        // Avant la fin : promo active
        PrixService.PrixResultat avant = service.calculerPrix(p);
        assertThat(avant.hasPromo()).isTrue();
        assertThat(avant.prixEffectif()).isEqualByComparingTo(BigDecimal.valueOf(90_000));
        assertThat(avant.nomPromotion()).isEqualTo("Soldes été — spécial frigo");

        // Après la fin : Clock avancée d'une heure
        PrixService serviceApres = newService(Clock.fixed(NOW_I.plusSeconds(3_600), ZoneId.of("UTC")));
        PrixService.PrixResultat apres = serviceApres.calculerPrix(p);
        assertThat(apres.hasPromo()).isFalse();
        assertThat(apres.prixEffectif()).isEqualByComparingTo(BigDecimal.valueOf(100_000));
    }
}
