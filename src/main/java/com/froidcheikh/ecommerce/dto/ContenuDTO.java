package com.froidcheikh.ecommerce.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

public final class ContenuDTO {
    private ContenuDTO() {}

    @Data
    @NoArgsConstructor
    public static class ImageHeroDTO {
        private Long id;
        private String urlImage;
        private String altText;
        private String titre;
        private String sousTitre;
        private String lienCta;
        private String texteCta;
        private Integer ordre;
        private Boolean actif;
    }

    @Data
    @NoArgsConstructor
    public static class PartenaireDTO {
        private Long id;
        private String nom;
        private String description;
        private String urlLogo;
        private String urlSite;
        private Integer ordre;
        private Boolean actif;
    }

    @Data
    @NoArgsConstructor
    public static class PrestationDTO {
        private Long id;
        private String titre;
        private String description;
        private String urlImage;
        private List<String> caracteristiques;
        private Integer ordre;
        private Boolean actif;
    }

    @Data
    @NoArgsConstructor
    public static class MembreEquipeDTO {
        private Long id;
        private String nom;
        private String poste;
        private String experience;
        private String bio;
        private String urlPhoto;
        private List<String> competences;
        private String urlLinkedin;
        private String emailMembre;
        private Integer ordre;
        private Boolean actif;
    }

    @Data
    @NoArgsConstructor
    public static class RealisationDTO {
        private Long id;
        private String titre;
        private String categorie;
        private String localisation;
        private String description;
        private String clientNom;
        private String annee;
        private String duree;
        private String surface;
        private List<String> images;
        private List<String> tags;
        private Integer ordre;
        private Boolean actif;
    }

    @Data
    @NoArgsConstructor
    public static class OrdreUpdateRequest {
        private List<OrdreItem> items;

        @Data
        @NoArgsConstructor
        public static class OrdreItem {
            private Long id;
            private Integer ordre;
        }
    }
}
