# neon-version.ps1 — affiche la version PostgreSQL de Neon
# Lit les identifiants depuis .env (jamais affichés). Lancez-le vous-même.

$envFile = Join-Path $PSScriptRoot ".." ".env"
if (-not (Test-Path $envFile)) {
    Write-Error "Fichier .env introuvable à : $envFile"; exit 1
}

# Charge uniquement les variables nécessaires, sans afficher aucune valeur
$vars = @{}
Get-Content $envFile | Where-Object { $_ -match '^\s*([^#=\s]+)\s*=\s*(.*)$' } | ForEach-Object {
    $vars[$Matches[1]] = $Matches[2]
}

$url  = $vars['DATABASE_URL']
$user = $vars['DATABASE_USERNAME']
$pass = $vars['DATABASE_PASSWORD']

if (-not $url -or -not $user -or -not $pass) {
    Write-Error "DATABASE_URL, DATABASE_USERNAME ou DATABASE_PASSWORD absent du .env"; exit 1
}

# Extrait host/port/dbname depuis l'URL JDBC
# Format attendu : jdbc:postgresql://host[:port]/dbname?params
if ($url -match 'jdbc:postgresql://([^/:]+)(?::(\d+))?/([^?]+)') {
    $pgHost = $Matches[1]
    $pgPort = if ($Matches[2]) { $Matches[2] } else { "5432" }
    $pgDb   = $Matches[3]
} else {
    Write-Error "Format DATABASE_URL non reconnu : $url"; exit 1
}

# Lance psql via Docker (évite d'avoir psql installé localement)
# Le mot de passe est passé via variable d'environnement PGPASSWORD, jamais affiché
Write-Host "Connexion à $pgHost:$pgPort/$pgDb en cours..."
$env:PGPASSWORD = $pass
$result = docker run --rm `
    -e PGPASSWORD=$pass `
    postgres:16 `
    psql "postgresql://${user}@${pgHost}:${pgPort}/${pgDb}?sslmode=require" `
    -t -c "SELECT version();"
Remove-Item Env:\PGPASSWORD -ErrorAction SilentlyContinue
Write-Host $result
