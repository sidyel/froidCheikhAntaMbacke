package com.froidcheikh.ecommerce.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

public final class ClientFacturationDTO {
    private ClientFacturationDTO() {}

    @Data @NoArgsConstructor
    public static class CreateRequest {
        @NotBlank
        private String nom;
        private String prenom;
        private String telephone;
        private String email;
        private String adresse;
        private String ninea;
        private String rccm;
    }

    @Data @NoArgsConstructor
    public static class Response {
        private Long id;
        private String nom;
        private String prenom;
        private String telephone;
        private String email;
        private String adresse;
        private String ninea;
        private String rccm;
        private LocalDateTime dateCreation;
    }
}
