package com.froidcheikh.ecommerce;

import com.froidcheikh.ecommerce.entity.*;
import com.froidcheikh.ecommerce.repository.*;
import com.froidcheikh.ecommerce.service.ParametresSiteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final AdministrateurRepository administrateurRepository;
    private final ParametresSiteService parametresSiteService;
    private final ImageHeroRepository imageHeroRepository;
    private final PartenaireRepository partenaireRepository;
    private final PrestationRepository prestationRepository;
    private final MembreEquipeRepository membreEquipeRepository;
    private final RealisationRepository realisationRepository;
    private final ZoneLivraisonRepository zoneLivraisonRepository;

    @Override
    public void run(String... args) {
        boolean superAdminExists = administrateurRepository.findAll().stream()
                .anyMatch(a -> Administrateur.RoleAdmin.SUPER_ADMIN.equals(a.getRole()));

        if (!superAdminExists) {
            log.info("Aucun SUPER_ADMIN trouvé — mode wizard d'installation actif");
            return;
        }

        if (!parametresSiteService.exists()) {
            log.info("SUPER_ADMIN présent sans ParametresSite — insertion de la config par défaut");
            parametresSiteService.initDefault(
                    "Froid Cheikh Anta Mbacké",
                    "Spécialiste de la climatisation et du froid au Sénégal",
                    "froidcheikhantambacke.dione65@gmail.com",
                    15
            );
        }

        if (imageHeroRepository.count() == 0) {
            log.info("Seed ImageHero...");
            seedHeroImages();
        }
        if (partenaireRepository.count() == 0) {
            log.info("Seed Partenaires...");
            seedPartenaires();
        }
        if (prestationRepository.count() == 0) {
            log.info("Seed Prestations...");
            seedPrestations();
        }
        if (membreEquipeRepository.count() == 0) {
            log.info("Seed MembreEquipe...");
            seedMembres();
        }
        if (realisationRepository.count() == 0) {
            log.info("Seed Realisations...");
            seedRealisations();
        }
        if (zoneLivraisonRepository.count() == 0) {
            log.info("Seed ZonesLivraison...");
            seedZonesLivraison();
        }
    }

    private void seedHeroImages() {
        createHero("assets/images/p1.jpg", "Électroménager moderne", 0);
        createHero("assets/images/im.jpg", "Climatiseurs professionnels", 1);
        createHero("assets/images/im2.jpg", "Équipements de réfrigération", 2);
    }

    private void createHero(String url, String alt, int ordre) {
        ImageHero h = new ImageHero();
        h.setUrlImage(url);
        h.setAltText(alt);
        h.setOrdre(ordre);
        h.setActif(true);
        imageHeroRepository.save(h);
    }

    private void seedPartenaires() {
        createPartenaire("Hopital Fann", "/assets/images/partners/fann.jpeg",
                "Contrat de prestation de service de maintenance et de reparation de toute les appareil froid", 0);
        createPartenaire("Air Senegal", "/assets/images/partners/airSenegal.jpeg",
                "Installation de 13 climatisseur cassete et contrat d'entretient des materiel froid", 1);
        createPartenaire("petrosen", "/assets/images/partners/petrosen.png",
                "Contrat de prestation de service de maintenance et de reparation de toute les appareil froid", 2);
        createPartenaire("hogip", "/assets/images/partners/hogip.jpeg",
                "Contrat de prestation de service de maintenance et de reparation de toute les appareil froid", 3);
        createPartenaire("Magic Land", "/assets/images/partners/magic.jpeg",
                "Contrat de maintenance et de reparation de toute les appareil froid et installation de mini Centrale dans les sale cinemas", 4);
        createPartenaire("Air Liquide", "/assets/images/partners/airLiquide.png",
                "installation de 3 chambres froides negatif", 5);
    }

    private void createPartenaire(String nom, String logo, String description, int ordre) {
        Partenaire p = new Partenaire();
        p.setNom(nom);
        p.setUrlLogo(logo);
        p.setDescription(description);
        p.setOrdre(ordre);
        p.setActif(true);
        partenaireRepository.save(p);
    }

    private void seedPrestations() {
        createPrestation("Vente d'Équipements", "/assets/images/services/vente.png",
                "Large gamme de climatiseurs et équipements de réfrigération des meilleures marques",
                List.of("Climatiseurs split et multi-split", "Systèmes VRV/VRF et chambre froide",
                        "Climatiseur Armoire et casette", "Équipements de réfrigération commerciale"), 0);
        createPrestation("Installation Professionnelle", "/assets/images/services/installation.jpg",
                "Installation certifiée par des techniciens qualifiés respectant les normes internationales",
                List.of("Étude technique préalable", "Installation selon normes NF",
                        "Test de performance", "Formation à l'utilisation"), 1);
        createPrestation("Maintenance & Réparation", "/assets/images/services/maintenance.jpg",
                "Service après-vente complet pour garantir la longévité de vos équipements",
                List.of("Contrats de maintenance préventive", "Intervention d'urgence 24h/7j",
                        "Diagnostic et réparation", "Nettoyage et désinfection"), 2);
        createPrestation("Encastrement de liaison frigorifique", "/assets/images/services/encastrement.jpg",
                "Installation discrète et soignée des liaisons frigorifiques encastrées dans murs ou dalles",
                List.of("Passage des tuyauteries cuivre isolées", "Intégration dans les murs ou faux-plafonds",
                        "Protection par gaines techniques", "Finition esthétique sans goulottes apparentes"), 3);
        createPrestation("Location d'Équipements", "/assets/images/services/location.jpeg",
                "Solutions temporaires pour vos besoins ponctuels ou saisonniers",
                List.of("Climatiseurs mobiles", "Solutions événementielles",
                        "Location courte et longue durée", "Livraison et installation incluses"), 4);
        createPrestation("Installation de chambres froides", "/assets/images/services/chambre-froide.jpeg",
                "Conception et installation de chambres froides positives, négatives et de morgues",
                List.of("Étude et dimensionnement adapté à vos besoins",
                        "Installation chambres froides positives (0°C à +8°C)",
                        "Installation chambres froides négatives (-18°C et plus)",
                        "Mise en place de chambres froides mortuaires (morgues)"), 5);
    }

    private void createPrestation(String titre, String image, String description, List<String> features, int ordre) {
        Prestation p = new Prestation();
        p.setTitre(titre);
        p.setUrlImage(image);
        p.setDescription(description);
        p.getCaracteristiques().addAll(features);
        p.setOrdre(ordre);
        p.setActif(true);
        prestationRepository.save(p);
    }

    private void seedMembres() {
        createMembre("Babacar dione", "Fondateur & Directeur Général", "20+ ans d'expérience",
                "/assets/images/team/cheikh-anta-mbacke.jpg",
                "Expert en climatisation avec une vision d'innovation et d'excellence. Pionnier du secteur au Sénégal.",
                List.of("Leadership", "Stratégie", "Innovation", "Développement commercial"), 0);
        createMembre("El Hadji Ousmane", "Responsable Technique", "7 ans d'expérience",
                "/assets/images/team/amadou-diallo.jpg",
                "Technicien certifié spécialisé dans les systèmes de climatisation industrielle et commerciale.",
                List.of("Installation", "Maintenance", "Diagnostic", "Formation"), 1);
        createMembre("Serigne Bassirou Gaye", "Technicien Froid", "8 ans d'expérience",
                "/assets/images/team/fatou-ndiaye.jpg",
                "Technicien Superieur en froid et climatisation avec une approche client personnalisée et efficace.",
                List.of("Installation", "Maintenance", "Diagnostic", "Formation"), 2);
        createMembre("Modou Mamoune", "Technicien Senior", "10 ans d'expérience",
                "/assets/images/team/moussa-sarr.jpg",
                "Expert en maintenance préventive et corrective, spécialisé dans les systèmes VRV/VRF.",
                List.of("Réparation", "Maintenance", "Électricité", "Frigoriste"), 3);
        createMembre("Issa Dione", "Technicien Senior", "10 ans d'expérience",
                "/assets/images/team/issa.jpg",
                "Expert en maintenance préventive et corrective, spécialisé dans les chambre froide.",
                List.of("Réparation", "Maintenance", "Électricité", "Frigoriste"), 4);
        createMembre("Issa Tine", "Technicien Froid", "8 ans d'expérience",
                "/assets/images/team/tine.jpg",
                "Technicien Superieur en froid et climatisation avec une approche client personnalisée et efficace.",
                List.of("Installation", "Maintenance", "Diagnostic", "Formation"), 5);
    }

    private void createMembre(String nom, String poste, String experience, String photo, String bio,
                               List<String> competences, int ordre) {
        MembreEquipe m = new MembreEquipe();
        m.setNom(nom);
        m.setPoste(poste);
        m.setExperience(experience);
        m.setUrlPhoto(photo);
        m.setBio(bio);
        m.getCompetences().addAll(competences);
        m.setOrdre(ordre);
        m.setActif(true);
        membreEquipeRepository.save(m);
    }

    private void seedRealisations() {
        createRealisation("Travaux Hospitaliers – CHNU de Fann", "Maintenance & Installation",
                "Dakar, Sénégal",
                "Prestations de maintenance, d'installation et de réparation des équipements de froid et de climatisation au Centre hospitalier national universitaire de Fann.",
                "Centre hospitalier national universitaire de Fann", "2024", "12 mois", null,
                List.of("/assets/images/achievements/fann.jpg", "/assets/images/achievements/fann2.jpg",
                        "/assets/images/achievements/fann3.jpg"),
                List.of("Hôpital", "chambre froide", "Maintenance", "Réparation", "Installation", "Froid", "Climatisation"), 0);
        createRealisation("Hôtel Océanie", "Hôtellerie",
                "Dakar, Sénégal",
                "Installation et maintenance des climatiseurs split ainsi que la modernisation du système de climatisation avec l'ajout de 30 unités split.",
                "Hôtel Océanie", "2022", "3 mois", "5,000 m²",
                List.of("/assets/images/achievements/clim.png", "/assets/images/achievements/clim2.png",
                        "/assets/images/achievements/clim3.jpg"),
                List.of("Installation", "Maintenance", "Hôtellerie", "Split", "Modernisation"), 1);
        createRealisation("Usine Air Liquide", "Industriel",
                "Rufisque, Sénégal",
                "Installation de trois chambres froides négatives pour le stockage spécialisé ainsi qu'un système de refroidissement de l'eau adapté aux besoins industriels.",
                "Air Liquide", "2023", "6 semaines", "2,500 m²",
                List.of("/assets/images/achievements/chambre.jpg", "/assets/images/achievements/chambre11.jpg",
                        "/assets/images/achievements/chambre12.jpg"),
                List.of("Industriel", "Chambre froide", "Refroidissement eau", "Négatif"), 2);
        createRealisation("Résidence Les Almadies", "Résidentiel",
                "Almadies, Dakar",
                "Réalisation de 60 encastrements de liaisons frigorifiques suivis de la fourniture et de l'installation de climatiseurs split inverter haute efficacité pour équiper l'ensemble des appartements.",
                "Résidence Les Almadies", "2024", "2 mois", "3,200 m²",
                List.of("/assets/images/achievements/almadies.jpg", "/assets/images/achievements/maison2.jpg",
                        "/assets/images/achievements/almadies3.jpg"),
                List.of("Résidentiel", "Split Inverter", "Installation", "Efficacité énergétique"), 3);
        createRealisation("Air Sénégal", "Transport Aérien",
                "Dakar, Sénégal",
                "Prestations de maintenance, d'installation, de réparation et de fourniture d'équipements de froid et de climatisation pour les infrastructures de la compagnie.",
                "Air Sénégal", "2023", "3 mois", "4,000 m²",
                List.of("/assets/images/achievements/air.jpg", "/assets/images/achievements/air1.jpg",
                        "/assets/images/achievements/air4.jpg"),
                List.of("Aérien", "Maintenance", "Installation", "Réparation", "Climatisation"), 4);
        createRealisation("Exploitation Agricole à Bambilor", "Agricole",
                "Bambilor, Sénégal",
                "Installation de deux chambres froides, l'une positive et l'autre négative, pour la conservation et le stockage des produits agricoles.",
                "Exploitation agricole à Bambilor", "2022", "5 mois", "6,500 m²",
                List.of("/assets/images/achievements/chambre.jpg", "/assets/images/achievements/chambre21.jpg",
                        "/assets/images/achievements/chambre23.jpg"),
                List.of("Agricole", "Chambre froide", "Positif", "Négatif", "Conservation"), 5);
    }

    private void createRealisation(String titre, String categorie, String localisation, String description,
                                    String client, String annee, String duree, String surface,
                                    List<String> images, List<String> tags, int ordre) {
        Realisation r = new Realisation();
        r.setTitre(titre);
        r.setCategorie(categorie);
        r.setLocalisation(localisation);
        r.setDescription(description);
        r.setClientNom(client);
        r.setAnnee(annee);
        r.setDuree(duree);
        r.setSurface(surface);
        r.getImages().addAll(images);
        r.getTags().addAll(tags);
        r.setOrdre(ordre);
        r.setActif(true);
        realisationRepository.save(r);
    }

    // ── Zones de livraison ──────────────────────────────────────────────────

    private void seedZonesLivraison() {
        // eligibleLivraisonGratuite = true uniquement pour Dakar/banlieue proche
        // format : ville, villeLabel, quartier, quartierLabel, prixStandard, eligibleGratuit, ordre
        List<Object[]> zones = new ArrayList<>();

        // DAKAR (prix 0–3000 FCFA, éligible livraison gratuite)
        zones.add(new Object[]{"dakar","Dakar","plateau","Plateau",0,true,0});
        zones.add(new Object[]{"dakar","Dakar","medina","Médina",0,true,1});
        zones.add(new Object[]{"dakar","Dakar","grand-dakar","Grand Dakar",500,true,2});
        zones.add(new Object[]{"dakar","Dakar","fann-point-e","Fann / Point E / Liberté",1000,true,3});
        zones.add(new Object[]{"dakar","Dakar","sacre-coeur","Sacré Cœur / Mermoz",1500,true,4});
        zones.add(new Object[]{"dakar","Dakar","almadies","Almadies / Ngor",2500,true,5});
        zones.add(new Object[]{"dakar","Dakar","ouakam","Ouakam / Yoff",2500,true,6});
        zones.add(new Object[]{"dakar","Dakar","hlm","HLM / Grand Yoff",1500,true,7});
        zones.add(new Object[]{"dakar","Dakar","parcelles-assainies","Parcelles Assainies",2000,true,8});
        zones.add(new Object[]{"dakar","Dakar","golf","Golf Sud / Cité Keur Gorgui",1500,true,9});
        zones.add(new Object[]{"dakar","Dakar","foire","Foire / Diamniadio direction",2500,true,10});
        zones.add(new Object[]{"dakar","Dakar","colobane","Colobane / Dieuppeul",1000,true,11});
        zones.add(new Object[]{"dakar","Dakar","bel-air","Bel Air / Zone industrielle",1500,true,12});

        // PIKINE (prix 2000–4000, éligible gratuit)
        zones.add(new Object[]{"pikine","Pikine","pikine-icotaf","Pikine Icotaf",2000,true,0});
        zones.add(new Object[]{"pikine","Pikine","pikine-guediawaye","Pikine / Guinaw Rails",2500,true,1});
        zones.add(new Object[]{"pikine","Pikine","thiaroye","Thiaroye",2500,true,2});
        zones.add(new Object[]{"pikine","Pikine","diamaguene","Diamaguène",3000,true,3});
        zones.add(new Object[]{"pikine","Pikine","yeumbeul","Yeumbeul",3000,true,4});
        zones.add(new Object[]{"pikine","Pikine","malika","Malika",4000,true,5});

        // GUÉDIAWAYE (prix 2500–3500, éligible gratuit)
        zones.add(new Object[]{"guediawaye","Guédiawaye","golf-nord","Golf Nord",2500,true,0});
        zones.add(new Object[]{"guediawaye","Guédiawaye","sam-notaire","Sam Notaire",3000,true,1});
        zones.add(new Object[]{"guediawaye","Guédiawaye","ndiarème","Ndiarème Limamoulaye",3000,true,2});
        zones.add(new Object[]{"guediawaye","Guédiawaye","wakhinane","Wakhinane Nimzatt",3500,true,3});

        // RUFISQUE (prix 3000–4000, éligible gratuit)
        zones.add(new Object[]{"rufisque","Rufisque","rufisque-centre","Rufisque Centre",3000,true,0});
        zones.add(new Object[]{"rufisque","Rufisque","rufisque-est","Rufisque Est",3500,true,1});
        zones.add(new Object[]{"rufisque","Rufisque","bargny","Bargny",4000,true,2});
        zones.add(new Object[]{"rufisque","Rufisque","sangalkam","Sangalkam",4000,true,3});

        // THIÈS (prix 5000–6000, NON éligible gratuit)
        zones.add(new Object[]{"thies","Thiès","thies-centre","Thiès Centre",5000,false,0});
        zones.add(new Object[]{"thies","Thiès","thies-nord","Thiès Nord",5000,false,1});
        zones.add(new Object[]{"thies","Thiès","thies-sud","Thiès Sud",5500,false,2});
        zones.add(new Object[]{"thies","Thiès","mbour","Mbour",6000,false,3});

        // KAOLACK (prix 7000–7500, NON éligible)
        zones.add(new Object[]{"kaolack","Kaolack","kaolack-centre","Kaolack Centre",7000,false,0});
        zones.add(new Object[]{"kaolack","Kaolack","kaolack-nord","Kaolack Nord",7000,false,1});
        zones.add(new Object[]{"kaolack","Kaolack","nioro","Nioro du Rip",7500,false,2});

        // SAINT-LOUIS (prix 8000–9000, NON éligible)
        zones.add(new Object[]{"saint-louis","Saint-Louis","saint-louis-centre","Saint-Louis Centre",8000,false,0});
        zones.add(new Object[]{"saint-louis","Saint-Louis","sor","Sor",8500,false,1});
        zones.add(new Object[]{"saint-louis","Saint-Louis","guet-ndar","Guet Ndar",9000,false,2});

        // ZIGUINCHOR (prix 10000–11000, NON éligible)
        zones.add(new Object[]{"ziguinchor","Ziguinchor","ziguinchor-centre","Ziguinchor Centre",10000,false,0});
        zones.add(new Object[]{"ziguinchor","Ziguinchor","ziguinchor-sud","Ziguinchor Sud",11000,false,1});
        zones.add(new Object[]{"ziguinchor","Ziguinchor","bignona","Bignona",11000,false,2});

        for (Object[] z : zones) {
            createZone((String)z[0],(String)z[1],(String)z[2],(String)z[3],
                    (int)z[4],(boolean)z[5],(int)z[6]);
        }

        // Zones "Autres quartiers" par ville — prix = max des quartiers de la ville
        creerAutresQuartiersParVille();

        log.info("✅ {} zones de livraison créées", zoneLivraisonRepository.count());
    }

    private void createZone(String ville, String villeLabel, String quartier, String quartierLabel,
                             int prixStandard, boolean eligibleGratuit, int ordre) {
        ZoneLivraison z = new ZoneLivraison();
        z.setVille(ville);
        z.setVilleLabel(villeLabel);
        z.setQuartier(quartier);
        z.setQuartierLabel(quartierLabel);
        z.setPrixStandard(BigDecimal.valueOf(prixStandard));
        z.setExpressDisponible(true);
        z.setDelaiEstime(delaiParVille(ville));
        z.setActif(true);
        z.setEligibleLivraisonGratuite(eligibleGratuit);
        z.setOrdre(ordre);
        zoneLivraisonRepository.save(z);
    }

    private void creerAutresQuartiersParVille() {
        List<String> villes = List.of("dakar","pikine","guediawaye","rufisque","thies","kaolack","saint-louis","ziguinchor");
        for (String ville : villes) {
            List<ZoneLivraison> zonesVille = zoneLivraisonRepository.findByVilleIgnoreCaseOrderByOrdreAsc(ville);
            if (zonesVille.isEmpty()) continue;
            BigDecimal prixMax = zonesVille.stream()
                    .map(ZoneLivraison::getPrixStandard)
                    .max(Comparator.naturalOrder())
                    .orElse(BigDecimal.ZERO);
            String villeLabel = zonesVille.get(0).getVilleLabel();
            boolean eligibleGratuit = zonesVille.get(0).getEligibleLivraisonGratuite();
            ZoneLivraison autres = new ZoneLivraison();
            autres.setVille(ville);
            autres.setVilleLabel(villeLabel);
            autres.setQuartier("autres");
            autres.setQuartierLabel("Autres quartiers");
            autres.setPrixStandard(prixMax);
            autres.setExpressDisponible(true);
            autres.setDelaiEstime(delaiParVille(ville));
            autres.setActif(true);
            autres.setEligibleLivraisonGratuite(eligibleGratuit);
            autres.setOrdre(99);
            zoneLivraisonRepository.save(autres);
        }
    }

    private String delaiParVille(String ville) {
        return switch (ville) {
            case "dakar" -> "24h";
            case "pikine", "guediawaye", "rufisque" -> "24-48h";
            case "thies" -> "2-3 jours";
            default -> "3-5 jours";
        };
    }
}
