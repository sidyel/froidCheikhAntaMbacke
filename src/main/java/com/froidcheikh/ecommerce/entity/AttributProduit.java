package com.froidcheikh.ecommerce.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "attributs_produit")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttributProduit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_attribut")
    private Long idAttribut;

    @NotBlank(message = "Le nom de l'attribut est obligatoire")
    @Column(name = "nom_attribut", nullable = false)
    private String nomAttribut;

    @NotBlank(message = "La valeur de l'attribut est obligatoire")
    @Column(name = "valeur_attribut", nullable = false)
    private String valeurAttribut;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "produit_id")
    private Produit produit;
}