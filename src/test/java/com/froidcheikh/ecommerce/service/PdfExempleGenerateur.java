package com.froidcheikh.ecommerce.service;

import com.froidcheikh.ecommerce.entity.CompteurDocument.TypeDocument;
import com.froidcheikh.ecommerce.entity.DocumentFacturation;
import com.froidcheikh.ecommerce.entity.DocumentFacturation.StatutDocument;
import com.froidcheikh.ecommerce.entity.LigneDocument;
import com.froidcheikh.ecommerce.entity.ModeEnTeteDocuments;
import com.froidcheikh.ecommerce.entity.ReglementDocument;
import com.froidcheikh.ecommerce.entity.ReglementDocument.ModeReglement;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Génère des PDFs de démo dans target/pdf-exemples/.
 * Lance avec : mvn test -Dtest=PdfExempleGenerateur
 * Aucun Spring context, aucune base de données requise.
 */
class PdfExempleGenerateur {

    private final PdfFacturationService pdfService    = new PdfFacturationService();
    private final CalculDocumentService calculService  = new CalculDocumentService();
    private final MontantEnLettresService lettresService = new MontantEnLettresService();

    @Test
    void genererExemples() throws IOException {
        Path dir = Paths.get("target/pdf-exemples");
        Files.createDirectories(dir);

        EnTeteDocumentConfig cfg = enTeteCompose();

        // 1 — FACTURE PARTIELLEMENT PAYÉE sur 2 pages
        DocumentFacturation facture = creerFactureLongue();
        calculerEtFiger(facture);
        ecrire(dir, "facture_partielle_2pages_FAC-2026-0001.pdf",
                pdfService.generer(facture, cfg, false));

        // 2 — AVOIR (sur la facture précédente)
        DocumentFacturation avoir = creerAvoir(facture);
        calculerEtFiger(avoir);
        ecrire(dir, "avoir_AVO-2026-0001.pdf",
                pdfService.generer(avoir, cfg, false));

        // 3 — BON DE LIVRAISON
        DocumentFacturation bl = creerBonLivraison();
        ecrire(dir, "bon_livraison_BL-2026-0001.pdf",
                pdfService.generer(bl, cfg, false));

        // 4 — DEVIS
        DocumentFacturation devis = creerDevis();
        calculerEtFiger(devis);
        ecrire(dir, "devis_DEV-2026-0001.pdf",
                pdfService.generer(devis, cfg, false));

        // 5 — BROUILLON (filigrane)
        DocumentFacturation brouillon = creerFactureLongue();
        brouillon.setStatut(StatutDocument.BROUILLON);
        brouillon.setNumero(null);
        calculerEtFiger(brouillon);
        ecrire(dir, "brouillon_BROUILLON.pdf",
                pdfService.generer(brouillon, cfg, true));

        // 6 — Mode IMAGE (repli composé si images absentes)
        DocumentFacturation factureImg = creerFactureLongue();
        factureImg.setNumero("FAC-2026-IMAGE");
        calculerEtFiger(factureImg);
        ecrire(dir, "facture_mode_image_FAC-2026-IMAGE.pdf",
                pdfService.generer(factureImg, enTeteImage(), false));

        System.out.println("\n=== PDFs générés dans : " + dir.toAbsolutePath() + " ===");
        System.out.println("  1. facture_partielle_2pages_FAC-2026-0001.pdf  (partiellement payée, 2 pages)");
        System.out.println("  2. avoir_AVO-2026-0001.pdf");
        System.out.println("  3. bon_livraison_BL-2026-0001.pdf");
        System.out.println("  4. devis_DEV-2026-0001.pdf");
        System.out.println("  5. brouillon_BROUILLON.pdf                     (filigrane)");
        System.out.println("  6. facture_mode_image_FAC-2026-IMAGE.pdf       (mode IMAGE — repli composé si absent)");
    }

    // ── Configs en-tête ───────────────────────────────────────────────────────

