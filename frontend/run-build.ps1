$ErrorActionPreference = 'Stop'

$projectRoot = Resolve-Path (Join-Path $PSScriptRoot '..')
$nodeHome = Join-Path $projectRoot '.tools\node\node-v20.20.2-win-x64'

if (-not (Test-Path $nodeHome)) {
    throw "Node local não encontrado em $nodeHome"
}

$env:Path = "$nodeHome;$env:Path"
Set-Location $PSScriptRoot

& (Join-Path $nodeHome 'npm.cmd') run build
