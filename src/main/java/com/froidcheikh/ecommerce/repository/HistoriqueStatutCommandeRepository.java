package com.froidcheikh.ecommerce.repository;

import com.froidcheikh.ecommerce.entity.HistoriqueStatutCommande;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HistoriqueStatutCommandeRepository extends JpaRepository<HistoriqueStatutCommande, Long> {

    List<HistoriqueStatutCommande> findByCommandeIdCommandeOrderByDateChangementAsc(Long commandeId);
}
