[CmdletBinding()]
param(
    [string]$DbUrl = $(if ($env:LOANFLOW_DB_URL) { $env:LOANFLOW_DB_URL } else { 'jdbc:sqlserver://localhost:1433;databaseName=loanflow;encrypt=false;trustServerCertificate=true' }),
    [string]$DbUsername = $(if ($env:LOANFLOW_DB_USERNAME) { $env:LOANFLOW_DB_USERNAME } else { 'loanflow_user' }),
    [string]$DbPassword = $(if ($env:LOANFLOW_DB_PASSWORD) { $env:LOANFLOW_DB_PASSWORD } else { 'Loanflow@12345' }),
    [string]$JwtSecret = $(if ($env:JWT_SECRET) { $env:JWT_SECRET } else { 'loanflow-demo-secret-change-me-loanflow-demo-secret-change-me' }),
    [int]$Port = 8080,
    [switch]$RebuildFrontend,
    [switch]$DryRun
)

$ErrorActionPreference = 'Stop'

$projectRoot = Resolve-Path $PSScriptRoot
$frontendDistIndex = Join-Path $projectRoot 'apresentacao\dist\index.html'
$frontendBuildScript = Join-Path $projectRoot 'apresentacao\run-build.ps1'
$mavenWrapper = Join-Path $projectRoot 'mvnw.cmd'

function Assert-MinLength {
    param(
        [string]$Value,
        [int]$MinLength,
        [string]$Name
    )

    if ([string]::IsNullOrWhiteSpace($Value) -or $Value.Length -lt $MinLength) {
        throw "$Name precisa ter pelo menos $MinLength caracteres."
    }
}

function Mask-Secret {
    param([string]$Value)

    if ([string]::IsNullOrEmpty($Value)) {
        return '<vazio>'
    }

    if ($Value.Length -le 4) {
        return '*' * $Value.Length
    }

    return ('*' * ($Value.Length - 4)) + $Value.Substring($Value.Length - 4)
}

if (-not (Test-Path $mavenWrapper)) {
    throw "Nao encontrei o Maven Wrapper em $mavenWrapper"
}

Assert-MinLength -Value $JwtSecret -MinLength 32 -Name 'JWT_SECRET'

if ($RebuildFrontend -or -not (Test-Path $frontendDistIndex)) {
    if (-not (Test-Path $frontendBuildScript)) {
        throw "Nao encontrei o script de build do front-end em $frontendBuildScript"
    }

    if ($DryRun) {
        Write-Host 'Dry run: o front-end seria rebuildado antes da API subir.'
    } else {
        Write-Host 'Gerando build do front-end para a demonstracao...'
        & $frontendBuildScript
    }
} else {
    Write-Host "Usando build existente da SPA em $frontendDistIndex"
}

$env:LOANFLOW_DB_URL = $DbUrl
$env:LOANFLOW_DB_USERNAME = $DbUsername
$env:LOANFLOW_DB_PASSWORD = $DbPassword
$env:JWT_SECRET = $JwtSecret
$env:LOANFLOW_FRONTEND_AUTO_BUILD = 'false'

Write-Host ''
Write-Host 'Configuracao de demonstracao aplicada:'
Write-Host "  Porta: $Port"
Write-Host "  Banco: $DbUrl"
Write-Host "  Usuario: $DbUsername"
Write-Host "  Senha: $(Mask-Secret $DbPassword)"
Write-Host "  JWT_SECRET: $(Mask-Secret $JwtSecret)"
Write-Host "  Front-end auto build: $env:LOANFLOW_FRONTEND_AUTO_BUILD"

if ($DryRun) {
    Write-Host ''
    Write-Host 'Dry run concluido. Nenhum processo foi iniciado.'
    return
}

Set-Location $projectRoot

& $mavenWrapper `
    "-Dspring-boot.run.jvmArguments=-Dloanflow.frontend.auto-build=false" `
    "-Dspring-boot.run.arguments=--server.port=$Port" `
    'spring-boot:run'
