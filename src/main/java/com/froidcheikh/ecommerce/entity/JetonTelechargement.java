package com.froidcheikh.ecommerce.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "jetons_telechargement")
@Data
@NoArgsConstructor
public class JetonTelechargement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_id", nullable = false)
    private DocumentFacturation document;

    @Column(name = "jeton", unique = true, nullable = false, length = 36)
    private String jeton;

    @Column(name = "date_expiration", nullable = false)
    private LocalDateTime dateExpiration;

    /** Révoqué manuellement avant expiration. */
    @Column(name = "revoque")
    private Boolean revoque = false;

    @CreationTimestamp
    @Column(name = "date_creation", updatable = false)
    private LocalDateTime dateCreation;

    @Column(name = "cree_par")
    private String creePar;

    public boolean isValide() {
        return !Boolean.TRUE.equals(revoque) && LocalDateTime.now().isBefore(dateExpiration);
    }
}
