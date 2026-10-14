package com.froidcheikh.ecommerce.repository;

import com.froidcheikh.ecommerce.entity.ImageHero;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ImageHeroRepository extends JpaRepository<ImageHero, Long> {
    List<ImageHero> findAllByActifTrueOrderByOrdreAsc();
}
