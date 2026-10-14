-- Supprime toutes les zones inventées du mauvais seed.
-- À exécuter UNE SEULE FOIS dans la console SQL Neon (dev).
-- Au prochain démarrage Spring Boot, DataInitializer réinsère les 48 zones correctes.
DELETE FROM zones_livraison;
