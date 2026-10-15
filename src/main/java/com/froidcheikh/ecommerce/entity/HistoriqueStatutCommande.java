package com.froidcheikh.ecommerce.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "historique_statut_commande")
@Data
@NoArgsConstructor
public class HistoriqueStatutCommande {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "commande_id", nullable = false)
    private Commande commande;

    @Enumerated(EnumType.STRING)
    @Column(name = "ancien_statut")
    private Commande.StatutCommande ancienStatut; // null pour la création

    @Enumerated(EnumType.STRING)
    @Column(name = "nouveau_statut", nullable = false)
    private Commande.StatutCommande nouveauStatut;

    @CreationTimestamp
    @Column(name = "date_changement", updatable = false)
    private LocalDateTime dateChangement;

    @Column(name = "auteur", length = 200)
    private String auteur; // email admin, "CLIENT" ou "SYSTEME"

    @Column(name = "commentaire", columnDefinition = "TEXT")
    private String commentaire;

    public HistoriqueStatutCommande(Commande commande,
                                     Commande.StatutCommande ancienStatut,
                                     Commande.StatutCommande nouveauStatut,
                                     String auteur,
                                     String commentaire) {
        this.commande = commande;
        this.ancienStatut = ancienStatut;
        this.nouveauStatut = nouveauStatut;
        this.auteur = auteur;
        this.commentaire = commentaire;
    }
}
