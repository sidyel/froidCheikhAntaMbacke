package com.froidcheikh.ecommerce.repository;

import com.froidcheikh.ecommerce.entity.Promotion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, Long> {

    // Toutes les promos actives dont la date de fin n'est pas encore passée (candidats au cache)
    @Query("SELECT p FROM Promotion p WHERE p.actif = true AND (p.dateFin IS NULL OR p.dateFin >= :now)")
    List<Promotion> findCandidates(@Param("now") LocalDateTime now);

    @Query("SELECT p FROM Promotion p WHERE p.actif = true AND p.dateDebut <= :now AND (p.dateFin IS NULL OR p.dateFin >= :now) ORDER BY p.priorite DESC")
    Page<Promotion> findActives(@Param("now") LocalDateTime now, Pageable pageable);

    @Query("SELECT p FROM Promotion p WHERE p.actif = true AND p.dateDebut > :now ORDER BY p.dateDebut ASC")
    Page<Promotion> findProgrammees(@Param("now") LocalDateTime now, Pageable pageable);

    @Query("SELECT p FROM Promotion p WHERE p.dateFin IS NOT NULL AND p.dateFin < :now ORDER BY p.dateFin DESC")
    Page<Promotion> findExpirees(@Param("now") LocalDateTime now, Pageable pageable);

    @Query("SELECT COUNT(l) > 0 FROM LigneCommande l WHERE l.promotionId = :promotionId")
    boolean isUsedInCommande(@Param("promotionId") Long promotionId);
}
