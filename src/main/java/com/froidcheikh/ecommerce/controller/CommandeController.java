package com.froidcheikh.ecommerce.controller;

import com.froidcheikh.ecommerce.dto.CommandeDTO;
import com.froidcheikh.ecommerce.entity.CompteurDocument.TypeDocument;
import com.froidcheikh.ecommerce.entity.DocumentFacturation.StatutDocument;
import com.froidcheikh.ecommerce.repository.DocumentFacturationRepository;
import com.froidcheikh.ecommerce.service.CommandeService;
import com.froidcheikh.ecommerce.service.ClientService;
import com.froidcheikh.ecommerce.service.DocumentFacturationService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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
    private final com.froidcheikh.ecommerce.service.SuiviRateLimiterService suiviRateLimiter;
    private final DocumentFacturationService documentFacturationService;

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

    // GET /invite/{numero} supprimé — remplacé par GET /suivi?numero=X&telephone=Y (sécurisé)

    @GetMapping("/suivi")
    public ResponseEntity<?> getSuiviCommande(
            @RequestParam String numero,
            @RequestParam String telephone,
            jakarta.servlet.http.HttpServletRequest request) {

        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank()) ip = request.getRemoteAddr();
        // Utiliser la première IP en cas de proxy chain
        if (ip.contains(",")) ip = ip.split(",")[0].trim();

        if (!suiviRateLimiter.tenterAcces(ip)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(Map.of(
                    "error", "Trop de tentatives", "message", "Réessayez dans 15 minutes"));
        }

        try {
            CommandeDTO suivi = commandeService.getSuiviCommande(numero, telephone);
            return ResponseEntity.ok(suivi);
        } catch (com.froidcheikh.ecommerce.exception.ResourceNotFoundException e) {
            // Réponse identique que la commande n'existe pas ou que le téléphone soit faux
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "error", "Commande non trouvée",
                    "message", "Aucune commande trouvée avec ce numéro et ce téléphone"));
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

    // Annulation par le client — uniquement sa propre commande et uniquement en EN_ATTENTE
    @PostMapping("/client/{commandeId}/annuler")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<?> annulerCommandeClient(
            @PathVariable Long commandeId,
            @RequestBody(required = false) Map<String, String> body,
            Authentication authentication) {

        try {
            String email = authentication.getName();
            Long clientId = clientService.getClientByEmail(email).getIdClient();
            String motif = (body != null) ? body.getOrDefault("motif", "") : "";
            CommandeDTO commande = commandeService.annulerCommandeClient(commandeId, clientId, motif);
            log.info("✅ Commande {} annulée par le client {}", commandeId, clientId);
            return ResponseEntity.ok(commande);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "error", "Annulation impossible", "message", e.getMessage()));
        } catch (org.springframework.security.access.AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "error", "Accès interdit", "message", e.getMessage()));
        } catch (Exception e) {
            log.error("❌ Erreur annulation client commande {}: ", commandeId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "error", "Erreur interne", "message", e.getMessage()));
        }
    }

    // Confirmation de paiement — admin uniquement
    @PostMapping("/{commandeId}/paiement")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'GESTIONNAIRE')")
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

    // Accès commande par ID — CLIENT (vérifie la propriété) ou ADMIN
    /** PDF de la facture émise pour une commande (client ou admin). */
    @GetMapping("/{commandeId}/facture/pdf")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> getFacturePdf(@PathVariable Long commandeId, Authentication authentication) {
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().matches("ROLE_ADMIN|ROLE_SUPER_ADMIN|ROLE_GESTIONNAIRE"));
        if (!isAdmin) {
            Long clientId = clientService.getClientByEmail(authentication.getName()).getIdClient();
            CommandeDTO commande = commandeService.getCommandeById(commandeId);
            if (commande.getClientId() == null || !commande.getClientId().equals(clientId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        }
        var factures = documentFacturationService.lister(TypeDocument.FACTURE, StatutDocument.EMIS, null,
                org.springframework.data.domain.PageRequest.of(0, 1,
                        org.springframework.data.domain.Sort.by("dateEmission").descending()));
        var factureOpt = factures.getContent().stream()
                .filter(f -> f.getId() != null).findFirst();
        if (factureOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        byte[] pdf = documentFacturationService.telechargerPdf(factureOpt.get().getId());
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"facture-commande-" + commandeId + ".pdf\"")
                .body(pdf);
    }

    @GetMapping("/{commandeId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getCommande(@PathVariable Long commandeId, Authentication authentication) {
        log.info("🔍 Recherche commande - ID: {}", commandeId);
        try {
            CommandeDTO commande = commandeService.getCommandeById(commandeId);

            boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().matches("ROLE_ADMIN|ROLE_SUPER_ADMIN|ROLE_GESTIONNAIRE"));
            if (!isAdmin) {
                // Pour un CLIENT, vérifie la propriété
                Long clientId = clientService.getClientByEmail(authentication.getName()).getIdClient();
                if (commande.getClientId() == null || !commande.getClientId().equals(clientId)) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                            "error", "Accès interdit", "message", "Vous n'avez pas accès à cette commande"));
                }
            }
            return ResponseEntity.ok(commande);
        } catch (Exception e) {
            log.error("❌ Erreur lors de la recherche de commande: ", e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "error", "Commande non trouvée", "message", "Aucune commande trouvée avec cet ID"));
        }
    }
}