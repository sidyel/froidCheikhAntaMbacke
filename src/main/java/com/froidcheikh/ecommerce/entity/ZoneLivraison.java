package com.froidcheikh.ecommerce.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "zones_livraison", uniqueConstraints = {
        @UniqueConstraint(name = "uk_zone_ville_quartier", columnNames = {"ville", "quartier"})
})
@Data
@NoArgsConstructor
public class ZoneLivraison {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(name = "ville", nullable = false, length = 100)
    private String ville;

    @NotBlank
    @Column(name = "ville_label", nullable = false, length = 100)
    private String villeLabel;

    @NotBlank
    @Column(name = "quartier", nullable = false, length = 100)
    private String quartier;

    @NotBlank
    @Column(name = "quartier_label", nullable = false, length = 150)
    private String quartierLabel;

    @NotNull
    @DecimalMin(value = "0.0")
    @Column(name = "prix_standard", nullable = false, precision = 10, scale = 2)
    private BigDecimal prixStandard;

    /** Null = prixStandard + supplementExpress de ParametresSite. */
    @DecimalMin(value = "0.0")
    @Column(name = "prix_express", precision = 10, scale = 2)
    private BigDecimal prixExpress;

    /** Si false, LIVRAISON_EXPRESS est refusée pour cette zone. */
    @Column(name = "express_disponible", nullable = false)
    private Boolean expressDisponible = Boolean.TRUE;

    @Column(name = "delai_estime", length = 50)
    private String delaiEstime;

    @Column(name = "actif", nullable = false)
    private Boolean actif = Boolean.TRUE;

    /**
     * Si true, les frais de zone tombent à 0 quand le montant dépasse le seuil.
     * Mettre à false pour les villes éloignées (Saint-Louis, Ziguinchor…).
     */
    @Column(name = "eligible_livraison_gratuite", nullable = false)
    private Boolean eligibleLivraisonGratuite = Boolean.FALSE;

    @Column(name = "ordre")
    private Integer ordre = 0;
}
