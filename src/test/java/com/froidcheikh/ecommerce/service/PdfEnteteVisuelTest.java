package com.froidcheikh.ecommerce.service;

import com.froidcheikh.ecommerce.entity.CompteurDocument.TypeDocument;
import com.froidcheikh.ecommerce.entity.DocumentFacturation;
import com.froidcheikh.ecommerce.entity.LigneDocument;
import com.froidcheikh.ecommerce.entity.ModeEnTeteDocuments;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

/**
 * Génère des PNGs de la première page du PDF (en-tête) pour vérification visuelle.
 * Ne s'exécute pas en CI — utilise mvn test -Dtest=PdfEnteteVisuelTest.
 * Les PNGs sont sauvegardés dans target/pdf-test/.
 */
class PdfEnteteVisuelTest {

    private static final String SORTIE = "target/pdf-test";
    private final PdfFacturationService service = new PdfFacturationService();

    // ── Logo de test : rectangle bleu marine 120x40px avec texte blanc ────────
    private static String buildTestLogoDataUri() throws Exception {
        int w = 360, h = 120;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(0, 0, 128)); // bleu marine
        g.fillRect(0, 0, w, h);
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 40));
        FontMetrics fm = g.getFontMetrics();
        String txt = "LOGO";
        g.drawString(txt, (w - fm.stringWidth(txt)) / 2, h / 2 + fm.getAscent() / 2);
        g.dispose();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "PNG", baos);
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(baos.toByteArray());
    }

    // ── Document de test ──────────────────────────────────────────────────────
    private DocumentFacturation buildDoc() {
        DocumentFacturation doc = new DocumentFacturation();
        doc.setTypeDocument(TypeDocument.FACTURE);
        doc.setNumero("FAC-0001");
        doc.setDateEmission(LocalDateTime.now());
        LigneDocument l = new LigneDocument();
        l.setDesignation("Climatiseur Astech 9000 BTU");
        l.setQuantite(BigDecimal.ONE);
        l.setPrixUnitaireHT(new BigDecimal("200000"));
        l.setTauxTva(new BigDecimal("18"));
        l.setRemisePourcent(BigDecimal.ZERO);
        l.setMontantHT(new BigDecimal("200000"));
        l.setMontantTva(new BigDecimal("36000"));
        l.setMontantTTC(new BigDecimal("236000"));
        doc.setLignes(List.of(l));
        doc.setMontantHT(new BigDecimal("200000"));
        doc.setMontantTva(new BigDecimal("36000"));
        doc.setMontantTTC(new BigDecimal("236000"));
        doc.setMontantEnLettres(
            "Arrêtée la présente facture à la somme de deux cent trente-six mille francs CFA");
        doc.setClientNom("Dupont");
        doc.setClientPrenom("Jean");
        doc.setEntDevise("FCFA");
        doc.setEntCouleurPrimaire("#000080");
        return doc;
    }

    private EnTeteDocumentConfig cfgBase(String logoDataUri) {
        return new EnTeteDocumentConfig(
                "FROID CHEIKH ANTA MBACKE - BABACAR DIONE",
                logoDataUri,
                "Réparation et Vente de tous Appareils de Froid : Electricité, Plomberie,"
                        + " Climatisation Centrale, Chambre Froide, Split Système",
                "Ouest Foire Cité Aelmas Villa N°168, Yoff, Dakar, Sénégal",
                "77 123 45 67 / 33 987 65 43",
                "B.P. 7605 Dakar",
                "contact@froidcheikh.sn",
                "92-A-5673",
                "0151340-2B1",
                "CBAO",
                "SN000000001234",
                ModeEnTeteDocuments.COMPOSE, null, null,
                "#000080", "FCFA"
        );
    }

    private void genererPng(byte[] pdf, String nom) throws Exception {
        new File(SORTIE).mkdirs();
        try (PDDocument pdDoc = Loader.loadPDF(pdf)) {
            PDFRenderer renderer = new PDFRenderer(pdDoc);
            BufferedImage page = renderer.renderImageWithDPI(0, 150);
            File out = new File(SORTIE + "/" + nom);
            ImageIO.write(page, "PNG", out);
            System.out.println("[PNG] Sauvegardé : " + out.getAbsolutePath());
        }
    }

    @Test
    void entete_avecLogo() throws Exception {
        String logo = buildTestLogoDataUri();
        byte[] pdf = service.generer(buildDoc(), cfgBase(logo), false);
        genererPng(pdf, "entete-avec-logo.png");
    }

    @Test
    void entete_sansLogo() throws Exception {
        byte[] pdf = service.generer(buildDoc(), cfgBase(null), false);
        genererPng(pdf, "entete-sans-logo.png");
    }
}
