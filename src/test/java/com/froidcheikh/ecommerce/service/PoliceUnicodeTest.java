package com.froidcheikh.ecommerce.service;

import com.froidcheikh.ecommerce.entity.CompteurDocument.TypeDocument;
import com.froidcheikh.ecommerce.entity.DocumentFacturation;
import com.froidcheikh.ecommerce.entity.DocumentFacturation.StatutDocument;
import com.froidcheikh.ecommerce.entity.LigneDocument;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Vérifie que PdfFacturationService charge la police TTF et génère un PDF valide
 * avec des caractères Unicode Latin (accents, cédilles, caractères spéciaux).
 * Aucun Spring context, aucune base de données requise.
 */
class PoliceUnicodeTest {

    private final PdfFacturationService pdfService = new PdfFacturationService();

    @Test
    void generePdfAvecCaracteresUnicodeSansException() {
        DocumentFacturation doc = new DocumentFacturation();
        doc.setTypeDocument(TypeDocument.FACTURE);
        doc.setStatut(StatutDocument.EMIS);
        doc.setNumero("FAC-2026-UNICODE");
        doc.setDateCreation(LocalDateTime.of(2026, 10, 15, 9, 0));
        doc.setDateEmission(LocalDateTime.of(2026, 10, 15, 9, 5));

        // Entreprise avec accents
        doc.setEntRaisonSociale("Froid Chèikh Anta Mbàcké SARL");
        doc.setEntAdresseSiege("Côte des Almadies, Dakar — Sénégal");
        doc.setEntNinea("000123456 2A1");
        doc.setEntRccm("SN DKR 2020 B 12345");
        doc.setEntCouleurPrimaire("#000080");
        doc.setEntDevise("FCFA");
        doc.setEntMentionsPied("é è à ç ô ë ñ — caractères accentués");

        // Client avec accents
        doc.setClientNom("Diédhiou");
        doc.setClientPrenom("Abdoulâye");
        doc.setClientAdresse("Rue de l'Indépendance, Ziguinchor");

        doc.setTauxTvaDefaut(BigDecimal.ZERO);
        doc.setPrixAfficheTTC(false);
        doc.setRemiseTotalePourcent(BigDecimal.ZERO);

        // Ligne avec désignation accentuée
        LigneDocument ligne = new LigneDocument();
        ligne.setDocument(doc);
        ligne.setOrdre(1);
        ligne.setDesignation("Réfrigérateur congélateur — modèle Écologique");
        ligne.setQuantite(BigDecimal.ONE);
        ligne.setPrixUnitaireHT(new BigDecimal("125000"));
        ligne.setTauxTva(BigDecimal.ZERO);
        ligne.setRemisePourcent(BigDecimal.ZERO);
        ligne.setMontantHT(new BigDecimal("125000"));
        ligne.setMontantTva(BigDecimal.ZERO);
        ligne.setMontantTTC(new BigDecimal("125000"));
        doc.getLignes().add(ligne);

        doc.setMontantHT(new BigDecimal("125000"));
        doc.setMontantTva(BigDecimal.ZERO);
        doc.setMontantTTC(new BigDecimal("125000"));
        doc.setMontantEnLettres("Cent vingt-cinq mille francs CFA");

        // Génération — ne doit pas lever d'exception
        byte[] pdf = pdfService.generer(doc, false);

        // Vérifie que c'est bien un PDF valide
        assertThat(pdf).isNotNull();
        assertThat(pdf.length).isGreaterThan(100);
        assertThat(new String(pdf, 0, 4)).isEqualTo("%PDF");
    }
}
