package com.froidcheikh.ecommerce.repository;

import com.froidcheikh.ecommerce.entity.ImageProduit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ImageProduitRepository extends JpaRepository<ImageProduit, Long> {

    List<ImageProduit> findByProduitIdProduitOrderByOrdreAsc(Long produitId);

    Optional<ImageProduit> findByProduitIdProduitAndPrincipaleTrue(Long produitId);

    @Query("SELECT COUNT(i) FROM ImageProduit i WHERE i.produit.idProduit = :produitId")
    long countByProduitId(@Param("produitId") Long produitId);

    boolean existsByProduitIdProduitAndIdImage(Long produitId, Long idImage);
}
