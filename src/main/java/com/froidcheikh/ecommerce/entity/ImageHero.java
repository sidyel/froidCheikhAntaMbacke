package com.froidcheikh.ecommerce.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "images_hero")
@Data
@NoArgsConstructor
public class ImageHero {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "url_image")
    private String urlImage;

    @Column(name = "public_id_cloudinary")
    private String publicIdCloudinary;

    @Column(name = "alt_text")
    private String altText;

    @Column(name = "titre")
    private String titre;

    @Column(name = "sous_titre")
    private String sousTitre;

    @Column(name = "lien_cta")
    private String lienCta;

    @Column(name = "texte_cta")
    private String texteCta;

    @Column(name = "ordre")
    private Integer ordre = 0;

    @Column(name = "actif")
    private Boolean actif = true;

    @CreationTimestamp
    @Column(name = "date_creation", updatable = false)
    private LocalDateTime dateCreation;

    @UpdateTimestamp
    @Column(name = "date_modification")
    private LocalDateTime dateModification;
}
