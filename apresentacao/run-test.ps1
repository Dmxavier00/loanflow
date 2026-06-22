$ErrorActionPreference = 'Stop'

$projectRoot = Resolve-Path (Join-Path $PSScriptRoot '..')
$nodeHome = Join-Path $projectRoot '.tools\node\node-v20.20.2-win-x64'
$nodeExe = Join-Path $nodeHome 'node.exe'

if (-not (Test-Path $nodeExe)) {
    throw "Node local nao encontrado em $nodeExe"
}

Set-Location $PSScriptRoot

& $nodeExe --input-type=module -e "import('./src/biblioteca/bankAccountPolicy.test.js')"