    private EnTeteDocumentConfig enTeteCompose() {
        return new EnTeteDocumentConfig(
                "FROID CHEIKH ANTA MBACKE - BABACAR DIONE",
                null, // logoUrl — null ici pour le test sans réseau
                "Réparation et Vente de tous Appareils de Froid : Électricité, Plomberie, Climatisation Centrale, Chambre Froide, Split Système, Climatiseur et Frigos",
                "Ouest Foire Cité Aelmas Villa N°168 Rue 577 Yoff, Dakar, Sénégal",
                "33 820 16 33 / 77 335 20 00",
                "B.P. 7605 Dakar",
                "froidcheikhantambacke.dione65@gmail.com",
                "92-A-5673",
                "0151340-2B1",
                "CBAO",
                "36017179-49",
                ModeEnTeteDocuments.COMPOSE,
                null,
                null,
                "#000080",
                "FCFA"
        );
    }

    private EnTeteDocumentConfig enTeteImage() {
        String imgHaut = toFileUrl("docs/papier-entete-haut.png");
        String imgBas  = toFileUrl("docs/papier-entete-bas.png");
        return new EnTeteDocumentConfig(
                "FROID CHEIKH ANTA MBACKE - BABACAR DIONE",
                null, null, null, null, null, null, null, null, null, null,
                ModeEnTeteDocuments.IMAGE, imgHaut, imgBas, "#000080", "FCFA"
        );
    }

    private String toFileUrl(String relativePath) {
        java.io.File f = new java.io.File(relativePath);
        return f.exists() ? f.toURI().toString() : null;
    }

    // ── Fabriques de documents ────────────────────────────────────────────────

    /** Facture avec 12 lignes pour déborder sur 2 pages, + règlement partiel. */
    private DocumentFacturation creerFactureLongue() {
        DocumentFacturation doc = new DocumentFacturation();
        doc.setTypeDocument(TypeDocument.FACTURE);
        doc.setStatut(StatutDocument.EMIS);
        doc.setNumero("FAC-2026-0001");
        doc.setDateCreation(LocalDateTime.of(2026, 10, 15, 9, 30));
        doc.setDateEmission(LocalDateTime.of(2026, 10, 15, 10, 0));

        figerEntreprise(doc);
        doc.setEntConditionsPaiement(null);        // vide → ne s'affiche pas
        doc.setEntMentionExonerationTva(null);     // vide → ne s'affiche pas
        doc.setEntMentionsPied(null);              // vide → ne s'affiche pas

        doc.setClientNom("Diallo");
        doc.setClientPrenom("Mamadou");
        doc.setClientTelephone("+221 77 123 45 67");
        doc.setClientEmail("mamadou.diallo@example.com");
        doc.setClientAdresse("Rue 10, Angle Rue 12, Médina, Dakar");

        doc.setTauxTvaDefaut(BigDecimal.ZERO);
        doc.setPrixAfficheTTC(false);
        doc.setRemiseTotalePourcent(BigDecimal.ZERO);

        // 12 lignes pour forcer le saut de page
        doc.getLignes().add(ligne(doc, 1,  "Climatiseur Astech 9000 BTU AST-09RB41-MA",           "1", "185000"));
        doc.getLignes().add(ligne(doc, 2,  "Climatiseur Samsung 12000 BTU AR12TXHQAS",             "1", "235000"));
        doc.getLignes().add(ligne(doc, 3,  "Climatiseur LG 18000 BTU S4-W18JA3WA Inverter",        "1", "320000"));
        doc.getLignes().add(ligne(doc, 4,  "Climatiseur Midea 24000 BTU MSMABU-24HRFN8",           "1", "420000"));
        doc.getLignes().add(ligne(doc, 5,  "Réfrigérateur combiné Hisense RT414N4AWU 300 L",       "1", "275000"));
        doc.getLignes().add(ligne(doc, 6,  "Réfrigérateur bar Astech 85 L AST-DH85",               "2", "65000"));
        doc.getLignes().add(ligne(doc, 7,  "Congélateur coffre Haier HCE301R 301 L",               "1", "185000"));
        doc.getLignes().add(ligne(doc, 8,  "Kit installation climatiseur (tuyaux 4 m + fixations)", "2", "25000"));
        doc.getLignes().add(ligne(doc, 9,  "Câble électrique 3×2,5 mm² (10 m)",                    "3", "8500"));
        doc.getLignes().add(ligne(doc, 10, "Main-d'œuvre pose et mise en service climatiseur",      "3", "15000"));
        doc.getLignes().add(ligne(doc, 11, "Main-d'œuvre livraison et installation frigo",          "2", "10000"));
        doc.getLignes().add(ligne(doc, 12, "Frais de livraison zone Médina",                        "1", "5000"));

        // Règlement partiel : 500 000 FCFA par Wave
        ReglementDocument r = new ReglementDocument();
        r.setDateReglement(LocalDate.of(2026, 10, 15));
        r.setMontant(new BigDecimal("500000"));
        r.setMode(ModeReglement.WAVE);
        r.setReference("WAVE-20261015-7834");
        r.setEnregistrePar("admin@froidcheikh.sn");
        doc.getReglements().add(r);

        return doc;
    }

