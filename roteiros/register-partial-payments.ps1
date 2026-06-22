param(
    [string]$BaseUrl = "http://localhost:8080",
    [string]$BatchFile = "",
    [int]$SolicitantesParaPagar = 4
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

$paymentsDir = Join-Path $PSScriptRoot "..\artefatos\payment-batches"
New-Item -ItemType Directory -Path $paymentsDir -Force | Out-Null

if ([string]::IsNullOrWhiteSpace($BatchFile)) {
    $latest = Get-ChildItem (Join-Path $PSScriptRoot "..\artefatos\seed-batches\seed-batch-*.json") |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1
    if (-not $latest) {
        throw "Nenhum arquivo de lote encontrado em artefatos\\seed-batches."
    }
    $BatchFile = $latest.FullName
}

$seedData = Get-Content $BatchFile -Raw | ConvertFrom-Json
$paymentBatchId = Get-Date -Format "yyyyMMddHHmmss"
$resultPath = Join-Path $paymentsDir ("payment-batch-{0}.json" -f $paymentBatchId)

$results = [ordered]@{
    paymentBatchId = $paymentBatchId
    sourceBatchFile = (Resolve-Path $BatchFile).Path
    sourceSeedBatchId = $seedData.batchId
    baseUrl = $BaseUrl
    startedAt = (Get-Date).ToString("s")
    pagamentos = @()
}

function Get-ErrorMessage {
    param([System.Management.Automation.ErrorRecord]$ErrorRecord)
    if ($ErrorRecord.ErrorDetails -and $ErrorRecord.ErrorDetails.Message) {
        return $ErrorRecord.ErrorDetails.Message
    }
    return $ErrorRecord.Exception.Message
}

function Invoke-Loanflow {
    param(
        [string]$Method,
        [string]$Path,
        [object]$Body = $null,
        [string]$Token
    )

    $headers = @{ Authorization = "Bearer $Token" }
    $params = @{
        Method = $Method
        Uri = ($BaseUrl.TrimEnd("/") + $Path)
        Headers = $headers
        ContentType = "application/json"
    }

    if ($null -ne $Body) {
        $params["Body"] = ($Body | ConvertTo-Json -Depth 10)
    }

    try {
        return Invoke-RestMethod @params
    }
    catch {
        $message = Get-ErrorMessage -ErrorRecord $_
        throw ("Falha em {0} {1}: {2}" -f $Method, $Path, $message)
    }
}

function Login-Solicitante {
    param([string]$Email)

    $login = Invoke-RestMethod -Method "POST" -Uri ($BaseUrl.TrimEnd("/") + "/auth/login") -ContentType "application/json" -Body (@{
        email = $Email
        senha = "Senha123!"
    } | ConvertTo-Json)

    return $login.accessToken
}

function Get-SolicitanteSession {
    param([string]$Email)
    $session = $seedData.solicitantes | Where-Object { $_.email -eq $Email } | Select-Object -First 1
    if (-not $session) {
        throw "Solicitante do email $Email nao encontrado no lote de origem."
    }
    $session | Add-Member -NotePropertyName freshToken -NotePropertyValue (Login-Solicitante -Email $Email) -Force
    return $session
}

function Register-Payment {
    param(
        [object]$Solicitante,
        [object]$Parcela,
        [decimal]$ValorPago,
        [string]$FormaPagamento,
        [string]$Comprovante
    )

    $payload = @{
        valorPago = $ValorPago
        formaPagamento = $FormaPagamento
        comprovante = $Comprovante
    }

            return Invoke-Loanflow -Method "POST" -Path ("/parcelas/{0}/pagamentos" -f $Parcela.id) -Body $payload -Token $Solicitante.freshToken
}

try {
    $contratos = @($seedData.contratosFormalizados | Select-Object -First $SolicitantesParaPagar)
    if ($contratos.Count -eq 0) {
        throw "Nenhum contrato formalizado encontrado no lote informado."
    }

    foreach ($contrato in $contratos) {
        $solicitante = Get-SolicitanteSession -Email $contrato.solicitanteEmail
        $parcelas = @((Invoke-Loanflow -Method "GET" -Path "/parcelas" -Token $solicitante.freshToken) |
            Where-Object { $_.contratoId -eq $contrato.contratoId } |
            Sort-Object numero)

        if ($parcelas.Count -lt 1) {
            throw "Nenhuma parcela acessivel encontrada para o contrato $($contrato.contratoId)."
        }

        $pagamentosPlanejados = @()
        $primeiraParcela = $parcelas[0]
        $pagamentosPlanejados += [pscustomobject]@{
            parcela = $primeiraParcela
            valorPago = [decimal]$primeiraParcela.valorPrevisto
            formaPagamento = "PIX_MANUAL"
            comprovante = "PIX manual simulado - parcela 1 do contrato $($contrato.contratoId)."
        }

        if ($parcelas.Count -ge 2) {
            $segundaParcela = $parcelas[1]
            $valorParcial = [decimal]([Math]::Round(([double]$segundaParcela.valorPrevisto * 0.45), 2))
            if ($valorParcial -le 0) {
                $valorParcial = [decimal]1.00
            }
            $pagamentosPlanejados += [pscustomobject]@{
                parcela = $segundaParcela
                valorPago = $valorParcial
                formaPagamento = "TRANSFERENCIA_SIMULADA"
                comprovante = "Transferencia simulada parcial - parcela 2 do contrato $($contrato.contratoId)."
            }
        }

        foreach ($plano in $pagamentosPlanejados) {
            $pagamento = Register-Payment -Solicitante $solicitante -Parcela $plano.parcela -ValorPago $plano.valorPago -FormaPagamento $plano.formaPagamento -Comprovante $plano.comprovante
            $parcelasAtualizadas = @((Invoke-Loanflow -Method "GET" -Path "/parcelas" -Token $solicitante.freshToken) |
                Where-Object { $_.contratoId -eq $contrato.contratoId -and $_.id -eq $plano.parcela.id })
            $parcelaAtualizada = $parcelasAtualizadas | Select-Object -First 1

            $results.pagamentos += [pscustomobject]@{
                contratoId = $contrato.contratoId
                propostaId = $contrato.propostaId
                solicitanteEmail = $contrato.solicitanteEmail
                credorEmail = $contrato.credorEmail
                parcelaId = $plano.parcela.id
                numeroParcela = $plano.parcela.numero
                valorPrevisto = [decimal]$plano.parcela.valorPrevisto
                valorPagoRegistrado = [decimal]$pagamento.valorPago
                formaPagamento = $pagamento.formaPagamento
                pagamentoId = $pagamento.id
                statusParcelaAtual = $parcelaAtualizada.status
                valorPagoAcumuladoAtual = [decimal]$parcelaAtualizada.valorPagoAcumulado
            }
        }
    }

    $results.finishedAt = (Get-Date).ToString("s")
    $results.summary = [ordered]@{
        contratosAfetados = (@($results.pagamentos | Select-Object -ExpandProperty contratoId -Unique)).Count
        pagamentosRegistrados = $results.pagamentos.Count
        parcelasTotalmentePagas = (@($results.pagamentos | Where-Object { $_.statusParcelaAtual -eq "PAGA" })).Count
        parcelasParcialmentePagas = (@($results.pagamentos | Where-Object { $_.statusParcelaAtual -eq "PARCIALMENTE_PAGA" })).Count
    }

    $results | ConvertTo-Json -Depth 10 | Set-Content -Path $resultPath

    Write-Host ("Lote de pagamentos {0} concluido." -f $paymentBatchId)
    Write-Host ("Contratos afetados: {0}" -f $results.summary.contratosAfetados)
    Write-Host ("Pagamentos registrados: {0}" -f $results.summary.pagamentosRegistrados)
    Write-Host ("Resultado salvo em: {0}" -f (Resolve-Path $resultPath))
}
catch {
    $results.failedAt = (Get-Date).ToString("s")
    $results.failure = $_.Exception.Message
    $results | ConvertTo-Json -Depth 10 | Set-Content -Path $resultPath
    throw
}
