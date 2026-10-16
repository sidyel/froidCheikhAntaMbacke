package com.froidcheikh.ecommerce.service;

import com.froidcheikh.ecommerce.entity.*;
import com.froidcheikh.ecommerce.repository.ParametresSiteRepository;
import com.froidcheikh.ecommerce.repository.ProduitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CatalogueFeedServiceTest {

    @Mock ProduitRepository produitRepository;
    @Mock ParametresSiteRepository parametresSiteRepository;
    @Mock PrixService prixService;

    CatalogueFeedService service;

    @BeforeEach
    void setUp() {
        service = new CatalogueFeedService(produitRepository, parametresSiteRepository, prixService);
    }

    // ── Utilitaires statiques ──────────────────────────────────────────────────

    @Test
    void xml_escapes_special_chars() {
        assertThat(CatalogueFeedService.xml("Climatiseur & co <Super>"))
                .isEqualTo("Climatiseur &amp; co &lt;Super&gt;");
    }

    @Test
    void xml_escapes_accents_without_change() {
        assertThat(CatalogueFeedService.xml("Réfrigérateur élégant")).isEqualTo("Réfrigérateur élégant");
    }

    @Test
    void stripHtml_removes_tags_and_normalizes_spaces() {
        String html = "<p>Climatiseur  <b>split</b>  &nbsp;  Astech</p>";
        assertThat(CatalogueFeedService.stripHtml(html)).isEqualTo("Climatiseur split Astech");
    }

    @Test
    void truncate_respects_max_length() {
        String s = "a".repeat(200);
        assertThat(CatalogueFeedService.truncate(s, 150)).hasSize(150);
        assertThat(CatalogueFeedService.truncate("court", 150)).isEqualTo("court");
    }

    @Test
    void slugify_removes_accents_and_normalizes() {
        assertThat(CatalogueFeedService.slugify("Climatiseur Réversible – Été")).isEqualTo("climatiseur-reversible-ete");
    }

    // ── Logique d'exclusion ────────────────────────────────────────────────────

    @Test
    void exclusion_produit_desactive() {
        Produit p = produitAvecTout();
        p.setDisponibilite(false);
        assertThat(service.exclusionReason(p)).isEqualTo("Produit désactivé");
    }

    @Test
    void exclusion_exclu_manuellement() {
        Produit p = produitAvecTout();
        p.setExcluCatalogue(true);
        assertThat(service.exclusionReason(p)).isEqualTo("Exclu manuellement du catalogue");
    }

    @Test
    void exclusion_sans_image_principale() {
        Produit p = produitAvecTout();
        p.getImages().get(0).setPrincipale(false);
        assertThat(service.exclusionReason(p)).isEqualTo("Pas d'image principale");
    }

    @Test
    void exclusion_prix_nul() {
        Produit p = produitAvecTout();
        p.setPrix(null);
        assertThat(service.exclusionReason(p)).isEqualTo("Prix absent ou nul");
    }

    @Test
    void exclusion_prix_zero() {
        Produit p = produitAvecTout();
        p.setPrix(BigDecimal.ZERO);
        assertThat(service.exclusionReason(p)).isEqualTo("Prix absent ou nul");
    }

    @Test
    void exclusion_marque_manquante() {
        Produit p = produitAvecTout();
        p.setMarque(null);
        assertThat(service.exclusionReason(p)).isEqualTo("Marque manquante");
    }

    @Test
    void exclusion_description_manquante() {
        Produit p = produitAvecTout();
        p.setDescriptionProduit(null);
        assertThat(service.exclusionReason(p)).isEqualTo("Description manquante");
    }

    @Test
    void exclusion_description_html_vide_apres_strip() {
        Produit p = produitAvecTout();
        p.setDescriptionProduit("<p>  </p>");
        assertThat(service.exclusionReason(p)).isEqualTo("Description manquante");
    }

    @Test
    void produit_complet_non_exclu() {
        Produit p = produitAvecTout();
        assertThat(service.exclusionReason(p)).isNull();
    }

    @Test
    void stock_zero_non_exclu_du_flux() {
        Produit p = produitAvecTout();
        p.setStockDisponible(0);
        assertThat(service.exclusionReason(p)).isNull();
    }

    // ── Génération du flux ─────────────────────────────────────────────────────

    @Test
    void flux_contient_produit_en_rupture_avec_out_of_stock() {
        Produit p = produitAvecTout();
        p.setStockDisponible(0);

        when(produitRepository.findAllForCatalogue()).thenReturn(List.of(p));
        when(parametresSiteRepository.findById(1L)).thenReturn(Optional.of(parametresSiteAvecUrl("https://example.sn")));
        when(prixService.calculerPrix(p)).thenReturn(new PrixService.PrixResultat(new BigDecimal("250000"), new BigDecimal("250000"), null, null, null, null));

        String xml = service.generateMetaFeed();
        assertThat(xml).contains("<g:availability>out of stock</g:availability>");
    }

    @Test
    void flux_contient_sale_price_pour_produit_en_promo() {
        Produit p = produitAvecTout();

        PrixService.PrixResultat promo = new PrixService.PrixResultat(
                new BigDecimal("300000"), new BigDecimal("240000"), 20,
                LocalDateTime.now().plusDays(7), 1L, "Promo été");

        when(produitRepository.findAllForCatalogue()).thenReturn(List.of(p));
        when(parametresSiteRepository.findById(1L)).thenReturn(Optional.of(parametresSiteAvecUrl("https://example.sn")));
        when(prixService.calculerPrix(p)).thenReturn(promo);

        String xml = service.generateMetaFeed();
        assertThat(xml).contains("<g:price>300000");
        assertThat(xml).contains("<g:sale_price>240000");
        assertThat(xml).contains("g:sale_price_effective_date");
    }

    @Test
    void flux_exclut_produit_sans_marque() {
        Produit exclu = produitAvecTout();
        exclu.setMarque(null);

        when(produitRepository.findAllForCatalogue()).thenReturn(List.of(exclu));
        when(parametresSiteRepository.findById(1L)).thenReturn(Optional.of(parametresSiteAvecUrl("https://example.sn")));

        String xml = service.generateMetaFeed();
        assertThat(xml).doesNotContain("<item>");
    }

    @Test
    void flux_encode_correctement_caracteres_speciaux() {
        Produit p = produitAvecTout();
        p.setNomProduit("Climatiseur & Réfrigérateur <Pro>");
        p.setDescriptionProduit("Modele top tres efficace");

        when(produitRepository.findAllForCatalogue()).thenReturn(List.of(p));
        when(parametresSiteRepository.findById(1L)).thenReturn(Optional.of(parametresSiteAvecUrl("https://example.sn")));
        when(prixService.calculerPrix(p)).thenReturn(new PrixService.PrixResultat(new BigDecimal("250000"), new BigDecimal("250000"), null, null, null, null));

        String xml = service.generateMetaFeed();
        assertThat(xml).contains("&amp;");
        assertThat(xml).contains("&lt;Pro&gt;");
    }

    @Test
    void flux_contient_product_type_avec_chemin_categorie() {
        Produit p = produitAvecTout();
        Categorie parent = new Categorie();
        parent.setNomCategorie("Climatisation");
        Categorie cat = new Categorie();
        cat.setNomCategorie("Split");
        cat.setParent(parent);
        p.setCategorie(cat);

        when(produitRepository.findAllForCatalogue()).thenReturn(List.of(p));
        when(parametresSiteRepository.findById(1L)).thenReturn(Optional.of(parametresSiteAvecUrl("https://example.sn")));
        when(prixService.calculerPrix(p)).thenReturn(new PrixService.PrixResultat(new BigDecimal("250000"), new BigDecimal("250000"), null, null, null, null));

        String xml = service.generateMetaFeed();
        assertThat(xml).contains("<g:product_type>Climatisation &gt; Split</g:product_type>");
    }

    // ── Rapport exclusions ─────────────────────────────────────────────────────

    @Test
    void rapport_exclusions_liste_raisons() {
        Produit ok      = produitAvecTout();
        Produit exclu   = produitAvecTout();
        exclu.setIdProduit(2L);
        exclu.setNomProduit("Produit exclu");
        exclu.setMarque(null);

        when(produitRepository.findAllForCatalogue()).thenReturn(List.of(ok, exclu));
        when(parametresSiteRepository.findById(1L)).thenReturn(Optional.of(parametresSiteAvecUrl("https://example.sn")));

        List<CatalogueFeedService.ExclusionDTO> exclusions = service.getExclusions();
        assertThat(exclusions).hasSize(1);
        assertThat(exclusions.get(0).idProduit()).isEqualTo(2L);
        assertThat(exclusions.get(0).raison()).isEqualTo("Marque manquante");
        assertThat(exclusions.get(0).lienAdmin()).contains("/admin/produits/2/edit");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Produit produitAvecTout() {
        Produit p = new Produit();
        p.setIdProduit(1L);
        p.setNomProduit("Climatiseur Astech 9000 BTU");
        p.setDescriptionProduit("Climatiseur split très efficace pour les régions tropicales.");
        p.setPrix(new BigDecimal("250000"));
        p.setDisponibilite(true);
        p.setExcluCatalogue(false);
        p.setStockDisponible(5);

        Marque m = new Marque();
        m.setIdMarque(1L);
        m.setNomMarque("Astech");
        p.setMarque(m);

        Categorie cat = new Categorie();
        cat.setIdCategorie(1L);
        cat.setNomCategorie("Climatiseur Split");
        p.setCategorie(cat);

        ImageProduit img = new ImageProduit();
        img.setIdImage(1L);
        img.setUrl("https://res.cloudinary.com/test/image/upload/produits/img.jpg");
        img.setPrincipale(true);
        img.setOrdre(0);
        p.setImages(new java.util.ArrayList<>(List.of(img)));

        return p;
    }

    private ParametresSite parametresSiteAvecUrl(String url) {
        ParametresSite ps = new ParametresSite();
        ps.setSiteUrl(url);
        ps.setDevise("XOF");
        return ps;
    }
}
