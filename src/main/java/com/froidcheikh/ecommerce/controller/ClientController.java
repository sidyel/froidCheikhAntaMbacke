package com.froidcheikh.ecommerce.controller;

import com.froidcheikh.ecommerce.dto.ClientDTO;
import com.froidcheikh.ecommerce.service.ClientService;
import com.froidcheikh.ecommerce.service.ProduitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/clients")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class ClientController {

    private final ClientService clientService;
    private final ProduitService produitService;

    @GetMapping("/profile")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<ClientDTO> getProfile(Authentication authentication) {
        String email = authentication.getName();
        ClientDTO client = clientService.getClientByEmail(email);
        return ResponseEntity.ok(client);
    }

    @PutMapping("/profile")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<ClientDTO> updateProfile(
            @Valid @RequestBody ClientDTO clientDTO,
            Authentication authentication) {

        String email = authentication.getName();
        ClientDTO currentClient = clientService.getClientByEmail(email);
        ClientDTO updatedClient = clientService.updateClient(currentClient.getIdClient(), clientDTO);
        return ResponseEntity.ok(updatedClient);
    }

    @PatchMapping("/password")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<String> updatePassword(
            @RequestBody PasswordUpdateRequest request,
            Authentication authentication) {

        String email = authentication.getName();
        ClientDTO client = clientService.getClientByEmail(email);
        clientService.updatePassword(client.getIdClient(), request.getNouveauMotDePasse());
        return ResponseEntity.ok("Mot de passe mis à jour avec succès");
    }

    // Gestion des adresses
    @GetMapping("/adresses")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<List<ClientDTO.AdresseDTO>> getAdresses(Authentication authentication) {
        String email = authentication.getName();
        ClientDTO client = clientService.getClientByEmail(email);
        List<ClientDTO.AdresseDTO> adresses = clientService.getAdressesClient(client.getIdClient());
        return ResponseEntity.ok(adresses);
    }

    @PostMapping("/adresses")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<ClientDTO.AdresseDTO> ajouterAdresse(
            @Valid @RequestBody ClientDTO.AdresseDTO adresseDTO,
            Authentication authentication) {

        String email = authentication.getName();
        ClientDTO client = clientService.getClientByEmail(email);
        ClientDTO.AdresseDTO adresse = clientService.ajouterAdresse(client.getIdClient(), adresseDTO);
        return ResponseEntity.ok(adresse);
    }

    @PutMapping("/adresses/{adresseId}")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<ClientDTO.AdresseDTO> updateAdresse(
            @PathVariable Long adresseId,
            @Valid @RequestBody ClientDTO.AdresseDTO adresseDTO,
            Authentication authentication) {

        String email = authentication.getName();
        ClientDTO client = clientService.getClientByEmail(email);
        ClientDTO.AdresseDTO adresse = clientService.updateAdresse(
                client.getIdClient(), adresseId, adresseDTO);
        return ResponseEntity.ok(adresse);
    }

    @DeleteMapping("/adresses/{adresseId}")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<Void> supprimerAdresse(
            @PathVariable Long adresseId,
            Authentication authentication) {

        String email = authentication.getName();
        ClientDTO client = clientService.getClientByEmail(email);
        clientService.supprimerAdresse(client.getIdClient(), adresseId);
        return ResponseEntity.ok().build();
    }

    // Gestion de la wishlist
    @GetMapping("/wishlist")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<List<Long>> getWishlist(Authentication authentication) {
        String email = authentication.getName();
        ClientDTO client = clientService.getClientByEmail(email);
        List<Long> wishlist = clientService.getWishlist(client.getIdClient());
        return ResponseEntity.ok(wishlist);
    }

    @PostMapping("/wishlist/{produitId}")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<String> ajouterAWishlist(
            @PathVariable Long produitId,
            Authentication authentication) {

        String email = authentication.getName();
        ClientDTO client = clientService.getClientByEmail(email);
        clientService.ajouterAWishlist(client.getIdClient(), produitId);
        return ResponseEntity.ok("Produit ajouté à la wishlist");
    }

    @DeleteMapping("/wishlist/{produitId}")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<String> retirerDeWishlist(
            @PathVariable Long produitId,
            Authentication authentication) {

        String email = authentication.getName();
        ClientDTO client = clientService.getClientByEmail(email);
        clientService.retirerDeWishlist(client.getIdClient(), produitId);
        return ResponseEntity.ok("Produit retiré de la wishlist");
    }

    // Classes internes pour les requêtes
    public static class PasswordUpdateRequest {
        private String ancienMotDePasse;
        private String nouveauMotDePasse;

        // Getters et setters
        public String getAncienMotDePasse() { return ancienMotDePasse; }
        public void setAncienMotDePasse(String ancienMotDePasse) { this.ancienMotDePasse = ancienMotDePasse; }

        public String getNouveauMotDePasse() { return nouveauMotDePasse; }
        public void setNouveauMotDePasse(String nouveauMotDePasse) { this.nouveauMotDePasse = nouveauMotDePasse; }
    }
}