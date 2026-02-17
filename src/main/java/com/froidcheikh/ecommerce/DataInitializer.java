package com.froidcheikh.ecommerce;

import com.froidcheikh.ecommerce.entity.Administrateur;
import com.froidcheikh.ecommerce.repository.AdministrateurRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final AdministrateurRepository administrateurRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        initializeAdminUser();
    }

    private void initializeAdminUser() {
        // Supprimer tous les admins existants avec des mots de passe non hashés
        try {
            log.info("🧹 Nettoyage des admins avec mots de passe invalides...");
            administrateurRepository.deleteAll();
        } catch (Exception e) {
            log.warn("⚠️ Erreur lors du nettoyage: {}", e.getMessage());
        }

        // Créer le super administrateur par défaut
        String rawPassword = "admin123";
        String hashedPassword = passwordEncoder.encode(rawPassword);

        log.info("🔐 Création du mot de passe:");
        log.info("   Raw: {}", rawPassword);
        log.info("   Hashed: {}", hashedPassword);
        log.info("   Hashed length: {}", hashedPassword.length());
        log.info("   Starts with $2a: {}", hashedPassword.startsWith("$2a$"));

        Administrateur superAdmin = new Administrateur();
        superAdmin.setNom("Super");
        superAdmin.setPrenom("Admin");
        superAdmin.setEmail("admin@admin.com");
        superAdmin.setMotDePasse(hashedPassword);
        superAdmin.setRole(Administrateur.RoleAdmin.SUPER_ADMIN);
        superAdmin.setActif(true);

        superAdmin = administrateurRepository.save(superAdmin);

        log.info("✅ Super administrateur créé avec ID: {}", superAdmin.getIdAdmin());
        log.info("   📧 Email: admin@admin.com");
        log.info("   🔐 Mot de passe: admin123");
        log.info("   🔑 Hash stocké: {}", superAdmin.getMotDePasse().substring(0, 20) + "...");

        // Test immédiat du mot de passe
        boolean matches = passwordEncoder.matches(rawPassword, superAdmin.getMotDePasse());
        log.info("   ✅ Test de vérification: {}", matches);

        if (!matches) {
            log.error("❌ ERREUR: Le mot de passe hashé ne correspond pas !");
        }

        log.warn("⚠️  CHANGEZ CE MOT DE PASSE EN PRODUCTION !");

        // Créer un gestionnaire par défaut aussi
        String gestRawPassword = "gestionnaire123";
        String gestHashedPassword = passwordEncoder.encode(gestRawPassword);

        Administrateur gestionnaire = new Administrateur();
        gestionnaire.setNom("Gestionnaire");
        gestionnaire.setPrenom("Test");
        gestionnaire.setEmail("gestionnaire@admin.com");
        gestionnaire.setMotDePasse(gestHashedPassword);
        gestionnaire.setRole(Administrateur.RoleAdmin.GESTIONNAIRE);
        gestionnaire.setActif(true);

        gestionnaire = administrateurRepository.save(gestionnaire);

        log.info("✅ Gestionnaire créé avec ID: {}", gestionnaire.getIdAdmin());
        log.info("   📧 Email: gestionnaire@admin.com");
        log.info("   🔐 Mot de passe: gestionnaire123");

        // Test du gestionnaire aussi
        boolean gestMatches = passwordEncoder.matches(gestRawPassword, gestionnaire.getMotDePasse());
        log.info("   ✅ Test de vérification gestionnaire: {}", gestMatches);
    }
}