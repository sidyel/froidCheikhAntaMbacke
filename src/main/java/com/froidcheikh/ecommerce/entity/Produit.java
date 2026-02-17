package com.froidcheikh.ecommerce.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "produits")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Produit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_produit")
    private Long idProduit;

    @NotBlank(message = "Le nom du produit est obligatoire")
    @Size(min = 2, max = 255, message = "Le nom doit contenir entre 2 et 255 caractères")
    @Column(name = "nom_produit", nullable = false)
    private String nomProduit;

    @Column(name = "description_produit", columnDefinition = "TEXT")
    private String descriptionProduit;

    @NotNull(message = "Le prix est obligatoire")
    @DecimalMin(value = "0.0", inclusive = false, message = "Le prix doit être positif")
    @Column(name = "prix", nullable = false, precision = 10, scale = 2)
    private BigDecimal prix;

    @Min(value = 0, message = "Le stock doit être positif ou nul")
    @Column(name = "stock_disponible")
    private Integer stockDisponible = 0;

    @Column(name = "ref_produit", unique = true)
    private String refProduit;

    @Column(name = "code_produit", unique = true)
    private String codeProduit;

    @Column(name = "garantie")
    private String garantie;

    @Column(name = "label_energie")
    @Enumerated(EnumType.STRING)
    private LabelEnergie labelEnergie;

    @Column(name = "puissance_btu")
    private Integer puissanceBTU;

    @Column(name = "consommation_watt")
    private Integer consommationWatt;

    @Column(name = "dimensions")
    private String dimensions;

    @Column(name = "poids")
    private Double poids;

    @Column(name = "fiche_technique_pdf")
    private String ficheTechniquePDF;

    @ElementCollection
    @CollectionTable(name = "produit_images", joinColumns = @JoinColumn(name = "produit_id"))
    @Column(name = "image_url", columnDefinition = "text")
    private List<String> listeImages;

    @ElementCollection
    @CollectionTable(name = "produit_videos", joinColumns = @JoinColumn(name = "produit_id"))
    @Column(name = "video_url")
    private List<String> videosOptionnelles;

    @Column(name = "disponibilite")
    private Boolean disponibilite = true;

    @CreationTimestamp
    @Column(name = "date_ajout")
    private LocalDateTime dateAjout;

    @UpdateTimestamp
    @Column(name = "date_modification")
    private LocalDateTime dateModification;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categorie_id")
    private Categorie categorie;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "marque_id")
    private Marque marque;

    @OneToMany(mappedBy = "produit", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<AttributProduit> attributs;

    public enum LabelEnergie {
        A_PLUS_PLUS_PLUS, A_PLUS_PLUS, A_PLUS, A, B, C, D, E, F, G
    }
}