    private DocumentFacturation creerAvoir(DocumentFacturation factureOrigine) {
        DocumentFacturation doc = new DocumentFacturation();
        doc.setTypeDocument(TypeDocument.AVOIR);
        doc.setStatut(StatutDocument.EMIS);
        doc.setNumero("AVO-2026-0001");
        doc.setDateCreation(LocalDateTime.of(2026, 10, 16, 14, 0));
        doc.setDateEmission(LocalDateTime.of(2026, 10, 16, 14, 5));
        doc.setDocumentOrigine(factureOrigine);

        figerEntreprise(doc);
        doc.setClientNom(factureOrigine.getClientNom());
        doc.setClientPrenom(factureOrigine.getClientPrenom());
        doc.setClientTelephone(factureOrigine.getClientTelephone());
        doc.setClientEmail(factureOrigine.getClientEmail());
        doc.setClientAdresse(factureOrigine.getClientAdresse());
        doc.setTauxTvaDefaut(BigDecimal.ZERO);
        doc.setPrixAfficheTTC(false);
        doc.setRemiseTotalePourcent(BigDecimal.ZERO);

        // Retour : Samsung + congélateur
        doc.getLignes().add(ligne(doc, 1, "Climatiseur Samsung 12000 BTU AR12TXHQAS [AVOIR]", "1", "235000"));
        doc.getLignes().add(ligne(doc, 2, "Congélateur coffre Haier HCE301R 301 L [AVOIR]",   "1", "185000"));
        return doc;
    }

    private DocumentFacturation creerBonLivraison() {
        DocumentFacturation doc = new DocumentFacturation();
        doc.setTypeDocument(TypeDocument.BON_LIVRAISON);
        doc.setStatut(StatutDocument.EMIS);
        doc.setNumero("BL-2026-0001");
        doc.setDateCreation(LocalDateTime.of(2026, 10, 15, 11, 0));
        doc.setDateEmission(LocalDateTime.of(2026, 10, 15, 11, 30));
        doc.setLivreurNom("Ibrahima Sow");
        doc.setLivreurTelephone("+221 76 987 65 43");

        figerEntreprise(doc);
        doc.setClientNom("Diallo");
        doc.setClientPrenom("Mamadou");
        doc.setClientTelephone("+221 77 123 45 67");
        doc.setClientAdresse("Rue 10, Angle Rue 12, Médina, Dakar");
        doc.setTauxTvaDefaut(BigDecimal.ZERO);
        doc.setPrixAfficheTTC(false);
        doc.setRemiseTotalePourcent(BigDecimal.ZERO);

        doc.getLignes().add(ligneBL(doc, 1, "Climatiseur Astech 9000 BTU AST-09RB41-MA",  "1"));
        doc.getLignes().add(ligneBL(doc, 2, "Climatiseur Samsung 12000 BTU AR12TXHQAS",    "1"));
        doc.getLignes().add(ligneBL(doc, 3, "Kit installation (tuyaux + supports + câble 3m)", "1"));
        return doc;
    }

