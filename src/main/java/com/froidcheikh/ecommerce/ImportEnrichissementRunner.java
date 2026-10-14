package com.froidcheikh.ecommerce;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.froidcheikh.ecommerce.dto.enrichissement.EnrichissementAttribut;
import com.froidcheikh.ecommerce.dto.enrichissement.EnrichissementColonnesTechniques;
import com.froidcheikh.ecommerce.dto.enrichissement.EnrichissementProduit;
import com.froidcheikh.ecommerce.dto.enrichissement.EnrichissementValeur;
import com.froidcheikh.ecommerce.entity.AttributProduit;
import com.froidcheikh.ecommerce.entity.ModeleAttribut;
import com.froidcheikh.ecommerce.entity.Produit;
import com.froidcheikh.ecommerce.repository.AttributProduitRepository;
import com.froidcheikh.ecommerce.repository.ModeleAttributRepository;
import com.froidcheikh.ecommerce.repository.ProduitRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * Importe un lot d'enrichissement de fiches produits depuis un fichier JSON.
 *
 * Simulation (défaut) :
 *   mvn spring-boot:run -Dspring-boot.run.profiles=import-enrichissement \
 *       -Dspring-boot.run.jvmArguments="-Dimport.fichier=enrichissement_lot_1.json"
 *
 * Écriture réelle :
 *   ... -Dspring-boot.run.jvmArguments="-Dimport.fichier=enrichissement_lot_1.json -Dimport.confirmer=true"
 */
@Component
@Profile("import-enrichissement")
@RequiredArgsConstructor
@Slf4j
public class ImportEnrichissementRunner implements CommandLineRunner {

    private static final String VALIDE = "valide";

    @Value("${import.fichier:enrichissement_lot_1.json}")
    private String fichier;

    @Value("${import.confirmer:false}")
    private boolean confirmer;

    private final ProduitRepository produitRepository;
    private final AttributProduitRepository attributProduitRepository;
    private final ModeleAttributRepository modeleAttributRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        log.info("══════════════════════════════════════════════════════");
        log.info("  Import enrichissement — fichier : {}", fichier);
        log.info("  Mode : {}", confirmer ? "ÉCRITURE (persistance activée)" : "SIMULATION (aucune écriture)");
        log.info("══════════════════════════════════════════════════════");

        Path path = Path.of(fichier);
        if (!Files.exists(path)) {
            log.error("Fichier introuvable : {}", path.toAbsolutePath());
            return;
        }

        List<EnrichissementProduit> lots = objectMapper.readValue(
                path.toFile(), new TypeReference<>() {});

        int nbModifies = 0, nbChamps = 0, nbIgnores = 0;

