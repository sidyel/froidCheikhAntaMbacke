package com.froidcheikh.ecommerce.repository;

import com.froidcheikh.ecommerce.entity.ZoneLivraison;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ZoneLivraisonRepository extends JpaRepository<ZoneLivraison, Long> {

    List<ZoneLivraison> findByActifTrueOrderByVilleLabelAscOrdreAsc();

    List<ZoneLivraison> findAllByOrderByVilleLabelAscOrdreAsc();

    List<ZoneLivraison> findByVilleIgnoreCaseOrderByOrdreAsc(String ville);

    Optional<ZoneLivraison> findByVilleIgnoreCaseAndQuartierIgnoreCase(String ville, String quartier);

    boolean existsByVilleIgnoreCaseAndQuartierIgnoreCase(String ville, String quartier);

    List<String> findDistinctVilleLabelByActifTrue();
}
