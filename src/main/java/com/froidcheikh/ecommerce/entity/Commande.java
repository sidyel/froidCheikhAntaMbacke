package com.froidcheikh.ecommerce.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "commandes")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Commande {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_commande")
    private Long idCommande;

    @Column(name = "numero_commande", unique = true)
    private String numeroCommande;

    @CreationTimestamp
    @Column(name = "date_commande")
    private LocalDateTime dateCommande;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "statut_commande")
    private StatutCommande statutCommande = StatutCommande.EN_ATTENTE;

    @NotNull
    @Column(name = "montant_total", precision = 10, scale = 2)
    private BigDecimal montantTotal;

    @Column(name = "frais_livraison", precision = 10, scale = 2)
    private BigDecimal fraisLivraison = BigDecimal.ZERO;

    @Column(name = "numero_suivi")
    private String numeroSuivi;

    @Column(name = "commentaire", columnDefinition = "TEXT")
    private String commentaire;

    @Enumerated(EnumType.STRING)
    @Column(name = "mode_livraison")
    private ModeLivraison modeLivraison;

    @UpdateTimestamp
    @Column(name = "date_modification")
    private LocalDateTime dateModification;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private Client client;

    @OneToMany(mappedBy = "commande", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<LigneCommande> lignesCommande;

    @OneToOne(mappedBy = "commande", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Paiement paiement;

    @Embedded
    private AdresseLivraison adresseLivraison;

    // Commande invité (sans compte client)
    @Column(name = "email_invite")
    private String emailInvite;

    @Column(name = "nom_invite")
    private String nomInvite;

    @Column(name = "prenom_invite")
    private String prenomInvite;

    @Column(name = "telephone_invite")
    private String telephoneInvite;

    public enum StatutCommande {
        EN_ATTENTE, CONFIRMEE, PAYEE, EN_PREPARATION, EXPEDIE, LIVREE, ANNULEE, REMBOURSEE
    }

    public enum ModeLivraison {
        LIVRAISON_DOMICILE, RETRAIT_MAGASIN, LIVRAISON_EXPRESS
    }

    @Embeddable
    @Data
    public static class AdresseLivraison {
        @Column(name = "livraison_nom")
        private String nom;

        @Column(name = "livraison_prenom")
        private String prenom;

        @Column(name = "livraison_ligne1")
        private String ligne1;

        @Column(name = "livraison_ligne2")
        private String ligne2;

        @Column(name = "livraison_ville")
        private String ville;

        @Column(name = "livraison_code_postal")
        private String codePostal;

        @Column(name = "livraison_telephone")
        private String telephone;
    }
}