        for (EnrichissementProduit ep : lots) {
            Optional<Produit> opt = produitRepository.findById(ep.getIdProduit());
            if (opt.isEmpty()) {
                log.warn("  ⚠  Produit {} introuvable — ignoré", ep.getIdProduit());
                nbIgnores++;
                continue;
            }
            Produit p = opt.get();
            log.info("  ─── Produit {} : {} ───", p.getIdProduit(), p.getNomProduit());

            int champsProduit = 0;
            int champsIgnores = 0;

            // ── Champs scalaires ──────────────────────────────────────────────────
            if (ep.getDescription() != null) {
                String actuel = p.getDescriptionProduit();
                if (estVide(actuel) || ep.isEcraser()) {
                    log(actuel, ep.getDescription(), "description");
                    if (confirmer) p.setDescriptionProduit(ep.getDescription());
                    champsProduit++;
                } else {
                    log.info("    SKIP description (déjà remplie, ecraser=false)");
                }
            }

            if (ep.getModeleFabricant() != null) {
                String actuel = p.getModeleFabricant();
                if (estVide(actuel) || ep.isEcraser()) {
                    log(actuel, ep.getModeleFabricant(), "modeleFabricant");
                    if (confirmer) p.setModeleFabricant(ep.getModeleFabricant());
                    champsProduit++;
                } else {
                    log.info("    SKIP modeleFabricant (déjà rempli, ecraser=false)");
                }
            }

            if (ep.getEan() != null) {
                String actuel = p.getEan();
                if (estVide(actuel) || ep.isEcraser()) {
                    log(actuel, ep.getEan(), "ean");
                    if (confirmer) p.setEan(ep.getEan());
                    champsProduit++;
                } else {
                    log.info("    SKIP ean (déjà rempli, ecraser=false)");
                }
            }

            // ── Colonnes techniques ───────────────────────────────────────────────
            if (ep.getColonnesTechniques() != null) {
                EnrichissementColonnesTechniques ct = ep.getColonnesTechniques();
                int[] res = appliquerColonnes(p, ct);
                champsProduit += res[0];
                champsIgnores += res[1];
            }

            // ── Attributs dynamiques ──────────────────────────────────────────────
            if (ep.getAttributs() != null) {
                List<AttributProduit> existants = attributProduitRepository.findByProduitIdProduit(p.getIdProduit());
                for (EnrichissementAttribut ea : ep.getAttributs()) {
                    if (!VALIDE.equalsIgnoreCase(ea.getConfiance())) {
                        log.info("    IGNORE attribut '{}' (confiance={})", ea.getNom(), ea.getConfiance());
                        champsIgnores++;
                        continue;
                    }
                    boolean dejaPresent = existants.stream()
                            .anyMatch(a -> a.getNomAttribut().equalsIgnoreCase(ea.getNom()));
                    if (dejaPresent && !ep.isEcraser()) {
                        log.info("    SKIP attribut '{}' (déjà présent, ecraser=false)", ea.getNom());
                        continue;
                    }
                    log.info("    + attribut '{}' = '{}' [src: {}]", ea.getNom(), ea.getValeur(), ea.getSource());
                    if (confirmer) {
                        AttributProduit attr = new AttributProduit();
                        attr.setNomAttribut(ea.getNom());
                        attr.setValeurAttribut(ea.getValeur());
                        attr.setProduit(p);
                        modeleAttributRepository.findByNomAttributIgnoreCase(ea.getNom())
                                .ifPresent(attr::setModeleAttribut);
                        attributProduitRepository.save(attr);
                    }
                    champsProduit++;
                }
            }

            if (champsProduit > 0) {
                if (confirmer) produitRepository.save(p);
                nbModifies++;
                nbChamps += champsProduit;
            }
            nbIgnores += champsIgnores;

            if (ep.getRemarques() != null) {
                log.info("    Remarque : {}", ep.getRemarques());
            }
        }

