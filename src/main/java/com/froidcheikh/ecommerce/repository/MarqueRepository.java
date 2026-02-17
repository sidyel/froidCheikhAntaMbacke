package com.froidcheikh.ecommerce.repository;

import com.froidcheikh.ecommerce.entity.Marque;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MarqueRepository extends JpaRepository<Marque, Long> {

    Optional<Marque> findByNomMarque(String nomMarque);

    @Query("SELECT m FROM Marque m ORDER BY m.nomMarque")
    List<Marque> findAllOrderByName();

    @Query("SELECT DISTINCT m FROM Marque m INNER JOIN m.produits p WHERE p.disponibilite = true")
    List<Marque> findMarquesWithAvailableProducts();
}