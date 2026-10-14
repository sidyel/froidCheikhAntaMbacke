package com.froidcheikh.ecommerce.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "prestations")
@Data
@NoArgsConstructor
public class Prestation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "titre", nullable = false)
    private String titre;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "url_image")
    private String urlImage;

    @Column(name = "public_id_cloudinary")
    private String publicIdCloudinary;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "prestation_caracteristiques", joinColumns = @JoinColumn(name = "prestation_id"))
    @Column(name = "caracteristique")
    private List<String> caracteristiques = new ArrayList<>();

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
