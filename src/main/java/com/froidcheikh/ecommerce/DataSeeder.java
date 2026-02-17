package com.froidcheikh.ecommerce;

import com.froidcheikh.ecommerce.entity.*;
import com.froidcheikh.ecommerce.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
@Slf4j
public class DataSeeder implements ApplicationRunner {
    @Override
    public void run(ApplicationArguments args) throws Exception {

    }
/*
    private final AdministrateurRepository administrateurRepository;
    private final MarqueRepository marqueRepository;
    private final CategorieRepository categorieRepository;
    private final ProduitRepository produitRepository;
    private final ClientRepository clientRepository;
    private final AdresseRepository adresseRepository;

    public DataSeeder(AdministrateurRepository administrateurRepository,
                      MarqueRepository marqueRepository,
                      CategorieRepository categorieRepository,
                      ProduitRepository produitRepository,
                      ClientRepository clientRepository,
                      AdresseRepository adresseRepository) {
        this.administrateurRepository = administrateurRepository;
        this.marqueRepository = marqueRepository;
        this.categorieRepository = categorieRepository;
        this.produitRepository = produitRepository;
        this.clientRepository = clientRepository;
        this.adresseRepository = adresseRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedAdministrateur();
        seedMarques();
        seedCategoriesAndSubCategories();
        seedProduits();
        seedClientEtAdresses();
        log.info("Initialisation des données terminée.");
    }

    private void seedAdministrateur() {
        String adminEmail = "admin@froidcheikh.sn";
        if (administrateurRepository.existsByEmail(adminEmail)) {
            log.info("Administrateur par défaut déjà présent : {}", adminEmail);
            return;
        }

        Administrateur admin = new Administrateur();
        admin.setNom("Admin");
        admin.setPrenom("Super");
        admin.setEmail(adminEmail);
        // Mot de passe hashé fourni dans ton script SQL (BCrypt)
        admin.setMotDePasse("$2a$10$N.zmdr9k7uH7NOARlXihU.4Y8Snt6Z9Q8UQ6C2LK6Vt2Ux8GU.KeS");
        admin.setRole(Administrateur.RoleAdmin.SUPER_ADMIN);
        admin.setActif(true);

        administrateurRepository.save(admin);
        log.info("Administrateur par défaut créé : {}", adminEmail);
    }

    private void seedMarques() {
        String[][] marques = {
                {"Samsung", "Marque coréenne leader en électroménager et climatisation"},
                {"LG", "Marque coréenne spécialisée en appareils électroménagers"},
                {"Daikin", "Marque japonaise leader en climatisation"},
                {"Mitsubishi", "Marque japonaise de climatiseurs et électroménager"},
                {"Sharp", "Marque japonaise d'électroménager"},
                {"Whirlpool", "Marque américaine d'électroménager"},
                {"Bosch", "Marque allemande d'électroménager de qualité"},
                {"Haier", "Marque chinoise d'électroménager"},
                {"Midea", "Marque chinoise de climatiseurs et réfrigérateurs"},
                {"Toshiba", "Marque japonaise d'électronique et électroménager"}
        };

        for (String[] m : marques) {
            String nom = m[0];
            String desc = m[1];
            if (marqueRepository.findByNomMarque(nom).isPresent()) {
                log.debug("Marque existante : {}", nom);
                continue;
            }
            Marque marque = new Marque();
            marque.setNomMarque(nom);
            marque.setDescription(desc);
            marqueRepository.save(marque);
            log.info("Marque créée : {}", nom);
        }
    }

    private void seedCategoriesAndSubCategories() {
        // Root categories
        createCategorieIfNotExists("Climatiseurs", "Systèmes de climatisation pour maisons et bureaux");
        createCategorieIfNotExists("Réfrigérateurs", "Réfrigérateurs et congélateurs de toutes tailles");
        createCategorieIfNotExists("Chambres Froides", "Solutions de réfrigération professionnelle");
        createCategorieIfNotExists("Ventilateurs", "Ventilateurs de plafond, sur pied et muraux");
        createCategorieIfNotExists("Électroménager", "Appareils électroménagers divers");
        createCategorieIfNotExists("Accessoires", "Accessoires et pièces détachées");
        createCategorieIfNotExists("Services", "Prestations d'installation et maintenance");

        // Sous-catégories pour Climatiseurs (parent = 'Climatiseurs')
        Optional<Categorie> climatRoot = categorieRepository.findByNomCategorie("Climatiseurs");
        climatRoot.ifPresent(p -> {
            createSubCategorieIfNotExists("Climatiseurs Split", "Climatiseurs split muraux et cassettes", p);
            createSubCategorieIfNotExists("Climatiseurs Window", "Climatiseurs fenêtre compacts", p);
            createSubCategorieIfNotExists("Climatiseurs Mobiles", "Climatiseurs portables", p);
            createSubCategorieIfNotExists("Climatiseurs Centraux", "Systèmes de climatisation centralisée", p);
        });

        // Sous-catégories pour Réfrigérateurs
        Optional<Categorie> refrRoot = categorieRepository.findByNomCategorie("Réfrigérateurs");
        refrRoot.ifPresent(p -> {
            createSubCategorieIfNotExists("Réfrigérateurs 1 Porte", "Réfrigérateurs simple porte", p);
            createSubCategorieIfNotExists("Réfrigérateurs 2 Portes", "Réfrigérateurs double porte", p);
            createSubCategorieIfNotExists("Réfrigérateurs Side by Side", "Réfrigérateurs américains", p);
            createSubCategorieIfNotExists("Mini Réfrigérateurs", "Petits réfrigérateurs et mini-bars", p);
            createSubCategorieIfNotExists("Congélateurs", "Congélateurs coffre et armoire", p);
        });

        // Sous-catégories pour Ventilateurs
        Optional<Categorie> ventRoot = categorieRepository.findByNomCategorie("Ventilateurs");
        ventRoot.ifPresent(p -> {
            createSubCategorieIfNotExists("Ventilateurs de Plafond", "Ventilateurs de plafond avec ou sans éclairage", p);
            createSubCategorieIfNotExists("Ventilateurs sur Pied", "Ventilateurs sur pied réglables", p);
            createSubCategorieIfNotExists("Ventilateurs Muraux", "Ventilateurs muraux oscillants", p);
            createSubCategorieIfNotExists("Ventilateurs de Table", "Petits ventilateurs de bureau", p);
        });
    }

    private void createCategorieIfNotExists(String nom, String description) {
        if (categorieRepository.findByNomCategorie(nom).isPresent()) {
            log.debug("Catégorie existante : {}", nom);
            return;
        }
        Categorie c = new Categorie();
        c.setNomCategorie(nom);
        c.setDescriptionCategorie(description);
        categorieRepository.save(c);
        log.info("Catégorie créée : {}", nom);
    }

    private void createSubCategorieIfNotExists(String nom, String description, Categorie parent) {
        Optional<Categorie> existing = categorieRepository.findByNomCategorie(nom);
        if (existing.isPresent()) {
            log.debug("Sous-catégorie existante : {}", nom);
            return;
        }
        Categorie c = new Categorie();
        c.setNomCategorie(nom);
        c.setDescriptionCategorie(description);
        c.setParent(parent);
        categorieRepository.save(c);
        log.info("Sous-catégorie créée : {} (parent={})", nom, parent.getNomCategorie());
    }

    private void seedProduits() {
        // helper lambdas to find marque / categorie by name
        java.util.function.Function<String, Marque> findMarque = name ->
                marqueRepository.findByNomMarque(name).orElse(null);

        java.util.function.Function<String, Categorie> findCategorie = name ->
                categorieRepository.findByNomCategorie(name).orElse(null);

        // Climatiseurs Samsung
        createProduitIfNotExists("Samsung AR12",
                "Climatiseur split Samsung 12000 BTU avec technologie Digital Inverter",
                new BigDecimal("320000"), 15, "SAM-AR12-001", "CLM001",
                findMarque.apply("Samsung"), findCategorie.apply("Climatiseurs Split"),
                12000, 1050, "2 ans", Produit.LabelEnergie.A_PLUS,
                List.of(),
                List.of(
                        makeAttribut("Couleur", "Blanc"),
                        makeAttribut("Type", "Split mural"),
                        makeAttribut("Fluide frigorigène", "R32"),
                        makeAttribut("Niveau sonore", "19 dB"),
                        makeAttribut("WiFi", "Oui")
                )
        );

        createProduitIfNotExists("Samsung AR18",
                "Climatiseur split Samsung 18000 BTU inverter",
                new BigDecimal("450000"), 12, "SAM-AR18-001", "CLM002",
                findMarque.apply("Samsung"), findCategorie.apply("Climatiseurs Split"),
                18000, 1400, "2 ans", Produit.LabelEnergie.A_PLUS,
                List.of(), List.of()
        );

        createProduitIfNotExists("Samsung AR24",
                "Climatiseur split Samsung 24000 BTU avec WiFi",
                new BigDecimal("580000"), 8, "SAM-AR24-001", "CLM003",
                findMarque.apply("Samsung"), findCategorie.apply("Climatiseurs Split"),
                24000, 1850, "2 ans", Produit.LabelEnergie.A_PLUS,
                List.of(), List.of()
        );

        // LG
        createProduitIfNotExists("LG Dual Cool",
                "Climatiseur LG 12000 BTU Dual Inverter",
                new BigDecimal("340000"), 10, "LG-DC12-001", "CLM004",
                findMarque.apply("LG"), findCategorie.apply("Climatiseurs Split"),
                12000, 980, "2 ans", Produit.LabelEnergie.A_PLUS_PLUS,
                List.of(), List.of(
                        makeAttribut("Couleur", "Blanc"),
                        makeAttribut("Type", "Split mural"),
                        makeAttribut("Fluide frigorigène", "R32"),
                        makeAttribut("Dual Inverter", "Oui"),
                        makeAttribut("Smart ThinQ", "Oui")
                )
        );

        createProduitIfNotExists("LG Art Cool",
                "Climatiseur LG 18000 BTU design premium",
                new BigDecimal("520000"), 6, "LG-AC18-001", "CLM005",
                findMarque.apply("LG"), findCategorie.apply("Climatiseurs Split"),
                18000, 1300, "2 ans", Produit.LabelEnergie.A_PLUS_PLUS,
                List.of(), List.of()
        );

        // Daikin
        createProduitIfNotExists("Daikin FTXS25K",
                "Climatiseur Daikin 9000 BTU silencieux",
                new BigDecimal("280000"), 20, "DAI-FTXS25-001", "CLM006",
                findMarque.apply("Daikin"), findCategorie.apply("Climatiseurs Split"),
                9000, 750, "3 ans", Produit.LabelEnergie.A_PLUS_PLUS_PLUS,
                List.of(), List.of()
        );

        createProduitIfNotExists("Daikin FTXS35K",
                "Climatiseur Daikin 12000 BTU eco-friendly",
                new BigDecimal("380000"), 15, "DAI-FTXS35-001", "CLM007",
                findMarque.apply("Daikin"), findCategorie.apply("Climatiseurs Split"),
                12000, 950, "3 ans", Produit.LabelEnergie.A_PLUS_PLUS_PLUS,
                List.of(), List.of()
        );

        // Réfrigérateurs (exemples) - trouver la catégorie "Réfrigérateurs" ou une sous-catégorie
        Categorie miniRefCat = categorieRepository.findByNomCategorie("Mini Réfrigérateurs").orElse(null);
        Categorie ref2Cat = categorieRepository.findByNomCategorie("Réfrigérateurs 2 Portes").orElse(null);
        Categorie refSideCat = categorieRepository.findByNomCategorie("Réfrigérateurs Side by Side").orElse(null);

        createProduitIfNotExists("Samsung RB34",
                "Réfrigérateur Samsung 2 portes 340L No Frost",
                new BigDecimal("280000"), 12, "SAM-RB34-001", "REF001",
                findMarque.apply("Samsung"), ref2Cat,
                null, 150, "2 ans", Produit.LabelEnergie.A_PLUS,
                List.of(),
                List.of(
                        makeAttribut("Couleur", "Silver"),
                        makeAttribut("Type", "Combiné"),
                        makeAttribut("No Frost", "Oui"),
                        makeAttribut("Compartiment congélateur", "98L"),
                        makeAttribut("Compartiment réfrigérateur", "242L")
                )
        );

        createProduitIfNotExists("Sharp SJ-K155",
                "Mini réfrigérateur Sharp 150L",
                new BigDecimal("120000"), 25, "SHA-SJK155-001", "REF005",
                findMarque.apply("Sharp"), miniRefCat,
                null, 85, "1 an", Produit.LabelEnergie.A,
                List.of(), List.of()
        );

        // Ventilateurs (exemples)
        Categorie ventPlafond = categorieRepository.findByNomCategorie("Ventilateurs de Plafond").orElse(null);
        createProduitIfNotExists("Samsung CF-1200",
                "Ventilateur de plafond Samsung 48\" avec éclairage LED",
                new BigDecimal("45000"), 40, "SAM-CF1200-001", "VEN001",
                findMarque.apply("Samsung"), ventPlafond,
                null, 75, "1 an", Produit.LabelEnergie.A,
                List.of(), List.of()
        );

        // Services (catégorie "Services")
        Categorie srvCat = categorieRepository.findByNomCategorie("Services").orElse(null);
        createProduitIfNotExistsService("Installation Climatiseur", "Service d'installation de climatiseur split",
                new BigDecimal("25000"), "SRV-INST-001", "SRV001", srvCat);
        createProduitIfNotExistsService("Maintenance Climatiseur", "Service de maintenance préventive climatiseur",
                new BigDecimal("15000"), "SRV-MAINT-001", "SRV002", srvCat);
        createProduitIfNotExistsService("Dépannage Urgence", "Service de dépannage d'urgence 24h/24",
                new BigDecimal("35000"), "SRV-URG-001", "SRV005", srvCat);
    }

    private AttributProduit makeAttribut(String nom, String valeur) {
        AttributProduit a = new AttributProduit();
        a.setNomAttribut(nom);
        a.setValeurAttribut(valeur);
        return a;
    }

    private void createProduitIfNotExists(String nomProduit,
                                          String description,
                                          BigDecimal prix,
                                          Integer stock,
                                          String refProduit,
                                          String codeProduit,
                                          Marque marque,
                                          Categorie categorie,
                                          Integer puissanceBTU,
                                          Integer consommationWatt,
                                          String garantie,
                                          Produit.LabelEnergie label,
                                          List<String> images,
                                          List<AttributProduit> attributs) {
        // Si ref unique existe, skip
        if (refProduit != null && produitRepository.findByRefProduit(refProduit).isPresent()) {
            log.debug("Produit (ref) existant : {}", refProduit);
            return;
        }

        Produit p = new Produit();
        p.setNomProduit(nomProduit);
        p.setDescriptionProduit(description);
        p.setPrix(prix != null ? prix : BigDecimal.ZERO);
        p.setStockDisponible(stock != null ? stock : 0);
        p.setRefProduit(refProduit);
        p.setCodeProduit(codeProduit);
        p.setMarque(marque);
        p.setCategorie(categorie);
        p.setPuissanceBTU(puissanceBTU);
        p.setConsommationWatt(consommationWatt);
        p.setGarantie(garantie);
        p.setLabelEnergie(label);
        p.setDisponibilite(true);

        // images (ElementCollection)
        if (images != null && !images.isEmpty()) {
            p.setListeImages(new ArrayList<>(images));
        }

        // attributs: set relation bidirectionnelle
        if (attributs != null && !attributs.isEmpty()) {
            List<AttributProduit> attribWithParent = new ArrayList<>();
            for (AttributProduit a : attributs) {
                a.setProduit(p);
                attribWithParent.add(a);
            }
            p.setAttributs(attribWithParent);
        }

        produitRepository.save(p);
        log.info("Produit créé : {} (ref={})", nomProduit, refProduit);
    }

    private void createProduitIfNotExistsService(String nomProduit, String description, BigDecimal prix,
                                                 String refProduit, String codeProduit, Categorie categorie) {
        createProduitIfNotExists(nomProduit, description, prix, 999, refProduit, codeProduit,
                null, categorie, null, null, "—", Produit.LabelEnergie.A, List.of(), List.of());
    }

    private void seedClientEtAdresses() {
        String clientEmail = "mamadou.diop@gmail.com";
        if (clientRepository.existsByEmail(clientEmail)) {
            log.info("Client test déjà présent : {}", clientEmail);
            return;
        }

        Client client = new Client();
        client.setNom("Diop");
        client.setPrenom("Mamadou");
        client.setEmail(clientEmail);
        // mot de passe hashé repris depuis ton script
        client.setMotDePasse("$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi");
        client.setTelephone("771234567");
        client.setActif(true);

        Client saved = clientRepository.save(client);
        log.info("Client test créé : {}", clientEmail);

        Adresse domicile = new Adresse();
        domicile.setLigne1("Cité Keur Gorgui");
        domicile.setLigne2("Villa n°25");
        domicile.setVille("Dakar");
        domicile.setCodePostal("12000");
        domicile.setPays("Sénégal");
        domicile.setTelephone("771234567");
        domicile.setTypeAdresse(Adresse.TypeAdresse.DOMICILE);
        domicile.setAdressePrincipale(true);
        domicile.setClient(saved);

        Adresse bureau = new Adresse();
        bureau.setLigne1("Zone industrielle");
        bureau.setLigne2("Immeuble Salam, 3ème étage");
        bureau.setVille("Dakar");
        bureau.setCodePostal("12500");
        bureau.setPays("Sénégal");
        bureau.setTelephone("338901234");
        bureau.setTypeAdresse(Adresse.TypeAdresse.BUREAU);
        bureau.setAdressePrincipale(false);
        bureau.setClient(saved);

        adresseRepository.save(domicile);
        adresseRepository.save(bureau);

        log.info("Adresses du client {} créées.", clientEmail);
    }*/
}
