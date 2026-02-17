package com.froidcheikh.ecommerce.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "paiements")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Paiement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_paiement")
    private Long idPaiement;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "methode_paiement")
    private MethodePaiement methodePaiement;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "statut_paiement")
    private StatutPaiement statutPaiement = StatutPaiement.EN_ATTENTE;

    @NotNull
    @Column(name = "montant", precision = 10, scale = 2)
    private BigDecimal montant;

    @CreationTimestamp
    @Column(name = "date_paiement")
    private LocalDateTime datePaiement;

    @Column(name = "reference_paiement", unique = true)
    private String referencePaiement;

    @Column(name = "reference_externe")
    private String referenceExterne;

    @Column(name = "details_paiement", columnDefinition = "TEXT")
    private String detailsPaiement;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "commande_id")
    private Commande commande;

    public enum MethodePaiement {
        WAVE, ORANGE_MONEY, VIREMENT_BANCAIRE, ESPECES, CARTE_BANCAIRE
    }

    public enum StatutPaiement {
        EN_ATTENTE, CONFIRME, ECHOUE, ANNULE, REMBOURSE
    }
}