package com.froidcheikh.ecommerce.dto;

import com.froidcheikh.ecommerce.entity.Commande;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommandeDTO {

    private Long idCommande;
    private String numeroCommande;
    private LocalDateTime dateCommande;

    // ✅ STATUT : Pas de validation - défini automatiquement par le backend
    private Commande.StatutCommande statutCommande;

    // ✅ MONTANT : Pas de validation - calculé automatiquement par le backend
    private BigDecimal montantTotal;

    // ✅ FRAIS : Pas de validation - calculés automatiquement par le backend
    private BigDecimal fraisLivraison;

    private String numeroSuivi;
    private String commentaire;

    // ✅ MODE LIVRAISON : Obligatoire
    @NotNull(message = "Le mode de livraison est obligatoire")
    private Commande.ModeLivraison modeLivraison;

    private LocalDateTime dateModification;

    // ✅ CLIENT : Défini automatiquement côté backend pour les clients authentifiés
    private Long clientId;

    // ✅ DONNÉES INVITÉ : Pas de validation ici - validées manuellement selon le type de commande
    // Ces champs sont NULL pour les clients authentifiés, remplis pour les invités
    private String emailInvite;
    private String nomInvite;
    private String prenomInvite;
    private String telephoneInvite;

    // ✅ ADRESSE DE LIVRAISON : Obligatoire pour tous
    @Valid
    @NotNull(message = "L'adresse de livraison est requise")
    private AdresseLivraisonDTO adresseLivraison;

    // ✅ LIGNES DE COMMANDE : Obligatoires pour tous
    @NotEmpty(message = "Une commande doit contenir au moins un produit")
    @Valid
    private List<LigneCommandeDTO> lignesCommande;

    // ✅ PAIEMENT : Optionnel au moment de la création
    @Valid
    private PaiementDTO paiement;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LigneCommandeDTO {
        private Long idLigneCommande;

        @NotNull(message = "L'ID du produit est requis")
        private Long produitId;

        @NotNull(message = "La quantité est requise")
        @Min(value = 1, message = "La quantité doit être au moins 1")
        private Integer quantite;

        // ✅ Prix et sous-total : Recalculés côté backend pour éviter les manipulations
        private BigDecimal prixUnitaire;
        private BigDecimal sousTotal;

        private String nomProduitCommande;
        private String refProduitCommande;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AdresseLivraisonDTO {
        @NotBlank(message = "Le nom est requis")
        private String nom;

        @NotBlank(message = "Le prénom est requis")
        private String prenom;

        @NotBlank(message = "L'adresse ligne 1 est requise")
        private String ligne1;

        private String ligne2;

        @NotBlank(message = "La ville est requise")
        private String ville;

        private String codePostal;

        @NotBlank(message = "Le téléphone est requis")
        private String telephone;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaiementDTO {
        private Long idPaiement;

        private String methodePaiement;
        private String statutPaiement;
        private BigDecimal montant;
        private LocalDateTime datePaiement;
        private String referencePaiement;
        private String referenceExterne;
    }
}