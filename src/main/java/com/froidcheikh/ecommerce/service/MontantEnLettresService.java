package com.froidcheikh.ecommerce.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Convertit un montant entier (FCFA) en toutes lettres, en français.
 * Ex : 250 000 → "deux cent cinquante mille francs CFA"
 */
@Service
public class MontantEnLettresService {

    private static final String[] UNITES = {
        "", "un", "deux", "trois", "quatre", "cinq", "six", "sept", "huit", "neuf",
        "dix", "onze", "douze", "treize", "quatorze", "quinze", "seize",
        "dix-sept", "dix-huit", "dix-neuf"
    };

    private static final String[] DIZAINES = {
        "", "", "vingt", "trente", "quarante", "cinquante",
        "soixante", "soixante", "quatre-vingt", "quatre-vingt"
    };

    public String convertir(BigDecimal montant) {
        long valeur = montant.setScale(0, java.math.RoundingMode.HALF_UP).longValueExact();
        if (valeur == 0) return "zéro franc CFA";
        if (valeur < 0) return "moins " + convertir(montant.negate());

        String lettres = convertirEntier(valeur);
        return "Arrêtée la présente facture à la somme de " + lettres + " francs CFA";
    }

    private String convertirEntier(long n) {
        if (n == 0) return "";
        if (n < 20) return UNITES[(int) n];
        if (n < 100) return dizaine(n);
        if (n < 1_000) return centaine(n);
        if (n < 1_000_000) return millier(n);
        if (n < 1_000_000_000) return million(n);
        return milliard(n);
    }

    private String dizaine(long n) {
        int d = (int) (n / 10);
        int u = (int) (n % 10);
        String base = DIZAINES[d];
        if (d == 7 || d == 9) {
            // 70-79 : soixante-dix... / 90-99 : quatre-vingt-dix...
            int sub = (d == 7) ? 10 + u : 10 + u;
            return DIZAINES[d] + "-" + UNITES[sub];
        }
        if (d == 8 && u == 0) return "quatre-vingts";
        if (u == 0) return base;
        if (u == 1 && d != 8) return base + "-et-un";
        return base + "-" + UNITES[u];
    }

    private String centaine(long n) {
        long c = n / 100;
        long reste = n % 100;
        String prefix = (c == 1) ? "cent" : convertirEntier(c) + " cent";
        if (reste == 0 && c > 1) return prefix + "s";
        return reste == 0 ? prefix : prefix + " " + convertirEntier(reste);
    }

    private String millier(long n) {
        long m = n / 1_000;
        long reste = n % 1_000;
        String prefix = (m == 1) ? "mille" : convertirEntier(m) + " mille";
        return reste == 0 ? prefix : prefix + " " + convertirEntier(reste);
    }

    private String million(long n) {
        long m = n / 1_000_000;
        long reste = n % 1_000_000;
        String prefix = convertirEntier(m) + (m == 1 ? " million" : " millions");
        return reste == 0 ? prefix : prefix + " " + convertirEntier(reste);
    }

    private String milliard(long n) {
        long m = n / 1_000_000_000;
        long reste = n % 1_000_000_000;
        String prefix = convertirEntier(m) + (m == 1 ? " milliard" : " milliards");
        return reste == 0 ? prefix : prefix + " " + convertirEntier(reste);
    }
}
