package com.froidcheikh.ecommerce;

import com.froidcheikh.ecommerce.entity.Administrateur;
import com.froidcheikh.ecommerce.repository.AdministrateurRepository;
import com.froidcheikh.ecommerce.service.ParametresSiteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final AdministrateurRepository administrateurRepository;
    private final ParametresSiteService parametresSiteService;

    @Override
    public void run(String... args) {
        boolean superAdminExists = administrateurRepository.findAll().stream()
                .anyMatch(a -> Administrateur.RoleAdmin.SUPER_ADMIN.equals(a.getRole()));

        if (!superAdminExists) {
            // Mode wizard — aucun SUPER_ADMIN, le frontend affichera l'assistant d'installation
            log.info("Aucun SUPER_ADMIN trouvé — mode wizard d'installation actif");
            return;
        }

        // SUPER_ADMIN présent mais pas encore de config : insérer les valeurs Froid Cheikh
        // pour assurer la compatibilité avec la base existante
        if (!parametresSiteService.exists()) {
            log.info("SUPER_ADMIN présent sans ParametresSite — insertion de la config par défaut");
            parametresSiteService.initDefault(
                    "Froid Cheikh Anta Mbacké",
                    "Spécialiste de la climatisation et du froid au Sénégal",
                    "froidcheikhantambacke.dione65@gmail.com",
                    15
            );
        }
    }
}
