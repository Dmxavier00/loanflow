param(
    [string]$BaseUrl = "http://localhost:8080",
    [int]$SolicitanteCount = 20,
    [int]$CredorCount = 10,
    [string]$BatchId = (Get-Date -Format "yyyyMMddHHmmss")
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

$outputDir = Join-Path $PSScriptRoot "..\artifacts\seed-batches"
New-Item -ItemType Directory -Path $outputDir -Force | Out-Null
$resultPath = Join-Path $outputDir ("seed-batch-{0}.json" -f $BatchId)

$banks = @(
    "Banco do Brasil",
    "Bradesco",
    "Santander",
    "Nubank",
    "Inter",
    "C6 Bank",
    "BTG Pactual",
    "Sicredi",
    "Sicoob",
    "Banco Safra",
    "PagBank",
    "Mercado Pago"
)

$firstNames = @(
    "Ana", "Bruno", "Carla", "Diego", "Elisa", "Felipe", "Gabriela", "Henrique",
    "Isabela", "Joao", "Karen", "Lucas", "Marina", "Nicolas", "Olivia", "Paulo",
    "Renata", "Sergio", "Talita", "Vinicius", "Yasmin", "Rafael", "Bianca", "Caio"
)

$lastNames = @(
    "Almeida", "Barbosa", "Campos", "Dias", "Esteves", "Ferreira", "Gomes",
    "Henriques", "Lima", "Martins", "Nogueira", "Oliveira", "Pereira", "Queiroz",
    "Ribeiro", "Santana", "Teixeira", "Vieira", "Costa", "Moura", "Rocha", "Araujo"
)

$solicitanteProfessions = @(
    "Analista Financeiro",
    "Vendedora Autonoma",
    "Motorista de Aplicativo",
    "Tecnico em Enfermagem",
    "Microempreendedor",
    "Assistente Administrativo",
    "Cabeleireira",
    "Professor Particular",
    "Confeiteira",
    "Tecnico em Informatica"
)

$credorProfessions = @(
    "Investidor Pessoa Fisica",
    "Empresario",
    "Consultor Patrimonial",
    "Socio de Comercio",
    "Administrador de Carteira Propria"
)

$tipoOcupacaoOptions = @("CLT", "AUTONOMO", "MEI", "SERVIDOR", "EMPRESARIO")
$estadoCivilOptions = @("SOLTEIRO", "CASADO", "UNIAO_ESTAVEL", "DIVORCIADO")
$tipoDocumentoOptions = @("RG", "CNH")
$tipoContaOptions = @("CORRENTE", "POUPANCA", "PAGAMENTO")

$addresses = @(
    @{ cep = "01001-000"; logradouro = "Praca da Se"; numeroBase = 100; bairro = "Centro"; cidade = "Sao Paulo"; uf = "SP"; complemento = "Sala" },
    @{ cep = "01310-100"; logradouro = "Avenida Paulista"; numeroBase = 900; bairro = "Bela Vista"; cidade = "Sao Paulo"; uf = "SP"; complemento = "Conjunto" },
    @{ cep = "30130-110"; logradouro = "Avenida Afonso Pena"; numeroBase = 700; bairro = "Centro"; cidade = "Belo Horizonte"; uf = "MG"; complemento = "Andar" },
    @{ cep = "80010-000"; logradouro = "Rua Marechal Deodoro"; numeroBase = 450; bairro = "Centro"; cidade = "Curitiba"; uf = "PR"; complemento = "Sala" },
    @{ cep = "40020-000"; logradouro = "Avenida Sete de Setembro"; numeroBase = 1200; bairro = "Centro"; cidade = "Salvador"; uf = "BA"; complemento = "Loja" },
    @{ cep = "60060-230"; logradouro = "Avenida Dom Luis"; numeroBase = 300; bairro = "Aldeota"; cidade = "Fortaleza"; uf = "CE"; complemento = "Conjunto" },
    @{ cep = "20040-020"; logradouro = "Rua da Assembleia"; numeroBase = 50; bairro = "Centro"; cidade = "Rio de Janeiro"; uf = "RJ"; complemento = "Sala" },
    @{ cep = "90010-150"; logradouro = "Rua dos Andradas"; numeroBase = 640; bairro = "Centro Historico"; cidade = "Porto Alegre"; uf = "RS"; complemento = "Andar" }
)

$purposeCatalog = @(
    @{
        categoria = "CAPITAL_DE_GIRO"
        finalidade = "Capital de giro para estoque"
        descricao = "Reforco de caixa para repor mercadorias e manter o fluxo de vendas."
        prazoMin = 6
        prazoMax = 10
        taxaMin = 5.5
        taxaMax = 9.5
    },
    @{
        categoria = "REFORMA"
        finalidade = "Reforma do ponto comercial"
        descricao = "Pequena reforma para melhorar a fachada e adequar o espaco de atendimento."
        prazoMin = 8
        prazoMax = 12
        taxaMin = 6.0
        taxaMax = 10.5
    },
    @{
        categoria = "QUITACAO_DE_DIVIDAS"
        finalidade = "Quitacao de dividas de curto prazo"
        descricao = "Consolidacao de compromissos mais caros para reorganizar o fluxo financeiro."
        prazoMin = 6
        prazoMax = 12
        taxaMin = 7.0
        taxaMax = 12.0
    },
    @{
        categoria = "EMERGENCIA"
        finalidade = "Reserva para emergencia familiar"
        descricao = "Cobertura de despesas urgentes sem comprometer a renda dos proximos meses."
        prazoMin = 4
        prazoMax = 8
        taxaMin = 5.0
        taxaMax = 8.5
    },
    @{
        categoria = "ESTUDO"
        finalidade = "Curso de qualificacao profissional"
        descricao = "Investimento em curso tecnico e compra de material para ampliar a renda."
        prazoMin = 6
        prazoMax = 12
        taxaMin = 5.0
        taxaMax = 9.0
    },
    @{
        categoria = "SAUDE"
        finalidade = "Tratamento e exames de saude"
        descricao = "Pagamento de consultas, exames e medicamentos com prazo mais confortavel."
        prazoMin = 4
        prazoMax = 10
        taxaMin = 5.0
        taxaMax = 8.0
    },
    @{
        categoria = "OUTRA"
        finalidade = "Ajustes operacionais do negocio"
        descricao = "Melhorias pontuais em equipamentos e rotina de trabalho para manter a operacao."
        prazoMin = 5
        prazoMax = 10
        taxaMin = 6.0
        taxaMax = 9.5
    }
)

$results = [ordered]@{
    batchId = $BatchId
    baseUrl = $BaseUrl
    startedAt = (Get-Date).ToString("s")
    solicitantes = @()
    credores = @()
    propostas = @()
    contratosFormalizados = @()
}

$seedNumber = [int64]($BatchId.Substring([Math]::Max(0, $BatchId.Length - 9)))
$rng = [System.Random]::new([int]($seedNumber % [int]::MaxValue))

function Get-RandomItem {
    param([object[]]$Items)
    return $Items[$rng.Next(0, $Items.Count)]
}

function Get-RandomDouble {
    param([double]$Min, [double]$Max)
    return $Min + (($Max - $Min) * $rng.NextDouble())
}

function Get-RandomDecimal {
    param([double]$Min, [double]$Max)
    return [decimal]([Math]::Round((Get-RandomDouble -Min $Min -Max $Max), 2))
}

function Get-RandomDateIso {
    param([int]$AgeMin, [int]$AgeMax)
    $days = $rng.Next($AgeMin * 365, $AgeMax * 365)
    return (Get-Date).Date.AddDays(-$days).ToString("yyyy-MM-dd")
}

function Remove-AccentsLikeSlug {
    param([string]$Text)
    $normalized = $Text.ToLowerInvariant()
    $normalized = $normalized -replace "[^a-z0-9]+", "."
    return ($normalized.Trim("."))
}

function New-Phone {
    $ddd = @(11, 21, 31, 41, 51, 61, 71, 81)[$rng.Next(0, 8)]
    $prefixo = $rng.Next(91000, 99999)
    $sufixo = $rng.Next(1000, 9999)
    return "({0}) {1}-{2}" -f $ddd, $prefixo, $sufixo
}

function Get-CpfCheckDigit {
    param(
        [string]$BaseDigits,
        [int]$PesoInicial
    )
    $soma = 0
    for ($i = 0; $i -lt $BaseDigits.Length; $i++) {
        $soma += [int]::Parse($BaseDigits[$i]) * ($PesoInicial - $i)
    }
    $resto = $soma % 11
    if ($resto -lt 2) {
        return 0
    }
    return 11 - $resto
}

function New-ValidCpf {
    param([int64]$Seed)
    $base = "{0:d9}" -f ($Seed % 1000000000)
    $d1 = Get-CpfCheckDigit -BaseDigits $base -PesoInicial 10
    $d2 = Get-CpfCheckDigit -BaseDigits ($base + $d1) -PesoInicial 11
    $cpf = $base + $d1 + $d2
    return "{0}.{1}.{2}-{3}" -f $cpf.Substring(0, 3), $cpf.Substring(3, 3), $cpf.Substring(6, 3), $cpf.Substring(9, 2)
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
        [string]$Token = $null
    )

    $headers = @{}
    if ($Token) {
        $headers["Authorization"] = "Bearer $Token"
    }

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

function New-FullName {
    $first = Get-RandomItem -Items $firstNames
    $last1 = Get-RandomItem -Items $lastNames
    $last2 = Get-RandomItem -Items $lastNames
    while ($last2 -eq $last1) {
        $last2 = Get-RandomItem -Items $lastNames
    }
    return "$first $last1 $last2"
}

function New-AddressData {
    param([int]$Index)
    $base = Get-RandomItem -Items $addresses
    return [ordered]@{
        cep = $base.cep
        logradouro = $base.logradouro
        numero = [string]($base.numeroBase + $Index)
        complemento = "{0} {1}" -f $base.complemento, (($Index % 20) + 1)
        bairro = $base.bairro
        cidade = $base.cidade
        uf = $base.uf
    }
}

function New-BankAccount {
    param(
        [string]$EmailSlug,
        [int]$Index
    )
    $agencia = "{0:0000}-{1}" -f $rng.Next(1, 9999), $rng.Next(0, 9)
    $numeroConta = "{0:000000}-{1}" -f $rng.Next(1, 999999), $rng.Next(0, 9)
    return [ordered]@{
        banco = Get-RandomItem -Items $banks
        agencia = $agencia
        numeroConta = $numeroConta
        tipoConta = Get-RandomItem -Items $tipoContaOptions
        chavePix = "{0}.{1}@pix.loanflow.test" -f $EmailSlug, $Index
    }
}

function Register-Solicitante {
    param([int]$Index)

    $fullName = New-FullName
    $slug = Remove-AccentsLikeSlug -Text $fullName
    $email = "{0}.sol.{1}.{2}@loanflow.test" -f $slug, $BatchId, $Index
    $address = New-AddressData -Index $Index
    $documentType = Get-RandomItem -Items $tipoDocumentoOptions
    $documentNumber = if ($documentType -eq "CNH") { [string]$rng.Next(100000000, 999999999) } else { [string]$rng.Next(1000000, 99999999) }
    $renda = Get-RandomDecimal -Min 4200 -Max 15000
    $payload = [ordered]@{
        nome = $fullName
        cpf = New-ValidCpf -Seed ($seedNumber + $Index)
        email = $email
        estadoCivil = Get-RandomItem -Items $estadoCivilOptions
        nacionalidade = "Brasileira"
        profissao = Get-RandomItem -Items $solicitanteProfessions
        dataNascimento = Get-RandomDateIso -AgeMin 24 -AgeMax 57
        telefone = New-Phone
        tipoDocumentoIdentidade = $documentType
        documentoIdentidade = $documentNumber
        orgaoEmissor = if ($documentType -eq "CNH") { "DETRAN-{0}" -f $address.uf } else { "SSP-{0}" -f $address.uf }
        pessoaExpostaPoliticamente = $false
        endereco = $address
        senha = "Senha123!"
        papel = "SOLICITANTE"
        rendaMensal = $renda
        tipoOcupacao = Get-RandomItem -Items $tipoOcupacaoOptions
        saldoDisponivelSimulado = $null
        contaBancaria = New-BankAccount -EmailSlug $slug -Index $Index
    }

    $response = Invoke-Loanflow -Method "POST" -Path "/auth/register" -Body $payload
    return [pscustomobject]@{
        index = $Index
        nome = $fullName
        email = $email
        cpf = $payload.cpf
        rendaMensal = [decimal]$renda
        token = $response.accessToken
        usuarioId = $response.usuario.id
        solicitanteId = $response.usuario.solicitanteId
        contaBancariaId = $response.usuario.contaBancariaId
    }
}

function Register-Credor {
    param([int]$Index)

    $fullName = New-FullName
    $slug = Remove-AccentsLikeSlug -Text $fullName
    $email = "{0}.cred.{1}.{2}@loanflow.test" -f $slug, $BatchId, $Index
    $address = New-AddressData -Index ($Index + 200)
    $documentType = Get-RandomItem -Items $tipoDocumentoOptions
    $documentNumber = if ($documentType -eq "CNH") { [string]$rng.Next(100000000, 999999999) } else { [string]$rng.Next(1000000, 99999999) }
    $saldo = Get-RandomDecimal -Min 35000 -Max 120000
    $payload = [ordered]@{
        nome = $fullName
        cpf = New-ValidCpf -Seed ($seedNumber + 1000 + $Index)
        email = $email
        estadoCivil = Get-RandomItem -Items $estadoCivilOptions
        nacionalidade = "Brasileira"
        profissao = Get-RandomItem -Items $credorProfessions
        dataNascimento = Get-RandomDateIso -AgeMin 30 -AgeMax 63
        telefone = New-Phone
        tipoDocumentoIdentidade = $documentType
        documentoIdentidade = $documentNumber
        orgaoEmissor = if ($documentType -eq "CNH") { "DETRAN-{0}" -f $address.uf } else { "SSP-{0}" -f $address.uf }
        pessoaExpostaPoliticamente = $false
        endereco = $address
        senha = "Senha123!"
        papel = "CREDOR"
        rendaMensal = $null
        tipoOcupacao = $null
        saldoDisponivelSimulado = $saldo
        contaBancaria = New-BankAccount -EmailSlug $slug -Index ($Index + 200)
    }

    $response = Invoke-Loanflow -Method "POST" -Path "/auth/register" -Body $payload
    return [pscustomobject]@{
        index = $Index
        nome = $fullName
        email = $email
        cpf = $payload.cpf
        saldoDisponivelSimulado = [decimal]$saldo
        token = $response.accessToken
        usuarioId = $response.usuario.id
        credorId = $response.usuario.credorId
        contaBancariaId = $response.usuario.contaBancariaId
    }
}

function New-PropostaPayload {
    param([decimal]$RendaMensal)

    $template = Get-RandomItem -Items $purposeCatalog
    $prazo = $rng.Next([int]$template.prazoMin, ([int]$template.prazoMax + 1))
    $taxa = Get-RandomDecimal -Min $template.taxaMin -Max $template.taxaMax
    $fatorJuros = 1.0 + ([double]$taxa / 100.0)
    $limiteMensal = [double]$RendaMensal * 0.30
    $valorMaximoSeguro = ($limiteMensal * $prazo / $fatorJuros) * 0.78
    $valorMaximoSeguro = [Math]::Min($valorMaximoSeguro, 18000.0)
    $valorMinimo = 900.0
    if ($valorMaximoSeguro -lt 1300.0) {
        $valorMaximoSeguro = 1300.0
    }
    if ($valorMaximoSeguro -le $valorMinimo) {
        $valorMaximoSeguro = $valorMinimo + 150.0
    }
    $valor = Get-RandomDecimal -Min $valorMinimo -Max $valorMaximoSeguro

    return [ordered]@{
        valorSolicitado = $valor
        taxaJuros = [decimal]$taxa
        prazoMeses = $prazo
        finalidade = $template.finalidade
        categoriaFinalidade = $template.categoria
        descricaoDetalhada = $template.descricao
    }
}

function Criar-Proposta {
    param($Solicitante)

    $payload = New-PropostaPayload -RendaMensal $Solicitante.rendaMensal
    $response = Invoke-Loanflow -Method "POST" -Path "/propostas" -Body $payload -Token $Solicitante.token
    return [pscustomobject]@{
        id = $response.id
        solicitanteId = $response.solicitanteId
        solicitanteEmail = $Solicitante.email
        valorSolicitado = [decimal]$payload.valorSolicitado
        taxaJuros = [decimal]$payload.taxaJuros
        prazoMeses = $payload.prazoMeses
        categoriaFinalidade = $payload.categoriaFinalidade
        finalidade = $payload.finalidade
        status = $response.status
    }
}

function Formalizar-PropostaComCredor {
    param(
        $Proposta,
        $Solicitante,
        $Credor
    )

    $aceita = Invoke-Loanflow -Method "POST" -Path ("/propostas/{0}/aceitar" -f $Proposta.id) -Token $Credor.token
    if ($aceita.status -ne "ACEITA") {
        throw "Proposta $($Proposta.id) nao ficou ACEITA."
    }

    $emAnalise = Invoke-Loanflow -Method "POST" -Path ("/propostas/{0}/iniciar-analise" -f $Proposta.id) -Token $Credor.token
    if ($emAnalise.status -ne "EM_ANALISE") {
        throw "Proposta $($Proposta.id) nao entrou em EM_ANALISE."
    }

    $aprovada = Invoke-Loanflow -Method "POST" -Path ("/propostas/{0}/aprovar" -f $Proposta.id) -Token $Credor.token
    if ($aprovada.status -ne "APROVADA") {
        throw "Proposta $($Proposta.id) nao ficou APROVADA."
    }

    $contrato = Invoke-Loanflow -Method "POST" -Path ("/contratos/proposta/{0}/gerar" -f $Proposta.id) -Token $Credor.token
    if ($contrato.status -ne "AGUARDANDO_ASSINATURAS") {
        throw "Contrato da proposta $($Proposta.id) nao ficou AGUARDANDO_ASSINATURAS."
    }

    $assinaturaSolicitante = Invoke-Loanflow -Method "POST" -Path ("/contratos/{0}/assinar" -f $contrato.id) -Body @{ aceite = $true } -Token $Solicitante.token
    if ($assinaturaSolicitante.papelSignatario -ne "SOLICITANTE") {
        throw "Assinatura do solicitante falhou no contrato $($contrato.id)."
    }

    $assinaturaCredor = Invoke-Loanflow -Method "POST" -Path ("/contratos/{0}/assinar" -f $contrato.id) -Body @{ aceite = $true } -Token $Credor.token
    if ($assinaturaCredor.papelSignatario -ne "CREDOR") {
        throw "Assinatura do credor falhou no contrato $($contrato.id)."
    }

    $contratoFinal = Invoke-Loanflow -Method "GET" -Path ("/contratos/{0}" -f $contrato.id) -Token $Credor.token
    $propostaFinal = Invoke-Loanflow -Method "GET" -Path ("/propostas/{0}" -f $Proposta.id) -Token $Solicitante.token

    if ($contratoFinal.status -ne "FORMALIZADO") {
        throw "Contrato $($contrato.id) nao foi formalizado."
    }
    if ($propostaFinal.status -ne "CONTRATADA") {
        throw "Proposta $($Proposta.id) nao ficou CONTRATADA."
    }

    return [pscustomobject]@{
        propostaId = $Proposta.id
        contratoId = $contrato.id
        solicitanteEmail = $Solicitante.email
        credorEmail = $Credor.email
        valorSolicitado = $Proposta.valorSolicitado
        statusContrato = $contratoFinal.status
        statusProposta = $propostaFinal.status
    }
}

try {
    for ($i = 1; $i -le $SolicitanteCount; $i++) {
        $solicitante = Register-Solicitante -Index $i
        $results.solicitantes += $solicitante
    }

    for ($i = 1; $i -le $CredorCount; $i++) {
        $credor = Register-Credor -Index $i
        $results.credores += $credor
    }

    foreach ($solicitante in $results.solicitantes) {
        $proposta = Criar-Proposta -Solicitante $solicitante
        $results.propostas += $proposta
    }

    $contratosParaFormalizar = [Math]::Min($results.credores.Count, $results.propostas.Count)
    for ($i = 0; $i -lt $contratosParaFormalizar; $i++) {
        $formalizacao = Formalizar-PropostaComCredor -Proposta $results.propostas[$i] -Solicitante $results.solicitantes[$i] -Credor $results.credores[$i]
        $results.contratosFormalizados += $formalizacao
    }

    $results.finishedAt = (Get-Date).ToString("s")
    $results.summary = [ordered]@{
        solicitantesCriados = $results.solicitantes.Count
        credoresCriados = $results.credores.Count
        propostasCriadas = $results.propostas.Count
        contratosFormalizados = $results.contratosFormalizados.Count
    }

    $results | ConvertTo-Json -Depth 10 | Set-Content -Path $resultPath

    Write-Host ("Lote {0} concluido." -f $BatchId)
    Write-Host ("Solicitantes criados: {0}" -f $results.solicitantes.Count)
    Write-Host ("Credores criados: {0}" -f $results.credores.Count)
    Write-Host ("Propostas criadas: {0}" -f $results.propostas.Count)
    Write-Host ("Contratos formalizados: {0}" -f $results.contratosFormalizados.Count)
    Write-Host ("Resultado salvo em: {0}" -f (Resolve-Path $resultPath))
}
catch {
    $results.failedAt = (Get-Date).ToString("s")
    $results.failure = $_.Exception.Message
    $results | ConvertTo-Json -Depth 10 | Set-Content -Path $resultPath
    throw
}
