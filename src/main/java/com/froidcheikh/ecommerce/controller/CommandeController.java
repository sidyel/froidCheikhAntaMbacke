package com.froidcheikh.ecommerce.controller;

import com.froidcheikh.ecommerce.dto.CommandeDTO;
import com.froidcheikh.ecommerce.service.CommandeService;
import com.froidcheikh.ecommerce.service.ClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/commandes")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class CommandeController {

    private final CommandeService commandeService;
    private final ClientService clientService;

    // ===========================================
    // ENDPOINTS SPÉCIFIQUES (ORDRE IMPORTANT)
    // ===========================================

    // Endpoints publics pour commandes invité (PLACER EN PREMIER)
    @PostMapping("/invite")
    public ResponseEntity<?> creerCommandeInvite(
            @Valid @RequestBody CommandeDTO commandeDTO,
            BindingResult bindingResult) {

        log.info("📥 === DÉBUT CRÉATION COMMANDE INVITÉ ===");
        log.info("📧 Email invité: '{}'", commandeDTO.getEmailInvite());
        log.info("👤 Nom: '{}', Prénom: '{}'", commandeDTO.getNomInvite(), commandeDTO.getPrenomInvite());
        log.info("📞 Téléphone: '{}'", commandeDTO.getTelephoneInvite());
        log.debug("📋 Données complètes: {}", commandeDTO);

        // Validation des erreurs de binding
        if (bindingResult.hasErrors()) {
            log.error("❌ Erreurs de validation:");
            Map<String, String> errors = new HashMap<>();
            bindingResult.getFieldErrors().forEach(error -> {
                log.error("   - Champ '{}': {}", error.getField(), error.getDefaultMessage());
                errors.put(error.getField(), error.getDefaultMessage());
            });

            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Erreurs de validation",
                    "details", errors,
                    "message", "Les données fournies ne sont pas valides"
            ));
        }

        try {
            // Validation métier supplémentaire
            if (commandeDTO.getEmailInvite() == null || commandeDTO.getEmailInvite().trim().isEmpty()) {
                log.error("❌ Email invité manquant");
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Email requis",
                        "message", "L'email de l'invité est obligatoire"
                ));
            }

            if (commandeDTO.getLignesCommande() == null || commandeDTO.getLignesCommande().isEmpty()) {
                log.error("❌ Aucun article dans la commande");
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Articles manquants",
                        "message", "La commande doit contenir au moins un article"
                ));
            }

            log.info("✅ Validation réussie, création de la commande...");
            CommandeDTO commande = commandeService.creerCommandeInvite(commandeDTO);

            log.info("🎉 Commande créée avec succès - Numéro: {}, ID: {}",
                    commande.getNumeroCommande(), commande.getIdCommande());

            return ResponseEntity.ok(commande);

        } catch (IllegalArgumentException e) {
            log.error("❌ Argument invalide: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Données invalides",
                    "message", e.getMessage()
            ));

        } catch (Exception e) {
            log.error("❌ Erreur lors de la création de commande invité: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "error", "Erreur interne du serveur",
                    "message", "Une erreur inattendue s'est produite: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/invite/{numeroCommande}")
    public ResponseEntity<?> getCommandeInvite(@PathVariable String numeroCommande) {
        log.info("🔍 Recherche commande invité - Numéro: {}", numeroCommande);

        try {
            CommandeDTO commande = commandeService.getCommandeByNumero(numeroCommande);
            log.info("✅ Commande invité trouvée - ID: {}", commande.getIdCommande());
            return ResponseEntity.ok(commande);

        } catch (Exception e) {
            log.error("❌ Erreur lors de la recherche de commande invité: ", e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "error", "Commande non trouvée",
                    "message", "Aucune commande trouvée avec ce numéro: " + numeroCommande
            ));
        }
    }

    // Endpoints clients authentifiés
    @PostMapping("/client")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<?> creerCommandeClient(
            @Valid @RequestBody CommandeDTO commandeDTO,
            Authentication authentication,
            BindingResult bindingResult) {

        log.info("📥 Réception commande client - Utilisateur: {}", authentication.getName());

        if (bindingResult.hasErrors()) {
            log.error("❌ Erreurs de validation client:");
            Map<String, String> errors = new HashMap<>();
            bindingResult.getFieldErrors().forEach(error -> {
                errors.put(error.getField(), error.getDefaultMessage());
            });

            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Erreurs de validation",
                    "details", errors
            ));
        }

        try {
            String email = authentication.getName();
            Long clientId = clientService.getClientByEmail(email).getIdClient();

            log.info("✅ Client identifié - ID: {}, Email: {}", clientId, email);

            CommandeDTO commande = commandeService.creerCommandeClient(clientId, commandeDTO);

            log.info("🎉 Commande client créée - Numéro: {}, ID: {}",
                    commande.getNumeroCommande(), commande.getIdCommande());

            return ResponseEntity.ok(commande);

        } catch (Exception e) {
            log.error("❌ Erreur lors de la création de commande client: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "error", "Erreur interne du serveur",
                    "message", e.getMessage()
            ));
        }
    }

    @GetMapping("/client/mes-commandes")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<?> getMesCommandes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {

        try {
            String email = authentication.getName();
            Long clientId = clientService.getClientByEmail(email).getIdClient();

            Sort sort = Sort.by("dateCommande").descending();
            Pageable pageable = PageRequest.of(page, size, sort);

            Page<CommandeDTO> commandes = commandeService.getCommandesClient(clientId, pageable);
            log.info("✅ {} commandes trouvées pour le client ID: {}", commandes.getTotalElements(), clientId);

            return ResponseEntity.ok(commandes);

        } catch (Exception e) {
            log.error("❌ Erreur lors de la récupération des commandes client: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "error", "Erreur interne du serveur",
                    "message", e.getMessage()
            ));
        }
    }

    @GetMapping("/client/{commandeId}")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<?> getCommandeClient(
            @PathVariable Long commandeId,
            Authentication authentication) {

        try {
            CommandeDTO commande = commandeService.getCommandeById(commandeId);

            // Vérifier que la commande appartient bien au client authentifié
            String email = authentication.getName();
            Long clientId = clientService.getClientByEmail(email).getIdClient();

            if (!commande.getClientId().equals(clientId)) {
                log.warn("⚠️ Tentative d'accès non autorisé - Client: {}, Commande: {}", clientId, commandeId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                        "error", "Accès interdit",
                        "message", "Vous n'avez pas accès à cette commande"
                ));
            }

            return ResponseEntity.ok(commande);

        } catch (Exception e) {
            log.error("❌ Erreur lors de la récupération de commande client: ", e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "error", "Commande non trouvée",
                    "message", "Aucune commande trouvée avec cet ID"
            ));
        }
    }

    // Endpoint de recherche par numéro (AVANT les endpoints génériques)
    @GetMapping("/numero/{numeroCommande}")
    public ResponseEntity<?> getCommandeByNumero(@PathVariable String numeroCommande) {
        log.info("🔍 Recherche commande par numéro: {}", numeroCommande);

        try {
            CommandeDTO commande = commandeService.getCommandeByNumero(numeroCommande);
            return ResponseEntity.ok(commande);

        } catch (Exception e) {
            log.error("❌ Erreur lors de la recherche de commande par numéro: ", e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "error", "Commande non trouvée",
                    "message", "Aucune commande trouvée avec ce numéro"
            ));
        }
    }

    // Endpoints de paiement
    @PostMapping("/{commandeId}/paiement")
    public ResponseEntity<?> confirmerPaiement(
            @PathVariable Long commandeId,
            @Valid @RequestBody CommandeDTO.PaiementDTO paiementDTO,
            BindingResult bindingResult) {

        log.info("💳 Confirmation de paiement - Commande ID: {}, Méthode: {}",
                commandeId, paiementDTO.getMethodePaiement());

        if (bindingResult.hasErrors()) {
            log.error("❌ Erreurs de validation paiement:");
            Map<String, String> errors = new HashMap<>();
            bindingResult.getFieldErrors().forEach(error -> {
                errors.put(error.getField(), error.getDefaultMessage());
            });

            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Erreurs de validation",
                    "details", errors
            ));
        }

        try {
            CommandeDTO commande = commandeService.confirmerPaiement(commandeId, paiementDTO);
            log.info("✅ Paiement confirmé - Commande: {}", commande.getNumeroCommande());
            return ResponseEntity.ok(commande);

        } catch (Exception e) {
            log.error("❌ Erreur lors de la confirmation de paiement: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "error", "Erreur lors du paiement",
                    "message", e.getMessage()
            ));
        }
    }

    // ===========================================
    // ENDPOINTS GÉNÉRIQUES (PLACER À LA FIN)
    // ===========================================

    // Endpoint général par ID (PLACER EN DERNIER pour éviter les conflits)
    @GetMapping("/{commandeId}")
    @PreAuthorize("permitAll()") // ✅ Ajoutez cette ligne pour permettre l'accès public
    public ResponseEntity<?> getCommande(@PathVariable Long commandeId) {
        log.info("🔍 Recherche commande - ID: {}", commandeId);

        try {
            CommandeDTO commande = commandeService.getCommandeById(commandeId);
            return ResponseEntity.ok(commande);

        } catch (Exception e) {
            log.error("❌ Erreur lors de la recherche de commande: ", e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "error", "Commande non trouvée",
                    "message", "Aucune commande trouvée avec cet ID"
            ));
        }
    }
}