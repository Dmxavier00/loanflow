[CmdletBinding()]
param(
    [string]$DbUrl = $(if ($env:LOANFLOW_DB_URL) { $env:LOANFLOW_DB_URL } else { 'jdbc:sqlserver://localhost:1433;databaseName=loanflow;encrypt=false;trustServerCertificate=true' }),
    [string]$DbUsername = $(if ($env:LOANFLOW_DB_USERNAME) { $env:LOANFLOW_DB_USERNAME } else { 'loanflow_user' }),
    [string]$DbPassword = $(if ($env:LOANFLOW_DB_PASSWORD) { $env:LOANFLOW_DB_PASSWORD } else { 'Loanflow@12345' }),
    [string]$JwtSecret = $(if ($env:JWT_SECRET) { $env:JWT_SECRET } else { 'loanflow-demo-secret-change-me-loanflow-demo-secret-change-me' }),
    [int]$Port = 8080,
    [string]$OpenPath = '/login',
    [int]$StartupTimeoutSeconds = 90,
    [switch]$RebuildFrontend,
    [switch]$DryRun
)

$ErrorActionPreference = 'Stop'

$projectRoot = Resolve-Path $PSScriptRoot
$runDemoScript = Join-Path $projectRoot 'run-demo.ps1'
$readinessUrl = "http://localhost:$Port/index.html"
$browserUrl = "http://localhost:$Port$OpenPath"

function Quote-Argument {
    param([string]$Value)

    if ($null -eq $Value) {
        return '""'
    }

    return '"' + $Value.Replace('"', '\"') + '"'
}

function Test-HttpReady {
    param([string]$Url)

    try {
        $response = Invoke-WebRequest -UseBasicParsing -Uri $Url -TimeoutSec 5
        return $response.StatusCode -eq 200
    } catch {
        return $false
    }
}

if (-not (Test-Path $runDemoScript)) {
    throw "Nao encontrei o script principal em $runDemoScript"
}

$argumentParts = @(
    '-ExecutionPolicy', 'Bypass',
    '-NoExit',
    '-File', (Quote-Argument $runDemoScript),
    '-DbUrl', (Quote-Argument $DbUrl),
    '-DbUsername', (Quote-Argument $DbUsername),
    '-DbPassword', (Quote-Argument $DbPassword),
    '-JwtSecret', (Quote-Argument $JwtSecret),
    '-Port', $Port
)

if ($RebuildFrontend) {
    $argumentParts += '-RebuildFrontend'
}

$argumentList = $argumentParts -join ' '

Write-Host "API sera iniciada em uma nova janela na porta $Port"
Write-Host "A SPA sera aberta automaticamente em $browserUrl"

if ($DryRun) {
    Write-Host ''
    Write-Host 'Dry run: a janela da API seria iniciada com este comando:'
    Write-Host "powershell $argumentList"
    Write-Host "Dry run: o script aguardaria ate $StartupTimeoutSeconds segundos antes de abrir o navegador."
    return
}

if (Test-HttpReady -Url $readinessUrl) {
    Write-Host "A aplicacao ja esta respondendo em $readinessUrl"
    Start-Process $browserUrl
    Write-Host "Navegador aberto em $browserUrl"
    return
}

Start-Process `
    -FilePath 'powershell.exe' `
    -ArgumentList $argumentList `
    -WorkingDirectory $projectRoot `
    -WindowStyle Normal

Write-Host 'Aguardando a aplicacao ficar pronta...'

$deadline = (Get-Date).AddSeconds($StartupTimeoutSeconds)
while ((Get-Date) -lt $deadline) {
    if (Test-HttpReady -Url $readinessUrl) {
        Start-Process $browserUrl
        Write-Host "Navegador aberto em $browserUrl"
        return
    }

    Start-Sleep -Seconds 2
}

Write-Warning "A aplicacao nao respondeu em $readinessUrl dentro de $StartupTimeoutSeconds segundos."
Write-Warning 'Confira a nova janela da API para ver os logs de inicializacao.'
