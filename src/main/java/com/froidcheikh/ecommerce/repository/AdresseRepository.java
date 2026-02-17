package com.froidcheikh.ecommerce.repository;

import com.froidcheikh.ecommerce.entity.Adresse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AdresseRepository extends JpaRepository<Adresse, Long> {

    List<Adresse> findByClientIdClient(Long clientId);

    Optional<Adresse> findByClientIdClientAndAdressePrincipaleTrue(Long clientId);
}