package com.froidcheikh.ecommerce.controller;

import com.froidcheikh.ecommerce.dto.CommandeDTO;
import com.froidcheikh.ecommerce.dto.ProduitDTO;
import com.froidcheikh.ecommerce.dto.ClientDTO;
import com.froidcheikh.ecommerce.entity.Client;
import com.froidcheikh.ecommerce.entity.Commande;
import com.froidcheikh.ecommerce.service.*;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'GESTIONNAIRE')")
public class AdminController {

    private final CommandeService commandeService;
    private final ProduitService produitService;
    private final ClientService clientService;
    private final StatistiquesService statistiquesService;

    // Dashboard et statistiques
    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboard() {
        Map<String, Object> dashboard = new HashMap<>();

        dashboard.put("statistiques", statistiquesService.getStatistiquesGenerales());
        dashboard.put("commandesRecentes", statistiquesService.getCommandesRecentes());
        dashboard.put("produitsStockFaible", produitService.getProduitsStockFaible(5));
        dashboard.put("ventesParJour", statistiquesService.getVentesParJour(30));

        return ResponseEntity.ok(dashboard);
    }

    @GetMapping("/admin1/produits")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'GESTIONNAIRE')")
    public ResponseEntity<Page<ProduitDTO>> getAllProduitsAdmin(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long categorieId,
            @RequestParam(required = false) Long marqueId,
            @RequestParam(required = false) String stockFilter,
            @RequestParam(defaultValue = "dateAjout") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("desc") ?
                Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        // Récupérer TOUS les produits (disponibles ET non disponibles)
        Page<ProduitDTO> produits = produitService.getAllProduitsAdmin(
                search, categorieId, marqueId, stockFilter, pageable);

        return ResponseEntity.ok(produits);
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStatistiques(
            @RequestParam(required = false) String periode) {

        Map<String, Object> stats = statistiquesService.getStatistiquesDetaillees(periode);
        return ResponseEntity.ok(stats);
    }

    // Gestion des commandes
    @GetMapping("/commandes1")
    public ResponseEntity<Page<CommandeDTO>> getAllCommandes1(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String statut) {

        Sort sort = Sort.by("dateCommande").descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<CommandeDTO> commandes;
        if (statut != null) {
            Commande.StatutCommande statutCommande = Commande.StatutCommande.valueOf(statut.toUpperCase());
            commandes = commandeService.getCommandesByStatut(statutCommande, pageable);
        } else {
            // Créer une méthode dans CommandeService pour récupérer toutes les commandes
            commandes = commandeService.getAllCommandes(pageable);
        }

        return ResponseEntity.ok(commandes);
    }

    @PatchMapping("/commandes1/{commandeId}/statut")
    public ResponseEntity<CommandeDTO> updateStatutCommande(
            @PathVariable Long commandeId,
            @RequestBody StatutUpdateRequest request) {

        Commande.StatutCommande nouveauStatut = Commande.StatutCommande.valueOf(request.getStatut().toUpperCase());
        CommandeDTO commande = commandeService.updateStatutCommande(commandeId, nouveauStatut);
        return ResponseEntity.ok(commande);
    }

    @PostMapping("/commandes1/{commandeId}/annuler")
    public ResponseEntity<CommandeDTO> annulerCommande(
            @PathVariable Long commandeId,
            @RequestBody AnnulationRequest request) {

        CommandeDTO commande = commandeService.annulerCommande(commandeId, request.getMotif());
        return ResponseEntity.ok(commande);
    }

    // Gestion des clients
    @GetMapping("/clients")
    public ResponseEntity<Page<ClientDTO>> getAllClients(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        // Implémenter getAllClients dans ClientService
        Page<ClientDTO> clients = clientService.getAllClients(PageRequest.of(page, size));
        return ResponseEntity.ok(clients);
    }

    @GetMapping("/clients/{clientId}")
    public ResponseEntity<ClientDTO> getClientById(@PathVariable Long clientId) {
        ClientDTO client = clientService.getClientById(clientId);
        return ResponseEntity.ok(client);
    }

    @PatchMapping("/clients/{clientId}/activer")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<String> activerClient(@PathVariable Long clientId) {
        clientService.activerClient(clientId);
        return ResponseEntity.ok("Client activé avec succès");
    }

    @PatchMapping("/clients/{clientId}/desactiver")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<String> desactiverClient(@PathVariable Long clientId) {
        clientService.desactiverClient(clientId);
        return ResponseEntity.ok("Client désactivé avec succès");
    }

    @PostMapping("/clients")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ClientDTO> createClient(@Valid @RequestBody CreateClientRequest request) {
        ClientDTO client = clientService.createClientByAdmin(request);
        return ResponseEntity.ok(client);
    }

    @PutMapping("/clients/{clientId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ClientDTO> updateClientByAdmin(
            @PathVariable Long clientId,
            @Valid @RequestBody UpdateClientRequest request) {

        ClientDTO client = clientService.updateClientByAdmin(clientId, request);
        return ResponseEntity.ok(client);
    }

    @DeleteMapping("/clients/{clientId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<String> deleteClient(@PathVariable Long clientId) {
        clientService.deleteClient(clientId);
        return ResponseEntity.ok("Client supprimé avec succès");
    }

    @GetMapping("/clients/search")
    public ResponseEntity<Page<ClientDTO>> searchClients(
            @RequestParam String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("dateCreation").descending());
        Page<ClientDTO> clients = clientService.searchClients(search, pageable);
        return ResponseEntity.ok(clients);
    }

    @GetMapping("/clients/{clientId}/stats")
    public ResponseEntity<Map<String, Object>> getClientStats(@PathVariable Long clientId) {
        Map<String, Object> stats = clientService.getClientStatistics(clientId);
        return ResponseEntity.ok(stats);
    }

    @PostMapping("/clients/batch-activate")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> batchActivateClients(@RequestBody List<Long> clientIds) {
        Map<String, Object> result = clientService.batchActivateClients(clientIds);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/clients/batch-deactivate")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> batchDeactivateClients(@RequestBody List<Long> clientIds) {
        Map<String, Object> result = clientService.batchDeactivateClients(clientIds);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/clients/batch-delete")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> batchDeleteClients(@RequestBody List<Long> clientIds) {
        Map<String, Object> result = clientService.batchDeleteClients(clientIds);
        return ResponseEntity.ok(result);
    }


    // Gestion du stock
    @GetMapping("/stock/faible")
    public ResponseEntity<List<ProduitDTO>> getProduitsStockFaible(
            @RequestParam(defaultValue = "5") Integer seuil) {
        List<ProduitDTO> produits = produitService.getProduitsStockFaible(seuil);
        return ResponseEntity.ok(produits);
    }

    @PostMapping("/stock/alerte")
    public ResponseEntity<String> envoyerAlerteStock() {
        // Implémenter l'envoi d'alertes par email
        return ResponseEntity.ok("Alertes de stock envoyées");
    }

    // Rapports
    @GetMapping("/rapports/ventes")
    public ResponseEntity<Object> getRapportVentes(
            @RequestParam String dateDebut,
            @RequestParam String dateFin) {

        Object rapport = statistiquesService.getRapportVentes(dateDebut, dateFin);
        return ResponseEntity.ok(rapport);
    }

    @GetMapping("/rapports/produits-populaires")
    public ResponseEntity<List<Object>> getProduitsPopulaires(
            @RequestParam(defaultValue = "30") int jours) {

        List<Object> produits = statistiquesService.getProduitsPopulaires(jours);
        return ResponseEntity.ok(produits);
    }

    // Classes internes pour les requêtes
    public static class StatutUpdateRequest {
        private String statut;

        public String getStatut() { return statut; }
        public void setStatut(String statut) { this.statut = statut; }
    }

    public static class AnnulationRequest {
        private String motif;

        public String getMotif() { return motif; }
        public void setMotif(String motif) { this.motif = motif; }
    }

    public static class CreateClientRequest {
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

        private String dateNaissance;

        @Enumerated(EnumType.STRING)
        private Client.Genre genre;

        private Boolean actif = true;

        // Getters et setters
        public String getNom() { return nom; }
        public void setNom(String nom) { this.nom = nom; }

        public String getPrenom() { return prenom; }
        public void setPrenom(String prenom) { this.prenom = prenom; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getMotDePasse() { return motDePasse; }
        public void setMotDePasse(String motDePasse) { this.motDePasse = motDePasse; }

        public String getTelephone() { return telephone; }
        public void setTelephone(String telephone) { this.telephone = telephone; }

        public String getDateNaissance() { return dateNaissance; }
        public void setDateNaissance(String dateNaissance) { this.dateNaissance = dateNaissance; }

        public Client.Genre getGenre() { return genre; }
        public void setGenre(Client.Genre genre) { this.genre = genre; }

        public Boolean getActif() { return actif; }
        public void setActif(Boolean actif) { this.actif = actif; }
    }

    public static class UpdateClientRequest {
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

        @Enumerated(EnumType.STRING)
        private Client.Genre genre;

        private Boolean actif;

        // Getters et setters
        public String getNom() { return nom; }
        public void setNom(String nom) { this.nom = nom; }

        public String getPrenom() { return prenom; }
        public void setPrenom(String prenom) { this.prenom = prenom; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getTelephone() { return telephone; }
        public void setTelephone(String telephone) { this.telephone = telephone; }

        public String getDateNaissance() { return dateNaissance; }
        public void setDateNaissance(String dateNaissance) { this.dateNaissance = dateNaissance; }

        public Client.Genre getGenre() { return genre; }
        public void setGenre(Client.Genre genre) { this.genre = genre; }

        public Boolean getActif() { return actif; }
        public void setActif(Boolean actif) { this.actif = actif; }
    }

    /**
     * Récupérer toutes les commandes avec pagination et filtrage
     */
    @GetMapping("/commandes")
    public ResponseEntity<?> getAllCommandes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String statut) {

        log.info("📋 Admin - Récupération commandes - Page: {}, Size: {}, Statut: {}", page, size, statut);

        try {
            Sort sort = Sort.by("dateCommande").descending();
            Pageable pageable = PageRequest.of(page, size, sort);

            Commande.StatutCommande statutEnum = null;
            if (statut != null && !statut.trim().isEmpty()) {
                try {
                    statutEnum = Commande.StatutCommande.valueOf(statut.toUpperCase());
                } catch (IllegalArgumentException e) {
                    log.warn("⚠️ Statut invalide reçu: {}", statut);
                    return ResponseEntity.badRequest().body(Map.of(
                            "error", "Statut invalide",
                            "message", "Le statut fourni n'est pas valide: " + statut
                    ));
                }
            }

            Page<CommandeDTO> commandes = commandeService.getAllCommandesAdmin(pageable, statutEnum);

            log.info("✅ {} commandes récupérées sur {} au total",
                    commandes.getNumberOfElements(), commandes.getTotalElements());

            return ResponseEntity.ok(commandes);

        } catch (Exception e) {
            log.error("❌ Erreur lors de la récupération des commandes admin: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "error", "Erreur interne du serveur",
                    "message", "Impossible de récupérer les commandes: " + e.getMessage()
            ));
        }
    }

    /**
     * Récupérer une commande par son ID
     */
    @GetMapping("/commandes/{commandeId}")
    public ResponseEntity<?> getCommandeById(@PathVariable Long commandeId) {
        log.info("🔍 Admin - Récupération commande - ID: {}", commandeId);

        try {
            CommandeDTO commande = commandeService.getCommandeById(commandeId);
            return ResponseEntity.ok(commande);

        } catch (Exception e) {
            log.error("❌ Erreur lors de la récupération de la commande ID {}: ", commandeId, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "error", "Commande non trouvée",
                    "message", "Aucune commande trouvée avec l'ID: " + commandeId
            ));
        }
    }

    /**
     * Mettre à jour le statut d'une commande
     */
    @PatchMapping("/commandes/{commandeId}/statut")
    public ResponseEntity<?> updateStatutCommande(
            @PathVariable Long commandeId,
            @RequestBody Map<String, String> request) {

        String nouveauStatut = request.get("statut");
        log.info("🔄 Admin - Mise à jour statut commande - ID: {}, Nouveau statut: {}",
                commandeId, nouveauStatut);

        if (nouveauStatut == null || nouveauStatut.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Statut manquant",
                    "message", "Le statut est obligatoire"
            ));
        }

        try {
            Commande.StatutCommande statutEnum = Commande.StatutCommande.valueOf(nouveauStatut.toUpperCase());
            CommandeDTO commande = commandeService.updateStatutCommande(commandeId, statutEnum);

            log.info("✅ Statut commande mis à jour - ID: {}, Nouveau statut: {}",
                    commandeId, statutEnum);

            return ResponseEntity.ok(commande);

        } catch (IllegalArgumentException e) {
            log.error("❌ Statut invalide: {}", nouveauStatut);
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Statut invalide",
                    "message", "Le statut fourni n'est pas valide: " + nouveauStatut
            ));

        } catch (IllegalStateException e) {
            log.error("❌ Changement de statut non autorisé: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Changement non autorisé",
                    "message", e.getMessage()
            ));

        } catch (Exception e) {
            log.error("❌ Erreur lors de la mise à jour du statut de la commande ID {}: ", commandeId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "error", "Erreur interne du serveur",
                    "message", "Impossible de mettre à jour le statut: " + e.getMessage()
            ));
        }
    }

    /**
     * Annuler une commande avec motif
     */
    @PostMapping("/commandes/{commandeId}/annuler")
    public ResponseEntity<?> annulerCommande(
            @PathVariable Long commandeId,
            @RequestBody Map<String, String> request) {

        String motif = request.get("motif");
        log.info("❌ Admin - Annulation commande - ID: {}, Motif: {}", commandeId, motif);

        if (motif == null || motif.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Motif manquant",
                    "message", "Le motif d'annulation est obligatoire"
            ));
        }

        try {
            CommandeDTO commande = commandeService.annulerCommande(commandeId, motif.trim());

            log.info("✅ Commande annulée - ID: {}", commandeId);

            return ResponseEntity.ok(commande);

        } catch (IllegalStateException e) {
            log.error("❌ Annulation non autorisée: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Annulation non autorisée",
                    "message", e.getMessage()
            ));

        } catch (Exception e) {
            log.error("❌ Erreur lors de l'annulation de la commande ID {}: ", commandeId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "error", "Erreur interne du serveur",
                    "message", "Impossible d'annuler la commande: " + e.getMessage()
            ));
        }
    }

    /**
     * Supprimer définitivement une commande (uniquement si annulée)
     */
    @DeleteMapping("/commandes/{commandeId}")
    public ResponseEntity<?> supprimerCommande(@PathVariable Long commandeId) {
        log.info("🗑️ Admin - Suppression commande - ID: {}", commandeId);

        try {
            commandeService.supprimerCommande(commandeId);

            log.info("✅ Commande supprimée - ID: {}", commandeId);

            return ResponseEntity.ok(Map.of(
                    "message", "Commande supprimée avec succès",
                    "commandeId", commandeId
            ));

        } catch (IllegalStateException e) {
            log.error("❌ Suppression non autorisée: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Suppression non autorisée",
                    "message", e.getMessage()
            ));

        } catch (Exception e) {
            log.error("❌ Erreur lors de la suppression de la commande ID {}: ", commandeId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "error", "Erreur interne du serveur",
                    "message", "Impossible de supprimer la commande: " + e.getMessage()
            ));
        }
    }

    // ============== STATISTIQUES COMMANDES ==============

    /**
     * Obtenir les statistiques des commandes
     */
    @GetMapping("/commandes/statistiques")
    public ResponseEntity<?> getStatistiquesCommandes() {
        log.info("📊 Admin - Récupération statistiques commandes");

        try {
            // Vous pouvez implémenter cette méthode dans le service
            // Map<String, Object> stats = commandeService.getStatistiquesCommandes();

            // Pour l'instant, retourner un placeholder
            Map<String, Object> stats = Map.of(
                    "totalCommandes", 0,
                    "commandesEnAttente", 0,
                    "commandesLivrees", 0,
                    "chiffreAffaires", 0
            );

            return ResponseEntity.ok(stats);

        } catch (Exception e) {
            log.error("❌ Erreur lors de la récupération des statistiques: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "error", "Erreur interne du serveur",
                    "message", "Impossible de récupérer les statistiques"
            ));
        }
    }
}
