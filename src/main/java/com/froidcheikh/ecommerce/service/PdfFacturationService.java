package com.froidcheikh.ecommerce.service;

import com.froidcheikh.ecommerce.entity.CompteurDocument.TypeDocument;
import com.froidcheikh.ecommerce.entity.DocumentFacturation;
import com.froidcheikh.ecommerce.entity.LigneDocument;
import com.froidcheikh.ecommerce.entity.ModeEnTeteDocuments;
import com.froidcheikh.ecommerce.entity.ReglementDocument;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.openhtmltopdf.util.XRLog;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.net.URLConnection;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Génère les PDFs de facturation avec openhtmltopdf (moteur PDFBox).
 * Templates HTML/CSS dans src/main/resources/templates/pdf/ (Thymeleaf XML mode).
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

    // Moteur Thymeleaf (lazy init thread-safe)
    private volatile TemplateEngine templateEngine;

    // ── Modèles de vue (données pré-formatées pour les templates) ─────────────

    @Value
    public static class LigneVM {
        String designation;
        String qte;
        String pu;
        String tva;
        String remise;
        String total;
    }

    @Value
    public static class ReglementVM {
        String date;
        String mode;
        String reference;
        String montant;
    }

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

    // ── Construction HTML via Thymeleaf ───────────────────────────────────────

    private String buildHtml(DocumentFacturation doc, EnTeteDocumentConfig cfg, boolean brouillon) {
        boolean isBL = doc.getTypeDocument() == TypeDocument.BON_LIVRAISON;
        String templateName = isBL ? "bon-livraison" : "facturation";
        Context ctx = buildContext(doc, cfg, brouillon);
        String html = getEngine().process(templateName, ctx);
        return "<!DOCTYPE html>" + html;
    }

    private Context buildContext(DocumentFacturation doc, EnTeteDocumentConfig cfg, boolean brouillon) {
        Context ctx = new Context();

        String couleur = safe(cfg.couleurHex(), "#000080");
        String devise  = safe(doc.getEntDevise(), safe(cfg.devise(), "FCFA"));
        boolean isAvoir = doc.getTypeDocument() == TypeDocument.AVOIR;
        boolean isDevis = doc.getTypeDocument() == TypeDocument.DEVIS;
        boolean hasTva  = doc.getTauxTvaDefaut() != null
                && doc.getTauxTvaDefaut().compareTo(BigDecimal.ZERO) > 0;
        boolean isImageMode = cfg.mode() == ModeEnTeteDocuments.IMAGE && hasValue(cfg.urlImageEnTete());
        boolean isImagePied = cfg.mode() == ModeEnTeteDocuments.IMAGE && hasValue(cfg.urlImagePied());

        // ── CSS / apparence ──────────────────────────────────────────────────
        ctx.setVariable("couleur",        couleur);
        ctx.setVariable("brouillon",      brouillon);
        ctx.setVariable("titreFontStyle",
                "font-family:'Oswald';font-weight:bold;font-size:13pt;color:" + couleur + ";white-space:nowrap;");
        ctx.setVariable("nomFontStyle",
                "font-family:'Oswald';font-weight:bold;font-size:" + nomFontPt(cfg.raisonSociale())
                        + "pt;color:" + couleur + ";white-space:nowrap;overflow:hidden;");

        // ── Mode en-tête / pied ──────────────────────────────────────────────
        ctx.setVariable("isImageMode",    isImageMode);
        ctx.setVariable("isImagePied",    isImagePied);
        ctx.setVariable("isAvoir",        isAvoir);
        ctx.setVariable("isDevis",        isDevis);

        // ── Images (base64 data URI) ─────────────────────────────────────────
        ctx.setVariable("logoUri",        loadImageDataUri(cfg.logoUrl()));
        ctx.setVariable("imageEnTeteUri", isImageMode ? loadImageDataUri(cfg.urlImageEnTete()) : null);
        ctx.setVariable("imagePiedUri",   isImagePied ? loadImageDataUri(cfg.urlImagePied()) : null);

        // ── En-tête entreprise ───────────────────────────────────────────────
        ctx.setVariable("raisonSociale",   safe(cfg.raisonSociale(), ""));
        ctx.setVariable("texteDescriptif", hasValue(cfg.texteDescriptif()) ? cfg.texteDescriptif() : null);

        // ── Identité document ────────────────────────────────────────────────
        ctx.setVariable("typeLabel", typeLabel(doc.getTypeDocument()));
        ctx.setVariable("numero",    hasValue(doc.getNumero()) ? doc.getNumero() : "(brouillon)");

        // ── Dates ────────────────────────────────────────────────────────────
        String dateStr = null, dateLabelStr = null;
        if (doc.getDateEmission() != null) {
            dateStr     = doc.getDateEmission().format(DATE_FMT);
            dateLabelStr = "Émis le :";
        } else if (doc.getDateCreation() != null) {
            dateStr     = doc.getDateCreation().format(DATE_FMT);
            dateLabelStr = "Créé le :";
        }
        ctx.setVariable("dateStr",       dateStr);
        ctx.setVariable("dateLabelStr",  dateLabelStr);
        ctx.setVariable("dateValiditeStr",
                doc.getDateValidite() != null ? doc.getDateValidite().format(DATE_FMT) : null);

        // ── Référence AVOIR ──────────────────────────────────────────────────
        String originRef = null;
        if (isAvoir && doc.getDocumentOrigine() != null
                && hasValue(doc.getDocumentOrigine().getNumero())) {
            originRef = doc.getDocumentOrigine().getNumero();
        }
        ctx.setVariable("originRef", originRef);

        // ── Bloc client ──────────────────────────────────────────────────────
        String clientNom = concat(doc.getClientPrenom(), doc.getClientNom());
        ctx.setVariable("clientNom",       hasValue(clientNom) ? clientNom : null);
        ctx.setVariable("clientAdresse",   hasValue(doc.getClientAdresse())   ? doc.getClientAdresse()   : null);
        ctx.setVariable("clientTelephone", hasValue(doc.getClientTelephone()) ? doc.getClientTelephone() : null);
        ctx.setVariable("clientEmail",     hasValue(doc.getClientEmail())     ? doc.getClientEmail()     : null);
        ctx.setVariable("clientNinea",     hasValue(doc.getClientNinea())     ? doc.getClientNinea()     : null);
        ctx.setVariable("clientRccm",      hasValue(doc.getClientRccm())      ? doc.getClientRccm()      : null);

        // ── Lignes ───────────────────────────────────────────────────────────
        ctx.setVariable("lignes",  buildLigneVMs(doc.getLignes(), hasTva));
        ctx.setVariable("hasTva",  hasTva);
        ctx.setVariable("devise",  devise);

        // ── Récapitulatif financier ──────────────────────────────────────────
        BigDecimal ht  = safe0(doc.getMontantHT());
        BigDecimal tva = safe0(doc.getMontantTva());
        BigDecimal ttc = safe0(doc.getMontantTTC());
        List<ReglementDocument> regsList  = reglementsActifs(doc);
        BigDecimal dejaRegle = regsList.stream().map(ReglementDocument::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        boolean hasReglements = dejaRegle.compareTo(BigDecimal.ZERO) > 0;
        BigDecimal reste = ttc.subtract(dejaRegle).max(BigDecimal.ZERO);

        ctx.setVariable("montantHT",       formatMontant(ht));
        ctx.setVariable("montantTva",      tva.compareTo(BigDecimal.ZERO) > 0 ? formatMontant(tva) : null);
        ctx.setVariable("tauxTva",         doc.getTauxTvaDefaut() != null ? formatPourcent(doc.getTauxTvaDefaut()) : null);
        ctx.setVariable("montantTTC",      formatMontant(ttc));
        ctx.setVariable("dejaRegle",       formatMontant(dejaRegle));
        ctx.setVariable("resteAPayer",     formatMontant(reste));
        ctx.setVariable("hasReglements",   hasReglements);
        ctx.setVariable("restePositif",    reste.compareTo(BigDecimal.ZERO) > 0);
        ctx.setVariable("montantEnLettres",
                hasValue(doc.getMontantEnLettres()) ? doc.getMontantEnLettres() : null);

        // Statut + couleur
        String statut, statutColor;
        if (!hasReglements)                                   { statut = "Non payée";              statutColor = "#dc2626"; }
        else if (reste.compareTo(BigDecimal.ZERO) == 0)       { statut = "Payée";                  statutColor = "#16a34a"; }
        else                                                   { statut = "Partiellement payée";   statutColor = "#d97706"; }
        ctx.setVariable("statut",       statut);
        ctx.setVariable("statutStyle",
                "font-family:'LiberationSans';font-weight:bold;font-size:8pt;color:" + statutColor + ";");

        // ── Règlements ───────────────────────────────────────────────────────
        ctx.setVariable("reglements", buildReglementVMs(regsList, devise));

        // ── Mentions ─────────────────────────────────────────────────────────
        ctx.setVariable("conditionsPaiement",
                hasValue(doc.getEntConditionsPaiement()) ? doc.getEntConditionsPaiement() : null);
        ctx.setVariable("mentionExoTva",
                hasValue(doc.getEntMentionExonerationTva()) ? doc.getEntMentionExonerationTva() : null);
        ctx.setVariable("mentionsPied",
                hasValue(doc.getEntMentionsPied()) ? doc.getEntMentionsPied() : null);
        ctx.setVariable("cachetUri", loadImageDataUri(doc.getEntCachetUrl()));

        // ── Pied de page (texte composé) ─────────────────────────────────────
        ctx.setVariable("piedAdresse",     hasValue(cfg.adresse())       ? cfg.adresse()       : null);
        ctx.setVariable("piedBoitePostale", hasValue(cfg.boitePostale()) ? cfg.boitePostale()  : null);
        ctx.setVariable("piedTelephone",   hasValue(cfg.telephone())     ? cfg.telephone()     : null);
        ctx.setVariable("piedRccm",        hasValue(cfg.rccm())          ? cfg.rccm()          : null);
        ctx.setVariable("piedNinea",       hasValue(cfg.ninea())         ? cfg.ninea()         : null);
        ctx.setVariable("piedBanque",      hasValue(cfg.banqueNom())     ? cfg.banqueNom()     : null);
        ctx.setVariable("piedCompte",      hasValue(cfg.numeroCompte())  ? cfg.numeroCompte()  : null);
        ctx.setVariable("piedEmail",       hasValue(cfg.email())         ? cfg.email()         : null);

        // ── Bon de livraison ─────────────────────────────────────────────────
        ctx.setVariable("livreurNom",       hasValue(doc.getLivreurNom())       ? doc.getLivreurNom()       : null);
        ctx.setVariable("livreurTelephone", hasValue(doc.getLivreurTelephone()) ? doc.getLivreurTelephone() : null);

        return ctx;
    }

    private List<LigneVM> buildLigneVMs(List<LigneDocument> lignes, boolean hasTva) {
        if (lignes == null) return List.of();
        List<LigneVM> result = new ArrayList<>(lignes.size());
        for (LigneDocument l : lignes) {
            BigDecimal total = hasTva ? l.getMontantTTC() : l.getMontantHT();
            result.add(new LigneVM(
                    safe(l.getDesignation(), ""),
                    formatQuantite(l.getQuantite()),
                    formatMontant(l.getPrixUnitaireHT()),
                    formatPourcent(l.getTauxTva()),
                    formatPourcent(l.getRemisePourcent()),
                    formatMontant(total)
            ));
        }
        return result;
    }

    private List<ReglementVM> buildReglementVMs(List<ReglementDocument> regs, String devise) {
        if (regs == null) return List.of();
        List<ReglementVM> result = new ArrayList<>(regs.size());
        for (ReglementDocument r : regs) {
            result.add(new ReglementVM(
                    r.getDateReglement() != null ? r.getDateReglement().format(DATE_FMT) : "-",
                    r.getMode() != null ? r.getMode().toString() : "-",
                    hasValue(r.getReference()) ? r.getReference() : "-",
                    formatMontant(r.getMontant()) + " " + devise
            ));
        }
        return result;
    }

    // ── Moteur Thymeleaf (lazy init thread-safe) ──────────────────────────────

    private TemplateEngine getEngine() {
        if (templateEngine != null) return templateEngine;
        synchronized (this) {
            if (templateEngine != null) return templateEngine;
            ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
            resolver.setPrefix("templates/pdf/");
            resolver.setSuffix(".html");
            resolver.setTemplateMode(TemplateMode.XML);
            resolver.setCharacterEncoding("UTF-8");
            resolver.setCacheable(true);
            TemplateEngine engine = new TemplateEngine();
            engine.addTemplateResolver(resolver);
            templateEngine = engine;
            return templateEngine;
        }
    }

    // ── Rendu HTML → PDF ──────────────────────────────────────────────────────

    private byte[] renderHtml(String html) {
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

    static boolean hasValue(String s) {
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
     * Formate un montant entier FCFA avec espace insécable comme séparateur des milliers.
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
            sb.append(' '); // espace insécable U+00A0
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
