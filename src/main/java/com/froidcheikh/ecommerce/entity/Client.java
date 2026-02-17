package com.froidcheikh.ecommerce.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "clients")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Client implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_client")
    private Long idClient;

    @NotBlank(message = "Le nom est obligatoire")
    @Size(min = 2, max = 50, message = "Le nom doit contenir entre 2 et 50 caractères")
    @Column(name = "nom", nullable = false)
    private String nom;

    @NotBlank(message = "Le prénom est obligatoire")
    @Size(min = 2, max = 50, message = "Le prénom doit contenir entre 2 et 50 caractères")
    @Column(name = "prenom", nullable = false)
    private String prenom;

    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "Format d'email invalide")
    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @NotBlank(message = "Le mot de passe est obligatoire")
    @Column(name = "mot_de_passe", nullable = false)
    private String motDePasse;

    @Pattern(regexp = "^(\\+221)?[0-9]{8,9}$", message = "Format de téléphone invalide")
    @Column(name = "telephone")
    private String telephone;

    @Column(name = "date_naissance")
    private String dateNaissance;

    @Enumerated(EnumType.STRING)
    @Column(name = "genre")
    private Genre genre;

    @Column(name = "actif")
    private Boolean actif = true;

    @CreationTimestamp
    @Column(name = "date_creation")
    private LocalDateTime dateCreation;

    @OneToMany(mappedBy = "client", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Adresse> adresses;

    @OneToMany(mappedBy = "client", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Commande> commandes;

    @ElementCollection
    @CollectionTable(name = "client_wishlist", joinColumns = @JoinColumn(name = "client_id"))
    @Column(name = "produit_id")
    private List<Long> wishlist;

    public enum Genre {
        HOMME, FEMME, AUTRE
    }

    // Implémentation UserDetails
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_CLIENT"));
    }

    @Override
    public String getPassword() {
        return motDePasse;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return actif;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return actif;
    }
}