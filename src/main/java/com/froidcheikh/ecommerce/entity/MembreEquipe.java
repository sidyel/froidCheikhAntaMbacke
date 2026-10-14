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
@Table(name = "membres_equipe")
@Data
@NoArgsConstructor
public class MembreEquipe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nom", nullable = false)
    private String nom;

    @Column(name = "poste")
    private String poste;

    @Column(name = "experience")
    private String experience;

    @Column(name = "bio", columnDefinition = "TEXT")
    private String bio;

    @Column(name = "url_photo")
    private String urlPhoto;

    @Column(name = "public_id_cloudinary")
    private String publicIdCloudinary;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "membre_competences", joinColumns = @JoinColumn(name = "membre_id"))
    @Column(name = "competence")
    private List<String> competences = new ArrayList<>();

    @Column(name = "url_linkedin")
    private String urlLinkedin;

    @Column(name = "email_membre")
    private String emailMembre;

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
