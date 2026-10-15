package com.froidcheikh.ecommerce.repository;

import com.froidcheikh.ecommerce.entity.CompteurDocument;
import com.froidcheikh.ecommerce.entity.CompteurDocument.TypeDocument;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CompteurDocumentRepository extends JpaRepository<CompteurDocument, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM CompteurDocument c WHERE c.typeDocument = :type AND c.annee = :annee")
    Optional<CompteurDocument> findByTypeAndAnneeForUpdate(
            @Param("type") TypeDocument type,
            @Param("annee") Integer annee);
}
