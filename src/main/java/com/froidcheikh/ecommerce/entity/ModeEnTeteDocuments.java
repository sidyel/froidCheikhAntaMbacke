package com.froidcheikh.ecommerce.entity;

/** Mode de rendu de l'en-tête sur les documents PDF de facturation. */
public enum ModeEnTeteDocuments {
    /** En-tête composé automatiquement : logo + nom entreprise + texte descriptif. */
    COMPOSE,
    /** En-tête image : une image pleine largeur fournie par l'administrateur. */
    IMAGE
}
