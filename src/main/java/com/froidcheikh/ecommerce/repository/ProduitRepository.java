package com.froidcheikh.ecommerce.repository;

import com.froidcheikh.ecommerce.entity.Produit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProduitRepository extends JpaRepository<Produit, Long> {

    Optional<Produit> findByRefProduit(String refProduit);

    Optional<Produit> findByCodeProduit(String codeProduit);

    Page<Produit> findByDisponibiliteTrue(Pageable pageable);

    Page<Produit> findByCategorieIdCategorie(Long categorieId, Pageable pageable);

    Page<Produit> findByMarqueIdMarque(Long marqueId, Pageable pageable);

    // SOLUTION 1: Requête JPA corrigée avec CAST explicite
    @Query("SELECT p FROM Produit p WHERE p.disponibilite = true AND " +
            "(:nomProduit IS NULL OR :nomProduit = '' OR LOWER(CAST(p.nomProduit AS string)) LIKE LOWER(CAST(CONCAT('%', :nomProduit, '%') AS string))) AND " +
            "(:prixMin IS NULL OR p.prix >= :prixMin) AND " +
            "(:prixMax IS NULL OR p.prix <= :prixMax) AND " +
            "(:categorieId IS NULL OR p.categorie.idCategorie = :categorieId) AND " +
            "(:marqueId IS NULL OR p.marque.idMarque = :marqueId)")
    Page<Produit> findWithFilters(
            @Param("nomProduit") String nomProduit,
            @Param("prixMin") BigDecimal prixMin,
            @Param("prixMax") BigDecimal prixMax,
            @Param("categorieId") Long categorieId,
            @Param("marqueId") Long marqueId,
            Pageable pageable
    );

    // SOLUTION 2: Requête SQL native (plus fiable pour PostgreSQL)
    @Query(value = """
        SELECT * FROM produits p 
        WHERE p.disponibilite = true 
        AND (:nomProduit IS NULL OR :nomProduit = '' OR LOWER(p.nom_produit::text) LIKE LOWER(CONCAT('%', :nomProduit::text, '%')))
        AND (:prixMin IS NULL OR p.prix >= :prixMin)
        AND (:prixMax IS NULL OR p.prix <= :prixMax)
        AND (:categorieId IS NULL OR p.categorie_id = :categorieId)
        AND (:marqueId IS NULL OR p.marque_id = :marqueId)
        ORDER BY 
            CASE WHEN :#{#pageable.sort.getOrderFor('dateAjout')} IS NOT NULL 
                 AND :#{#pageable.sort.getOrderFor('dateAjout').direction.name()} = 'DESC' 
                 THEN p.date_ajout END DESC,
            CASE WHEN :#{#pageable.sort.getOrderFor('dateAjout')} IS NOT NULL 
                 AND :#{#pageable.sort.getOrderFor('dateAjout').direction.name()} = 'ASC' 
                 THEN p.date_ajout END ASC,
            CASE WHEN :#{#pageable.sort.getOrderFor('prix')} IS NOT NULL 
                 AND :#{#pageable.sort.getOrderFor('prix').direction.name()} = 'DESC' 
                 THEN p.prix END DESC,
            CASE WHEN :#{#pageable.sort.getOrderFor('prix')} IS NOT NULL 
                 AND :#{#pageable.sort.getOrderFor('prix').direction.name()} = 'ASC' 
                 THEN p.prix END ASC,
            CASE WHEN :#{#pageable.sort.getOrderFor('nomProduit')} IS NOT NULL 
                 AND :#{#pageable.sort.getOrderFor('nomProduit').direction.name()} = 'DESC' 
                 THEN p.nom_produit END DESC,
            CASE WHEN :#{#pageable.sort.getOrderFor('nomProduit')} IS NOT NULL 
                 AND :#{#pageable.sort.getOrderFor('nomProduit').direction.name()} = 'ASC' 
                 THEN p.nom_produit END ASC,
            p.date_ajout DESC
        """,
            countQuery = """
        SELECT COUNT(*) FROM produits p 
        WHERE p.disponibilite = true 
        AND (:nomProduit IS NULL OR :nomProduit = '' OR LOWER(p.nom_produit::text) LIKE LOWER(CONCAT('%', :nomProduit::text, '%')))
        AND (:prixMin IS NULL OR p.prix >= :prixMin)
        AND (:prixMax IS NULL OR p.prix <= :prixMax)
        AND (:categorieId IS NULL OR p.categorie_id = :categorieId)
        AND (:marqueId IS NULL OR p.marque_id = :marqueId)
        """,
            nativeQuery = true)
    Page<Produit> findWithFiltersNative(
            @Param("nomProduit") String nomProduit,
            @Param("prixMin") BigDecimal prixMin,
            @Param("prixMax") BigDecimal prixMax,
            @Param("categorieId") Long categorieId,
            @Param("marqueId") Long marqueId,
            Pageable pageable
    );

    // SOLUTION 3: Requête simplifiée sans recherche textuelle (fallback)
    @Query("SELECT p FROM Produit p WHERE p.disponibilite = true AND " +
            "(:prixMin IS NULL OR p.prix >= :prixMin) AND " +
            "(:prixMax IS NULL OR p.prix <= :prixMax) AND " +
            "(:categorieId IS NULL OR p.categorie.idCategorie = :categorieId) AND " +
            "(:marqueId IS NULL OR p.marque.idMarque = :marqueId)")
    Page<Produit> findWithFiltersNoSearch(
            @Param("prixMin") BigDecimal prixMin,
            @Param("prixMax") BigDecimal prixMax,
            @Param("categorieId") Long categorieId,
            @Param("marqueId") Long marqueId,
            Pageable pageable
    );

    // SOLUTION 4: Recherche textuelle séparée
    @Query("SELECT p FROM Produit p WHERE p.disponibilite = true AND " +
            "(LOWER(p.nomProduit) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(p.descriptionProduit) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    Page<Produit> findBySearchTerm(@Param("searchTerm") String searchTerm, Pageable pageable);

    // Méthodes existantes conservées
    List<Produit> findByStockDisponibleLessThan(Integer seuil);

    @Query("SELECT p FROM Produit p ORDER BY p.dateAjout DESC")
    Page<Produit> findLatestProducts(Pageable pageable);

    @Query("SELECT p FROM Produit p WHERE p.idProduit IN :ids")
    List<Produit> findByIdIn(@Param("ids") List<Long> ids);

    // Nouvelles méthodes pour les catégories et marques avec filtres
    @Query("SELECT p FROM Produit p WHERE p.disponibilite = true AND p.categorie.idCategorie = :categorieId AND " +
            "(:prixMin IS NULL OR p.prix >= :prixMin) AND " +
            "(:prixMax IS NULL OR p.prix <= :prixMax)")
    Page<Produit> findByCategorieWithFilters(
            @Param("categorieId") Long categorieId,
            @Param("prixMin") BigDecimal prixMin,
            @Param("prixMax") BigDecimal prixMax,
            Pageable pageable
    );

    @Query("SELECT p FROM Produit p WHERE p.disponibilite = true AND p.marque.idMarque = :marqueId AND " +
            "(:prixMin IS NULL OR p.prix >= :prixMin) AND " +
            "(:prixMax IS NULL OR p.prix <= :prixMax)")
    Page<Produit> findByMarqueWithFilters(
            @Param("marqueId") Long marqueId,
            @Param("prixMin") BigDecimal prixMin,
            @Param("prixMax") BigDecimal prixMax,
            Pageable pageable
    );

    @Query("SELECT p FROM Produit p WHERE " +
            "(:search IS NULL OR :search = '' OR LOWER(p.nomProduit) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
            "(:categorieId IS NULL OR p.categorie.idCategorie = :categorieId) AND " +
            "(:marqueId IS NULL OR p.marque.idMarque = :marqueId) AND " +
            "(:stockFilter IS NULL OR " +
            "  (:stockFilter = 'disponible' AND p.stockDisponible > 0) OR " +
            "  (:stockFilter = 'rupture' AND p.stockDisponible = 0) OR " +
            "  (:stockFilter = 'faible' AND p.stockDisponible > 0 AND p.stockDisponible <= 5))")
    Page<Produit> findAllWithAdminFilters(
            @Param("search") String search,
            @Param("categorieId") Long categorieId,
            @Param("marqueId") Long marqueId,
            @Param("stockFilter") String stockFilter,
            Pageable pageable
    );
}