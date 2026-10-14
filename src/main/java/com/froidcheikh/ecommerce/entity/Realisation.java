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
@Table(name = "realisations")
@Data
@NoArgsConstructor
public class Realisation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "titre", nullable = false)
    private String titre;

    @Column(name = "categorie")
    private String categorie;

    @Column(name = "localisation")
    private String localisation;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "client_nom")
    private String clientNom;

    @Column(name = "annee")
    private String annee;

    @Column(name = "duree")
    private String duree;

    @Column(name = "surface")
    private String surface;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "realisation_images", joinColumns = @JoinColumn(name = "realisation_id"))
    @Column(name = "url_image")
    private List<String> images = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "realisation_public_ids", joinColumns = @JoinColumn(name = "realisation_id"))
    @Column(name = "public_id")
    private List<String> publicIds = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "realisation_tags", joinColumns = @JoinColumn(name = "realisation_id"))
    @Column(name = "tag")
    private List<String> tags = new ArrayList<>();

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
