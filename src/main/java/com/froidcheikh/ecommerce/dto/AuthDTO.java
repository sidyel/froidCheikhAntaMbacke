package com.froidcheikh.ecommerce.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import jakarta.validation.constraints.*;

public final class AuthDTO {
    private AuthDTO() { /* classe utilitaire - empêcher l'instanciation */ }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoginRequest {
        @NotBlank(message = "L'email est obligatoire")
        @Email(message = "Format d'email invalide")
        private String email;

        @NotBlank(message = "Le mot de passe est obligatoire")
        private String motDePasse;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RegisterRequest {
        @NotBlank(message = "Le nom est obligatoire")
        @Size(min = 2, max = 50)
        private String nom;

        @NotBlank(message = "Le prénom est obligatoire")
        @Size(min = 2, max = 50)
        private String prenom;

        @NotBlank(message = "L'email est obligatoire")
        @Email(message = "Format d'email invalide")
        private String email;

        @NotBlank(message = "Le mot de passe est obligatoire")
        @Size(min = 6, message = "Le mot de passe doit contenir au moins 6 caractères")
        private String motDePasse;

        @Pattern(regexp = "^(\\+221)?[0-9]{8,9}$", message = "Format de téléphone invalide")
        private String telephone;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuthResponse {
        private String accessToken;
        private String refreshToken;
        private String tokenType = "Bearer";
        private Long expiresIn;
        private UserInfo userInfo;

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class UserInfo {
            private Long id;
            private String nom;
            private String prenom;
            private String email;
            private String role;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RefreshTokenRequest {
        @NotBlank(message = "Le refresh token est obligatoire")
        private String refreshToken;
    }

    // Dans AuthDTO.java, ajoutez cette classe interne
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResetPasswordRequest {
        @NotBlank(message = "L'email est obligatoire")
        @Email(message = "Format d'email invalide")
        private String email;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResetPasswordResponse {
        private String message;
        private String email;
    }
}
