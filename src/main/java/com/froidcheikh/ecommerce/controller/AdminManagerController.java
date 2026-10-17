package com.froidcheikh.ecommerce.controller;

import com.froidcheikh.ecommerce.entity.Administrateur;
import com.froidcheikh.ecommerce.repository.AdministrateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@Controller
@RequestMapping("/manage")
@RequiredArgsConstructor
public class AdminManagerController {

    @Value("${ADMIN_MANAGER_TOKEN:}")
    private String managerToken;

    private final AdministrateurRepository adminRepository;
    private final PasswordEncoder passwordEncoder;

    private boolean tokenValide(String token) {
        return managerToken != null && !managerToken.isBlank() && managerToken.equals(token);
    }

    // ─── Page HTML ─────────────────────────────────────────────────────────────

    @GetMapping("/admins")
    public String page(@RequestParam(value = "token", defaultValue = "") String token) {
        if (!tokenValide(token)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Token invalide");
        return "admin-manager";
    }

    // ─── API REST ──────────────────────────────────────────────────────────────

    @GetMapping(value = "/api/admins", produces = "application/json")
    @ResponseBody
    public ResponseEntity<?> liste(@RequestParam("token") String token) {
        if (!tokenValide(token)) return ResponseEntity.status(403).build();
        return ResponseEntity.ok(adminRepository.findAll().stream().map(this::toDto).toList());
    }

    @PostMapping(value = "/api/admins", consumes = "application/json", produces = "application/json")
    @ResponseBody
    public ResponseEntity<?> creer(@RequestParam("token") String token, @RequestBody AdminForm form) {
        if (!tokenValide(token)) return ResponseEntity.status(403).build();
        if (form.email() == null || form.email().isBlank())
            return ResponseEntity.badRequest().body(Map.of("erreur", "Email requis"));
        if (adminRepository.existsByEmail(form.email()))
            return ResponseEntity.badRequest().body(Map.of("erreur", "Email déjà utilisé"));
        if (form.motDePasse() == null || form.motDePasse().length() < 6)
            return ResponseEntity.badRequest().body(Map.of("erreur", "Mot de passe trop court (min 6 caractères)"));

        Administrateur admin = new Administrateur();
        admin.setNom(form.nom());
        admin.setPrenom(form.prenom());
        admin.setEmail(form.email());
        admin.setMotDePasse(passwordEncoder.encode(form.motDePasse()));
        try {
            admin.setRole(Administrateur.RoleAdmin.valueOf(form.role()));
        } catch (Exception e) {
            admin.setRole(Administrateur.RoleAdmin.ADMIN);
        }
        admin.setActif(true);
        return ResponseEntity.ok(toDto(adminRepository.save(admin)));
    }

    @PutMapping(value = "/api/admins/{id}", consumes = "application/json", produces = "application/json")
    @ResponseBody
    public ResponseEntity<?> modifier(@RequestParam("token") String token,
                                      @PathVariable Long id,
                                      @RequestBody AdminUpdateForm form) {
        if (!tokenValide(token)) return ResponseEntity.status(403).build();
        return adminRepository.findById(id).map(admin -> {
            if (form.nom() != null && !form.nom().isBlank())      admin.setNom(form.nom());
            if (form.prenom() != null && !form.prenom().isBlank()) admin.setPrenom(form.prenom());
            if (form.email() != null && !form.email().isBlank())   admin.setEmail(form.email());
            if (form.role() != null) {
                try { admin.setRole(Administrateur.RoleAdmin.valueOf(form.role())); } catch (Exception ignored) {}
            }
            if (form.actif() != null) admin.setActif(form.actif());
            if (form.motDePasse() != null && !form.motDePasse().isBlank())
                admin.setMotDePasse(passwordEncoder.encode(form.motDePasse()));
            return ResponseEntity.ok(toDto(adminRepository.save(admin)));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping(value = "/api/admins/{id}", produces = "application/json")
    @ResponseBody
    public ResponseEntity<?> supprimer(@RequestParam("token") String token, @PathVariable Long id) {
        if (!tokenValide(token)) return ResponseEntity.status(403).build();
        if (!adminRepository.existsById(id)) return ResponseEntity.notFound().build();
        adminRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // ─── DTOs ─────────────────────────────────────────────────────────────────

    record AdminDto(Long id, String nom, String prenom, String email, String role, boolean actif, String dateCreation) {}
    record AdminForm(String nom, String prenom, String email, String motDePasse, String role) {}
    record AdminUpdateForm(String nom, String prenom, String email, String motDePasse, String role, Boolean actif) {}

    private AdminDto toDto(Administrateur a) {
        return new AdminDto(
                a.getIdAdmin(), a.getNom(), a.getPrenom(), a.getEmail(),
                a.getRole().name(), Boolean.TRUE.equals(a.getActif()),
                a.getDateCreation() != null ? a.getDateCreation().toLocalDate().toString() : null
        );
    }
}
