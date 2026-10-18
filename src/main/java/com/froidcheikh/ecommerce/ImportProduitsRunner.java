package com.froidcheikh.ecommerce;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.froidcheikh.ecommerce.dto.enrichissement.EnrichissementAttribut;
import com.froidcheikh.ecommerce.dto.enrichissement.EnrichissementColonnesTechniques;
import com.froidcheikh.ecommerce.dto.enrichissement.EnrichissementValeur;
import com.froidcheikh.ecommerce.dto.importproduit.NouveauProduitImport;
import com.froidcheikh.ecommerce.entity.*;
import com.froidcheikh.ecommerce.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.*;

/**
 * Crée de nouveaux produits depuis un fichier JSON.
 *
 * <p>Simulation (défaut) :
 * <pre>
 *   mvn spring-boot:run -Dspring-boot.run.profiles=import-produits \
 *       -Dspring-boot.run.jvmArguments="-Dimport.fichier=import_climatiseurs_midea.json"
 * </pre>
 *
 * <p>Écriture réelle :
 * <pre>
 *   ... -Dspring-boot.run.jvmArguments="-Dimport.fichier=import_climatiseurs_midea.json -Dimport.confirmer=true"
 * </pre>
 *
 * <p>Idempotence : la clé est {@code marque + modeleFabricant}. Un deuxième passage
 * complète les champs vides sans créer de doublon.
 */
@Component
@Profile("import-produits")
@RequiredArgsConstructor
@Slf4j
public class ImportProduitsRunner implements CommandLineRunner {

    private static final String VALIDE = "valide";

    /** Variantes acceptées pour la catégorie "Climatiseurs". */
    private static final Set<String> VARIANTES_CLIMATISEURS = Set.of(
            "climatiseur", "climatiseurs", "climatisation", "clim", "airconditioning",
            "air conditioning", "split"
    );

    /** Prix de référence par puissance (XOF) quand aucun prix n'est fourni. */
    private static final Map<Integer, BigDecimal> PRIX_REFERENCE_BTU = Map.of(
            9000,  new BigDecimal("230000"),
            12000, new BigDecimal("290000"),
            18000, new BigDecimal("380000"),
            24000, new BigDecimal("490000"),
            36000, new BigDecimal("650000"),
            48000, new BigDecimal("850000")
    );

    /**
     * Alias vers colonnes Produit : même liste que dans ImportEnrichissementRunner,
     * pour que les attributs redondants soient redirigés vers la bonne colonne.
     */
    private static final Map<String, String> ALIAS_VERS_COLONNE = Map.ofEntries(
            Map.entry("garantie",              "garantie"),
            Map.entry("labelenergie",          "labelEnergie"),
            Map.entry("classeenergetique",     "labelEnergie"),
            Map.entry("consommationwatt",      "consommationWatt"),
            Map.entry("puissancenominale",     "consommationWatt"),
            Map.entry("dimensions",            "dimensions"),
            Map.entry("poids",                 "poids"),
            Map.entry("poidsnet",              "poids"),
            Map.entry("puissancebtu",          "puissanceBTU"),
            Map.entry("btu",                   "puissanceBTU"),
            Map.entry("capacitebtu",           "puissanceBTU")
    );

    @Value("${import.fichier:import_produits.json}")
    private String fichier;

    @Value("${import.confirmer:false}")
    private boolean confirmer;

    private final ProduitRepository        produitRepository;
    private final MarqueRepository         marqueRepository;
    private final CategorieRepository      categorieRepository;
    private final AttributProduitRepository attributProduitRepository;
    private final ModeleAttributRepository  modeleAttributRepository;
    private final ImageProduitRepository   imageProduitRepository;
    private final Cloudinary               cloudinary;
    private final ObjectMapper             objectMapper;

    // ── Rapport ───────────────────────────────────────────────────────────────
    private int nbCrees      = 0;
    private int nbMisAJour   = 0;
    private int nbInchanges  = 0;
    private int nbImages     = 0;
    private int nbMarquesCreees     = 0;
    private int nbCategoriesCreees  = 0;
    private final List<String> prixAVerifier     = new ArrayList<>();
    private final List<String> stockZero         = new ArrayList<>();
    private final List<String> imagesManquantes  = new ArrayList<>();

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        log.info("══════════════════════════════════════════════════════════════");
        log.info("  Import produits — fichier : {}", fichier);
        log.info("  Mode : {}", confirmer ? "ÉCRITURE (persistance activée)" : "SIMULATION (aucune écriture)");
        log.info("══════════════════════════════════════════════════════════════");

