package com.froidcheikh.ecommerce.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "adresses")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Adresse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_adresse")
    private Long idAdresse;

    @NotBlank(message = "La ligne 1 de l'adresse est obligatoire")
    @Column(name = "ligne1", nullable = false)
    private String ligne1;

    @Column(name = "ligne2")
    private String ligne2;

    @NotBlank(message = "La ville est obligatoire")
    @Column(name = "ville", nullable = false)
    private String ville;

    @Column(name = "code_postal")
    private String codePostal;

    @Column(name = "pays")
    private String pays = "Sénégal";

    @Column(name = "telephone")
    private String telephone;

    @Column(name = "type_adresse")
    @Enumerated(EnumType.STRING)
    private TypeAdresse typeAdresse;

    @Column(name = "adresse_principale")
    private Boolean adressePrincipale = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private Client client;

    public enum TypeAdresse {
        DOMICILE, BUREAU, AUTRE
    }
}