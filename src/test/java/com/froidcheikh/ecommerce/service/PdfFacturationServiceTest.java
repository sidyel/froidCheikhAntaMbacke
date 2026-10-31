package com.froidcheikh.ecommerce.service;

import com.froidcheikh.ecommerce.entity.CompteurDocument.TypeDocument;
import com.froidcheikh.ecommerce.entity.DocumentFacturation;
import com.froidcheikh.ecommerce.entity.LigneDocument;
import com.froidcheikh.ecommerce.entity.ModeEnTeteDocuments;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests unitaires de PdfFacturationService : logique TVA, montant en lettres,
 * rendu HTML (sans génération PDF réelle).
 */
class PdfFacturationServiceTest {

    private final PdfFacturationService service = new PdfFacturationService();

    private static final EnTeteDocumentConfig CFG_SIMPLE = new EnTeteDocumentConfig(
            "TEST ENTREPRISE", null, null,
            null, null, null, null,
            null, null, null, null,
            ModeEnTeteDocuments.COMPOSE, null, null,
            "#000080", "FCFA"
    );

    // ── Helpers ──────────────────────────────────────────────────────────────

    private DocumentFacturation docAvecLignes(List<LigneDocument> lignes,
                                               BigDecimal montantHT,
                                               BigDecimal montantTva,
                                               BigDecimal montantTTC,
                                               String montantEnLettres) {
        DocumentFacturation doc = new DocumentFacturation();
        doc.setTypeDocument(TypeDocument.FACTURE);
        doc.setNumero("FAC-0001");
        doc.setDateEmission(LocalDateTime.now());
        doc.setMontantHT(montantHT);
        doc.setMontantTva(montantTva);
        doc.setMontantTTC(montantTTC);
        doc.setMontantEnLettres(montantEnLettres);
        doc.setLignes(lignes);
        return doc;
    }

    private LigneDocument ligne(String designation, BigDecimal pu, BigDecimal taux,
                                 BigDecimal ht, BigDecimal tva, BigDecimal ttc) {
        LigneDocument l = new LigneDocument();
        l.setDesignation(designation);
        l.setQuantite(BigDecimal.ONE);
        l.setPrixUnitaireHT(pu);
        l.setTauxTva(taux);
        l.setRemisePourcent(BigDecimal.ZERO);
        l.setMontantHT(ht);
        l.setMontantTva(tva);
        l.setMontantTTC(ttc);
        return l;
    }

    // ── Tests hasTva ─────────────────────────────────────────────────────────

    @Test
    void hasTva_vraiquandMontantTvaPositif() {
        // doc avec montantTva > 0 → hasTva doit être true dans le HTML
        LigneDocument l = ligne("Climatiseur 9000 BTU",
                new BigDecimal("200000"), new BigDecimal("18"),
                new BigDecimal("200000"), new BigDecimal("36000"), new BigDecimal("236000"));
        DocumentFacturation doc = docAvecLignes(
                List.of(l),
                new BigDecimal("200000"), new BigDecimal("36000"), new BigDecimal("236000"),
                "Arrêtée la présente facture à la somme de deux cent trente-six mille francs CFA");

        String html = service.buildHtml(doc, CFG_SIMPLE, false);

        assertThat(html).contains("Montant HT");   // entête colonne mode TVA
        assertThat(html).contains("Total HT");     // récap Total HT
        assertThat(html).contains("Total TTC");    // récap Total TTC
        assertThat(html).contains("TVA (18%)");    // ligne TVA
    }

    @Test
    void hasTva_vraiquandLigneTvaPositiveSansMontantDocumentTva() {
        // montantTva du document est null, mais la ligne a un taux > 0
        LigneDocument l = ligne("Réfrigérateur",
                new BigDecimal("150000"), new BigDecimal("18"),
                new BigDecimal("150000"), new BigDecimal("27000"), new BigDecimal("177000"));
        DocumentFacturation doc = docAvecLignes(
                List.of(l),
                new BigDecimal("150000"), null, new BigDecimal("177000"), null);

        String html = service.buildHtml(doc, CFG_SIMPLE, false);

        assertThat(html).contains("Montant HT");
        assertThat(html).contains("TVA (18%)");
    }

    @Test
    void hasTva_fauxSansTva() {
        // aucune TVA → hasTva false : pas de colonne TVA, pas de "Total HT"
        LigneDocument l = ligne("Main d'oeuvre",
                new BigDecimal("50000"), BigDecimal.ZERO,
                new BigDecimal("50000"), BigDecimal.ZERO, new BigDecimal("50000"));
        DocumentFacturation doc = docAvecLignes(
                List.of(l),
                new BigDecimal("50000"), BigDecimal.ZERO, new BigDecimal("50000"), null);

        String html = service.buildHtml(doc, CFG_SIMPLE, false);

        assertThat(html).doesNotContain("Total HT");
        assertThat(html).doesNotContain("Montant HT");
        assertThat(html).doesNotContain("TVA (");
        assertThat(html).contains("TOTAL");
    }

    // ── Tests multi-taux TVA ─────────────────────────────────────────────────