        Path path = Path.of(fichier);
        if (!Files.exists(path)) {
            log.error("Fichier introuvable : {}", path.toAbsolutePath());
            return;
        }

        List<NouveauProduitImport> lots = objectMapper.readValue(
                path.toFile(), new TypeReference<>() {});

        for (NouveauProduitImport entry : lots) {
            traiterProduit(entry);
        }

        afficherRapport();

        if (!confirmer) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            log.info("  ↑ SIMULATION — transaction annulée (rollback), aucune donnée écrite");
            log.info("  ↑ Relancer avec -Dimport.confirmer=true pour écrire");
        }
        log.info("══════════════════════════════════════════════════════════════");
    }

    // ─────────────────────────────────────────────────────────────────────────

    private void traiterProduit(NouveauProduitImport entry) {
        log.info("");
        log.info("  ─── {} / {} ───", entry.getMarque(), entry.getModeleFabricant());

        // Validation minimale
        if (entry.getModeleFabricant() == null || entry.getModeleFabricant().isBlank()) {
            log.warn("    ⚠  modeleFabricant absent — ignoré");
            nbInchanges++;
            return;
        }
        if (entry.getMarque() == null || entry.getMarque().isBlank()) {
            log.warn("    ⚠  marque absente — ignoré");
            nbInchanges++;
            return;
        }

        // Résolution marque
        Marque marque = resoudreOuCreerMarque(entry.getMarque());
        Categorie categorie = resoudreOuCreerCategorie(entry.getCategorie());

        // Idempotence : cherche par marque + modeleFabricant
        Optional<Produit> existant = trouverProduitExistant(marque, entry.getModeleFabricant());
        boolean estNouveau = existant.isEmpty();

        Produit produit = existant.orElseGet(Produit::new);

        if (estNouveau) {
            // ── Création
            log.info("    + NOUVEAU produit");
            produit.setModeleFabricant(entry.getModeleFabricant());
            produit.setMarque(marque);
            produit.setCategorie(categorie);
            produit.setDisponibilite(false); // désactivé par défaut
            produit.setStockDisponible(0);
            produit.setExcluCatalogue(false);
        } else {
            log.info("    ~ Produit existant — mise à jour des champs vides");
        }

        int champsModifies = 0;

        // Nom
        if (entry.getNomProduit() != null && !entry.getNomProduit().isBlank()) {
            if (estNouveau || estVide(produit.getNomProduit())) {
                logChamp("nomProduit", produit.getNomProduit(), entry.getNomProduit());
                if (confirmer) produit.setNomProduit(entry.getNomProduit());
                champsModifies++;
            } else {
                log.info("    SKIP nomProduit (déjà rempli)");
            }
        } else if (estNouveau) {
            log.warn("    ⚠  nomProduit manquant — le produit ne sera pas visible sans nom");
        }

        // Description
        if (entry.getDescription() != null && !entry.getDescription().isBlank()) {
            if (estVide(produit.getDescriptionProduit())) {
                logChamp("description", null, entry.getDescription().substring(0, Math.min(60, entry.getDescription().length())) + "…");
                if (entry.getDescription().length() < 150) {
                    log.warn("    ⚠  description courte ({} caractères — min. 150)", entry.getDescription().length());
                }
                if (confirmer) produit.setDescriptionProduit(entry.getDescription());
                champsModifies++;
            } else {
                log.info("    SKIP description (déjà remplie)");
            }
        }

        // Prix
        BigDecimal prix = entry.getPrix();
        if (prix == null || prix.compareTo(BigDecimal.ZERO) <= 0) {
            prix = prixParDefaut(entry);
            log.info("    ~ Prix de référence appliqué : {} XOF (à vérifier)", prix);
            entry.setPrixIndicatif(true);
        }
        if (estNouveau || produit.getPrix() == null) {
            logChamp("prix", null, prix.toPlainString() + " XOF");
            if (confirmer) produit.setPrix(prix);
            champsModifies++;
        }

        if (entry.isPrixIndicatif()) {
            prixAVerifier.add(entry.getModeleFabricant() + " (" + entry.getNomProduit() + ")");
        }
        stockZero.add(entry.getModeleFabricant() + " (" + entry.getNomProduit() + ")");

        // Garantie
        if (!estVide(entry.getGarantie()) && estVide(produit.getGarantie())) {
            logChamp("garantie", null, entry.getGarantie());
            if (confirmer) produit.setGarantie(entry.getGarantie());
            champsModifies++;
        }

        // Sauvegarde intermédiaire (pour avoir un ID avant les attributs)
        if (confirmer && estNouveau) {
            produit = produitRepository.save(produit);
            log.info("    ✓ Produit créé en base (id={})", produit.getIdProduit());
        }

        // Colonnes techniques
        if (entry.getColonnesTechniques() != null) {
            champsModifies += appliquerColonnes(produit, entry.getColonnesTechniques());
        }

        // Attributs dynamiques
        if (entry.getAttributs() != null) {
            List<AttributProduit> existants = confirmer
                    ? attributProduitRepository.findByProduitIdProduit(produit.getIdProduit())
                    : List.of();
            champsModifies += appliquerAttributs(produit, entry.getAttributs(), existants);
        }

        // Sauvegarde finale
        if (confirmer) {
            produitRepository.save(produit);
        }

        // Images (JAMAIS en simulation)
        int imagesChargees = 0;
        if (entry.getUrlsImages() != null && !entry.getUrlsImages().isEmpty()) {
            imagesChargees = traiterImages(produit, entry.getUrlsImages(), estNouveau);
        } else {
            imagesManquantes.add(entry.getModeleFabricant() + " (" + entry.getNomProduit() + ")");
        }

        nbImages += imagesChargees;

        if (estNouveau) {
            nbCrees++;
            log.info("    → CRÉÉ : {} champ(s) rempli(s), {} image(s)", champsModifies, imagesChargees);
        } else if (champsModifies > 0) {
            nbMisAJour++;
            log.info("    → MIS À JOUR : {} champ(s) complété(s)", champsModifies);
        } else {
            nbInchanges++;
            log.info("    → INCHANGÉ (aucun champ vide à compléter)");
        }

        if (entry.getRemarques() != null) {
            log.info("    Remarque : {}", entry.getRemarques());
        }
    }

    // ── Résolution marque ──────────────────────────────────────────────────────

    private Marque resoudreOuCreerMarque(String nom) {
        List<Marque> toutes = marqueRepository.findAll();
        String nomNorm = normaliser(nom);
        for (Marque m : toutes) {
            if (normaliser(m.getNomMarque()).equals(nomNorm)) {
                log.info("    Marque trouvée : {} (id={})", m.getNomMarque(), m.getIdMarque());
                return m;
            }
        }
        log.info("    + Marque '{}' non trouvée — création", nom);
        nbMarquesCreees++;
        Marque m = new Marque();
        m.setNomMarque(nom);
        if (confirmer) m = marqueRepository.save(m);
        else m.setIdMarque(-1L); // ID fictif pour la simulation
        return m;
    }

    // ── Résolution catégorie ───────────────────────────────────────────────────

    private Categorie resoudreOuCreerCategorie(String nom) {
        if (nom == null || nom.isBlank()) {
            log.warn("    ⚠  catégorie non spécifiée — pas de catégorie assignée");
            return null;
        }
        List<Categorie> toutes = categorieRepository.findAll();
        String nomNorm = normaliser(nom);

        // 1. Correspondance exacte normalisée
        for (Categorie c : toutes) {
            if (normaliser(c.getNomCategorie()).equals(nomNorm)) {
                log.info("    Catégorie trouvée : {} (id={})", c.getNomCategorie(), c.getIdCategorie());
                return c;
            }
        }

        // 2. Correspondance par variante connue (climatiseur / climatiseurs / etc.)
        if (VARIANTES_CLIMATISEURS.contains(nomNorm)) {
            for (Categorie c : toutes) {
                if (VARIANTES_CLIMATISEURS.contains(normaliser(c.getNomCategorie()))) {
                    log.info("    Catégorie trouvée par variante : {} (id={})", c.getNomCategorie(), c.getIdCategorie());
                    return c;
                }
            }
        }

        log.info("    + Catégorie '{}' non trouvée — création", nom);
        nbCategoriesCreees++;
        Categorie c = new Categorie();
        c.setNomCategorie(nom);
        c.setGabaritLourd(true); // les climatiseurs sont gabarit lourd par défaut
        if (confirmer) c = categorieRepository.save(c);
        else c.setIdCategorie(-1L);
        return c;
    }

    // ── Idempotence ───────────────────────────────────────────────────────────

    private Optional<Produit> trouverProduitExistant(Marque marque, String modeleFabricant) {
        if (marque.getIdMarque() == null || marque.getIdMarque() < 0) return Optional.empty();
        return produitRepository.findAll().stream()
                .filter(p -> p.getMarque() != null
                          && p.getMarque().getIdMarque().equals(marque.getIdMarque())
                          && modeleFabricant.equalsIgnoreCase(p.getModeleFabricant()))
                .findFirst();
    }

    // ── Colonnes techniques ────────────────────────────────────────────────────

    private int appliquerColonnes(Produit p, EnrichissementColonnesTechniques ct) {
        int ok = 0;
        if (ct.getPuissanceBTU() != null) ok += appliquerPuissanceBTU(p, ct.getPuissanceBTU());
        if (ct.getLabelEnergie() != null) ok += appliquerLabelEnergie(p, ct.getLabelEnergie());
        if (ct.getConsommationWatt() != null) ok += appliquerConsommationWatt(p, ct.getConsommationWatt());
        if (ct.getDimensions() != null) ok += appliquerDimensions(p, ct.getDimensions());
        if (ct.getPoids() != null) ok += appliquerPoids(p, ct.getPoids());
        if (ct.getGarantie() != null) ok += appliquerGarantieCt(p, ct.getGarantie());
        return ok;
    }

    private int appliquerPuissanceBTU(Produit p, EnrichissementValeur v) {
        if (!VALIDE.equalsIgnoreCase(v.getConfiance())) return 0;
        if (p.getPuissanceBTU() != null) { log.info("    SKIP puissanceBTU (déjà rempli)"); return 0; }
        logChamp("puissanceBTU", null, v.getValeur());
        if (confirmer) {
            try { p.setPuissanceBTU(Integer.valueOf(v.getValeur().replaceAll("[^0-9]", ""))); }
            catch (NumberFormatException e) { log.warn("    puissanceBTU invalide : {}", v.getValeur()); return 0; }
        }
        return 1;
    }

    private int appliquerLabelEnergie(Produit p, EnrichissementValeur v) {
        if (!VALIDE.equalsIgnoreCase(v.getConfiance())) return 0;
        if (p.getLabelEnergie() != null) { log.info("    SKIP labelEnergie (déjà rempli)"); return 0; }
        Produit.LabelEnergie label = parseLabelEnergie(v.getValeur());
        if (label == null) {
            log.warn("    labelEnergie invalide : '{}'", v.getValeur());
            return 0;
        }
        logChamp("labelEnergie", null, label.name());
        if (confirmer) p.setLabelEnergie(label);
        return 1;
    }

    private int appliquerConsommationWatt(Produit p, EnrichissementValeur v) {
        if (!VALIDE.equalsIgnoreCase(v.getConfiance())) return 0;
        if (p.getConsommationWatt() != null) { log.info("    SKIP consommationWatt (déjà rempli)"); return 0; }
        logChamp("consommationWatt", null, v.getValeur());
        if (confirmer) {
            try { p.setConsommationWatt(Integer.valueOf(v.getValeur().replaceAll("[^0-9]", ""))); }
            catch (NumberFormatException e) { log.warn("    consommationWatt invalide : {}", v.getValeur()); return 0; }
        }
        return 1;
    }

    private int appliquerDimensions(Produit p, EnrichissementValeur v) {
        if (!VALIDE.equalsIgnoreCase(v.getConfiance())) return 0;
        if (!estVide(p.getDimensions())) { log.info("    SKIP dimensions (déjà rempli)"); return 0; }
        logChamp("dimensions", null, v.getValeur());
        if (confirmer) p.setDimensions(v.getValeur());
        return 1;
    }

    private int appliquerPoids(Produit p, EnrichissementValeur v) {
        if (!VALIDE.equalsIgnoreCase(v.getConfiance())) return 0;
        if (p.getPoids() != null) { log.info("    SKIP poids (déjà rempli)"); return 0; }
        logChamp("poids", null, v.getValeur());
        if (confirmer) {
            try { p.setPoids(Double.parseDouble(v.getValeur().replaceAll("[^0-9.,]", "").replace(",", "."))); }
            catch (NumberFormatException e) { log.warn("    poids invalide : {}", v.getValeur()); return 0; }
        }
        return 1;
    }

    private int appliquerGarantieCt(Produit p, EnrichissementValeur v) {
        if (!VALIDE.equalsIgnoreCase(v.getConfiance())) return 0;
        if (!estVide(p.getGarantie())) { log.info("    SKIP garantie (déjà rempli)"); return 0; }
        logChamp("garantie (colonnes)", null, v.getValeur());
        if (confirmer) p.setGarantie(v.getValeur());
        return 1;
    }

    // ── Attributs dynamiques ──────────────────────────────────────────────────

    private int appliquerAttributs(Produit produit, List<EnrichissementAttribut> attributs,
                                    List<AttributProduit> existants) {
        int ok = 0;
        for (EnrichissementAttribut ea : attributs) {
            if (!VALIDE.equalsIgnoreCase(ea.getConfiance())) {
                log.info("    IGNORE attribut '{}' (confiance={})", ea.getNom(), ea.getConfiance());
                continue;
            }
            String colonne = ALIAS_VERS_COLONNE.get(normaliserNom(ea.getNom()));
            if (colonne != null) {
                log.info("    REDIRECT attribut '{}' → colonne '{}'", ea.getNom(), colonne);
                continue;
            }
            boolean dejaPresent = existants.stream()
                    .anyMatch(a -> a.getNomAttribut().equalsIgnoreCase(ea.getNom()));
            if (dejaPresent) {
                log.info("    SKIP attribut '{}' (déjà présent)", ea.getNom());
                continue;
            }
            Optional<ModeleAttribut> modele = modeleAttributRepository.findByNomAttributIgnoreCase(ea.getNom());
            if (modele.isPresent() && modele.get().getTypeValeur() == TypeAttribut.NUMBER) {
                String vTrim = ea.getValeur() != null ? ea.getValeur().trim().replace(",", ".") : "";
                if (!vTrim.matches("-?\\d+(\\.\\d+)?")) {
                    log.warn("    REFUS attribut '{}' — NUMBER attendu, valeur '{}' non numérique", ea.getNom(), ea.getValeur());
                    continue;
                }
            }
            String uniteAffichee = modele.map(ModeleAttribut::getUnite).orElse("");
            log.info("    + attribut '{}' = '{}{}'  [src: {}]",
                    ea.getNom(), ea.getValeur(),
                    uniteAffichee.isBlank() ? "" : " " + uniteAffichee,
                    ea.getSource());
            if (confirmer) {
                AttributProduit attr = new AttributProduit();
                attr.setNomAttribut(ea.getNom());
                attr.setValeurAttribut(ea.getValeur());
                attr.setProduit(produit);
                modele.ifPresent(attr::setModeleAttribut);
                attributProduitRepository.save(attr);
            }
            ok++;
        }
        return ok;
    }

    // ── Images ────────────────────────────────────────────────────────────────

    private int traiterImages(Produit produit, List<String> urls, boolean estNouveau) {
        int chargeees = 0;
        List<String> urlsLimitees = urls.stream().limit(3).toList();

        // En simulation : log seulement
        if (!confirmer) {
            for (String url : urlsLimitees) {
                log.info("    [SIM] Image à uploader : {}", url);
            }
            return urlsLimitees.size(); // comptabilisé dans le rapport de simulation
        }

        // Vérification : pas d'images déjà présentes (pour l'idempotence)
        long existantes = imageProduitRepository.countByProduitId(produit.getIdProduit());
        if (existantes > 0 && !estNouveau) {
            log.info("    SKIP images ({} déjà présente(s))", existantes);
            return 0;
        }

        boolean premierImage = true;
        for (String url : urlsLimitees) {
            try {
                log.info("    ↓ Téléchargement image : {}", url);
                @SuppressWarnings("unchecked")
                Map<String, Object> result = (Map<String, Object>) cloudinary.uploader().upload(
                        url,
                        ObjectUtils.asMap(
                                "folder",        "produits/images",
                                "resource_type", "image",
                                "type",          "upload",
                                "access_mode",   "public"
                        )
                );
                String cloudUrl   = (String) result.get("secure_url");
                String publicId   = (String) result.get("public_id");
                boolean principale = premierImage;

                ImageProduit img = new ImageProduit();
                img.setProduit(produit);
                img.setUrl(cloudUrl);
                img.setPublicId(publicId);
                img.setOrdre(chargeees);
                img.setPrincipale(principale);
                img.setTexteAlternatif(produit.getNomProduit());
                imageProduitRepository.save(img);

                log.info("    ✓ Image uploadée : {} (principale={})", cloudUrl, principale);
                chargeees++;
                premierImage = false;

            } catch (IOException e) {
                log.warn("    ⚠  Échec upload image '{}' : {}", url, e.getMessage());
            }
        }
        return chargeees;
    }

    // ── Rapport ───────────────────────────────────────────────────────────────

    private void afficherRapport() {
        log.info("");
        log.info("══════════════════════════════════════════════════════════════");
        log.info("  RAPPORT D'IMPORT");
        log.info("  Mode : {}", confirmer ? "ÉCRITURE" : "SIMULATION");
        log.info("──────────────────────────────────────────────────────────────");
        log.info("  Produits créés     : {}", nbCrees);
        log.info("  Produits mis à jour: {}", nbMisAJour);
        log.info("  Produits inchangés : {}", nbInchanges);
        log.info("  Images envoyées    : {}", nbImages);
        if (nbMarquesCreees > 0)    log.info("  ⚡ Marques créées      : {}", nbMarquesCreees);
        if (nbCategoriesCreees > 0) log.info("  ⚡ Catégories créées   : {}", nbCategoriesCreees);
        log.info("──────────────────────────────────────────────────────────────");

        if (!prixAVerifier.isEmpty()) {
            log.info("  ⚠  PRIX INDICATIFS À CONFIRMER ({}) :", prixAVerifier.size());
            prixAVerifier.forEach(s -> log.info("     • {}", s));
        }
        if (!imagesManquantes.isEmpty()) {
            log.info("  ⚠  PRODUITS SANS IMAGE ({}) :", imagesManquantes.size());
            imagesManquantes.forEach(s -> log.info("     • {}", s));
        }
        log.info("──────────────────────────────────────────────────────────────");
        log.info("  CHECKLIST POST-IMPORT :");
        log.info("  □ Vérifier et corriger les prix indicatifs");
        log.info("  □ Mettre à jour le stock avant activation des produits");
        log.info("  □ Vérifier les images (qualité, pertinence)");
        log.info("  □ Relire les descriptions");
        log.info("  □ Activer les produits validés (disponibilite=true)");
        log.info("══════════════════════════════════════════════════════════════");
    }

    // ── Utilitaires ───────────────────────────────────────────────────────────

    private BigDecimal prixParDefaut(NouveauProduitImport entry) {
        if (entry.getColonnesTechniques() != null && entry.getColonnesTechniques().getPuissanceBTU() != null) {
            String val = entry.getColonnesTechniques().getPuissanceBTU().getValeur();
            if (val != null) {
                try {
                    int btu = Integer.parseInt(val.replaceAll("[^0-9]", ""));
                    // Trouver la puissance la plus proche dans la table de référence
                    return PRIX_REFERENCE_BTU.entrySet().stream()
                            .min(Comparator.comparingInt(e -> Math.abs(e.getKey() - btu)))
                            .map(Map.Entry::getValue)
                            .orElse(new BigDecimal("300000"));
                } catch (NumberFormatException ignored) {}
            }
        }
        return new BigDecimal("300000");
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

    private boolean estVide(String s) { return s == null || s.isBlank(); }

    private void logChamp(String nom, String avant, String apres) {
        String a = avant != null ? (avant.length() > 60 ? avant.substring(0, 60) + "…" : avant) : "(vide)";
        String b = apres != null ? (apres.length() > 60 ? apres.substring(0, 60) + "…" : apres) : "(vide)";
        log.info("    {} : [{}] → [{}]", nom, a, b);
    }

    /** Normalise : minuscules + suppression accents + suppression espaces/ponctuation. */
    static String normaliser(String s) {
        if (s == null) return "";
        String sans = Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}", "");
        return sans.toLowerCase().replaceAll("[^a-z0-9]", "");
    }

    /** Normalise pour la table ALIAS_VERS_COLONNE (même logique que ImportEnrichissementRunner). */
    static String normaliserNom(String s) {
        return normaliser(s);
    }
}
