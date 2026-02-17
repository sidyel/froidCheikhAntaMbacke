package com.froidcheikh.ecommerce.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "lignes_commande")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LigneCommande {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_ligne_commande")
    private Long idLigneCommande;

    @NotNull
    @Min(value = 1, message = "La quantité doit être au moins 1")
    @Column(name = "quantite", nullable = false)
    private Integer quantite;

    @NotNull
    @Column(name = "prix_unitaire", nullable = false, precision = 10, scale = 2)
    private BigDecimal prixUnitaire;

    @NotNull
    @Column(name = "sous_total", nullable = false, precision = 10, scale = 2)
    private BigDecimal sousTotal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "commande_id")
    private Commande commande;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "produit_id")
    private Produit produit;

    // Données produit au moment de la commande (pour historique)
    @Column(name = "nom_produit_commande")
    private String nomProduitCommande;

    @Column(name = "ref_produit_commande")
    private String refProduitCommande;
}