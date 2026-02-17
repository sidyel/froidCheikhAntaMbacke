package com.froidcheikh.ecommerce.repository;

import com.froidcheikh.ecommerce.entity.Administrateur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AdministrateurRepository extends JpaRepository<Administrateur, Long> {

    Optional<Administrateur> findByEmail(String email);

    boolean existsByEmail(String email);
}