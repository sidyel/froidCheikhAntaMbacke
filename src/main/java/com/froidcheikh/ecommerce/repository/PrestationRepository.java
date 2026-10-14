package com.froidcheikh.ecommerce.repository;

import com.froidcheikh.ecommerce.entity.Prestation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PrestationRepository extends JpaRepository<Prestation, Long> {
    List<Prestation> findAllByActifTrueOrderByOrdreAsc();
}
