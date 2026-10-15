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

## Stratégie de branches (backend + frontend)

Les deux dépôts suivent le même modèle :

| Branche | Rôle |
|---|---|
| `develop` | Branche d'intégration — reçoit les `feature/*` fusionnés |
| `main` | Branche de production — ne reçoit `develop` qu'au moment d'une mise en production |
| `feature/<nom>` | Branche de fonctionnalité — part de `develop`, fusionnée dans `develop` uniquement |

**Règle absolue** : `main` ne doit jamais recevoir directement une branche `feature/*`. Toute fusion passe d'abord par `develop`, puis `develop → main` seulement lors d'un déploiement validé.

> Le frontend utilisait `main` comme branche principale avant oct. 2026 (pas de `develop` existant). La branche `develop` a été créée depuis `main` le 15 oct. 2026 — elle est désormais la branche d'intégration pour les deux dépôts.

---

## Règles de travail

1. **Branche par fonctionnalité** — toujours créer `feature/<nom>` depuis `develop` (backend et frontend). Ne jamais travailler directement sur `develop`.
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

## Méthode d'enrichissement des fiches produits (validée oct. 2026)

### Principe général

L'enrichissement se fait **par lots de 5 à 10 produits** en JSON (`enrichissement_lot_N.json` ou `enrichissement_correction_*.json`). Ces fichiers sont dans `.gitignore` et ne doivent jamais être commités.

### Processus obligatoire

1. **Sauvegarde Neon** — créer une branche `backup-avant-enrichissement-YYYYMMDD` depuis le dashboard Neon avant toute écriture réelle.
2. **Rédiger le JSON** — suivre le format `EnrichissementProduit` (voir DTOs `dto/enrichissement/`). Pour chaque valeur : `confiance: "valide"` si confirmée, `"a_verifier"` sinon (sera ignoré par le runner).
3. **Lancer en simulation** (aucune écriture, rollback explicite en fin de transaction) :
   ```bash
   mvn spring-boot:run -Dspring-boot.run.profiles=import-enrichissement \
       -Dspring-boot.run.jvmArguments="-Dimport.fichier=enrichissement_lot_N.json"
   ```
4. **Vérifier le rapport** — relever les SKIP, IGNORE et REDIRECT. Corriger le JSON si nécessaire.
5. **Écriture réelle** (seulement après validation du rapport de simulation) :
   ```bash
   mvn spring-boot:run -Dspring-boot.run.profiles=import-enrichissement \
       -Dspring-boot.run.jvmArguments="-Dimport.fichier=enrichissement_lot_N.json -Dimport.confirmer=true"
   ```
6. **Vérification post-écriture** — lancer `verif-produits` pour confirmer que les valeurs sont bien en base :
   ```bash
   mvn spring-boot:run -Dspring-boot.run.profiles=verif-produits \
       -Dspring-boot.run.jvmArguments="-Dverif.ids=X,Y,Z"
   ```

### Règles de sourçage (sécurité éditoriale)

- **Priorité au site fabricant** officiel ; revendeurs utilisés uniquement pour identifier le modèle ou recouper.
- **Pour les marques locales sans site détaillé (Astech, etc.)** : priorité aux **photos de plaques techniques** (plaquette signalétique ou étiquette énergétique visible sur le produit ou ses photos). Ces plaques font foi pour le modèle, la puissance, la consommation et le label énergie.
- Consultation **ponctuelle uniquement** — ne jamais copier une description existante d'un revendeur.
- N'importer que ce qui est **confirmé par les caractéristiques techniques**.
- Ignorer les valeurs marquées `"a_verifier"` — les laisser dans le JSON pour traçabilité.

### Règle de confiance par catégorie de caractéristique

#### Caractéristiques à faible risque → `"valide"` si ≥ 2 sources revendeurs concordantes sans contradiction
Ces données varient peu d'un modèle à l'autre et n'impactent pas directement la décision d'achat :
- `Tension` (ex. 220–240 V / 50 Hz)
- `Classe climatique` (ex. T tropical)
- `Fermeture à clé`
- `Panier amovible`
- `Type de condenseur`

#### Caractéristiques importantes pour l'achat → restent `"a_verifier"` sans site officiel ou photo de plaque
Ces données sont déterminantes pour le choix du client et fréquemment erronées chez les revendeurs :
- `Consommation` (Watt, kWh/an, kWh/24h)
- `Classe énergétique` / `labelEnergie`
- `Poids`
- `Dimensions`
- `Capacité totale` / `Capacité nette`
- `Gaz réfrigérant`
- `Référence` / `modeleFabricant`
- `Niveau sonore`

### Support `effacer` dans colonnesTechniques

Quand une colonne contient une **valeur incorrecte** (ex. capacité stockée par erreur dans le champ `dimensions`), utiliser `effacer: true` + `ecraser: true` au niveau produit pour vider la colonne :

```json
{
  "idProduit": 19,
  "ecraser": true,
  "colonnesTechniques": {
    "dimensions": {"valeur": null, "effacer": true, "source": "...", "confiance": "valide"}
  },
  "attributs": [
    {"nom": "Capacité totale", "valeur": "195 L", "source": "...", "confiance": "valide"}
  ]
}
```

