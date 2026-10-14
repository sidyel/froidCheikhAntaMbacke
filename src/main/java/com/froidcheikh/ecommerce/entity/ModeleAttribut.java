package com.froidcheikh.ecommerce.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "modele_attribut")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ModeleAttribut {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_modele")
    private Long idModele;

    @NotBlank(message = "Le nom de l'attribut modèle est obligatoire")
    @Column(name = "nom_attribut", nullable = false, length = 100)
    private String nomAttribut;

    @Column(name = "description", length = 255)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_valeur", nullable = false)
    private TypeAttribut typeValeur = TypeAttribut.TEXT;

    @Column(name = "valeurs_autorisees", columnDefinition = "TEXT")
    private String valeursAutorisees;

    @Column(name = "unite", length = 30)
    private String unite;

    @Column(name = "obligatoire", nullable = false)
    private boolean obligatoire = false;

    @Column(name = "ordre", nullable = false)
    private int ordre = 0;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "modele_attribut_categories",
        joinColumns = @JoinColumn(name = "modele_attribut_id"),
        inverseJoinColumns = @JoinColumn(name = "categorie_id")
    )
    private Set<Categorie> categories = new HashSet<>();
}
