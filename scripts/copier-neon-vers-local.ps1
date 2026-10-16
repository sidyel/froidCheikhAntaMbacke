# copier-neon-vers-local.ps1
# Copie complète de la base Neon vers le conteneur Docker local.
# Lancez-le vous-même APRÈS avoir démarré le conteneur (docker compose up -d).
#
# Utilise pg_dump/pg_restore DU CONTENEUR (même version PostgreSQL que Neon)
# pour garantir la compatibilité du dump binaire.

param(
    [switch]$Confirmer   # Passez -Confirmer pour exécuter réellement la copie
)

$root    = Split-Path $PSScriptRoot -Parent
$envFile = Join-Path $root ".env"

if (-not (Test-Path $envFile)) {
    Write-Error "Fichier .env introuvable à : $envFile"; exit 1
}

# Charge les variables Neon depuis .env (valeurs jamais affichées)
$vars = @{}
Get-Content $envFile | Where-Object { $_ -match '^\s*([^#=\s]+)\s*=\s*(.*)$' } | ForEach-Object {
    $vars[$Matches[1]] = $Matches[2]
}

$neonUrl  = $vars['DATABASE_URL']
$neonUser = $vars['DATABASE_USERNAME']
$neonPass = $vars['DATABASE_PASSWORD']

if (-not $neonUrl -or -not $neonUser -or -not $neonPass) {
    Write-Error "DATABASE_URL, DATABASE_USERNAME ou DATABASE_PASSWORD absent du .env"; exit 1
}

# Parse l'URL JDBC Neon
if ($neonUrl -match 'jdbc:postgresql://([^/:]+)(?::(\d+))?/([^?]+)') {
    $neonHost = $Matches[1]
    $neonPort = if ($Matches[2]) { $Matches[2] } else { "5432" }
    $neonDb   = $Matches[3]
} else {
    Write-Error "Format DATABASE_URL non reconnu"; exit 1
}

# Paramètres de la base locale
$localContainer = "froidcheikh-postgres-local"
$localUser      = "froidcheikh_user"
$localDb        = "froidcheikh_local"

# Vérifie que le conteneur tourne
$status = docker inspect --format "{{.State.Running}}" $localContainer 2>$null
if ($status -ne "true") {
    Write-Error "Conteneur '$localContainer' non démarré. Lancez d'abord : docker compose up -d"
    exit 1
}

Write-Host ""
Write-Host "=== Copie Neon → local ==="
Write-Host "  Source  : $neonHost:$neonPort/$neonDb (Neon)"
Write-Host "  Cible   : localhost:5433/$localDb (Docker)"
Write-Host ""

if (-not $Confirmer) {
    Write-Host "  Simulation uniquement — ajoutez -Confirmer pour exécuter."
    Write-Host "  Commandes qui seraient lancées :"
    Write-Host "    1. pg_dump (depuis le conteneur, vers un fichier dump temporaire)"
    Write-Host "    2. pg_restore (dans le conteneur, avec --clean --if-exists)"
    Write-Host "    3. Vérification : SELECT COUNT(*) sur produits, commandes, image_produit"
    exit 0
}

$dumpFile = "/tmp/neon_dump_$(Get-Date -Format 'yyyyMMdd_HHmmss').dump"
Write-Host "Étape 1/3 — pg_dump depuis Neon (format custom)..."

docker run --rm `
    -e PGPASSWORD=$neonPass `
    postgres:16 `
    pg_dump `
        --host=$neonHost `
        --port=$neonPort `
        --username=$neonUser `
        --dbname=$neonDb `
        --format=custom `
        --no-owner `
        --no-acl `
        --sslmode=require `
    > "$env:TEMP\neon_dump.dump"

if ($LASTEXITCODE -ne 0) {
    Write-Error "pg_dump a échoué (code $LASTEXITCODE)"; exit 1
}

$dumpSize = (Get-Item "$env:TEMP\neon_dump.dump").Length / 1MB
Write-Host "  Dump OK — $([Math]::Round($dumpSize, 2)) Mo"

Write-Host "Étape 2/3 — Copie du dump dans le conteneur..."
docker cp "$env:TEMP\neon_dump.dump" "${localContainer}:/tmp/neon_dump.dump"

Write-Host "Étape 3/3 — pg_restore dans la base locale (--clean --if-exists)..."
docker exec -e PGPASSWORD="" $localContainer `
    pg_restore `
        --username=$localUser `
        --dbname=$localDb `
        --clean `
        --if-exists `
        --no-owner `
        --no-acl `
        /tmp/neon_dump.dump

if ($LASTEXITCODE -ne 0) {
    Write-Warning "pg_restore a retourné des avertissements (normal si objets pré-existants)"
}

Write-Host ""
Write-Host "=== Vérification ==="
docker exec $localContainer psql -U $localUser -d $localDb -t -c @"
SELECT
    (SELECT COUNT(*) FROM produits)       AS produits,
    (SELECT COUNT(*) FROM commandes)      AS commandes,
    (SELECT COUNT(*) FROM image_produit)  AS images_produit;
"@

Write-Host ""
Write-Host "Comparez ces chiffres avec votre base Neon."
Write-Host "Si identiques : basculez .env.local et démarrez le backend."

# Nettoyage
Remove-Item "$env:TEMP\neon_dump.dump" -ErrorAction SilentlyContinue
docker exec $localContainer rm -f /tmp/neon_dump.dump
