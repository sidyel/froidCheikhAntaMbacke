package com.froidcheikh.ecommerce.service;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class FormatMontantTest {

    private final PdfFacturationService service = new PdfFacturationService();

    @ParameterizedTest(name = "{0} -> \"{1}\"")
    @CsvSource({
        "0,             0",
        "100,           100",
        "1000,          1 000",
        "9999,          9 999",
        "10000,         10 000",
        "185000,        185 000",
        "250000,        250 000",
        "999999,        999 999",
        "1000000,       1 000 000",
        "1250000,       1 250 000",
        "10000000,      10 000 000",
        "999999999,     999 999 999",
    })
    void formatageMontantFCFA(long valeur, String attendu) {
        assertThat(service.formatMontant(BigDecimal.valueOf(valeur))).isEqualTo(attendu);
    }
}
