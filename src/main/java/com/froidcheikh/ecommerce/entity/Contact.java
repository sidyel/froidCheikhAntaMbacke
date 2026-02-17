package com.froidcheikh.ecommerce.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "contacts")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Contact {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nom;

    @Column(nullable = false)
    private String prenom;

    @Column(nullable = false)
    private String email;

    private String telephone;

    @Column(nullable = false)
    private String sujet;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(nullable = false, updatable = false)
    private LocalDateTime dateEnvoi;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutContact statut = StatutContact.NON_LU;

    @PrePersist
    protected void onCreate() {
        dateEnvoi = LocalDateTime.now();
    }

    public enum StatutContact {
        NON_LU,
        LU,
        EN_COURS,
        TRAITE
    }
}