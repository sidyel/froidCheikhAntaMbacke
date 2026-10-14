package com.froidcheikh.ecommerce;

import com.froidcheikh.ecommerce.entity.Produit;
import com.froidcheikh.ecommerce.repository.ProduitRepository;
import com.froidcheikh.ecommerce.service.ProduitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Génère produits_a_enrichir.json dans le répertoire de travail courant.
 * Contient les produits dont le score de complétude < 80 %.
 * Exécution : mvn spring-boot:run -Dspring-boot.run.profiles=export
 */
@Component
@Profile("export")
@RequiredArgsConstructor
@Slf4j
public class ExportEnrichissementRunner implements CommandLineRunner {

    private final ProduitRepository produitRepository;
    private final ProduitService produitService;

    @Override
    public void run(String... args) throws IOException {
        log.info("══════════════════════════════════════════");
        log.info("  Export produits_a_enrichir.json");
        log.info("══════════════════════════════════════════");

        List<Produit> produits = produitRepository.findAll();

        List<String> lignes = produits.stream()
                .filter(p -> produitService.calculerScore(p) < 80)
                .map(p -> buildJson(p, produitService.calculerScore(p)))
                .collect(Collectors.toList());

        String json = "[\n" + String.join(",\n", lignes) + "\n]";
        Path out = Path.of("produits_a_enrichir.json");
        Files.writeString(out, json);

        log.info("  {} produit(s) avec score < 80 exportés → {}", lignes.size(), out.toAbsolutePath());
        log.info("══════════════════════════════════════════");
    }

    private String buildJson(Produit p, int score) {
        String ean = p.getEan() != null ? "\"" + esc(p.getEan()) + "\"" : "null";
        String modele = p.getModeleFabricant() != null ? "\"" + esc(p.getModeleFabricant()) + "\"" : "null";
        String desc = p.getDescriptionProduit();
        int descLen = desc != null ? desc.length() : 0;
        int nbImages = p.getImages() != null ? p.getImages().size() : 0;
        int nbAttributs = p.getAttributs() != null ? p.getAttributs().size() : 0;
        return String.format(
                "  {\"id\":%d,\"nom\":\"%s\",\"score\":%d," +
                "\"nbImages\":%d,\"descLen\":%d,\"nbAttributs\":%d," +
                "\"ean\":%s,\"modeleFabricant\":%s}",
                p.getIdProduit(), esc(p.getNomProduit()), score,
                nbImages, descLen, nbAttributs, ean, modele);
    }

    private String esc(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
