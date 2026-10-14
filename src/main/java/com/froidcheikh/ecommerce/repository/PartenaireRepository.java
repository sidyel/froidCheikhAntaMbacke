package com.froidcheikh.ecommerce.repository;

import com.froidcheikh.ecommerce.entity.Partenaire;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PartenaireRepository extends JpaRepository<Partenaire, Long> {
    List<Partenaire> findAllByActifTrueOrderByOrdreAsc();
}
