package com.froidcheikh.ecommerce.service;

import com.froidcheikh.ecommerce.entity.ModeEnTeteDocuments;

/**
 * Configuration immuable de l'en-tête/pied de page pour la génération PDF.
 * Construite depuis ParametresSite avant l'émission ; transmise à PdfFacturationService.
 */
public record EnTeteDocumentConfig(
        String raisonSociale,
        String logoUrl,
        String texteDescriptif,
        String adresse,
        String telephone,
        String boitePostale,
        String email,
        String rccm,
        String ninea,
        String banqueNom,
        String numeroCompte,
        ModeEnTeteDocuments mode,
        String urlImageEnTete,
        String urlImagePied,
        String couleurHex,
        String devise
) {
    public EnTeteDocumentConfig {
        // valeurs par défaut si mode non renseigné
        if (mode == null) mode = ModeEnTeteDocuments.COMPOSE;
        if (devise == null || devise.isBlank()) devise = "FCFA";
        if (couleurHex == null || couleurHex.isBlank()) couleurHex = "#000080";
    }
}
