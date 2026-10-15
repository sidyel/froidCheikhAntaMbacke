package com.froidcheikh.ecommerce.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "compteurs_document",
       uniqueConstraints = @UniqueConstraint(columnNames = {"type_document", "annee"}))
@Data
@NoArgsConstructor
public class CompteurDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_document", nullable = false, length = 20)
    private TypeDocument typeDocument;

    @Column(name = "annee", nullable = false)
    private Integer annee;

    @Column(name = "dernier_numero", nullable = false)
    private Integer dernierNumero = 0;

    public enum TypeDocument {
        FACTURE, DEVIS, AVOIR, BON_LIVRAISON
    }
}
