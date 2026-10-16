package com.froidcheikh.ecommerce.service;

import com.froidcheikh.ecommerce.entity.CompteurDocument.TypeDocument;
import com.froidcheikh.ecommerce.entity.DocumentFacturation;
import com.froidcheikh.ecommerce.entity.LigneDocument;
import com.froidcheikh.ecommerce.entity.ModeEnTeteDocuments;
import com.froidcheikh.ecommerce.entity.ReglementDocument;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.openhtmltopdf.util.XRLog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.net.URLConnection;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;

/**
 * Génère les PDFs de facturation avec openhtmltopdf (moteur PDFBox).
 * Polices TTF chargées depuis le classpath (LiberationSans, Oswald).
 * Aucune dépendance aux polices système : compatible Alpine JRE sans fontconfig.
 */
@Service
@Slf4j
public class PdfFacturationService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // Cache polices TTF (thread-safe via double-checked locking)
    private volatile byte[] fontLsBytes;
    private volatile byte[] fontOswBytes;

    // ── API publique (signatures inchangées) ──────────────────────────────────

    /** Sans config : construit un contexte minimal depuis les champs figés du document. */
    public byte[] generer(DocumentFacturation doc, boolean brouillon) {
        EnTeteDocumentConfig cfg = new EnTeteDocumentConfig(
                doc.getEntRaisonSociale(), null, null,
                doc.getEntAdresseSiege(), null, null, null,
                doc.getEntRccm(), doc.getEntNinea(), null, null,
                ModeEnTeteDocuments.COMPOSE, null, null,
                doc.getEntCouleurPrimaire(), doc.getEntDevise()
        );
        return generer(doc, cfg, brouillon);
    }

    public byte[] generer(DocumentFacturation doc, EnTeteDocumentConfig cfg, boolean brouillon) {
        try {
            return renderHtml(buildHtml(doc, cfg, brouillon));
        } catch (Exception e) {
            throw new RuntimeException("Erreur génération PDF : " + e.getMessage(), e);
        }
    }

    // ── Construction HTML ─────────────────────────────────────────────────────

    private String buildHtml(DocumentFacturation doc, EnTeteDocumentConfig cfg, boolean brouillon) {
        String couleur  = safe(cfg.couleurHex(), "#000080");
        String devise   = safe(doc.getEntDevise(), safe(cfg.devise(), "FCFA"));
        boolean isBL    = doc.getTypeDocument() == TypeDocument.BON_LIVRAISON;
        boolean hasTva  = doc.getTauxTvaDefaut() != null
                && doc.getTauxTvaDefaut().compareTo(BigDecimal.ZERO) > 0;
        String logoUri  = loadImageDataUri(cfg.logoUrl());

        StringBuilder sb = new StringBuilder(8192);
        sb.append("<!DOCTYPE html><html xmlns=\"http://www.w3.org/1999/xhtml\"><head><meta charset=\"UTF-8\"/><style>");
        appendCss(sb, couleur, brouillon);
        sb.append("</style></head><body>");

        if (brouillon) sb.append("<div id=\"brouillon\">BROUILLON</div>");

        // Pied répété sur chaque page (running element — extrait du flux avant le corps)
        appendRunningFooter(sb, cfg);

        // En-tête
        if (cfg.mode() == ModeEnTeteDocuments.IMAGE && hasValue(cfg.urlImageEnTete())) {
            appendEnTeteImage(sb, cfg);
        } else {
            appendEnTeteCompose(sb, cfg, couleur, logoUri);
        }

        // Double trait (épais + fin)
        sb.append("<div style=\"border-top:3pt solid ").append(couleur)
          .append(";border-bottom:1pt solid ").append(couleur)
          .append(";height:0;padding:2pt 0;margin:2.5mm 0 4mm 0;\"></div>");

        // Titre + date
        appendTitreDate(sb, doc, couleur);

        // Bloc client
        appendBlocClient(sb, doc);

        sb.append("<div style=\"height:3mm;\"></div>");

        // Corps du document
        if (isBL) {
            appendTableauBL(sb, doc, couleur);
            appendSignatureBL(sb, doc);
        } else {
            appendTableauLignes(sb, doc, couleur, hasTva, devise);
            appendRecap(sb, doc, hasTva, couleur, devise);
            List<ReglementDocument> regs = reglementsActifs(doc);
            if (!regs.isEmpty()) appendTableauReglements(sb, regs, couleur, devise);
        }

        appendMentions(sb, doc);
        sb.append("</body></html>");
        return sb.toString();
    }

    // ── CSS ───────────────────────────────────────────────────────────────────

    private void appendCss(StringBuilder sb, String couleur, boolean brouillon) {
        sb.append("@page{size:A4;margin:15mm 15mm 30mm 15mm}");
        // Pied ancré dans la marge basse de chaque page
        sb.append("@page{@bottom-left{content:element(docFooter);font-family:'LiberationSans';font-size:7pt;vertical-align:top}}");
        sb.append("@page{@bottom-right{content:\"Page \" counter(page) \" / \" counter(pages);font-family:'LiberationSans';font-size:7pt;color:#888;vertical-align:top;padding-top:4pt}}");
        sb.append("body{font-family:'LiberationSans';font-size:9pt;color:#1a1a1a;margin:0;padding:0}");
        sb.append("#docFooter{position:running(docFooter)}");
        sb.append("table{border-collapse:collapse;width:100%}");
        sb.append("td,th{padding:3pt 5pt;font-size:8.5pt}");
        sb.append(".th{background:").append(couleur).append(";color:#fff;font-weight:bold;font-size:8pt;padding:4pt 5pt}");
        sb.append(".alt{background:#f7f7f7}");
        sb.append(".r{text-align:right;white-space:nowrap}");
        sb.append(".c{text-align:center}");
        sb.append(".sec{font-weight:bold;font-size:8.5pt;color:").append(couleur)
          .append(";border-bottom:0.5pt solid ").append(couleur)
          .append(";padding-bottom:1pt;margin:4mm 0 1.5mm 0}");
        if (brouillon) {
            sb.append("#brouillon{position:fixed;top:38%;left:5%;width:90%;text-align:center;")
              .append("font-family:'Oswald';font-weight:bold;font-size:70pt;")
              .append("color:rgba(200,0,0,0.13);transform:rotate(-30deg);z-index:-1}");
        }
    }

    // ── Pied de page (running element) ────────────────────────────────────────

    private void appendRunningFooter(StringBuilder sb, EnTeteDocumentConfig cfg) {
        sb.append("<div id=\"docFooter\">");
        if (cfg.mode() == ModeEnTeteDocuments.IMAGE && hasValue(cfg.urlImagePied())) {
            String imgUri = loadImageDataUri(cfg.urlImagePied());
            if (imgUri != null) {
                sb.append("<img src=\"").append(imgUri)
                  .append("\" style=\"width:100%;display:block;\"/>");
                sb.append("</div>");
                return;
            }
        }
        // Mode composé : texte compact sur 2-3 lignes
        sb.append("<div style=\"border-top:0.5pt solid #bbb;padding-top:2pt;font-size:7pt;color:#555;\">");
        if (hasValue(cfg.adresse())) {
            sb.append(h(cfg.adresse()));
            if (hasValue(cfg.boitePostale())) sb.append(" — ").append(h(cfg.boitePostale()));
            sb.append("<br/>");
        }
        // Ligne 2 : téléphone | RC | NINEA
        boolean l2 = false;
        if (hasValue(cfg.telephone())) { sb.append("Tél : ").append(h(cfg.telephone())); l2 = true; }
        if (hasValue(cfg.rccm()))      { if (l2) sb.append(" | "); sb.append("RC : ").append(h(cfg.rccm())); l2 = true; }
        if (hasValue(cfg.ninea()))     { if (l2) sb.append(" | "); sb.append("NINEA : ").append(h(cfg.ninea())); l2 = true; }
        if (l2) sb.append("<br/>");
        // Ligne 3 : banque | email
        boolean l3 = false;
        if (hasValue(cfg.banqueNom()) || hasValue(cfg.numeroCompte())) {
            sb.append("Banque : ").append(h(safe(cfg.banqueNom(), "")));
            if (hasValue(cfg.numeroCompte())) sb.append(" N° ").append(h(cfg.numeroCompte()));
            l3 = true;
        }
        if (hasValue(cfg.email())) { if (l3) sb.append(" | "); sb.append(h(cfg.email())); }
        sb.append("</div></div>");
    }

    // ── En-tête composé ───────────────────────────────────────────────────────

    private void appendEnTeteCompose(StringBuilder sb, EnTeteDocumentConfig cfg,
                                     String couleur, String logoUri) {
        int fs = nomFontPt(cfg.raisonSociale());
        sb.append("<table width=\"100%\"><tr>");
        if (hasValue(logoUri)) {
            sb.append("<td width=\"18%\" style=\"vertical-align:middle;padding-right:3mm;\">")
              .append("<img src=\"").append(logoUri)
              .append("\" style=\"max-width:100%;max-height:22mm;display:block;\"/>")
              .append("</td>")
              .append("<td style=\"vertical-align:middle;text-align:center;\">");
        } else {
            sb.append("<td style=\"vertical-align:middle;text-align:center;\">");
        }
        sb.append("<div style=\"font-family:'Oswald';font-weight:bold;font-size:").append(fs)
          .append("pt;color:").append(couleur).append(";white-space:nowrap;overflow:hidden;\">")
          .append(h(safe(cfg.raisonSociale(), "")))
          .append("</div>");
        if (hasValue(cfg.texteDescriptif())) {
            sb.append("<div style=\"font-size:7pt;color:#555;margin-top:2pt;\">")
              .append(h(cfg.texteDescriptif())).append("</div>");
        }
        sb.append("</td></tr></table>");
    }

    private void appendEnTeteImage(StringBuilder sb, EnTeteDocumentConfig cfg) {
        String imgUri = loadImageDataUri(cfg.urlImageEnTete());
        if (imgUri == null) {
            appendEnTeteCompose(sb, cfg, "#000080", loadImageDataUri(cfg.logoUrl()));
            return;
        }
        sb.append("<div style=\"text-align:center;\"><img src=\"").append(imgUri)
          .append("\" style=\"display:block;width:100%;max-height:48mm;\"/></div>");
    }

    // ── Titre + date ──────────────────────────────────────────────────────────

    private void appendTitreDate(StringBuilder sb, DocumentFacturation doc, String couleur) {
        String label  = typeLabel(doc.getTypeDocument());
        String numero = hasValue(doc.getNumero()) ? doc.getNumero() : "(brouillon)";
        String titre  = label + " N° " + h(numero);

        // Référence à la facture origine pour les avoirs
        String originRef = null;
        if (doc.getTypeDocument() == TypeDocument.AVOIR && doc.getDocumentOrigine() != null
                && hasValue(doc.getDocumentOrigine().getNumero())) {
            originRef = doc.getDocumentOrigine().getNumero();
        }

        sb.append("<table width=\"100%\"><tr>");
        sb.append("<td style=\"vertical-align:top;\">")
          .append("<span style=\"font-family:'Oswald';font-weight:bold;font-size:13pt;color:")
          .append(couleur).append(";white-space:nowrap;\">").append(titre).append("</span>");
        if (originRef != null) {
            sb.append("<br/><span style=\"font-size:8pt;color:#555;\">Avoir sur ")
              .append(h(originRef)).append("</span>");
        }
        sb.append("</td>");
        sb.append("<td style=\"text-align:right;vertical-align:top;font-size:8pt;\">");
        if (doc.getDateEmission() != null)
            sb.append("Émis le : <strong>").append(doc.getDateEmission().format(DATE_FMT)).append("</strong><br/>");
        else if (doc.getDateCreation() != null)
            sb.append("Créé le : <strong>").append(doc.getDateCreation().format(DATE_FMT)).append("</strong><br/>");
        if (doc.getDateValidite() != null)
            sb.append("Valide jusqu'au : <strong>").append(doc.getDateValidite().format(DATE_FMT)).append("</strong>");
        sb.append("</td></tr></table>");
        sb.append("<div style=\"height:3mm;\"></div>");
    }

    // ── Bloc client ───────────────────────────────────────────────────────────

    private void appendBlocClient(StringBuilder sb, DocumentFacturation doc) {
        sb.append("<table width=\"100%\"><tr>");
        sb.append("<td width=\"48%\" style=\"border:0.5pt solid #ddd;padding:4pt 6pt;vertical-align:top;\">");
        sb.append("<div style=\"font-size:7pt;font-weight:bold;text-transform:uppercase;color:#999;margin-bottom:2pt;\">Facturé à</div>");
        String nom = concat(doc.getClientPrenom(), doc.getClientNom());
        if (hasValue(nom))                  sb.append("<strong>").append(h(nom)).append("</strong><br/>");
        if (hasValue(doc.getClientAdresse())) sb.append(h(doc.getClientAdresse())).append("<br/>");
        if (hasValue(doc.getClientTelephone())) sb.append("Tél : ").append(h(doc.getClientTelephone())).append("<br/>");
        if (hasValue(doc.getClientEmail()))    sb.append(h(doc.getClientEmail())).append("<br/>");
        if (hasValue(doc.getClientNinea()))    sb.append("NINEA : ").append(h(doc.getClientNinea())).append("<br/>");
        if (hasValue(doc.getClientRccm()))     sb.append("RC : ").append(h(doc.getClientRccm()));
        sb.append("</td><td></td></tr></table>");
    }

    // ── Tableau des lignes (FACTURE / DEVIS / AVOIR) ──────────────────────────

    private void appendTableauLignes(StringBuilder sb, DocumentFacturation doc,
                                     String couleur, boolean hasTva, String devise) {
        List<LigneDocument> lignes = doc.getLignes();
        if (lignes == null || lignes.isEmpty()) return;
        sb.append("<div class=\"sec\">Détail des prestations</div>");
        sb.append("<table width=\"100%\">");
        sb.append("<tr>");
        th(sb, "Désignation",              "width:40%");
        th(sb, "Qté",                      "width:7%;text-align:center");
        th(sb, "Prix unit. HT",            "width:14%;text-align:right");
        if (hasTva) th(sb, "TVA",          "width:8%;text-align:center");
        th(sb, "Remise",                   "width:9%;text-align:center");
        th(sb, hasTva ? "Total TTC" : "Total HT", "width:14%;text-align:right");
        sb.append("</tr>");
        for (int i = 0; i < lignes.size(); i++) {
            LigneDocument l = lignes.get(i);
            sb.append("<tr").append(i % 2 == 1 ? " class=\"alt\"" : "").append(">");
            td(sb, h(l.getDesignation()), "");
            td(sb, formatQuantite(l.getQuantite()), "text-align:center");
            td(sb, formatMontant(l.getPrixUnitaireHT()), "text-align:right");
            if (hasTva) td(sb, formatPourcent(l.getTauxTva()), "text-align:center");
            td(sb, formatPourcent(l.getRemisePourcent()), "text-align:center");
            BigDecimal total = hasTva ? l.getMontantTTC() : l.getMontantHT();
            td(sb, "<strong>" + formatMontant(total) + "</strong>", "text-align:right");
            sb.append("</tr>");
        }
        sb.append("</table>");
    }

    // ── Récapitulatif ─────────────────────────────────────────────────────────

    private void appendRecap(StringBuilder sb, DocumentFacturation doc,
                             boolean hasTva, String couleur, String devise) {
        BigDecimal ht  = safe0(doc.getMontantHT());
        BigDecimal tva = safe0(doc.getMontantTva());
        BigDecimal ttc = safe0(doc.getMontantTTC());

        BigDecimal dejaRegle = reglementsActifs(doc).stream()
                .map(ReglementDocument::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        boolean hasRegl = dejaRegle.compareTo(BigDecimal.ZERO) > 0;
        BigDecimal reste = ttc.subtract(dejaRegle).max(BigDecimal.ZERO);

        String statut; String statutColor;
        if (!hasRegl) { statut = "Non payée"; statutColor = "#dc2626"; }
        else if (reste.compareTo(BigDecimal.ZERO) == 0) { statut = "Payée"; statutColor = "#16a34a"; }
        else { statut = "Partiellement payée"; statutColor = "#d97706"; }

        sb.append("<div style=\"height:3mm;\"></div><table width=\"100%\"><tr><td width=\"52%\">");
        if (hasValue(doc.getMontantEnLettres())) {
            sb.append("<div style=\"font-size:8pt;font-style:italic;color:#555;border:0.5pt solid #ddd;padding:3pt 5pt;\">")
              .append("Arrêtée à : <strong>").append(h(doc.getMontantEnLettres())).append("</strong></div>");
        }
        sb.append("</td><td width=\"48%\"><table width=\"100%\">");
        if (hasTva) {
            recapRow(sb, "Total HT", formatMontant(ht) + " " + h(devise), "#1a1a1a", false);
            if (tva.compareTo(BigDecimal.ZERO) > 0)
                recapRow(sb, "TVA (" + formatPourcent(doc.getTauxTvaDefaut()) + ")",
                        formatMontant(tva) + " " + h(devise), "#1a1a1a", false);
        }
        recapRow(sb, "Total " + (hasTva ? "TTC" : "HT"), formatMontant(ttc) + " " + h(devise), couleur, true);
        if (hasRegl) {
            recapRow(sb, "Déjà réglé", "(" + formatMontant(dejaRegle) + " " + h(devise) + ")", "#16a34a", false);
            if (reste.compareTo(BigDecimal.ZERO) > 0)
                recapRow(sb, "Reste à payer", formatMontant(reste) + " " + h(devise), "#dc2626", true);
        }
        sb.append("</table>");
        sb.append("<div style=\"text-align:right;margin-top:2pt;\">")
          .append("<span style=\"font-family:'LiberationSans';font-weight:bold;font-size:8pt;color:")
          .append(statutColor).append(";\">").append(statut).append("</span></div>");
        sb.append("</td></tr></table>");
    }

    private void recapRow(StringBuilder sb, String label, String valeur, String color, boolean gras) {
        String g = gras ? "font-weight:bold;" : "";
        sb.append("<tr>")
          .append("<td style=\"padding:2pt 5pt;text-align:right;font-size:8.5pt;").append(g).append("\">")
          .append(h(label)).append("</td>")
          .append("<td style=\"padding:2pt 5pt;text-align:right;white-space:nowrap;font-size:8.5pt;")
          .append(g).append("color:").append(color).append(";\">").append(valeur).append("</td>")
          .append("</tr>");
    }

    // ── Tableau des règlements ─────────────────────────────────────────────────

    private void appendTableauReglements(StringBuilder sb, List<ReglementDocument> regs,
                                         String couleur, String devise) {
        sb.append("<div class=\"sec\" style=\"margin-top:5mm;\">Règlements</div>");
        sb.append("<table width=\"100%\"><tr>");
        th(sb, "Date",       "width:20%");
        th(sb, "Mode",       "width:20%");
        th(sb, "Référence",  "width:42%");
        th(sb, "Montant",    "width:18%;text-align:right");
        sb.append("</tr>");
        for (int i = 0; i < regs.size(); i++) {
            ReglementDocument r = regs.get(i);
            sb.append("<tr").append(i % 2 == 1 ? " class=\"alt\"" : "").append(">");
            td(sb, r.getDateReglement() != null ? r.getDateReglement().format(DATE_FMT) : "-", "");
            td(sb, r.getMode() != null ? h(r.getMode().toString()) : "-", "");
            td(sb, hasValue(r.getReference()) ? h(r.getReference()) : "-", "");
            td(sb, "<strong>" + formatMontant(r.getMontant()) + " " + h(devise) + "</strong>", "text-align:right");
            sb.append("</tr>");
        }
        sb.append("</table>");
    }

    // ── Bon de livraison ──────────────────────────────────────────────────────

    private void appendTableauBL(StringBuilder sb, DocumentFacturation doc, String couleur) {
        List<LigneDocument> lignes = doc.getLignes();
        if (lignes == null || lignes.isEmpty()) return;

        // Infos livreur
        if (hasValue(doc.getLivreurNom()) || hasValue(doc.getLivreurTelephone())) {
            sb.append("<div style=\"font-size:8.5pt;margin-bottom:3mm;\">");
            sb.append("<strong>Livreur : </strong>").append(h(safe(doc.getLivreurNom(), "")));
            if (hasValue(doc.getLivreurTelephone()))
                sb.append(" — Tél : ").append(h(doc.getLivreurTelephone()));
            sb.append("</div>");
        }

        sb.append("<div class=\"sec\">Articles livrés</div>");
        sb.append("<table width=\"100%\"><tr>");
        th(sb, "Désignation",    "width:55%");
        th(sb, "Qté commandée",  "width:15%;text-align:center");
        th(sb, "Qté livrée",     "width:15%;text-align:center");
        th(sb, "Observations",   "width:15%");
        sb.append("</tr>");
        for (int i = 0; i < lignes.size(); i++) {
            LigneDocument l = lignes.get(i);
            sb.append("<tr").append(i % 2 == 1 ? " class=\"alt\"" : "").append(">");
            td(sb, h(l.getDesignation()), "");
            td(sb, formatQuantite(l.getQuantite()), "text-align:center");
            td(sb, formatQuantite(l.getQuantite()), "text-align:center");
            td(sb, "", "");
            sb.append("</tr>");
        }
        sb.append("</table>");
    }

    private void appendSignatureBL(StringBuilder sb, DocumentFacturation doc) {
        sb.append("<div style=\"height:10mm;\"></div>");
        sb.append("<table width=\"100%\"><tr>")
          .append("<td width=\"45%\" style=\"border-top:0.5pt solid #aaa;padding-top:2pt;font-size:8pt;\">Signature émetteur</td>")
          .append("<td width=\"10%\"></td>")
          .append("<td width=\"45%\" style=\"border-top:0.5pt solid #aaa;padding-top:2pt;font-size:8pt;\">Signature destinataire (Bon pour accord)</td>")
          .append("</tr></table>");
    }

    // ── Mentions conditionnelles ──────────────────────────────────────────────

    private void appendMentions(StringBuilder sb, DocumentFacturation doc) {
        if (hasValue(doc.getEntConditionsPaiement())) {
            sb.append("<div style=\"margin-top:4mm;font-size:7.5pt;color:#444;\"><strong>Conditions de paiement : </strong>")
              .append(h(doc.getEntConditionsPaiement())).append("</div>");
        }
        if (hasValue(doc.getEntMentionExonerationTva())) {
            sb.append("<div style=\"margin-top:2mm;font-size:7.5pt;font-style:italic;color:#555;\">")
              .append(h(doc.getEntMentionExonerationTva())).append("</div>");
        }
        if (hasValue(doc.getEntMentionsPied())) {
            sb.append("<div style=\"margin-top:2mm;font-size:7pt;color:#777;\">")
              .append(h(doc.getEntMentionsPied())).append("</div>");
        }
        if (hasValue(doc.getEntCachetUrl())) {
            String uri = loadImageDataUri(doc.getEntCachetUrl());
            if (uri != null) {
                sb.append("<div style=\"margin-top:4mm;text-align:right;\">")
                  .append("<img src=\"").append(uri)
                  .append("\" style=\"max-width:35mm;max-height:20mm;\"/>")
                  .append("</div>");
            }
        }
    }

    // ── Rendu HTML → PDF ──────────────────────────────────────────────────────

    private byte[] renderHtml(String html) {
        // Réduit les logs verbeux de la bibliothèque
        XRLog.setLoggingEnabled(false);
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            byte[] ls  = fontBytes("/fonts/LiberationSans-Regular.ttf", true);
            byte[] osw = fontBytes("/fonts/Oswald-Bold.ttf", false);
            PdfRendererBuilder builder = new PdfRendererBuilder();
            if (ls != null) {
                final byte[] lsf = ls;
                builder.useFont(() -> new ByteArrayInputStream(lsf), "LiberationSans", 400,
                        PdfRendererBuilder.FontStyle.NORMAL, true);
                builder.useFont(() -> new ByteArrayInputStream(lsf), "LiberationSans", 700,
                        PdfRendererBuilder.FontStyle.NORMAL, true);
            }
            if (osw != null) {
                final byte[] owf = osw;
                builder.useFont(() -> new ByteArrayInputStream(owf), "Oswald", 700,
                        PdfRendererBuilder.FontStyle.NORMAL, true);
            }
            builder.withHtmlContent(html, null);
            builder.toStream(baos);
            builder.run();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Rendu HTML→PDF : " + e.getMessage(), e);
        }
    }

    // ── Cache polices ─────────────────────────────────────────────────────────

    private byte[] fontBytes(String path, boolean isLs) {
        byte[] cached = isLs ? fontLsBytes : fontOswBytes;
        if (cached != null) return cached;
        synchronized (this) {
            cached = isLs ? fontLsBytes : fontOswBytes;
            if (cached != null) return cached;
            try (InputStream is = getClass().getResourceAsStream(path)) {
                if (is == null) { log.warn("Police absente du classpath : {}", path); return null; }
                byte[] b = is.readAllBytes();
                if (isLs) fontLsBytes = b; else fontOswBytes = b;
                return b;
            } catch (Exception e) {
                log.warn("Erreur lecture police {} : {}", path, e.getMessage());
                return null;
            }
        }
    }

    // ── Chargement image → base64 data URI ───────────────────────────────────

    private String loadImageDataUri(String url) {
        if (!hasValue(url)) return null;
        try {
            URLConnection conn = new URL(url).openConnection();
            conn.setConnectTimeout(6000);
            conn.setReadTimeout(10000);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            byte[] bytes = conn.getInputStream().readAllBytes();
            String mime = conn.getContentType() != null
                    ? conn.getContentType().split(";")[0].trim() : "image/jpeg";
            return "data:" + mime + ";base64," + Base64.getEncoder().encodeToString(bytes);
        } catch (Exception e) {
            log.warn("Chargement image échoué ({}) : {}", url, e.getMessage());
            return null;
        }
    }

    // ── Utilitaires HTML ──────────────────────────────────────────────────────

    private void th(StringBuilder sb, String text, String style) {
        sb.append("<td class=\"th\"");
        if (hasValue(style)) sb.append(" style=\"").append(style).append("\"");
        sb.append(">").append(h(text)).append("</td>");
    }

    private void td(StringBuilder sb, String html, String style) {
        sb.append("<td");
        if (hasValue(style)) sb.append(" style=\"").append(style).append("\"");
        sb.append(">").append(html).append("</td>");
    }

    /** Échappe les caractères HTML spéciaux dans le contenu utilisateur. */
    private static String h(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    // ── Utilitaires métier ────────────────────────────────────────────────────

    private static List<ReglementDocument> reglementsActifs(DocumentFacturation doc) {
        if (doc.getReglements() == null) return List.of();
        return doc.getReglements().stream()
                .filter(r -> r.getSupprimePar() == null)
                .toList();
    }

    private static String typeLabel(TypeDocument t) {
        if (t == null) return "DOCUMENT";
        return switch (t) {
            case FACTURE       -> "FACTURE";
            case DEVIS         -> "DEVIS";
            case AVOIR         -> "AVOIR";
            case BON_LIVRAISON -> "BON DE LIVRAISON";
        };
    }

    /**
     * Taille de police (en pt) pour le nom d'entreprise sur une seule ligne.
     * Adapte automatiquement à la longueur de la chaîne.
     */
    private static int nomFontPt(String nom) {
        if (nom == null) return 18;
        int len = nom.length();
        if (len <= 25) return 20;
        if (len <= 35) return 17;
        if (len <= 45) return 15;
        return 12;
    }

    private static String safe(String s, String def) {
        return (s != null && !s.isBlank()) ? s : def;
    }

    private static BigDecimal safe0(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    private static boolean hasValue(String s) {
        return s != null && !s.isBlank();
    }

    private static String concat(String a, String b) {
        String pa = a != null ? a.trim() : "";
        String pb = b != null ? b.trim() : "";
        if (pa.isEmpty()) return pb;
        if (pb.isEmpty()) return pa;
        return pa + " " + pb;
    }

    /**
     * Formate un montant entier FCFA avec espace comme séparateur des milliers.
     * Visible pour les tests unitaires (FormatMontantTest).
     * Exemple : 185000 → "185 000", 1250000 → "1 250 000", 0 → "0".
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
            sb.append(' '); // espace insécable U+00A0 — identique à l'original OpenPDF
            sb.append(s, i, i + 3);
        }
        return neg ? "-" + sb : sb.toString();
    }

    private static String formatQuantite(BigDecimal val) {
        if (val == null) return "-";
        if (val.scale() == 0 || val.stripTrailingZeros().scale() <= 0)
            return Long.toString(val.longValue());
        return val.stripTrailingZeros().toPlainString();
    }

    private static String formatPourcent(BigDecimal val) {
        if (val == null) return "-";
        return val.stripTrailingZeros().toPlainString() + "%";
    }
}
