# CLAUDE.md — Backend froidCheikhAntaMbacke

## Stack technique

| Couche | Technologie |
|---|---|
| Framework | Spring Boot 3.x (Java 17+) |
| Base de données | PostgreSQL hébergé sur **Neon** (serverless, SSL obligatoire) |
| ORM | Hibernate / Spring Data JPA (`ddl-auto=update`) |
| Sécurité | Spring Security + JWT (stateless) |
| Stockage fichiers | **Cloudinary** (images produits, logos, PDFs) |
| Cache | Spring Cache (ConcurrentMapCacheManager, cache `siteConfig`) |
| Build | Maven |
| Mapping | ModelMapper |
| Validation | Jakarta Validation (`@Valid`, `@NotBlank`, `@Pattern`, etc.) |

Dépôt frontend associé : `../froidCheikhAntaMbackeFront`

---

## Règles de travail

1. **Branche par fonctionnalité** — toujours créer `feature/<nom>` depuis `develop` (branche principale). Ne jamais travailler directement sur `develop`.
2. **Analyse → Conception → Validation → Code** — ne modifier aucun fichier avant que l'utilisateur ait validé la conception proposée.
3. **Un commit par étape logique** — message clair, se terminer par :
   ```
   Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>
   ```
4. **Ne jamais fusionner ni pousser** sans accord explicite de l'utilisateur.
5. **Aucun secret dans le code** — toutes les clés, URLs et mots de passe sont dans des variables d'environnement (`${VARIABLE}` sans valeur par défaut dans `application.properties`). Voir `.env.example` pour la liste complète.
6. **Vérifier `mvn compile`** après chaque étape backend importante avant de commiter.
7. **`target/` et `.idea/` sont dans `.gitignore`** — ne jamais les commiter.

