package com.froidcheikh.ecommerce.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContactDTO {

    @NotBlank(message = "Le nom est obligatoire")
    @Size(min = 2, message = "Le nom doit contenir au moins 2 caractères")
    private String nom;

    @NotBlank(message = "Le prénom est obligatoire")
    @Size(min = 2, message = "Le prénom doit contenir au moins 2 caractères")
    private String prenom;

    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "Format d'email invalide")
    private String email;

    @Pattern(regexp = "^(\\+221)?[0-9]{8,9}$", message = "Format de téléphone invalide")
    private String telephone;

    @NotBlank(message = "Le sujet est obligatoire")
    private String sujet;

    @NotBlank(message = "Le message est obligatoire")
    @Size(min = 10, message = "Le message doit contenir au moins 10 caractères")
    private String message;
}