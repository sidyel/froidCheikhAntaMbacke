package com.froidcheikh.ecommerce.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Entity
@Table(name = "marques")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Marque {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_marque")
    private Long idMarque;

    @NotBlank(message = "Le nom de la marque est obligatoire")
    @Size(min = 2, max = 100, message = "Le nom doit contenir entre 2 et 100 caractères")
    @Column(name = "nom_marque", nullable = false, unique = true)
    private String nomMarque;

    @Column(name = "logo",columnDefinition = "text")
    private String logo;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @OneToMany(mappedBy = "marque", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Produit> produits;
}