### Variables d'environnement requises (voir `.env.example`)
`DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`, `JWT_SECRET`, `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, `CLOUDINARY_API_SECRET`, `ALLOWED_ORIGINS`, `INSTALL_TOKEN`

---

## Architecture — entité `ParametresSite`

Table `parametres_site` — **une seule ligne** (id = 1 par convention).
Entité : `src/main/java/com/froidcheikh/ecommerce/entity/ParametresSite.java`

| Groupe | Champs clés |
|---|---|
| **IDENTITE** | `nomBoutique`, `sloganCourt`, `sloganLong`, `descriptionSite`, `mission`, `vision`, `anneeCreation`, `urlLogo`, `urlFavicon` |
| **CONTACT** | `email`, `@ElementCollection List<TelephoneSite> telephones` (table `site_telephones` : `numero`, `libelle`, `whatsapp`), `adresseComplete`, `adresseCourte` |
| **RESEAUX** | `urlFacebook`, `urlInstagram`, `urlYoutube`, `telephoneWhatsapp` |
| **SEO** | `titreSite`, `metaDescription`, `metaKeywords`, `langue` |
| **APPARENCE** | `couleurPrimaire` (#HEX), `couleurSecondaire` (#HEX), `nomPolice`, `urlPoliceGoogle` |
| **COMMERCE** | `devise` (défaut `XOF`), `seuilLivraisonGratuite` |
| **TEXTES_LEGAUX** | `cgv`, `mentionsLegales`, `politiqueConfidentialite` (TEXT brut/markdown, jamais de HTML) |
| **ENTREPRISE_FACTURATION** | `raisonSociale`, `formeJuridique`, `ninea`, `rccm`, `adresseSiege`, `tauxTvaDefaut`, `prixAfficheTTC`, `prefixeFacture/Avoir/Devis/BonLivraison`, `conditionsPaiementDefaut`, `mentionsPiedDocument`, `urlCachetSignature` |
| **META** | `installed` (Boolean), `dateModification` (`@UpdateTimestamp`), `modifiePar` |

### API Configuration
- `GET /api/config` — public, retourne `ParametresSitePublicDTO`, **mis en cache** (`@Cacheable("siteConfig")`)
- `GET /api/admin/config` — SUPER_ADMIN, retourne `ParametresSiteAdminDTO`
- `PUT /api/admin/config` — SUPER_ADMIN, valide + sauvegarde + **invalide le cache** (`@CacheEvict`)
- `POST /api/admin/config/logo` — upload via `FileService` (dossier Cloudinary `site/logos`)
- `POST /api/admin/config/favicon` — upload via `FileService` (dossier `site/favicons`)

### Logique d'initialisation (`DataInitializer`)
- `ParametresSite` existe → rien
- Pas de `ParametresSite` + SUPER_ADMIN existe → insère les valeurs **Froid Cheikh** avec `installed = true`
- Pas de `ParametresSite` + pas de SUPER_ADMIN → ne crée rien (l'assistant d'installation s'affiche côté frontend)

### Endpoint d'installation
`POST /api/auth/install` — vérifie `INSTALL_TOKEN` (env), refuse (403) si SUPER_ADMIN existe déjà, verrou JVM contre appels simultanés.

---

## Architecture — autres entités produit

| Entité | Table | Notes |
|---|---|---|
| `Produit` | `produits` | Images dans `produit_images` (@ElementCollection d'URLs Cloudinary). Pas d'image principale désignée (première par convention). Pas d'EAN/SKU fabricant structuré. |
| `AttributProduit` | `attributs_produit` | Clé/valeur dynamique. Pas d'endpoint CRUD dédié pour l'instant. |
| `Categorie` | `categories` | Hiérarchie auto-référencée (`parent_id`). |
| `Marque` | `marques` | Logo URL Cloudinary. |
| `Commande` | `commandes` | Frais calculés en dur dans `CommandeService`. Voir point connu #1. |

---

## Feuille de route des modules restants

1. **Module zones de livraison** — entité `ZoneLivraison` (`ville`, `quartier`, `prixLivraison`) pour remplacer la table en dur dans `checkout.component.ts` ET les constantes dans `CommandeService`. Voir point connu #1.
2. **Enrichissement automatique des fiches produits** — ajout de champs `ean`, `modeleFabricant` sur `Produit` ; endpoint CRUD dédié pour `AttributProduit` ; script `CommandLineRunner` d'enrichissement par lot.
3. **Module facturation** — génération de factures/avoirs/devis/BL en PDF à partir des paramètres du groupe `ENTREPRISE_FACTURATION` de `ParametresSite`.
4. **Image principale désignée** — champ `imagePrincipale` ou convention `listeImages[0]` documentée.
5. **Stockage `public_id` Cloudinary** — actuellement recalculé à la volée depuis l'URL. Risque de suppression incorrecte si l'URL change.
6. **Migrations versionnées** — passer de `ddl-auto=update` à Flyway pour les environnements de production.

---

## État d'avancement (oct. 2026)

| Module | Statut | Branche |
|---|---|---|
| Site configurable (ParametresSite, API config, hardcodes) | ✅ Mergé dans `develop` + `main` | — |
| Entités de contenu gérable (ImageHero, Partenaire, Prestation, MembreEquipe, Realisation) | ✅ Mergé dans `develop` + `main` | — |
| Zones de livraison (ZoneLivraison) | ✅ Mergé dans `develop` + `main` | — |
| Promotions produits (PrixService, PromotionService, CRUD, fiche, catalogue, panier) | ✅ Mergé dans `develop` + `main` | — |
| Enrichissement fiches produits (EAN, ModeleFabricant, CRUD AttributProduit) | ⏳ Planifié | — |
| Module facturation (PDF) | ⏳ Planifié | — |
| Migrations Flyway | ⏳ Planifié | — |

---

## Checklist de mise en production

À effectuer **avant le premier démarrage en production**. Ne jamais sauter ces étapes.

### 1 — Variables d'environnement à configurer chez l'hébergeur

| Variable | Valeur attendue |
|---|---|
| `DATABASE_URL` | URL Neon production avec `sslmode=require&connectTimeout=30&socketTimeout=60` |
| `DATABASE_USERNAME` | Utilisateur Neon production |
| `DATABASE_PASSWORD` | Mot de passe Neon production (**régénéré**, voir ci-dessous) |
| `JWT_SECRET` | Clé aléatoire ≥ 64 caractères (**régénérée**, voir ci-dessous) |
| `CLOUDINARY_CLOUD_NAME` | Cloud name Cloudinary production |
| `CLOUDINARY_API_KEY` | Clé API Cloudinary production (**régénérée**) |
| `CLOUDINARY_API_SECRET` | Secret API Cloudinary production (**régénéré**) |
| `ALLOWED_ORIGINS` | Vrai domaine de production, ex. `https://www.froidcheikhantambacke.sn` |
| `INSTALL_TOKEN` | Jeton fort à usage unique pour la première installation |
| `PORT` | Port exposé par l'hébergeur (optionnel, défaut 8080) |

### 2 — Régénérer toutes les clés (secrets historiquement exposés)

