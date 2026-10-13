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

## Points connus à corriger

### #1 — Incohérence des frais de livraison ⚠️
**Symptôme** : deux systèmes coexistent sans se parler.
- `CommandeService.calculerFraisLivraison()` : 0 FCFA (retrait), 2 500 FCFA (domicile), 5 000 FCFA (express) — calculé côté backend.
- `checkout.component.ts` : table de ~30 quartiers avec prix 0–9 000 FCFA par zone — affiché côté frontend mais **ignoré par le backend**.
**Résultat** : le montant total en base ne correspond pas au prix affiché à l'utilisateur lors du choix de quartier.
**Solution** : module zones de livraison dédié (voir feuille de route #1).

### #2 — Secrets historiquement exposés
Les clés Cloudinary, le mot de passe Neon et le secret JWT étaient en clair dans `application.properties` commité. **Régénérer toutes ces clés** avant tout déploiement.

### #3 — `DataSeeder` désactivé
`DataSeeder.java` contient du code commenté de seed initial. Nettoyer ou supprimer ce fichier.

### #4 — Endpoint dupliqué dans `ProduitController`
Le code de `POST /{id}/images` et `POST /{id}/fiche-technique` apparaît deux fois (une version commentée et une active). Supprimer le bloc commenté.
