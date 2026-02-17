package com.froidcheikh.ecommerce.repository;

import com.froidcheikh.ecommerce.entity.Commande;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CommandeRepository extends JpaRepository<Commande, Long> {

    Optional<Commande> findByNumeroCommande(String numeroCommande);

    Page<Commande> findByClientIdClient(Long clientId, Pageable pageable);

    @Query("SELECT c FROM Commande c LEFT JOIN FETCH c.lignesCommande WHERE c.idCommande = :id")
    Optional<Commande> findByIdWithLignes(@Param("id") Long id);

    @Query("SELECT c FROM Commande c WHERE c.dateCommande BETWEEN :startDate AND :endDate")
    List<Commande> findByDateCommandeBetween(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Query("SELECT c FROM Commande c WHERE c.statutCommande = :statut")
    Page<Commande> findByStatutCommande1(
            @Param("statut") Commande.StatutCommande statut,
            Pageable pageable
    );

    @Query("SELECT COUNT(c) FROM Commande c WHERE c.dateCommande >= :date")
    Long countCommandesSince(@Param("date") LocalDateTime date);

    /**
     * Trouver toutes les commandes triées par date décroissante
     */
    Page<Commande> findAllByOrderByDateCommandeDesc(Pageable pageable);

    /**
     * Trouver toutes les commandes avec un statut spécifique
     */
    Page<Commande> findByStatutCommande(Commande.StatutCommande statutCommande, Pageable pageable);

    /**
     * Trouver toutes les commandes avec un statut spécifique triées par date
     */
    Page<Commande> findByStatutCommandeOrderByDateCommandeDesc(Commande.StatutCommande statutCommande, Pageable pageable);

    /**
     * Compter les commandes par statut
     */
    long countByStatutCommande(Commande.StatutCommande statutCommande);

    /**
     * Trouver les commandes créées dans une période donnée
     */
    @Query("SELECT c FROM Commande c WHERE c.dateCommande BETWEEN :dateDebut AND :dateFin ORDER BY c.dateCommande DESC")
    Page<Commande> findCommandesByDateRange(
            @Param("dateDebut") LocalDateTime dateDebut,
            @Param("dateFin") LocalDateTime dateFin,
            Pageable pageable);

    /**
     * Trouver les commandes d'un client avec un statut spécifique
     */
    Page<Commande> findByClientIdClientAndStatutCommande(Long clientId, Commande.StatutCommande statutCommande, Pageable pageable);

    /**
     * Rechercher des commandes par numéro (recherche partielle)
     */
    @Query("SELECT c FROM Commande c WHERE c.numeroCommande LIKE %:numero% ORDER BY c.dateCommande DESC")
    Page<Commande> findByNumeroCommandeContaining(@Param("numero") String numero, Pageable pageable);

    /**
     * Rechercher des commandes par email d'invité
     */
    @Query("SELECT c FROM Commande c WHERE c.emailInvite LIKE %:email% ORDER BY c.dateCommande DESC")
    Page<Commande> findByEmailInviteContaining(@Param("email") String email, Pageable pageable);

    /**
     * Rechercher des commandes par nom/prénom d'invité
     */
    @Query("SELECT c FROM Commande c WHERE LOWER(c.nomInvite) LIKE LOWER(CONCAT('%', :nom, '%')) OR LOWER(c.prenomInvite) LIKE LOWER(CONCAT('%', :nom, '%')) ORDER BY c.dateCommande DESC")
    Page<Commande> findByNomInviteContaining(@Param("nom") String nom, Pageable pageable);

    /**
     * Trouver les commandes avec des lignes de commande (optimisé)
     */
    @Query("SELECT DISTINCT c FROM Commande c LEFT JOIN FETCH c.lignesCommande lc LEFT JOIN FETCH lc.produit ORDER BY c.dateCommande DESC")
    List<Commande> findAllWithLignesCommande();

    /**
     * Statistiques : Somme du montant total par statut
     */
    @Query("SELECT c.statutCommande, SUM(c.montantTotal) FROM Commande c GROUP BY c.statutCommande")
    List<Object[]> sumMontantTotalByStatut();

    /**
     * Statistiques : Nombre de commandes par jour (derniers 30 jours)
     */
    @Query(value = "SELECT DATE(c.date_commande) as jour, COUNT(*) as nombre " +
            "FROM commandes c " +
            "WHERE c.date_commande >= DATE_SUB(CURRENT_DATE, INTERVAL 30 DAY) " +
            "GROUP BY DATE(c.date_commande) " +
            "ORDER BY jour DESC", nativeQuery = true)
    List<Object[]> getCommandesParJour();

    /**
     * Trouver les commandes en retard (expédiées mais pas livrées depuis plus de X jours)
     */
    @Query("SELECT c FROM Commande c WHERE c.statutCommande = 'EXPEDIE' AND c.dateModification < :dateLimit ORDER BY c.dateModification ASC")
    List<Commande> findCommandesEnRetard(@Param("dateLimit") LocalDateTime dateLimit);

    /**
     * Trouver les commandes sans paiement confirmé depuis plus de X heures
     */
    @Query("SELECT c FROM Commande c WHERE c.statutCommande IN ('EN_ATTENTE', 'CONFIRMEE') AND c.dateCommande < :dateLimit ORDER BY c.dateCommande ASC")
    List<Commande> findCommandesSansPaiement(@Param("dateLimit") LocalDateTime dateLimit);
}