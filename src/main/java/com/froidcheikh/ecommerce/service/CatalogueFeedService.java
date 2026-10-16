package com.froidcheikh.ecommerce.service;

import com.froidcheikh.ecommerce.entity.Categorie;
import com.froidcheikh.ecommerce.entity.ImageProduit;
import com.froidcheikh.ecommerce.entity.ParametresSite;
import com.froidcheikh.ecommerce.entity.Produit;
import com.froidcheikh.ecommerce.repository.ParametresSiteRepository;
import com.froidcheikh.ecommerce.repository.ProduitRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class CatalogueFeedService {

    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final int TITLE_MAX = 150;
    private static final int DESC_MAX  = 5000;

    private final ProduitRepository produitRepository;
    private final ParametresSiteRepository parametresSiteRepository;
    private final PrixService prixService;

    public CatalogueFeedService(ProduitRepository produitRepository,
                                ParametresSiteRepository parametresSiteRepository,
                                PrixService prixService) {
        this.produitRepository = produitRepository;
        this.parametresSiteRepository = parametresSiteRepository;
        this.prixService = prixService;
    }

    // ── Flux Meta ──────────────────────────────────────────────────────────────

    @Cacheable("feedMeta")
    @Transactional(readOnly = true)
    public String generateMetaFeed() {
        return buildFeed(loadSiteUrl(), loadDevise(), "meta");
    }

    // ── Flux Google ────────────────────────────────────────────────────────────

    @Cacheable("feedGoogle")
    @Transactional(readOnly = true)
    public String generateGoogleFeed() {
        return buildFeed(loadSiteUrl(), loadDevise(), "google");
    }

    // ── Rapport d'exclusions (admin) ───────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ExclusionDTO> getExclusions() {
        String siteUrl = loadSiteUrl();
        List<Produit> all = produitRepository.findAllForCatalogue();
        List<ExclusionDTO> result = new ArrayList<>();
        for (Produit p : all) {
            String raison = exclusionReason(p);
            if (raison != null) {
                result.add(new ExclusionDTO(
                        p.getIdProduit(),
                        p.getNomProduit(),
                        raison,
                        siteUrl + "/admin/produits/" + p.getIdProduit() + "/edit"
                ));
            }
        }
        result.sort(Comparator.comparing(ExclusionDTO::idProduit));
        return result;
    }

    // ── Invalidation cache ─────────────────────────────────────────────────────

    @Caching(evict = {
        @CacheEvict("feedMeta"),
        @CacheEvict("feedGoogle")
    })
    public void evictFeedCache() { /* appelé par ProduitService et PromotionService */ }

    // ── Construction XML ───────────────────────────────────────────────────────

    private String buildFeed(String siteUrl, String devise, String platform) {
        List<Produit> all = produitRepository.findAllForCatalogue();

        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<rss version=\"2.0\" xmlns:g=\"http://base.google.com/ns/1.0\">\n");
        sb.append("  <channel>\n");
        sb.append("    <title>").append(xml("Catalogue Froid Cheikh Anta Mbacké")).append("</title>\n");
        sb.append("    <link>").append(xml(siteUrl)).append("</link>\n");
        sb.append("    <description>Climatisation, réfrigération et électroménager au Sénégal</description>\n");

        for (Produit p : all) {
            if (exclusionReason(p) != null) continue;

            PrixService.PrixResultat prix = prixService.calculerPrix(p);
            String slug   = slugify(p.getNomProduit());
            String urlProd = siteUrl + "/produit/" + p.getIdProduit() + "-" + slug;

            sb.append("    <item>\n");
            sb.append("      <g:id>").append(p.getIdProduit()).append("</g:id>\n");
            sb.append("      <g:title>").append(xml(truncate(p.getNomProduit(), TITLE_MAX))).append("</g:title>\n");
            sb.append("      <g:description>").append(xml(truncate(stripHtml(p.getDescriptionProduit()), DESC_MAX))).append("</g:description>\n");
            sb.append("      <g:link>").append(xml(urlProd)).append("</g:link>\n");

            // Image principale (Cloudinary 1080×1080 c_pad b_white)
            imagePrincipale(p).ifPresent(img -> {
                String imgUrl = cloudinarySquare(img.getUrl());
                sb.append("      <g:image_link>").append(xml(imgUrl)).append("</g:image_link>\n");
            });

            // Images supplémentaires
            p.getImages().stream()
                    .filter(img -> !img.isPrincipale())
                    .limit(9)
                    .forEach(img -> sb.append("      <g:additional_image_link>")
                            .append(xml(img.getUrl())).append("</g:additional_image_link>\n"));

            // Disponibilité
            boolean inStock = Boolean.TRUE.equals(p.getDisponibilite())
                    && (p.getStockDisponible() == null || p.getStockDisponible() > 0);
            sb.append("      <g:availability>").append(inStock ? "in stock" : "out of stock").append("</g:availability>\n");
            sb.append("      <g:condition>new</g:condition>\n");

            // Prix
            sb.append("      <g:price>").append(prix.prixOrigine()).append(" ").append(devise).append("</g:price>\n");
            if (prix.hasPromo()) {
                sb.append("      <g:sale_price>").append(prix.prixEffectif()).append(" ").append(devise).append("</g:sale_price>\n");
                if (prix.dateFinPromo() != null) {
                    sb.append("      <g:sale_price_effective_date>")
                      .append("2000-01-01T00:00+00:00/")
                      .append(prix.dateFinPromo().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm+00:00")))
                      .append("</g:sale_price_effective_date>\n");
                }
            }

            // Marque
            sb.append("      <g:brand>").append(xml(p.getMarque().getNomMarque())).append("</g:brand>\n");

            // GTIN / MPN
            if (p.getEan() != null && !p.getEan().isBlank())
                sb.append("      <g:gtin>").append(xml(p.getEan())).append("</g:gtin>\n");
            if (p.getModeleFabricant() != null && !p.getModeleFabricant().isBlank())
                sb.append("      <g:mpn>").append(xml(p.getModeleFabricant())).append("</g:mpn>\n");

            // Catégorie interne (g:product_type)
            buildProductType(p.getCategorie()).ifPresent(pt ->
                sb.append("      <g:product_type>").append(xml(pt)).append("</g:product_type>\n"));

            sb.append("    </item>\n");
        }

        sb.append("  </channel>\n");
        sb.append("</rss>");
        return sb.toString();
    }

    // ── Logique d'exclusion ────────────────────────────────────────────────────

    String exclusionReason(Produit p) {
        if (!Boolean.TRUE.equals(p.getDisponibilite()))         return "Produit désactivé";
        if (Boolean.TRUE.equals(p.getExcluCatalogue()))         return "Exclu manuellement du catalogue";
        if (imagePrincipale(p).isEmpty())                       return "Pas d'image principale";
        if (p.getPrix() == null || p.getPrix().compareTo(BigDecimal.ZERO) <= 0) return "Prix absent ou nul";
        if (p.getMarque() == null)                              return "Marque manquante";
        if (p.getDescriptionProduit() == null || stripHtml(p.getDescriptionProduit()).isBlank())
                                                                return "Description manquante";
        return null;
    }

    // ── Utilitaires ────────────────────────────────────────────────────────────

    private java.util.Optional<ImageProduit> imagePrincipale(Produit p) {
        if (p.getImages() == null) return java.util.Optional.empty();
        return p.getImages().stream().filter(ImageProduit::isPrincipale).findFirst();
    }

    private java.util.Optional<String> buildProductType(Categorie cat) {
        if (cat == null) return java.util.Optional.empty();
        if (cat.getParent() != null && cat.getParent().getNomCategorie() != null)
            return java.util.Optional.of(cat.getParent().getNomCategorie() + " > " + cat.getNomCategorie());
        return java.util.Optional.of(cat.getNomCategorie());
    }

    /** Transforme une URL Cloudinary pour générer une image 1080×1080 c_pad b_white. */
    private String cloudinarySquare(String url) {
        if (url == null) return "";
        // Insère la transformation avant /upload/ si c'est une URL Cloudinary
        if (url.contains("/upload/")) {
            return url.replaceFirst("/upload/", "/upload/w_1080,h_1080,c_pad,b_white,f_jpg,q_auto/");
        }
        return url;
    }

    private String loadSiteUrl() {
        return parametresSiteRepository.findById(1L)
                .map(ParametresSite::getSiteUrl)
                .filter(u -> u != null && !u.isBlank())
                .orElse("http://localhost:4000");
    }

    private String loadDevise() {
        return parametresSiteRepository.findById(1L)
                .map(ParametresSite::getDevise)
                .filter(d -> d != null && !d.isBlank())
                .orElse("XOF");
    }

    /** Échappe les caractères spéciaux XML. */
    static String xml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    /** Retire les balises HTML et normalise les espaces. */
    static String stripHtml(String html) {
        if (html == null) return "";
        return html.replaceAll("<[^>]+>", " ")
                   .replaceAll("&[a-z]+;", " ")
                   .replaceAll("\\s+", " ")
                   .trim();
    }

    static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max);
    }

    static String slugify(String text) {
        if (text == null) return "";
        String norm = Normalizer.normalize(text, Normalizer.Form.NFD);
        return Pattern.compile("[\\p{InCombiningDiacriticalMarks}]")
                .matcher(norm).replaceAll("")
                .toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
    }

    // ── DTO rapport exclusions ─────────────────────────────────────────────────

    public record ExclusionDTO(Long idProduit, String nomProduit, String raison, String lienAdmin) {}
}
