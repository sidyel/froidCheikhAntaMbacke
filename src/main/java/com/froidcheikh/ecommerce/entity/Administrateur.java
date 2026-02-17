package com.froidcheikh.ecommerce.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
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
@Table(name = "administrateurs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Administrateur implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_admin")
    private Long idAdmin;

    @NotBlank
    @Column(name = "nom", nullable = false)
    private String nom;

    @NotBlank
    @Column(name = "prenom", nullable = false)
    private String prenom;

    @NotBlank
    @Email
    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @NotBlank
    @Column(name = "mot_de_passe", nullable = false)
    private String motDePasse;

    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    private RoleAdmin role = RoleAdmin.ADMIN;

    @Column(name = "actif")
    private Boolean actif = true;

    @CreationTimestamp
    @Column(name = "date_creation")
    private LocalDateTime dateCreation;

    public enum RoleAdmin {
        SUPER_ADMIN, ADMIN, GESTIONNAIRE
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
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