package com.froidcheikh.ecommerce.controller;

import com.froidcheikh.ecommerce.dto.AuthDTO;
import com.froidcheikh.ecommerce.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthService authService;

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

    // Ajoutez cette méthode dans AuthController.java
    @PostMapping("/reset-password")
    public ResponseEntity<AuthDTO.ResetPasswordResponse> resetPassword(
            @Valid @RequestBody AuthDTO.ResetPasswordRequest request) {
        log.info("Demande de réinitialisation de mot de passe pour : {}", request.getEmail());
        AuthDTO.ResetPasswordResponse response = authService.resetPassword(request);
        return ResponseEntity.ok(response);
    }
}