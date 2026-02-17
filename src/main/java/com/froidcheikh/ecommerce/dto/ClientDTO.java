package com.froidcheikh.ecommerce.dto;

import com.froidcheikh.ecommerce.entity.Client;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClientDTO {

    private Long idClient;

    @NotBlank(message = "Le nom est obligatoire")
    @Size(min = 2, max = 50)
    private String nom;

    @NotBlank(message = "Le prénom est obligatoire")
    @Size(min = 2, max = 50)
    private String prenom;

    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "Format d'email invalide")
    private String email;

    @Pattern(regexp = "^(\\+221)?[0-9]{8,9}$", message = "Format de téléphone invalide")
    private String telephone;

    private String dateNaissance;
    private Client.Genre genre;
    private Boolean actif;
    private LocalDateTime dateCreation;

    private List<AdresseDTO> adresses;
    private List<Long> wishlist;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AdresseDTO {
        private Long idAdresse;

        @NotBlank(message = "La ligne 1 de l'adresse est obligatoire")
        private String ligne1;

        private String ligne2;

        @NotBlank(message = "La ville est obligatoire")
        private String ville;

        private String codePostal;
        private String pays;
        private String telephone;
        private String typeAdresse;
        private Boolean adressePrincipale;
    }
}