### Runners disponibles (profils Spring dédiés — jamais au démarrage normal)

| Profil | Runner | Usage |
|---|---|---|
| `import-enrichissement` | `ImportEnrichissementRunner` | Import lot JSON (simulation par défaut) |
| `export` | `ExportEnrichissementRunner` | Export fiches produits en JSON pour enrichissement |
| `verif-produits` | `VerifProduitsRunner` | Vérification base par IDs ou recherche dimensions suspectes |
| `migration` | `MigrationImagesRunner` | Migration images `produit_images` → `image_produit` (idempotent) |

### Bilan des lots exécutés (oct. 2026)

| Fichier | Produits ciblés | Résultat | Date |
|---|---|---|---|
| `enrichissement_lot_1.json` | 3, 8, 19, 29, 105 | 3 modifiés : `modeleFabricant` produits 3 (`AST-09RB41-MA`) et 8 (`09INV8MA`), attribut `Fermeture à clé = Oui` produit 19 | 15 oct. 2026 |
| `enrichissement_correction_dimensions.json` | 19–32, 101, 103–105 (17 produits) | 17 modifiés : colonne `dimensions` effacée (contenait la capacité), attribut `Capacité totale` créé pour chacun (valeur numérique, unité L via ModeleAttribut) | 15 oct. 2026 |

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
| `ImageProduit` | `image_produit` | url, publicId, texteAlternatif, ordre, principale. @OneToMany sur Produit avec orphanRemoval. |
| `ModeleAttribut` | `modele_attribut` | Template d'attribut par catégorie (TypeAttribut: TEXT/NUMBER/BOOLEAN/SELECT). Table de jointure `modele_attribut_categories`. |
| `AttributProduit` | `attributs_produit` | Clé/valeur dynamique + référence optionnelle vers `ModeleAttribut` (modele_attribut_id). |

---

## Feuille de route des modules restants

1. **Module zones de livraison** ✅ Résolu — mergé.
2. **Enrichissement automatique des fiches produits** ✅ Résolu — EAN, modeleFabricant, CRUD AttributProduit, ModeleAttribut, score, export, migration images.
3. **Module facturation** — génération de factures/avoirs/devis/BL en PDF à partir des paramètres du groupe `ENTREPRISE_FACTURATION` de `ParametresSite`.
4. **Image principale désignée** ✅ Résolu — ImageProduit.principale garanti par le service (si suppression de la principale, la suivante devient principale). Frontend public doit migrer de `listeImages[]` vers `images[]` puis retirer `listeImages`.
5. **Stockage `public_id` Cloudinary** ✅ Résolu — ImageProduit.publicId stocké explicitement (FileService.UploadResult + uploadFileWithPublicId).
6. **Migrations versionnées** — passer de `ddl-auto=update` à Flyway pour les environnements de production.

---

## État d'avancement (oct. 2026)

| Module | Statut | Branche |
|---|---|---|
| Site configurable (ParametresSite, API config, hardcodes) | ✅ Mergé dans `develop` + `main` | — |
| Entités de contenu gérable (ImageHero, Partenaire, Prestation, MembreEquipe, Realisation) | ✅ Mergé dans `develop` + `main` | — |
| Zones de livraison (ZoneLivraison) | ✅ Mergé dans `develop` + `main` | — |
| Promotions produits (PrixService, PromotionService, CRUD, fiche, catalogue, panier) | ✅ Mergé dans `develop` + `main` | — |
| Enrichissement fiches produits (runners import/export/verif, ModeleAttribut, AttributProduit, ImageProduit, score — lots 1 + correction_dimensions écrits en base oct. 2026) | ✅ Mergé dans `develop` + `main` | — |
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

### 8 — Nettoyer les commandes et comptes de test

Avant la mise en ligne, supprimer toutes les commandes et tous les comptes clients créés pendant le développement et les tests. Depuis la console SQL Neon :

```sql
-- Vérifier d'abord les commandes existantes
SELECT id_commande, numero_commande, date_commande, montant_total FROM commandes ORDER BY date_commande;
-- Supprimer après vérification
DELETE FROM commandes;

-- Vérifier les comptes clients de test (emails @test.local, @test.com, etc.)
SELECT id_client, email, prenom, nom FROM clients ORDER BY id_client;
-- Supprimer les comptes de test après vérification (exemples créés lors des tests de sécurité)
-- sec_test_client@test.local, client2sec@test.local, autre_client_sec@test.local, etc.
DELETE FROM clients WHERE email LIKE '%@test.local' OR email LIKE '%@test.com';
```

### 7 — Railway (si déploiement via Railway)

- Vérifier la configuration du déploiement automatique (branche suivie, triggers).
- **Régénérer les identifiants Railway exposés dans l'historique git** : le mot de passe PostgreSQL Railway (`svYhBmqtXnJkAaFEbairZjCtElvTESMe`) et l'URL interne (`postgres.railway.internal:5432/railway`) étaient committés en clair dans `application.properties` sur `origin/main`. Ces identifiants sont compromis — les réinitialiser depuis le dashboard Railway avant tout déploiement.

