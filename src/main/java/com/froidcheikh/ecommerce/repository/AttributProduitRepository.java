package com.froidcheikh.ecommerce.repository;

import com.froidcheikh.ecommerce.entity.AttributProduit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AttributProduitRepository extends JpaRepository<AttributProduit, Long> {

    List<AttributProduit> findByProduitIdProduit(Long produitId);

    boolean existsByProduitIdProduitAndIdAttribut(Long produitId, Long idAttribut);
}
