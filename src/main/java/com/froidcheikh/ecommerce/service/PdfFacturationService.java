package com.froidcheikh.ecommerce.service;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.*;
import com.froidcheikh.ecommerce.entity.CompteurDocument.TypeDocument;
import com.froidcheikh.ecommerce.entity.DocumentFacturation;
import com.froidcheikh.ecommerce.entity.DocumentFacturation.StatutDocument;
import com.froidcheikh.ecommerce.entity.LigneDocument;
import com.froidcheikh.ecommerce.entity.ReglementDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Génère les PDFs de facturation avec OpenPDF.
 * Police LiberationSans TTF (src/main/resources/fonts/) chargée en IDENTITY_H + embedded,
 * ce qui assure un rendu correct de tous les caractères Latin (é è à ç ô ë ñ ŋ…).
 * Repli sur Helvetica/CP1252 si la ressource est absente.
 * Compatible Alpine sans fontconfig (police embarquée dans le JAR).
 */
@Service
@RequiredArgsConstructor
public class PdfFacturationService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // Couleurs
    private static final Color GRIS_CLAIRE = new Color(0xF5, 0xF5, 0xF5);
    private static final Color GRIS_BORDURE = new Color(0xDD, 0xDD, 0xDD);
    private static final Color BLANC = Color.WHITE;
    private static final Color NOIR = Color.BLACK;
    private static final Color ROUGE_FILIGRANE = new Color(220, 38, 38, 60);

    // ---- Cache police TTF ----
    private volatile BaseFont baseFontCached;

    /**
     * Charge LiberationSans-Regular.ttf depuis le classpath avec IDENTITY_H (Unicode complet).
     * Résultat mis en cache (thread-safe via double-checked locking).
     * Repli sur Helvetica/CP1252 si la ressource est introuvable.
     */
    BaseFont chargerPolice() {
        if (baseFontCached != null) return baseFontCached;
        synchronized (this) {
            if (baseFontCached != null) return baseFontCached;
            try (InputStream is = getClass().getResourceAsStream("/fonts/LiberationSans-Regular.ttf")) {
                if (is != null) {
                    byte[] bytes = is.readAllBytes();
                    baseFontCached = BaseFont.createFont("LiberationSans-Regular.ttf",
                            BaseFont.IDENTITY_H, BaseFont.EMBEDDED, true, bytes, null);
                    return baseFontCached;
                }
            } catch (Exception e) {
                // fall through to Helvetica fallback
            }
            try {
                baseFontCached = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
            } catch (Exception e2) {
                throw new RuntimeException("Impossible de charger la police de facturation", e2);
            }
            return baseFontCached;
        }
    }

    // ---- Fonts ----
    private Font fontTitre(Color couleur) {
        return new Font(chargerPolice(), 18, Font.BOLD, couleur);
    }
    private Font fontSousTitre() {
        return new Font(chargerPolice(), 12, Font.BOLD, NOIR);
    }
    private Font fontNormal() {
        return new Font(chargerPolice(), 11, Font.NORMAL, NOIR);
    }
    private Font fontNormalGras() {
        return new Font(chargerPolice(), 11, Font.BOLD, NOIR);
    }
    private Font fontPetit() {
        return new Font(chargerPolice(), 9, Font.NORMAL, new Color(0x55, 0x55, 0x55));
    }
    private Font fontEntete(Color couleur) {
        return new Font(chargerPolice(), 11, Font.BOLD, couleur);
    }
    /** Total intermédiaire (HT, TVA, remise) : 12 pt gras. */
    private Font fontTotal() {
        return new Font(chargerPolice(), 12, Font.BOLD, NOIR);
    }
    /** Ligne TOTAL / TOTAL TTC : 13 pt gras. */
    private Font fontTotalTTC() {
        return new Font(chargerPolice(), 13, Font.BOLD, NOIR);
    }

    /**
     * Génère un PDF pour le document donné.
     *
     * @param document  le document à rendre (lignes et règlements chargés)
     * @param brouillon si true, ajoute le filigrane "BROUILLON" et ne stocke pas en base
     * @return bytes PDF
     */
    public byte[] generer(DocumentFacturation document, boolean brouillon) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Document pdf = new Document(PageSize.A4, 40, 40, 50, 60);
            PdfWriter writer = PdfWriter.getInstance(pdf, baos);
            pdf.open();

            Color couleurPrimaire = parseCouleur(document.getEntCouleurPrimaire(), new Color(0x25, 0x63, 0xEB));
            String typeLabel = typeLabel(document.getTypeDocument());
            // Devise depuis le document figé (ex. "FCFA") ; repli sur "FCFA"
            String devise = document.getEntDevise() != null && !document.getEntDevise().isBlank()
                    ? document.getEntDevise() : "FCFA";

            // ---- EN-TÊTE ----
            ecrireEnTete(pdf, writer, document, couleurPrimaire, typeLabel, brouillon);

            pdf.add(Chunk.NEWLINE);

            // ---- BLOC ADRESSES ----
            ecrireAdresses(pdf, document, couleurPrimaire);

            pdf.add(new Paragraph(" "));

            // ---- TABLEAU LIGNES ----
            boolean afficheTva = document.getTauxTvaDefaut() != null
                    && document.getTauxTvaDefaut().compareTo(BigDecimal.ZERO) > 0;
            // Pour BON_LIVRAISON : pas de prix
            if (document.getTypeDocument() != TypeDocument.BON_LIVRAISON) {
                ecrireTableauLignes(pdf, document.getLignes(), couleurPrimaire, afficheTva, devise);
                pdf.add(new Paragraph(" "));
                ecireTotaux(pdf, document, afficheTva, couleurPrimaire, devise);
                pdf.add(new Paragraph(" "));
                if (document.getMontantEnLettres() != null) {
                    ecireMontantEnLettres(pdf, document.getMontantEnLettres());
                    pdf.add(new Paragraph(" "));
                }
                if (!document.getReglements().isEmpty()) {
                    ecrireReglements(pdf, document.getReglements(), couleurPrimaire, devise);
                    pdf.add(new Paragraph(" "));
                }
            } else {
                ecrireTableauBL(pdf, document.getLignes(), couleurPrimaire);
                pdf.add(new Paragraph(" "));
                ecrireSignatureBL(pdf, document);
            }

            // ---- PIED ----
            ecrirePied(pdf, document, afficheTva);

            // ---- FILIGRANE BROUILLON ----
            if (brouillon) {
                ajouterFiligrane(writer, "BROUILLON");
            }

            pdf.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Erreur génération PDF : " + e.getMessage(), e);
        }
    }

    // ---- SECTIONS ----

    private void ecrireEnTete(Document pdf, PdfWriter writer,
                               DocumentFacturation doc, Color couleur,
                               String typeLabel, boolean brouillon) throws DocumentException {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1.5f, 1f});

        // Colonne gauche : infos entreprise
        PdfPCell gauche = new PdfPCell();
        gauche.setBorder(Rectangle.NO_BORDER);
        gauche.setPaddingBottom(8);
        if (doc.getEntRaisonSociale() != null) {
            gauche.addElement(new Paragraph(doc.getEntRaisonSociale(), fontSousTitre()));
        }
        if (doc.getEntAdresseSiege() != null) {
            gauche.addElement(new Paragraph(doc.getEntAdresseSiege(), fontNormal()));
        }
        if (doc.getEntNinea() != null) {
            gauche.addElement(new Paragraph("NINEA : " + doc.getEntNinea(), fontNormal()));
        }
        if (doc.getEntRccm() != null) {
            gauche.addElement(new Paragraph("RCCM : " + doc.getEntRccm(), fontNormal()));
        }
        table.addCell(gauche);

        // Colonne droite : type + numéro
        PdfPCell droite = new PdfPCell();
        droite.setBorder(Rectangle.NO_BORDER);
        droite.setHorizontalAlignment(Element.ALIGN_RIGHT);
        droite.setPaddingBottom(8);
        Paragraph titre = new Paragraph(typeLabel, fontTitre(couleur));
        titre.setAlignment(Element.ALIGN_RIGHT);
        droite.addElement(titre);
        if (doc.getNumero() != null) {
            Paragraph num = new Paragraph("N° " + doc.getNumero(), fontNormalGras());
            num.setAlignment(Element.ALIGN_RIGHT);
            droite.addElement(num);
        }
        if (doc.getDateEmission() != null) {
            Paragraph date = new Paragraph("Émise le " + doc.getDateEmission().format(DATE_FMT), fontNormal());
            date.setAlignment(Element.ALIGN_RIGHT);
            droite.addElement(date);
        } else {
            Paragraph date = new Paragraph("Créée le " + doc.getDateCreation().format(DATE_FMT), fontNormal());
            date.setAlignment(Element.ALIGN_RIGHT);
            droite.addElement(date);
        }
        if (doc.getDateValidite() != null) {
            Paragraph dv = new Paragraph("Valable jusqu'au " + doc.getDateValidite().format(DATE_FMT), fontNormal());
            dv.setAlignment(Element.ALIGN_RIGHT);
            droite.addElement(dv);
        }
        table.addCell(droite);

        // Séparateur coloré
        PdfPCell sep = new PdfPCell(new Phrase(""));
        sep.setColspan(2);
        sep.setFixedHeight(3f);
        sep.setBackgroundColor(couleur);
        sep.setBorder(Rectangle.NO_BORDER);
        table.addCell(sep);

        pdf.add(table);
    }

    private void ecrireAdresses(Document pdf, DocumentFacturation doc, Color couleur) throws DocumentException {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1f, 1f});
        table.setSpacingBefore(10);

        PdfPCell facturer = cellAdresse("FACTURER À", doc, couleur);
        table.addCell(facturer);

        // Commande liée
        PdfPCell refCell = new PdfPCell();
        refCell.setBorder(Rectangle.NO_BORDER);
        refCell.setPadding(8);
        if (doc.getCommande() != null) {
            refCell.addElement(new Paragraph("Référence commande :", fontPetit()));
            refCell.addElement(new Paragraph(doc.getCommande().getNumeroCommande(), fontNormalGras()));
        }
        if (doc.getDocumentOrigine() != null && doc.getDocumentOrigine().getNumero() != null) {
            refCell.addElement(new Paragraph("Document d'origine :", fontPetit()));
            refCell.addElement(new Paragraph(doc.getDocumentOrigine().getNumero(), fontNormalGras()));
        }
        table.addCell(refCell);

        pdf.add(table);
    }

    private PdfPCell cellAdresse(String titre, DocumentFacturation doc, Color couleur) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.BOX);
        cell.setBorderColor(GRIS_BORDURE);
        cell.setPadding(8);
        Paragraph t = new Paragraph(titre, fontEntete(couleur));
        cell.addElement(t);
        cell.addElement(new Phrase(" "));
        String nom = concat(doc.getClientPrenom(), doc.getClientNom());
        if (!nom.isBlank()) cell.addElement(new Paragraph(nom, fontNormalGras()));
        if (doc.getClientAdresse() != null) cell.addElement(new Paragraph(doc.getClientAdresse(), fontNormal()));
        if (doc.getClientTelephone() != null) cell.addElement(new Paragraph("Tél : " + doc.getClientTelephone(), fontNormal()));
        if (doc.getClientEmail() != null) cell.addElement(new Paragraph(doc.getClientEmail(), fontNormal()));
        if (doc.getClientNinea() != null) cell.addElement(new Paragraph("NINEA : " + doc.getClientNinea(), fontNormal()));
        if (doc.getClientRccm() != null) cell.addElement(new Paragraph("RCCM : " + doc.getClientRccm(), fontNormal()));
        return cell;
    }

    private void ecrireTableauLignes(Document pdf, List<LigneDocument> lignes,
                                      Color couleur, boolean afficheTva, String devise) throws DocumentException {
        int cols = afficheTva ? 7 : 5;
        PdfPTable table = new PdfPTable(cols);
        table.setWidthPercentage(100);
        if (afficheTva) {
            table.setWidths(new float[]{3.0f, 0.6f, 1.4f, 0.8f, 1.6f, 0.7f, 1.7f});
        } else {
            table.setWidths(new float[]{4.0f, 0.7f, 1.5f, 0.9f, 1.7f});
        }

        // En-têtes — devise indiquée dans les colonnes montants
        String[] headers = afficheTva
                ? new String[]{"Désignation", "Qté", "P.U. (" + devise + ")", "Remise", "Montant HT (" + devise + ")", "TVA", "Montant TTC (" + devise + ")"}
                : new String[]{"Désignation", "Qté", "P.U. (" + devise + ")", "Remise", "Montant (" + devise + ")"};

        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, fontEntete(BLANC)));
            cell.setBackgroundColor(couleur);
            cell.setPadding(5);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(cell);
        }

        // Lignes
        boolean gris = false;
        for (LigneDocument ligne : lignes) {
            Color bg = gris ? GRIS_CLAIRE : BLANC;
            gris = !gris;

            addCell(table, ligne.getDesignation(), Element.ALIGN_LEFT, bg);
            addCell(table, formatQuantite(ligne.getQuantite()), Element.ALIGN_CENTER, bg);
            addCell(table, formatMontant(ligne.getPrixUnitaireHT()), Element.ALIGN_RIGHT, bg);
            String remise = ligne.getRemisePourcent() != null && ligne.getRemisePourcent().compareTo(BigDecimal.ZERO) > 0
                    ? formatPourcent(ligne.getRemisePourcent()) : "-";
            addCell(table, remise, Element.ALIGN_CENTER, bg);

            if (afficheTva) {
                addCell(table, formatMontant(ligne.getMontantHT()), Element.ALIGN_RIGHT, bg);
                addCell(table, formatPourcent(ligne.getTauxTva()), Element.ALIGN_CENTER, bg);
                addCell(table, formatMontant(ligne.getMontantTTC()), Element.ALIGN_RIGHT, bg);
            } else {
                addCell(table, formatMontant(ligne.getMontantHT()), Element.ALIGN_RIGHT, bg);
            }
        }

        pdf.add(table);
    }

    private void ecrireTableauBL(Document pdf, List<LigneDocument> lignes, Color couleur) throws DocumentException {
        PdfPTable table = new PdfPTable(3);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{4f, 1f, 0.8f});

        String[] headers = {"Désignation", "Quantité", "Reçu"};
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, fontEntete(BLANC)));
            cell.setBackgroundColor(couleur);
            cell.setPadding(5);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(cell);
        }

        boolean gris = false;
        for (LigneDocument ligne : lignes) {
            Color bg = gris ? GRIS_CLAIRE : BLANC;
            gris = !gris;
            addCell(table, ligne.getDesignation(), Element.ALIGN_LEFT, bg);
            addCell(table, formatQuantite(ligne.getQuantite()), Element.ALIGN_CENTER, bg);
            // Case de signature
            PdfPCell sig = new PdfPCell(new Phrase(""));
            sig.setFixedHeight(20f);
            sig.setBackgroundColor(bg);
            table.addCell(sig);
        }
        pdf.add(table);
    }

    private void ecireTotaux(Document pdf, DocumentFacturation doc,
                              boolean afficheTva, Color couleur, String devise) throws DocumentException {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(50);
        table.setHorizontalAlignment(Element.ALIGN_RIGHT);

        if (afficheTva) {
            ligneTotal(table, "Total HT", doc.getMontantHT(), false, devise);
            ligneTotal(table, "TVA (" + doc.getTauxTvaDefaut() + "%)", doc.getMontantTva(), false, devise);
            if (doc.getRemiseTotalePourcent() != null && doc.getRemiseTotalePourcent().compareTo(BigDecimal.ZERO) > 0) {
                ligneTotal(table, "Remise globale (" + formatPourcent(doc.getRemiseTotalePourcent()) + ")", null, false, devise);
            }
            ligneTotal(table, "TOTAL TTC", doc.getMontantTTC(), true, devise);
        } else {
            if (doc.getEntMentionExonerationTva() != null) {
                PdfPCell mention = new PdfPCell(new Phrase(doc.getEntMentionExonerationTva(), fontPetit()));
                mention.setColspan(2);
                mention.setBorder(Rectangle.NO_BORDER);
                mention.setHorizontalAlignment(Element.ALIGN_RIGHT);
                table.addCell(mention);
            }
            ligneTotal(table, "TOTAL", doc.getMontantTTC(), true, devise);
        }
        pdf.add(table);
    }

    private void ligneTotal(PdfPTable table, String label, BigDecimal valeur, boolean isTTC, String devise) {
        Font f = isTTC ? fontTotalTTC() : fontTotal();
        PdfPCell cLabel = new PdfPCell(new Phrase(label, f));
        cLabel.setHorizontalAlignment(Element.ALIGN_RIGHT);
        cLabel.setPadding(5);
        cLabel.setBorderColor(GRIS_BORDURE);
        if (isTTC) cLabel.setBackgroundColor(GRIS_CLAIRE);
        table.addCell(cLabel);

        String valStr = valeur != null ? formatMontant(valeur) + " " + devise : "";
        PdfPCell cVal = new PdfPCell(new Phrase(valStr, f));
        cVal.setHorizontalAlignment(Element.ALIGN_RIGHT);
        cVal.setPadding(5);
        cVal.setBorderColor(GRIS_BORDURE);
        if (isTTC) cVal.setBackgroundColor(GRIS_CLAIRE);
        table.addCell(cVal);
    }

    private void ecireMontantEnLettres(Document pdf, String lettres) throws DocumentException {
        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100);
        // 11 pt = fontNormal() — déjà à la bonne taille
        PdfPCell cell = new PdfPCell(new Phrase(lettres, fontNormal()));
        cell.setPadding(7);
        cell.setBorderColor(GRIS_BORDURE);
        table.addCell(cell);
        pdf.add(table);
    }

    private void ecrireReglements(Document pdf, List<ReglementDocument> reglements,
                                   Color couleur, String devise) throws DocumentException {
        pdf.add(new Paragraph("Règlements enregistrés", fontSousTitre()));
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(80);
        String[] h = {"Date", "Mode", "Référence", "Montant"};
        for (String header : h) {
            PdfPCell cell = new PdfPCell(new Phrase(header, fontEntete(BLANC)));
            cell.setBackgroundColor(couleur);
            cell.setPadding(4);
            table.addCell(cell);
        }
        for (ReglementDocument r : reglements) {
            if (r.getSupprimePar() != null) continue;
            addCell(table, r.getDateReglement().format(DATE_FMT), Element.ALIGN_CENTER, BLANC);
            addCell(table, r.getMode().name(), Element.ALIGN_CENTER, BLANC);
            addCell(table, r.getReference() != null ? r.getReference() : "-", Element.ALIGN_LEFT, BLANC);
            addCell(table, formatMontant(r.getMontant()) + " " + devise, Element.ALIGN_RIGHT, BLANC);
        }
        pdf.add(table);
    }

    private void ecrireSignatureBL(Document pdf, DocumentFacturation doc) throws DocumentException {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        if (doc.getLivreurNom() != null) {
            PdfPCell left = new PdfPCell();
            left.setBorder(Rectangle.BOX);
            left.setPadding(8);
            left.setMinimumHeight(60f);
            left.addElement(new Paragraph("Livreur : " + doc.getLivreurNom(), fontNormal()));
            if (doc.getLivreurTelephone() != null) {
                left.addElement(new Paragraph("Tél : " + doc.getLivreurTelephone(), fontNormal()));
            }
            table.addCell(left);
        }
        PdfPCell right = new PdfPCell();
        right.setBorder(Rectangle.BOX);
        right.setPadding(8);
        right.setMinimumHeight(60f);
        right.addElement(new Paragraph("Signature du client :", fontNormal()));
        table.addCell(right);
        pdf.add(table);
    }

    private void ecrirePied(Document pdf, DocumentFacturation doc, boolean afficheTva) throws DocumentException {
        pdf.add(new Paragraph(" "));
        if (doc.getEntConditionsPaiement() != null) {
            pdf.add(new Paragraph("Conditions de paiement : " + doc.getEntConditionsPaiement(), fontPetit()));
        }
        if (doc.getEntMentionsPied() != null) {
            pdf.add(new Paragraph(doc.getEntMentionsPied(), fontPetit()));
        }
        if (!afficheTva && doc.getEntMentionExonerationTva() != null) {
            pdf.add(new Paragraph(doc.getEntMentionExonerationTva(), fontPetit()));
        }
    }

    private void ajouterFiligrane(PdfWriter writer, String texte) throws DocumentException {
        PdfContentByte canvas = writer.getDirectContentUnder();
        BaseFont bf;
        try {
            bf = BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
        } catch (Exception e) {
            return;
        }
        canvas.saveState();
        canvas.setColorFill(ROUGE_FILIGRANE);
        canvas.beginText();
        canvas.setFontAndSize(bf, 80);
        canvas.showTextAligned(Element.ALIGN_CENTER, texte,
                PageSize.A4.getWidth() / 2, PageSize.A4.getHeight() / 2, 45);
        canvas.endText();
        canvas.restoreState();
    }

    // ---- HELPERS ----

    private void addCell(PdfPTable table, String text, int align, Color bg) {
        PdfPCell cell = new PdfPCell(new Phrase(text, fontNormal()));
        cell.setHorizontalAlignment(align);
        cell.setPadding(4);
        cell.setBorderColor(GRIS_BORDURE);
        cell.setBackgroundColor(bg);
        table.addCell(cell);
    }

    private String typeLabel(TypeDocument type) {
        return switch (type) {
            case FACTURE -> "FACTURE";
            case DEVIS -> "DEVIS";
            case AVOIR -> "AVOIR";
            case BON_LIVRAISON -> "BON DE LIVRAISON";
        };
    }

    private Color parseCouleur(String hex, Color defaut) {
        if (hex == null || !hex.startsWith("#") || hex.length() != 7) return defaut;
        try {
            int r = Integer.parseInt(hex.substring(1, 3), 16);
            int g = Integer.parseInt(hex.substring(3, 5), 16);
            int b = Integer.parseInt(hex.substring(5, 7), 16);
            return new Color(r, g, b);
        } catch (NumberFormatException e) {
            return defaut;
        }
    }

    /**
     * Visible pour les tests unitaires.
     * Formate un montant entier FCFA avec espace insécable U+00A0 comme séparateur des milliers.
     * Exemple : 185000 → "185 000", 1250000 → "1 250 000", 0 → "0".
     * Pas de décimales, pas de symbole devise (ajouté à l'appel).
     * Utilise U+00A0 au lieu de U+202F (espace fine insécable) car CP1252 ne supporte pas U+202F.
     */
    String formatMontant(BigDecimal val) {
        if (val == null) return "-";
        long v = val.setScale(0, RoundingMode.HALF_UP).longValue();
        if (v == 0) return "0";
        boolean neg = v < 0;
        String s = Long.toString(Math.abs(v));
        int len = s.length();
        int first = len % 3;
        if (first == 0) first = 3;
        StringBuilder sb = new StringBuilder();
        sb.append(s, 0, first);
        for (int i = first; i < len; i += 3) {
            sb.append(' ');
            sb.append(s, i, i + 3);
        }
        return neg ? "-" + sb : sb.toString();
    }

    private String formatQuantite(BigDecimal val) {
        if (val == null) return "-";
        if (val.scale() == 0 || val.stripTrailingZeros().scale() <= 0) {
            return formatMontant(val);
        }
        return val.stripTrailingZeros().toPlainString();
    }

    private String formatPourcent(BigDecimal val) {
        if (val == null) return "-";
        return val.stripTrailingZeros().toPlainString() + "%";
    }

    private String concat(String a, String b) {
        String pa = a != null ? a.trim() : "";
        String pb = b != null ? b.trim() : "";
        if (pa.isEmpty()) return pb;
        if (pb.isEmpty()) return pa;
        return pa + " " + pb;
    }
}