### 10 — Migration des statuts de commande (module suivi-commandes)

> ⚠️ **Critique** — sans cette étape, toute commande avec un ancien statut (`PAYEE`, `EN_PREPARATION`, `EXPEDIE`, `REMBOURSEE`) causera une `IllegalArgumentException` au démarrage de l'application et empêchera celle-ci de répondre.

1. **Sauvegarder la base** (branche Neon ou dump Railway) avant toute opération.
2. **Exécuter le script** depuis la console SQL Neon (ou psql) :
   ```bash
   psql $DATABASE_URL -f scripts/migration_statuts_suivi_commandes.sql
   ```
3. **Vérifier** que le dernier SELECT ne retourne aucune ligne (aucun ancien statut résiduel).
4. **Démarrer l'application** — le démarrage doit réussir sans erreur JPA.

Le script est idempotent : relancé deux fois, il n'a aucun effet supplémentaire.

### 9 — Migration des images produit (profil `migration`)

> ⚠️ **Critique** — sans cette étape, les produits n'auront plus d'images en production.

1. **Identifier la vraie base de production** avant toute chose : Neon ou Railway ? Les deux `DATABASE_URL` doivent pointer sur la même base. Configurer la variable d'environnement sur l'hébergeur cible.
2. **Créer une branche de sauvegarde Neon** (ou dump Railway) avant l'opération (voir étape 3).
3. **Lancer le profil `migration` AVANT ou immédiatement au premier démarrage** de la version incluant `feature/modele-produit` :
   ```bash
   mvn spring-boot:run -Dspring-boot.run.profiles=migration
   ```
   Rapport attendu : `X migrées, 0 produits ignorés, 0 URLs sans publicId`.
   Si le rapport indique `0 migrées, N produits ignorés` → migration déjà faite, OK.
4. **Ne jamais supprimer la table `produit_images`** avant d'avoir :
   - Vérifié que toutes les lignes de `image_produit` sont présentes en base production.
   - Confirmé que le frontend affiche bien les images sur toutes les fiches produit.
5. Une fois vérifié, la table `produit_images` peut être supprimée manuellement via SQL :
   ```sql
   DROP TABLE produit_images;
   ```

### 11 — Valider le modèle de facture avant la mise en production

> ⚠️ **Important** — faire valider un modèle de facture imprimé (avec données réelles) par le comptable ou le conseiller fiscal **avant** la mise en production. Points à vérifier : mentions obligatoires selon la réglementation sénégalaise (NINEA, RCCM, numérotation séquentielle, date d'émission, dénomination sociale), traitement de la TVA (assujetti ou non), libellés des conditions de paiement. Ne pas émettre de factures clients sans cette validation.

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

### #4 — Endpoint dupliqué dans `ProduitController` ✅ Résolu
Blocs commentés supprimés (commit `ea3e5c6`).

### #7 — Migration frontend listeImages → images[] (partiellement résolu)
Migration backend terminée (88 images, 34 produits, oct. 2026). `@ElementCollection listeImages` supprimé de `Produit.java`. `ProduitDTO.listeImages` conservé (calculé depuis `images[]`) pour la rétrocompatibilité du frontend public. Une fois le frontend migré pour utiliser `images[]` (`ImageProduitDTO` avec `idImage`, `url`, `publicId`, `principale`), retirer `listeImages` du DTO et du service.

### #8 — Fallback produit_images ✅ Résolu
Migration lancée (oct. 2026) et idempotence vérifiée. Bloc `else if` retiré de `ProduitService.convertToDTO`. `@ElementCollection listeImages` supprimé de `Produit.java`. `MigrationImagesRunner` lit désormais `produit_images` via `JdbcTemplate` (ORDER BY ctid).

### #5 — `FroidCheikhEcommerceApplicationTests` se connecte à la vraie base Neon
Le test de contexte `FroidCheikhEcommerceApplicationTests` charge le contexte Spring complet, se connecte à la base Neon configurée dans les variables d'environnement et exécute `DataInitializer` (seed zones si table vide + SUPER_ADMIN existant). Il ne s'exécute pas en isolation : il dépend d'une base accessible et d'un état cohérent. À remplacer par une base de test dédiée (H2 in-memory ou Testcontainers) pour isoler les tests de l'environnement réel.

### #9 — Garanties `1 ans` à corriger en `1 an`
15 produits Astech (IDs 3, 8, 19, 20, 21, 22, 23, 24, 26, 27, 28, 29, 30, 31, 32) ont `garantie = '1 ans'` (faute) au lieu de `'1 an'`. Les 4 produits SMART TECHNOLOGY (101, 103, 104, 105) ont déjà `'1 an'`. Corriger via SQL ou un lot d'enrichissement dédié avant la mise en production :
```sql
UPDATE produits SET garantie = '1 an' WHERE garantie = '1 ans';
```
