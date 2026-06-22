[CmdletBinding()]
param(
    [int]$Port = 8080
)

$ErrorActionPreference = 'Stop'

$connections = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue

if (-not $connections) {
    Write-Host "Nenhum processo em escuta na porta $Port."
    return
}

$processIds = $connections | Select-Object -ExpandProperty OwningProcess -Unique

foreach ($processId in $processIds) {
    $process = Get-Process -Id $processId -ErrorAction SilentlyContinue

    if (-not $process) {
        Write-Host "Processo $processId ja nao esta mais ativo."
        continue
    }

    Write-Host "Encerrando processo $($process.ProcessName) (PID $processId) na porta $Port..."
    Stop-Process -Id $processId -Force
}

Start-Sleep -Milliseconds 500

if (Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue) {
    throw "A porta $Port continua ocupada apos a tentativa de encerramento."
}

Write-Host "Porta $Port liberada com sucesso."