- **Neon** : réinitialiser le mot de passe depuis le dashboard Neon → copier le nouveau `DATABASE_URL`.
- **Cloudinary** : régénérer l'API Key + API Secret depuis la console Cloudinary.
- **JWT** : générer une nouvelle clé aléatoire (ex. `openssl rand -hex 64`).
- Supprimer ou archiver l'ancien `.env` de développement.

### 3 — Sauvegarde Neon avant le premier démarrage

Depuis le dashboard Neon, créer une **branche de sauvegarde** du projet (ex. `backup-avant-prod-YYYYMMDD`) avant le premier `ddl-auto=update` en production. En cas de migration incorrecte, cette branche permet de restaurer l'état exact.

### 4 — Désactiver les logs SQL

Dans `application.properties` (ou via variable d'environnement) :
```properties
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.format_sql=false
```
Ces deux propriétés sont actuellement à `true` (utile en dev, verbeux et non sécurisé en prod).

### 5 — Vérifier `ddl-auto`

`spring.jpa.hibernate.ddl-auto=update` convient pour le premier démarrage. Après stabilisation du schéma, passer à `validate` (ou migrer vers Flyway — voir feuille de route #6) pour éviter toute modification accidentelle de la base.

### 6 — Dockerfile Alpine et génération PDF

L'image de production utilise `eclipse-temurin:21-jre-alpine`. Alpine ne contient pas de polices système par défaut. Avant d'activer le module facturation PDF (voir feuille de route #3), ajouter dans le `Dockerfile` :

```dockerfile
RUN apk add --no-cache fontconfig ttf-dejavu
```

Sans cette ligne, toute génération de PDF (iText, JasperReports, Apache PDFBox…) échouera silencieusement en production avec une erreur de police manquante.

### 8 — Nettoyer les commandes de test

Avant la mise en ligne, supprimer toutes les commandes créées pendant le développement et les tests. Ces enregistrements ont des montants, des adresses et des modes de livraison qui ne correspondent pas à de vraies transactions. Depuis la console SQL Neon :

```sql
-- Vérifier d'abord les commandes existantes
SELECT id_commande, numero_commande, date_commande, montant_total FROM commandes ORDER BY date_commande;
-- Supprimer après vérification
DELETE FROM commandes;
```

### 7 — Railway (si déploiement via Railway)

- Vérifier la configuration du déploiement automatique (branche suivie, triggers).
- **Régénérer les identifiants Railway exposés dans l'historique git** : le mot de passe PostgreSQL Railway (`svYhBmqtXnJkAaFEbairZjCtElvTESMe`) et l'URL interne (`postgres.railway.internal:5432/railway`) étaient committés en clair dans `application.properties` sur `origin/main`. Ces identifiants sont compromis — les réinitialiser depuis le dashboard Railway avant tout déploiement.

---

## Points connus à corriger

### #1 — Incohérence des frais de livraison ✅ Résolu
Module `ZoneLivraison` mergé dans `develop` + `main` : `CalculCommandeService` délègue au repository, `checkout.component.ts` appelle `/api/livraison/estimation`, les constantes hardcodées sont supprimées.

### #6 — Checkout : afficher le prix barré par ligne (amélioration future)
Le récapitulatif du checkout affiche `item.sousTotal` (déjà correct, calculé avec `prixPromo`) mais n'affiche pas le détail visuel prix barré / prix promo par ligne comme le panier. À améliorer : ajouter le même traitement que `cart.component.ts` (badge `-N%`, prix catalogue barré, prix promo en vert) dans la section "Cart Items" du checkout.

### #2 — Secrets historiquement exposés
Les clés Cloudinary, le mot de passe Neon et le secret JWT étaient en clair dans `application.properties` commité. **Régénérer toutes ces clés** avant tout déploiement.

### #3 — `DataSeeder` désactivé
`DataSeeder.java` contient du code commenté de seed initial. Nettoyer ou supprimer ce fichier.

### #4 — Endpoint dupliqué dans `ProduitController`
Le code de `POST /{id}/images` et `POST /{id}/fiche-technique` apparaît deux fois (une version commentée et une active). Supprimer le bloc commenté.

### #5 — `FroidCheikhEcommerceApplicationTests` se connecte à la vraie base Neon
Le test de contexte `FroidCheikhEcommerceApplicationTests` charge le contexte Spring complet, se connecte à la base Neon configurée dans les variables d'environnement et exécute `DataInitializer` (seed zones si table vide + SUPER_ADMIN existant). Il ne s'exécute pas en isolation : il dépend d'une base accessible et d'un état cohérent. À remplacer par une base de test dédiée (H2 in-memory ou Testcontainers) pour isoler les tests de l'environnement réel.
