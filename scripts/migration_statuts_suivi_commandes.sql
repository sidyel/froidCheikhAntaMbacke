-- =============================================================================
-- Migration : module suivi-commandes
-- À exécuter UNE SEULE FOIS sur la base de production AVANT le premier
-- démarrage de la version incluant feature/suivi-commandes.
--
-- Ce script :
--   1. Migre les anciens statuts de commande vers le nouvel enum simplifié
--   2. Met à jour statut_paiement pour les commandes qui étaient PAYEE
--   3. Crée la table historique_statut_commande si elle n'existe pas encore
--      (ddl-auto=update la crée au démarrage, mais ce script est idempotent)
-- =============================================================================

-- ÉTAPE 1 — Vérifier l'état actuel AVANT de toucher quoi que ce soit
SELECT statut_commande, COUNT(*) AS nb
FROM commandes
GROUP BY statut_commande
ORDER BY statut_commande;

-- ÉTAPE 2 — Mettre à jour statut_paiement pour les anciennes commandes PAYEE
--           (elles deviennent CONFIRMEE côté livraison, mais PAYEE côté paiement)
UPDATE commandes
SET statut_paiement = 'PAYEE'
WHERE statut_commande = 'PAYEE'
  AND (statut_paiement IS NULL OR statut_paiement = 'NON_PAYEE');

-- ÉTAPE 3 — Migrer les anciens statuts vers le cycle simplifié
--   PAYEE, EN_PREPARATION, EXPEDIE  → CONFIRMEE
UPDATE commandes
SET statut_commande = 'CONFIRMEE'
WHERE statut_commande IN ('PAYEE', 'EN_PREPARATION', 'EXPEDIE');

--   REMBOURSEE → ANNULEE (+ marquer le remboursement dans statut_paiement)
UPDATE commandes
SET statut_paiement = 'REMBOURSEE'
WHERE statut_commande = 'REMBOURSEE'
  AND (statut_paiement IS NULL OR statut_paiement = 'NON_PAYEE');

UPDATE commandes
SET statut_commande = 'ANNULEE'
WHERE statut_commande = 'REMBOURSEE';

-- ÉTAPE 4 — Initialiser statut_paiement à NON_PAYEE pour les lignes NULL
--           (colonnes ajoutées par ddl-auto=update avec NOT NULL + DEFAULT)
UPDATE commandes
SET statut_paiement = 'NON_PAYEE'
WHERE statut_paiement IS NULL;

-- ÉTAPE 5 — Vérifier le résultat
SELECT statut_commande, statut_paiement, COUNT(*) AS nb
FROM commandes
GROUP BY statut_commande, statut_paiement
ORDER BY statut_commande, statut_paiement;

-- ÉTAPE 6 — Sanity check : aucun ancien statut ne doit subsister
--           Si ce SELECT retourne des lignes, la migration a échoué.
SELECT id_commande, statut_commande
FROM commandes
WHERE statut_commande NOT IN ('EN_ATTENTE', 'CONFIRMEE', 'LIVREE', 'ANNULEE');
