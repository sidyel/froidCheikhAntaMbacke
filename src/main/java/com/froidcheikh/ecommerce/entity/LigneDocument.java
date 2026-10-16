package com.froidcheikh.ecommerce.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "lignes_document")
@Data
@NoArgsConstructor
public class LigneDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_id", nullable = false)
    private DocumentFacturation document;

    @Column(name = "ordre", nullable = false)
    private Integer ordre = 0;

    @Column(name = "designation", nullable = false, length = 500)
    private String designation;

    /** Référence au produit catalogue — null si ligne libre. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "produit_id")
    private Produit produit;

    /** Accepte les demi-unités (ex. 0.5 m²). */
    @Column(name = "quantite", nullable = false, precision = 10, scale = 3)
    private BigDecimal quantite;

    @Column(name = "prix_unitaire_ht", nullable = false, precision = 12, scale = 2)
    private BigDecimal prixUnitaireHT;

    /** Taux TVA pour cette ligne en %. Hérite de DocumentFacturation.tauxTvaDefaut si non surchargé. */
    @Column(name = "taux_tva", nullable = false, precision = 5, scale = 2)
    private BigDecimal tauxTva = BigDecimal.ZERO;

    @Column(name = "remise_pourcent", nullable = false, precision = 5, scale = 2)
    private BigDecimal remisePourcent = BigDecimal.ZERO;

    @Column(name = "montant_ht", nullable = false, precision = 12, scale = 2)
    private BigDecimal montantHT;

    @Column(name = "montant_tva", nullable = false, precision = 12, scale = 2)
    private BigDecimal montantTva;

    @Column(name = "montant_ttc", nullable = false, precision = 12, scale = 2)
    private BigDecimal montantTTC;
}
