package com.froidcheikh.ecommerce.repository;

import com.froidcheikh.ecommerce.entity.Paiement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaiementRepository extends JpaRepository<Paiement, Long> {

    Optional<Paiement> findByReferencePaiement(String referencePaiement);

    Optional<Paiement> findByCommandeIdCommande(Long commandeId);
}