package com.froidcheikh.ecommerce.repository;

import com.froidcheikh.ecommerce.entity.ModeleAttribut;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ModeleAttributRepository extends JpaRepository<ModeleAttribut, Long> {

    List<ModeleAttribut> findByCategoriesIdCategorieOrderByOrdreAsc(Long categorieId);

    boolean existsByNomAttribut(String nomAttribut);
}