        log.info("══════════════════════════════════════════════════════");
        log.info("  RAPPORT : {} produit(s) modifié(s), {} champ(s) rempli(s), {} valeur(s) ignorée(s)",
                nbModifies, nbChamps, nbIgnores);
        if (!confirmer) {
            log.info("  ↑ SIMULATION — relancer avec -Dimport.confirmer=true pour écrire");
        }
        log.info("══════════════════════════════════════════════════════");
    }

    // ─────────────────────────────────────────────────────────────────────────

    private int[] appliquerColonnes(Produit p, EnrichissementColonnesTechniques ct) {
        int ok = 0, skip = 0;

        if (ct.getLabelEnergie() != null) {
            int[] r = appliquerLabelEnergie(p, ct.getLabelEnergie());
            ok += r[0]; skip += r[1];
        }
        if (ct.getConsommationWatt() != null) {
            EnrichissementValeur v = ct.getConsommationWatt();
            if (!VALIDE.equalsIgnoreCase(v.getConfiance())) { skip++; }
            else if (estVide(p.getConsommationWatt() != null ? p.getConsommationWatt().toString() : null) || p.getConsommationWatt() == null) {
                log(p.getConsommationWatt() != null ? p.getConsommationWatt().toString() : null, v.getValeur(), "consommationWatt");
                if (confirmer) { try { p.setConsommationWatt(Integer.valueOf(v.getValeur().replaceAll("[^0-9]", ""))); } catch (NumberFormatException e) { log.warn("    consommationWatt invalide : {}", v.getValeur()); } }
                ok++;
            } else { log.info("    SKIP consommationWatt (déjà rempli)"); }
        }
        if (ct.getDimensions() != null) {
            EnrichissementValeur v = ct.getDimensions();
            if (!VALIDE.equalsIgnoreCase(v.getConfiance())) { skip++; }
            else if (estVide(p.getDimensions())) {
                log(p.getDimensions(), v.getValeur(), "dimensions");
                if (confirmer) p.setDimensions(v.getValeur());
                ok++;
            } else { log.info("    SKIP dimensions (déjà rempli)"); }
        }
        if (ct.getPoids() != null) {
            EnrichissementValeur v = ct.getPoids();
            if (!VALIDE.equalsIgnoreCase(v.getConfiance())) { skip++; }
            else if (p.getPoids() == null) {
                log(null, v.getValeur(), "poids");
                if (confirmer) { try { p.setPoids(Double.parseDouble(v.getValeur().replaceAll("[^0-9.,]", "").replace(",", "."))); } catch (NumberFormatException e) { log.warn("    poids invalide : {}", v.getValeur()); } }
                ok++;
            } else { log.info("    SKIP poids (déjà rempli)"); }
        }
        if (ct.getGarantie() != null) {
            EnrichissementValeur v = ct.getGarantie();
            if (!VALIDE.equalsIgnoreCase(v.getConfiance())) { skip++; }
            else if (estVide(p.getGarantie())) {
                log(p.getGarantie(), v.getValeur(), "garantie");
                if (confirmer) p.setGarantie(v.getValeur());
                ok++;
            } else { log.info("    SKIP garantie (déjà rempli)"); }
        }
        if (ct.getPuissanceBTU() != null) {
            EnrichissementValeur v = ct.getPuissanceBTU();
            if (!VALIDE.equalsIgnoreCase(v.getConfiance())) { skip++; }
            else {
                List<AttributProduit> existants = attributProduitRepository.findByProduitIdProduit(p.getIdProduit());
                boolean dejaPresent = existants.stream().anyMatch(a -> "Puissance BTU".equalsIgnoreCase(a.getNomAttribut()));
                if (!dejaPresent) {
                    log.info("    + attribut 'Puissance BTU' = '{}' [src: {}]", v.getValeur(), v.getSource());
                    if (confirmer) {
                        AttributProduit attr = new AttributProduit();
                        attr.setNomAttribut("Puissance BTU");
                        attr.setValeurAttribut(v.getValeur());
                        attr.setProduit(p);
                        modeleAttributRepository.findByNomAttributIgnoreCase("Puissance BTU")
                                .ifPresent(attr::setModeleAttribut);
                        attributProduitRepository.save(attr);
                    }
                    ok++;
                } else { log.info("    SKIP Puissance BTU (déjà présent)"); }
            }
        }
        return new int[]{ok, skip};
    }

    private int[] appliquerLabelEnergie(Produit p, EnrichissementValeur v) {
        if (!VALIDE.equalsIgnoreCase(v.getConfiance())) return new int[]{0, 1};
        if (p.getLabelEnergie() != null) {
            log.info("    SKIP labelEnergie (déjà rempli)");
            return new int[]{0, 0};
        }
        Produit.LabelEnergie label = parseLabelEnergie(v.getValeur());
        if (label == null) {
            log.warn("    labelEnergie invalide : '{}' — valeurs acceptées : A+++, A++, A+, A, B, C, D, E, F, G", v.getValeur());
            return new int[]{0, 1};
        }
        log(null, label.name(), "labelEnergie");
        if (confirmer) p.setLabelEnergie(label);
        return new int[]{1, 0};
    }

    private Produit.LabelEnergie parseLabelEnergie(String valeur) {
        if (valeur == null) return null;
        return switch (valeur.trim().toUpperCase()
                .replace("+++", "_PLUS_PLUS_PLUS")
                .replace("++", "_PLUS_PLUS")
                .replace("+", "_PLUS")
                .replaceAll("^A_PLUS", "A_PLUS")) {
            case "A_PLUS_PLUS_PLUS" -> Produit.LabelEnergie.A_PLUS_PLUS_PLUS;
            case "A_PLUS_PLUS"      -> Produit.LabelEnergie.A_PLUS_PLUS;
            case "A_PLUS"           -> Produit.LabelEnergie.A_PLUS;
            case "A"                -> Produit.LabelEnergie.A;
            case "B"                -> Produit.LabelEnergie.B;
            case "C"                -> Produit.LabelEnergie.C;
            case "D"                -> Produit.LabelEnergie.D;
            case "E"                -> Produit.LabelEnergie.E;
            case "F"                -> Produit.LabelEnergie.F;
            case "G"                -> Produit.LabelEnergie.G;
            default                 -> null;
        };
    }

    private boolean estVide(String s) {
        return s == null || s.isBlank();
    }

    private void log(String avant, String apres, String champ) {
        String a = avant != null ? (avant.length() > 60 ? avant.substring(0, 60) + "…" : avant) : "(vide)";
        String b = apres != null ? (apres.length() > 60 ? apres.substring(0, 60) + "…" : apres) : "(vide)";
        log.info("    {} : [{}] → [{}]", champ, a, b);
    }
}
