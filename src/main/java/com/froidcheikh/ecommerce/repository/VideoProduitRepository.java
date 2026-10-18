package com.froidcheikh.ecommerce.repository;

import com.froidcheikh.ecommerce.entity.VideoProduit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VideoProduitRepository extends JpaRepository<VideoProduit, Long> {

    List<VideoProduit> findByProduitIdProduitOrderByOrdreAsc(Long produitId);

    List<VideoProduit> findByProduitIdProduitAndActiveOrderByOrdreAsc(Long produitId, boolean active);

    long countByProduitIdProduit(Long produitId);
}
