package com.froidcheikh.ecommerce.repository;

import com.froidcheikh.ecommerce.entity.Client;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface ClientRepository extends JpaRepository<Client, Long> {

    Optional<Client> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query("SELECT c FROM Client c LEFT JOIN FETCH c.adresses WHERE c.email = :email")
    Optional<Client> findByEmailWithAdresses(@Param("email") String email);

    @Query("SELECT c FROM Client c LEFT JOIN FETCH c.commandes WHERE c.idClient = :id")
    Optional<Client> findByIdWithCommandes(@Param("id") Long id);
    // Ajout dans ClientRepository.java

    @Query("SELECT c FROM Client c WHERE " +
            "LOWER(c.nom) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(c.prenom) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(c.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "c.telephone LIKE CONCAT('%', :searchTerm, '%')")
    Page<Client> searchClients(@Param("searchTerm") String searchTerm, Pageable pageable);

    @Query("SELECT c FROM Client c WHERE c.actif = :actif")
    Page<Client> findByActif(@Param("actif") Boolean actif, Pageable pageable);

    @Query("SELECT c FROM Client c WHERE c.genre = :genre")
    Page<Client> findByGenre(@Param("genre") Client.Genre genre, Pageable pageable);

    @Query("SELECT c FROM Client c WHERE c.dateCreation >= :dateDebut")
    Page<Client> findByDateCreationAfter(@Param("dateDebut") LocalDateTime dateDebut, Pageable pageable);

    @Query("SELECT COUNT(c) FROM Client c WHERE c.dateCreation >= :date")
    Long countClientsSince(@Param("date") LocalDateTime date);

    @Query("SELECT c FROM Client c LEFT JOIN FETCH c.adresses LEFT JOIN FETCH c.commandes WHERE c.idClient = :id")
    Optional<Client> findByIdWithAdressesAndCommandes(@Param("id") Long id);
}