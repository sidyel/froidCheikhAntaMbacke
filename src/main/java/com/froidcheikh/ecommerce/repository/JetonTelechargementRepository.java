package com.froidcheikh.ecommerce.repository;

import com.froidcheikh.ecommerce.entity.JetonTelechargement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JetonTelechargementRepository extends JpaRepository<JetonTelechargement, Long> {

    Optional<JetonTelechargement> findByJeton(String jeton);
}
