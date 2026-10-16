package com.froidcheikh.ecommerce.service;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class MontantEnLettresServiceTest {

    private final MontantEnLettresService service = new MontantEnLettresService();

    private String conv(long v) {
        return service.convertir(BigDecimal.valueOf(v));
    }

    @ParameterizedTest
    @CsvSource({
        "0,     zéro franc CFA",
        "1,     Arrêtée la présente facture à la somme de un francs CFA",
        "11,    Arrêtée la présente facture à la somme de onze francs CFA",
        "21,    Arrêtée la présente facture à la somme de vingt-et-un francs CFA",
        "80,    Arrêtée la présente facture à la somme de quatre-vingts francs CFA",
        "100,   Arrêtée la présente facture à la somme de cent francs CFA",
        "200,   Arrêtée la présente facture à la somme de deux cents francs CFA",
        "999,   Arrêtée la présente facture à la somme de neuf cent quatre-vingt-dix-neuf francs CFA",
        "1000,  Arrêtée la présente facture à la somme de mille francs CFA",
        "1500,  Arrêtée la présente facture à la somme de mille cinq cents francs CFA",
        "250000,Arrêtée la présente facture à la somme de deux cent cinquante mille francs CFA",
        "1000000,Arrêtée la présente facture à la somme de un million francs CFA",
        "999999999,Arrêtée la présente facture à la somme de neuf cent quatre-vingt-dix-neuf millions neuf cent quatre-vingt-dix-neuf mille neuf cent quatre-vingt-dix-neuf francs CFA"
    })
    void casNominaux(long valeur, String attendu) {
        assertThat(conv(valeur)).isEqualTo(attendu);
    }
}
