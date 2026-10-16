package com.froidcheikh.ecommerce.controller;

import com.froidcheikh.ecommerce.entity.Categorie;
import com.froidcheikh.ecommerce.entity.ParametresSite;
import com.froidcheikh.ecommerce.entity.Produit;
import com.froidcheikh.ecommerce.repository.CategorieRepository;
import com.froidcheikh.ecommerce.repository.ParametresSiteRepository;
import com.froidcheikh.ecommerce.repository.ProduitRepository;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.text.Normalizer;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.regex.Pattern;

@RestController
public class SitemapController {

    private final ParametresSiteRepository parametresSiteRepository;
    private final ProduitRepository produitRepository;
    private final CategorieRepository categorieRepository;

    public SitemapController(ParametresSiteRepository parametresSiteRepository,
                             ProduitRepository produitRepository,
                             CategorieRepository categorieRepository) {
        this.parametresSiteRepository = parametresSiteRepository;
        this.produitRepository = produitRepository;
        this.categorieRepository = categorieRepository;
    }

    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> sitemap() {
        String baseUrl = parametresSiteRepository.findById(1L)
                .map(ParametresSite::getSiteUrl)
                .filter(u -> u != null && !u.isBlank())
                .orElse("http://localhost:4000");

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");

        // Pages fixes
        addUrl(xml, baseUrl + "/", "weekly", "1.0", null);
        addUrl(xml, baseUrl + "/produits", "daily", "0.9", null);
        addUrl(xml, baseUrl + "/a-propos", "monthly", "0.7", null);
        addUrl(xml, baseUrl + "/contact", "monthly", "0.7", null);

        // Catégories
        List<Categorie> categories = categorieRepository.findAll();
        for (Categorie cat : categories) {
            if (cat.getNomCategorie() == null) continue;
            String slug = slugify(cat.getNomCategorie());
            String loc = baseUrl + "/produits/categorie/" + cat.getIdCategorie() + "-" + slug;
            addUrl(xml, loc, "weekly", "0.8", null);
        }

        // Produits actifs (stockDisponible > 0)
        List<Produit> produits = produitRepository.findAll();
        for (Produit p : produits) {
            if (p.getNomProduit() == null) continue;
            if (p.getStockDisponible() != null && p.getStockDisponible() <= 0) continue;
            String slug = slugify(p.getNomProduit());
            String loc = baseUrl + "/produit/" + p.getIdProduit() + "-" + slug;
            String lastmod = p.getDateModification() != null
                    ? p.getDateModification().format(fmt) : null;
            addUrl(xml, loc, "weekly", "0.9", lastmod);
        }

        xml.append("</urlset>");
        return ResponseEntity.ok(xml.toString());
    }

    private void addUrl(StringBuilder xml, String loc, String changefreq, String priority, String lastmod) {
        xml.append("  <url>\n");
        xml.append("    <loc>").append(escapeXml(loc)).append("</loc>\n");
        if (lastmod != null) xml.append("    <lastmod>").append(lastmod).append("</lastmod>\n");
        xml.append("    <changefreq>").append(changefreq).append("</changefreq>\n");
        xml.append("    <priority>").append(priority).append("</priority>\n");
        xml.append("  </url>\n");
    }

    private String escapeXml(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    public static String slugify(String text) {
        if (text == null) return "";
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFD);
        String withoutAccents = Pattern.compile("[\\p{InCombiningDiacriticalMarks}]")
                .matcher(normalized).replaceAll("");
        return withoutAccents.toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
    }
}
