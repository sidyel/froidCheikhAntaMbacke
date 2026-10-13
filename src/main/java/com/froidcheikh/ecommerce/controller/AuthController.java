package com.froidcheikh.ecommerce.controller;

import com.froidcheikh.ecommerce.dto.AuthDTO;
import com.froidcheikh.ecommerce.entity.Administrateur;
import com.froidcheikh.ecommerce.entity.ParametresSite;
import com.froidcheikh.ecommerce.repository.AdministrateurRepository;
import com.froidcheikh.ecommerce.service.AuthService;
import com.froidcheikh.ecommerce.service.ParametresSiteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;
    private final AdministrateurRepository administrateurRepository;
    private final ParametresSiteService parametresSiteService;
    private final PasswordEncoder passwordEncoder;

    @Value("${INSTALL_TOKEN:}")
    private String installToken;

    /** Verrou JVM contre deux appels simultanés à /install */
    private static final Object INSTALL_LOCK = new Object();

    @PostMapping("/login")
    public ResponseEntity<AuthDTO.AuthResponse> login(@Valid @RequestBody AuthDTO.LoginRequest loginRequest) {
        log.info("Tentative de connexion pour : {}", loginRequest.getEmail());
        AuthDTO.AuthResponse response = authService.login(loginRequest);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    public ResponseEntity<AuthDTO.AuthResponse> register(@Valid @RequestBody AuthDTO.RegisterRequest registerRequest) {
        log.info("Tentative d'inscription pour : {}", registerRequest.getEmail());
        AuthDTO.AuthResponse response = authService.register(registerRequest);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthDTO.AuthResponse> refreshToken(@Valid @RequestBody AuthDTO.RefreshTokenRequest refreshRequest) {
        AuthDTO.AuthResponse response = authService.refreshToken(refreshRequest);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout() {
        // Dans une implémentation complète, vous pourriez blacklister le token
        return ResponseEntity.ok("Déconnexion réussie");
    }

    @PostMapping("/reset-password")
    public ResponseEntity<AuthDTO.ResetPasswordResponse> resetPassword(
            @Valid @RequestBody AuthDTO.ResetPasswordRequest request) {
        log.info("Demande de réinitialisation de mot de passe pour : {}", request.getEmail());
        AuthDTO.ResetPasswordResponse response = authService.resetPassword(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Installation initiale : crée le premier SUPER_ADMIN et la config du site.
     * Refuse (403) si un SUPER_ADMIN existe déjà.
     * Protégé par INSTALL_TOKEN pour éviter toute invocation non autorisée.
     */
    @PostMapping("/install")
    public ResponseEntity<?> install(
            @RequestHeader(value = "X-Install-Token", required = false) String providedToken,
            @Valid @RequestBody AuthDTO.InstallRequest request) {

        // Vérification du jeton d'installation
        if (installToken == null || installToken.isBlank()) {
            log.warn("Tentative d'installation sans INSTALL_TOKEN configuré");
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Installation désactivée : INSTALL_TOKEN non configuré"));
        }
        if (!installToken.equals(providedToken)) {
            log.warn("Tentative d'installation avec un jeton invalide");
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Jeton d'installation invalide"));
        }

        synchronized (INSTALL_LOCK) {
            boolean superAdminExists = administrateurRepository.findAll().stream()
                    .anyMatch(a -> Administrateur.RoleAdmin.SUPER_ADMIN.equals(a.getRole()));

            if (superAdminExists) {
                log.warn("Tentative d'installation sur un site déjà installé");
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Le site est déjà installé"));
            }

            Administrateur superAdmin = new Administrateur();
            superAdmin.setNom(request.getNom());
            superAdmin.setPrenom(request.getPrenom());
            superAdmin.setEmail(request.getEmail());
            superAdmin.setMotDePasse(passwordEncoder.encode(request.getMotDePasse()));
            superAdmin.setRole(Administrateur.RoleAdmin.SUPER_ADMIN);
            superAdmin.setActif(true);
            administrateurRepository.save(superAdmin);

            if (request.getNomSite() != null && !request.getNomSite().isBlank()) {
                parametresSiteService.initDefault(
                        request.getNomSite(),
                        request.getSloganSite(),
                        request.getEmailContact());
            }

            log.info("Installation réussie — SUPER_ADMIN créé : {}", request.getEmail());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of("message", "Installation réussie", "email", request.getEmail()));
        }
    }
}