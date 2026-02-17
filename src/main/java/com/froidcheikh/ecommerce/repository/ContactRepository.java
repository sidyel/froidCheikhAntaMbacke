package com.froidcheikh.ecommerce.repository;


import com.froidcheikh.ecommerce.entity.Contact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ContactRepository extends JpaRepository<Contact, Long> {

    List<Contact> findByStatut(Contact.StatutContact statut);

    List<Contact> findByDateEnvoiBetween(LocalDateTime debut, LocalDateTime fin);

    List<Contact> findByEmailContaining(String email);

    long countByStatut(Contact.StatutContact statut);
}