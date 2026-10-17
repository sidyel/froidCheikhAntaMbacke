package com.froidcheikh.ecommerce.repository;

import com.froidcheikh.ecommerce.entity.CompteurDocument.TypeDocument;
import com.froidcheikh.ecommerce.entity.DocumentFacturation;
import com.froidcheikh.ecommerce.entity.DocumentFacturation.StatutDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DocumentFacturationRepository extends JpaRepository<DocumentFacturation, Long> {

    @Query("SELECT d FROM DocumentFacturation d WHERE " +
           "(:type IS NULL OR d.typeDocument = :type) AND " +
           "(:statut IS NULL OR d.statut = :statut) AND " +
           "(:q IS NULL OR LOWER(d.clientNom) LIKE LOWER(CONCAT('%', CAST(:q AS String), '%')) OR " +
           "LOWER(COALESCE(d.clientPrenom, '')) LIKE LOWER(CONCAT('%', CAST(:q AS String), '%')) OR " +
           "LOWER(COALESCE(d.numero, '')) LIKE LOWER(CONCAT('%', CAST(:q AS String), '%')))")
    Page<DocumentFacturation> rechercher(
            @Param("type") TypeDocument type,
            @Param("statut") StatutDocument statut,
            @Param("q") String q,
            Pageable pageable);

    Optional<DocumentFacturation> findByNumero(String numero);

    /** Compte les factures actives (non annulées par un avoir total) liées à une commande. */
    @Query("SELECT COUNT(d) FROM DocumentFacturation d WHERE d.commande.idCommande = :commandeId " +
           "AND d.typeDocument = 'FACTURE' AND d.statut = 'EMIS'")
    long countFacturesEmisesParCommande(@Param("commandeId") Long commandeId);

    List<DocumentFacturation> findByCommande_IdCommandeAndTypeDocumentAndStatut(
            Long commandeId, TypeDocument type, StatutDocument statut);
}