    @Test
    void tvaLignes_unTaux_18pct() {
        LigneDocument l = ligne("Climatiseur",
                new BigDecimal("200000"), new BigDecimal("18"),
                new BigDecimal("200000"), new BigDecimal("36000"), new BigDecimal("236000"));
        DocumentFacturation doc = docAvecLignes(
                List.of(l),
                new BigDecimal("200000"), new BigDecimal("36000"), new BigDecimal("236000"), null);

        String html = service.buildHtml(doc, CFG_SIMPLE, false);

        assertThat(html).containsOnlyOnce("TVA (18%)");
        // formatMontant utilise U+00A0 comme séparateur de milliers
        assertThat(html).contains("36 000");
    }

    @Test
    void tvaLignes_deuxTauxDifferents() {
        LigneDocument l1 = ligne("Climatiseur",
                new BigDecimal("200000"), new BigDecimal("18"),
                new BigDecimal("200000"), new BigDecimal("36000"), new BigDecimal("236000"));
        LigneDocument l2 = ligne("Service installation",
                new BigDecimal("10000"), new BigDecimal("10"),
                new BigDecimal("10000"), new BigDecimal("1000"), new BigDecimal("11000"));
        DocumentFacturation doc = docAvecLignes(
                List.of(l1, l2),
                new BigDecimal("210000"), new BigDecimal("37000"), new BigDecimal("247000"), null);

        String html = service.buildHtml(doc, CFG_SIMPLE, false);

        assertThat(html).containsOnlyOnce("TVA (18%)");
        assertThat(html).containsOnlyOnce("TVA (10%)");
    }

    @Test
    void tvaLignes_memesTauxRegroupes() {
        LigneDocument l1 = ligne("Climatiseur A",
                new BigDecimal("200000"), new BigDecimal("18"),
                new BigDecimal("200000"), new BigDecimal("36000"), new BigDecimal("236000"));
        LigneDocument l2 = ligne("Climatiseur B",
                new BigDecimal("100000"), new BigDecimal("18"),
                new BigDecimal("100000"), new BigDecimal("18000"), new BigDecimal("118000"));
        DocumentFacturation doc = docAvecLignes(
                List.of(l1, l2),
                new BigDecimal("300000"), new BigDecimal("54000"), new BigDecimal("354000"), null);

        String html = service.buildHtml(doc, CFG_SIMPLE, false);

        // Un seul taux → une seule ligne TVA (18%)
        assertThat(html).containsOnlyOnce("TVA (18%)");
    }

    // ── Tests montant en lettres ─────────────────────────────────────────────

    @Test
    void montantEnLettres_pasDeDedoublement() {
        // La phrase complète est dans montantEnLettres (MontantEnLettresService).
        // Le template ne doit PAS y ajouter un second préfixe.
        String montantEnLettres = "Arrêtée la présente facture à la somme de deux cent trente-six mille francs CFA";
        LigneDocument l = ligne("Climatiseur",
                new BigDecimal("200000"), new BigDecimal("18"),
                new BigDecimal("200000"), new BigDecimal("36000"), new BigDecimal("236000"));
        DocumentFacturation doc = docAvecLignes(
                List.of(l),
                new BigDecimal("200000"), new BigDecimal("36000"), new BigDecimal("236000"),
                montantEnLettres);

        String html = service.buildHtml(doc, CFG_SIMPLE, false);

        // Le HTML doit contenir la phrase exactement une fois
        assertThat(html).containsOnlyOnce("la présente facture");
    }

    @Test
    void montantEnLettres_ancienDocument_avecPhraseDejaStockee() {
        // Ancien document dont montantEnLettres contient déjà la phrase complète
        String stocke = "Arrêtée la présente facture à la somme de cinquante mille francs CFA";
        LigneDocument l = ligne("Service",
                new BigDecimal("50000"), BigDecimal.ZERO,
                new BigDecimal("50000"), BigDecimal.ZERO, new BigDecimal("50000"));
        DocumentFacturation doc = docAvecLignes(
                List.of(l),
                new BigDecimal("50000"), BigDecimal.ZERO, new BigDecimal("50000"), stocke);

        String html = service.buildHtml(doc, CFG_SIMPLE, false);

        assertThat(html).containsOnlyOnce("la présente facture");
    }

    // ── Test couleur documents ─────────────────────────────────────────────

    @Test
    void couleurDocuments_utiliseeAuLieuDeCouleurPrimaire() {
        EnTeteDocumentConfig cfgBleuMarine = new EnTeteDocumentConfig(
                "FROID CHEIKH", null, null,
                null, null, null, null,
                null, null, null, null,
                ModeEnTeteDocuments.COMPOSE, null, null,
                "#000080", "FCFA"
        );
        LigneDocument l = ligne("Climatiseur",
                new BigDecimal("200000"), BigDecimal.ZERO,
                new BigDecimal("200000"), BigDecimal.ZERO, new BigDecimal("200000"));
        DocumentFacturation doc = docAvecLignes(
                List.of(l), new BigDecimal("200000"), BigDecimal.ZERO, new BigDecimal("200000"), null);

        String html = service.buildHtml(doc, cfgBleuMarine, false);

        assertThat(html).contains("#000080");
    }
}