    private DocumentFacturation creerDevis() {
        DocumentFacturation doc = new DocumentFacturation();
        doc.setTypeDocument(TypeDocument.DEVIS);
        doc.setStatut(StatutDocument.EMIS);
        doc.setNumero("DEV-2026-0001");
        doc.setDateCreation(LocalDateTime.of(2026, 10, 15, 14, 0));
        doc.setDateEmission(LocalDateTime.of(2026, 10, 15, 14, 0));
        doc.setDateValidite(LocalDate.of(2026, 11, 15));

        figerEntreprise(doc);
        doc.setClientNom("Ndiaye");
        doc.setClientPrenom("Oumar");
        doc.setClientTelephone("+221 77 456 78 90");
        doc.setClientEmail("oumar.ndiaye@example.com");
        doc.setClientAdresse("Sicap Liberté 4, Dakar");
        doc.setTauxTvaDefaut(BigDecimal.ZERO);
        doc.setPrixAfficheTTC(false);
        doc.setRemiseTotalePourcent(BigDecimal.ZERO);

        doc.getLignes().add(ligne(doc, 1, "Climatiseur Inverter LG 18000 BTU S4-W18JA3WA", "2", "320000"));
        doc.getLignes().add(ligne(doc, 2, "Kit installation (tuyaux cuivre + fixations)", "2", "28000"));
        doc.getLignes().add(ligne(doc, 3, "Main-d'œuvre pose et mise en service",          "2", "15000"));
        doc.getLignes().add(ligne(doc, 4, "Frais de livraison",                             "1", "5000"));
        return doc;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void figerEntreprise(DocumentFacturation doc) {
        doc.setEntRaisonSociale("FROID CHEIKH ANTA MBACKE - BABACAR DIONE");
        doc.setEntAdresseSiege("Ouest Foire Cité Aelmas Villa N°168 Rue 577 Yoff, Dakar, Sénégal");
        doc.setEntNinea("0151340-2B1");
        doc.setEntRccm("92-A-5673");
        doc.setEntCouleurPrimaire("#000080");
        doc.setEntDevise("FCFA");
    }

    private void calculerEtFiger(DocumentFacturation doc) {
        boolean ttc = Boolean.TRUE.equals(doc.getPrixAfficheTTC());
        BigDecimal[] totaux = calculService.calculerTotaux(
                doc.getLignes(), doc.getRemiseTotalePourcent(), ttc);
        doc.setMontantHT(totaux[0]);
        doc.setMontantTva(totaux[1]);
        doc.setMontantTTC(totaux[2]);
        doc.setMontantEnLettres(lettresService.convertir(totaux[2]));
    }

    private LigneDocument ligne(DocumentFacturation doc, int ordre,
                                String designation, String qte, String prix) {
        LigneDocument l = new LigneDocument();
        l.setDocument(doc);
        l.setOrdre(ordre);
        l.setDesignation(designation);
        l.setQuantite(new BigDecimal(qte));
        l.setPrixUnitaireHT(new BigDecimal(prix));
        l.setTauxTva(BigDecimal.ZERO);
        l.setRemisePourcent(BigDecimal.ZERO);
        l.setMontantHT(BigDecimal.ZERO);
        l.setMontantTva(BigDecimal.ZERO);
        l.setMontantTTC(BigDecimal.ZERO);
        return l;
    }

    private LigneDocument ligneBL(DocumentFacturation doc, int ordre,
                                  String designation, String qte) {
        LigneDocument l = new LigneDocument();
        l.setDocument(doc);
        l.setOrdre(ordre);
        l.setDesignation(designation);
        l.setQuantite(new BigDecimal(qte));
        l.setPrixUnitaireHT(BigDecimal.ZERO);
        l.setTauxTva(BigDecimal.ZERO);
        l.setRemisePourcent(BigDecimal.ZERO);
        l.setMontantHT(BigDecimal.ZERO);
        l.setMontantTva(BigDecimal.ZERO);
        l.setMontantTTC(BigDecimal.ZERO);
        return l;
    }

    private void ecrire(Path dir, String nom, byte[] bytes) throws IOException {
        Path fichier = dir.resolve(nom);
        Files.write(fichier, bytes);
        System.out.println("  Écrit : " + fichier.toAbsolutePath() + " (" + bytes.length + " octets)");
    }
}
