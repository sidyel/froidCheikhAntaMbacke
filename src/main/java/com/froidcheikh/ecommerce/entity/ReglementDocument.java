package com.froidcheikh.ecommerce.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "reglements_document")
@Data
@NoArgsConstructor
public class ReglementDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_id", nullable = false)
    private DocumentFacturation document;

    @Column(name = "date_reglement", nullable = false)
    private LocalDate dateReglement;

    @Column(name = "montant", nullable = false, precision = 12, scale = 2)
    private BigDecimal montant;

    @Enumerated(EnumType.STRING)
    @Column(name = "mode", nullable = false, length = 20)
    private ModeReglement mode;

    @Column(name = "reference", length = 100)
    private String reference;

    /** Login de l'admin qui a enregistré ce règlement. */
    @Column(name = "enregistre_par")
    private String enregistrePar;

    @CreationTimestamp
    @Column(name = "date_saisie", updatable = false)
    private LocalDateTime dateSaisie;

    /** Si non null : règlement supprimé (soft delete pour traçabilité). */
    @Column(name = "supprime_par")
    private String supprimePar;

    @Column(name = "date_suppression")
    private LocalDateTime dateSuppression;

    public enum ModeReglement {
        ESPECES, WAVE, ORANGE_MONEY, VIREMENT, AUTRE
    }
}
