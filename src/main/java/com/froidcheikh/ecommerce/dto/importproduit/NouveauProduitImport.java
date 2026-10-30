package com.froidcheikh.ecommerce.dto.importproduit;

import com.froidcheikh.ecommerce.dto.enrichissement.EnrichissementAttribut;
import com.froidcheikh.ecommerce.dto.enrichissement.EnrichissementColonnesTechniques;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO pour la création de nouveaux produits via le profil Spring {@code import-produits}.
 *
 * <p>Clé d'idempotence : {@code marque + modeleFabricant}. Si un produit avec cette
 * combinaison existe déjà en base, le runner complète les champs vides sans créer de doublon.
 */
@Data
public class NouveauProduitImport {

    /** Référence fabricant — clé d'idempotence avec {@code marque}. Obligatoire. */
    private String modeleFabricant;

    /** Nom de la marque (ex. "Midea"). Obligatoire. */
    private String marque;

    /** Nom de la catégorie (ex. "Climatiseurs"). Obligatoire. */
    private String categorie;

    /** Nom complet du produit tel qu'il apparaît sur le site. Obligatoire. */
    private String nomProduit;

    /** Description longue en français. Min. 150 caractères. */
    private String description;

    /** Prix de vente en XOF. Obligatoire. */
    private BigDecimal prix;

    /**
     * Si {@code true}, le prix est estimé et non confirmé.
     * Ce produit apparaîtra dans la section "Prix à vérifier" du rapport.
     */
    private boolean prixIndicatif = false;

    /** Durée de garantie (ex. "1 an", "2 ans"). */
    private String garantie;

    /**
     * Colonnes techniques structurées (puissanceBTU, labelEnergie, consommationWatt,
     * dimensions, poids, garantie). Valeurs {@code "valide"} uniquement traitées.
     */
    private EnrichissementColonnesTechniques colonnesTechniques;

    /**
     * Attributs dynamiques (Type, Inverseur, Gaz réfrigérant, etc.).
     * Utiliser les noms exacts des ModèleAttribut de la catégorie.
     * Valeurs {@code "valide"} uniquement traitées.
     */
    private List<EnrichissementAttribut> attributs;

    /**
     * URLs des images à télécharger et envoyer sur Cloudinary.
     * UNIQUEMENT depuis le site officiel Midea. Maximum 3.
     * La première image sera définie comme image principale.
     */
    private List<String> urlsImages;

    /**
     * Si {@code true}, le runner écrase les champs déjà remplis en base au lieu de les ignorer.
     * Utile pour corriger un import précédemment confirmé.
     * N'a aucun effet pour les nouveaux produits (créés de toute façon).
     */
    private boolean ecraser = false;

    /**
     * Rendre le produit disponible dès l'import (défaut : false — nécessite activation manuelle après photos).
     * Mettre à {@code true} uniquement si le produit est prêt à la vente.
     */
    private boolean disponibilite = false;

    /** Stock initial. Défaut : 0. */
    private int stockDisponible = 0;

    /** Remarques internes (non affichées sur le site, conservées dans les logs). */
    private String remarques;
}
