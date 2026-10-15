package com.froidcheikh.ecommerce.service;

import com.froidcheikh.ecommerce.entity.CompteurDocument.TypeDocument;
import com.froidcheikh.ecommerce.entity.DocumentFacturation;
import com.froidcheikh.ecommerce.entity.DocumentFacturation.StatutDocument;
import com.froidcheikh.ecommerce.entity.LigneDocument;
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

    private final PdfFacturationService pdfService = new PdfFacturationService();
    private final CalculDocumentService calculService = new CalculDocumentService();
    private final MontantEnLettresService lettresService = new MontantEnLettresService();

    @Test
    void genererExemples() throws IOException {
        Path dir = Paths.get("target/pdf-exemples");
        Files.createDirectories(dir);

        // ---- FACTURE ----
        DocumentFacturation facture = creerFacture();
        calculerEtFiger(facture);
        byte[] pdfFacture = pdfService.generer(facture, false);
        ecrire(dir, "facture_FAC-2026-0001.pdf", pdfFacture);

        // ---- FACTURE BROUILLON (filigrane) ----
        DocumentFacturation brouillon = creerFacture();
        brouillon.setStatut(StatutDocument.BROUILLON);
        brouillon.setNumero(null);
        calculerEtFiger(brouillon);
        byte[] pdfBrouillon = pdfService.generer(brouillon, true);
        ecrire(dir, "brouillon_BROUILLON.pdf", pdfBrouillon);

        // ---- AVOIR ----
        DocumentFacturation avoir = creerAvoir(facture);
        calculerEtFiger(avoir);
        byte[] pdfAvoir = pdfService.generer(avoir, false);
        ecrire(dir, "avoir_AVO-2026-0001.pdf", pdfAvoir);

        // ---- BON DE LIVRAISON ----
        DocumentFacturation bl = creerBonLivraison();
        byte[] pdfBL = pdfService.generer(bl, false);
        ecrire(dir, "bon_livraison_BL-2026-0001.pdf", pdfBL);

        System.out.println("\n=== PDFs générés dans : " + dir.toAbsolutePath() + " ===");
        System.out.println("  - facture_FAC-2026-0001.pdf");
        System.out.println("  - brouillon_BROUILLON.pdf");
        System.out.println("  - avoir_AVO-2026-0001.pdf");
        System.out.println("  - bon_livraison_BL-2026-0001.pdf");
    }

    // ---- FABRIQUE FACTURE ----

    private DocumentFacturation creerFacture() {
        DocumentFacturation doc = new DocumentFacturation();
        doc.setTypeDocument(TypeDocument.FACTURE);
        doc.setStatut(StatutDocument.EMIS);
        doc.setNumero("FAC-2026-0001");
        doc.setDateCreation(LocalDateTime.of(2026, 10, 15, 9, 30));
        doc.setDateEmission(LocalDateTime.of(2026, 10, 15, 10, 0));

        // Entreprise (copie figée)
        doc.setEntRaisonSociale("Froid Cheikh Anta Mbacke SARL");
        doc.setEntAdresseSiege("Zone Industrielle, Lot 47, Dakar, Sénégal");
        doc.setEntNinea("000123456 2A1");
        doc.setEntRccm("SN DKR 2020 B 12345");
        doc.setEntCouleurPrimaire("#000080");
        doc.setEntConditionsPaiement("Paiement à 30 jours. En cas de retard, pénalités de 1,5% par mois.");
        doc.setEntMentionsPied("Froid Cheikh Anta Mbacke SARL — Capital social : 10 000 000 FCFA");
        doc.setEntMentionExonerationTva("TVA non applicable — Article 275 du CGI");

        // Client (copie figée)
        doc.setClientNom("Diallo");
        doc.setClientPrenom("Mamadou");
        doc.setClientTelephone("+221 77 123 45 67");
        doc.setClientEmail("mamadou.diallo@example.com");
        doc.setClientAdresse("Rue 10, Angle Rue 12, Médina, Dakar");

        // Paramètres TVA : non assujetti (taux 0)
        doc.setTauxTvaDefaut(BigDecimal.ZERO);
        doc.setPrixAfficheTTC(false);
        doc.setRemiseTotalePourcent(BigDecimal.ZERO);

        // Lignes
        doc.getLignes().add(ligne(doc, 1, "Climatiseur Astech 9000 BTU AST-09RB41-MA",
                "1", "185000", BigDecimal.ZERO, BigDecimal.ZERO));
        doc.getLignes().add(ligne(doc, 2, "Climatiseur Samsung 12000 BTU AR12TXHQAS",
                "1", "235000", BigDecimal.ZERO, BigDecimal.ZERO));
        doc.getLignes().add(ligne(doc, 3, "Kit installation (tuyaux + supports + câble 3m)",
                "1", "25000", BigDecimal.ZERO, BigDecimal.ZERO));
        doc.getLignes().add(ligne(doc, 4, "Main d'œuvre pose et mise en service",
                "2", "15000", BigDecimal.ZERO, BigDecimal.ZERO));
        doc.getLignes().add(ligne(doc, 5, "Frais de livraison (Zone Médina)",
                "1", "3000", BigDecimal.ZERO, BigDecimal.ZERO));

        // Règlement partiel
        ReglementDocument r1 = new ReglementDocument();
        r1.setDateReglement(LocalDate.of(2026, 10, 15));
        r1.setMontant(new BigDecimal("250000"));
        r1.setMode(ModeReglement.WAVE);
        r1.setReference("WAVE-20261015-7834");
        r1.setEnregistrePar("admin@froidcheikh.sn");
        doc.getReglements().add(r1);

        return doc;
    }

    // ---- FABRIQUE AVOIR ----

    private DocumentFacturation creerAvoir(DocumentFacturation factureOrigine) {
        DocumentFacturation doc = new DocumentFacturation();
        doc.setTypeDocument(TypeDocument.AVOIR);
        doc.setStatut(StatutDocument.EMIS);
        doc.setNumero("AVO-2026-0001");
        doc.setDateCreation(LocalDateTime.of(2026, 10, 16, 14, 0));
        doc.setDateEmission(LocalDateTime.of(2026, 10, 16, 14, 5));
        doc.setDocumentOrigine(factureOrigine);

        // Mêmes infos entreprise et client
        doc.setEntRaisonSociale(factureOrigine.getEntRaisonSociale());
        doc.setEntAdresseSiege(factureOrigine.getEntAdresseSiege());
        doc.setEntNinea(factureOrigine.getEntNinea());
        doc.setEntRccm(factureOrigine.getEntRccm());
        doc.setEntCouleurPrimaire(factureOrigine.getEntCouleurPrimaire());
        doc.setEntMentionsPied(factureOrigine.getEntMentionsPied());
        doc.setEntMentionExonerationTva(factureOrigine.getEntMentionExonerationTva());
        doc.setClientNom(factureOrigine.getClientNom());
        doc.setClientPrenom(factureOrigine.getClientPrenom());
        doc.setClientTelephone(factureOrigine.getClientTelephone());
        doc.setClientEmail(factureOrigine.getClientEmail());
        doc.setClientAdresse(factureOrigine.getClientAdresse());

        doc.setTauxTvaDefaut(BigDecimal.ZERO);
        doc.setPrixAfficheTTC(false);
        doc.setRemiseTotalePourcent(BigDecimal.ZERO);

        // Retour partiel : le Samsung uniquement
        doc.getLignes().add(ligne(doc, 1, "Climatiseur Samsung 12000 BTU AR12TXHQAS [AVOIR]",
                "1", "235000", BigDecimal.ZERO, BigDecimal.ZERO));

        return doc;
    }

    // ---- FABRIQUE BON DE LIVRAISON ----

    private DocumentFacturation creerBonLivraison() {
        DocumentFacturation doc = new DocumentFacturation();
        doc.setTypeDocument(TypeDocument.BON_LIVRAISON);
        doc.setStatut(StatutDocument.EMIS);
        doc.setNumero("BL-2026-0001");
        doc.setDateCreation(LocalDateTime.of(2026, 10, 15, 11, 0));
        doc.setDateEmission(LocalDateTime.of(2026, 10, 15, 11, 30));
        doc.setLivreurNom("Ibrahima Sow");
        doc.setLivreurTelephone("+221 76 987 65 43");

        doc.setEntRaisonSociale("Froid Cheikh Anta Mbacke SARL");
        doc.setEntAdresseSiege("Zone Industrielle, Lot 47, Dakar, Sénégal");
        doc.setEntNinea("000123456 2A1");
        doc.setEntCouleurPrimaire("#000080");
        doc.setEntMentionsPied("Froid Cheikh Anta Mbacke SARL — Capital social : 10 000 000 FCFA");

        doc.setClientNom("Diallo");
        doc.setClientPrenom("Mamadou");
        doc.setClientTelephone("+221 77 123 45 67");
        doc.setClientAdresse("Rue 10, Angle Rue 12, Médina, Dakar");

        doc.setTauxTvaDefaut(BigDecimal.ZERO);
        doc.setPrixAfficheTTC(false);
        doc.setRemiseTotalePourcent(BigDecimal.ZERO);

        doc.getLignes().add(ligneBL(doc, 1, "Climatiseur Astech 9000 BTU AST-09RB41-MA", "1"));
        doc.getLignes().add(ligneBL(doc, 2, "Climatiseur Samsung 12000 BTU AR12TXHQAS", "1"));
        doc.getLignes().add(ligneBL(doc, 3, "Kit installation (tuyaux + supports + câble 3m)", "1"));

        return doc;
    }

    // ---- HELPERS ----

    private void calculerEtFiger(DocumentFacturation doc) {
        boolean ttc = Boolean.TRUE.equals(doc.getPrixAfficheTTC());
        BigDecimal[] totaux = calculService.calculerTotaux(doc.getLignes(), doc.getRemiseTotalePourcent(), ttc);
        doc.setMontantHT(totaux[0]);
        doc.setMontantTva(totaux[1]);
        doc.setMontantTTC(totaux[2]);
        doc.setMontantEnLettres(lettresService.convertir(totaux[2]));
    }

    private LigneDocument ligne(DocumentFacturation doc, int ordre, String designation,
                                 String qte, String prix, BigDecimal taux, BigDecimal remise) {
        LigneDocument l = new LigneDocument();
        l.setDocument(doc);
        l.setOrdre(ordre);
        l.setDesignation(designation);
        l.setQuantite(new BigDecimal(qte));
        l.setPrixUnitaireHT(new BigDecimal(prix));
        l.setTauxTva(taux);
        l.setRemisePourcent(remise);
        l.setMontantHT(BigDecimal.ZERO);
        l.setMontantTva(BigDecimal.ZERO);
        l.setMontantTTC(BigDecimal.ZERO);
        return l;
    }

    private LigneDocument ligneBL(DocumentFacturation doc, int ordre, String designation, String qte) {
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
