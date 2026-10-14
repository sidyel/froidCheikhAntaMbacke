package com.froidcheikh.ecommerce.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "image_produit")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImageProduit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_image")
    private Long idImage;

    @Column(name = "url", nullable = false, columnDefinition = "TEXT")
    private String url;

    @Column(name = "public_id")
    private String publicId;

    @Column(name = "texte_alternatif", length = 255)
    private String texteAlternatif;

    @Column(name = "ordre", nullable = false)
    private int ordre = 0;

    @Column(name = "principale", nullable = false)
    private boolean principale = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "produit_id", nullable = false)
    private Produit produit;
}
