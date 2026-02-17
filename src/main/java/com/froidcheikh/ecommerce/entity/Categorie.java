package com.froidcheikh.ecommerce.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Entity
@Table(name = "categories")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Categorie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_categorie")
    private Long idCategorie;

    @NotBlank(message = "Le nom de la catégorie est obligatoire")
    @Size(min = 2, max = 100, message = "Le nom doit contenir entre 2 et 100 caractères")
    @Column(name = "nom_categorie", nullable = false, unique = true)
    private String nomCategorie;

    @Column(name = "description_categorie", columnDefinition = "TEXT")
    private String descriptionCategorie;

    @Column(name = "image_categorie",columnDefinition = "text")
    private String imageCategorie;

    @OneToMany(mappedBy = "categorie", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Produit> produits;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Categorie parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Categorie> sousCategories;
}