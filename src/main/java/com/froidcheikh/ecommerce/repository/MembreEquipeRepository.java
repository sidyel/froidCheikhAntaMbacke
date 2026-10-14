package com.froidcheikh.ecommerce.repository;

import com.froidcheikh.ecommerce.entity.MembreEquipe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MembreEquipeRepository extends JpaRepository<MembreEquipe, Long> {
    List<MembreEquipe> findAllByActifTrueOrderByOrdreAsc();
}
