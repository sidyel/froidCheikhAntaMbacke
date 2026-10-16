package com.froidcheikh.ecommerce.service;

import com.froidcheikh.ecommerce.entity.LigneDocument;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CalculDocumentServiceTest {

    private final CalculDocumentService service = new CalculDocumentService();

    private LigneDocument ligne(BigDecimal prix, BigDecimal qte, BigDecimal taux, BigDecimal remise) {
        LigneDocument l = new LigneDocument();
        l.setDesignation("test");
        l.setPrixUnitaireHT(prix);
        l.setQuantite(qte);
        l.setTauxTva(taux);
        l.setRemisePourcent(remise != null ? remise : BigDecimal.ZERO);
        l.setMontantHT(BigDecimal.ZERO);
        l.setMontantTva(BigDecimal.ZERO);
        l.setMontantTTC(BigDecimal.ZERO);
        return l;
    }

    @Test
    void tauxZeroPrixHTDirectement() {
        LigneDocument l = ligne(bd("10000"), bd("2"), BigDecimal.ZERO, null);
        service.calculerLigne(l, false);
        assertThat(l.getMontantHT()).isEqualByComparingTo("20000");
        assertThat(l.getMontantTva()).isEqualByComparingTo("0");
        assertThat(l.getMontantTTC()).isEqualByComparingTo("20000");
    }

    @Test
    void tva18Pourcent() {
        LigneDocument l = ligne(bd("100000"), bd("1"), bd("18"), null);
        service.calculerLigne(l, false);
        assertThat(l.getMontantHT()).isEqualByComparingTo("100000");
        assertThat(l.getMontantTva()).isEqualByComparingTo("18000");
        assertThat(l.getMontantTTC()).isEqualByComparingTo("118000");
    }

    @Test
    void remiseLigne10Pourcent() {
        LigneDocument l = ligne(bd("100000"), bd("1"), BigDecimal.ZERO, bd("10"));
        service.calculerLigne(l, false);
        assertThat(l.getMontantHT()).isEqualByComparingTo("90000");
    }

    @Test
    void prixTTCConversionEnHT() {
        // TTC = 118 000, taux 18% → HT = 100 000
        LigneDocument l = ligne(bd("118000"), bd("1"), bd("18"), null);
        service.calculerLigne(l, true); // prixAfficheTTC = true
        assertThat(l.getMontantHT()).isEqualByComparingTo("100000");
        assertThat(l.getMontantTva()).isEqualByComparingTo("18000");
        assertThat(l.getMontantTTC()).isEqualByComparingTo("118000");
    }

    @Test
    void arrondiFCFA() {
        // 3 * 33333 FCFA HT = 99999 — pas de centimes
        LigneDocument l = ligne(bd("33333"), bd("3"), BigDecimal.ZERO, null);
        service.calculerLigne(l, false);
        assertThat(l.getMontantHT().scale()).isEqualTo(0);
        assertThat(l.getMontantHT()).isEqualByComparingTo("99999");
    }

    @Test
    void totauxAvecRemiseTotale() {
        LigneDocument l1 = ligne(bd("100000"), bd("1"), BigDecimal.ZERO, null);
        LigneDocument l2 = ligne(bd("50000"), bd("2"), BigDecimal.ZERO, null);
        BigDecimal[] t = service.calculerTotaux(List.of(l1, l2), bd("10"), false);
        // HT brut = 200 000, remise 10% → 180 000
        assertThat(t[0]).isEqualByComparingTo("180000");
        assertThat(t[2]).isEqualByComparingTo("180000"); // taux 0%
    }

    private BigDecimal bd(String v) {
        return new BigDecimal(v);
    }
}
