package com.froidcheikh.ecommerce.repository;

import com.froidcheikh.ecommerce.entity.Categorie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface CategorieRepository extends JpaRepository<Categorie, Long> {

    Optional<Categorie> findByNomCategorie(String nomCategorie);

    List<Categorie> findByParentIsNull();

    List<Categorie> findByParentIdCategorie(Long parentId);

    @Query("SELECT c FROM Categorie c WHERE c.parent IS NULL ORDER BY c.nomCategorie")
    List<Categorie> findRootCategoriesOrderByName();

    @Query("SELECT c FROM Categorie c LEFT JOIN FETCH c.sousCategories WHERE c.parent IS NULL")
    List<Categorie> findRootCategoriesWithSubCategories();

    @Query("SELECT c FROM Categorie c ORDER BY c.nomCategorie") // remplace c.nom par le bon nom de champ
    List<Categorie> findAllOrderByName(); // nom de méthode libre

}