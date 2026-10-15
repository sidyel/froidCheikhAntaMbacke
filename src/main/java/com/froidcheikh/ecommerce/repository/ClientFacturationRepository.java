package com.froidcheikh.ecommerce.repository;

import com.froidcheikh.ecommerce.entity.ClientFacturation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ClientFacturationRepository extends JpaRepository<ClientFacturation, Long> {

    @Query("SELECT c FROM ClientFacturation c WHERE " +
           "LOWER(c.nom) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(COALESCE(c.prenom, '')) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "COALESCE(c.telephone, '') LIKE CONCAT('%', :q, '%') OR " +
           "LOWER(COALESCE(c.email, '')) LIKE LOWER(CONCAT('%', :q, '%'))")
    List<ClientFacturation> rechercher(@Param("q") String q);
}
