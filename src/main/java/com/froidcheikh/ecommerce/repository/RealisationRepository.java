package com.froidcheikh.ecommerce.repository;

import com.froidcheikh.ecommerce.entity.Realisation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RealisationRepository extends JpaRepository<Realisation, Long> {
    List<Realisation> findAllByActifTrueOrderByOrdreAsc();
}
