# Roteiro Postman

Este roteiro valida o fluxo principal do MVP do LoanFlow.

## Configuração Inicial

Crie um Environment no Postman com estas variáveis:

```text
baseUrl = http://localhost:8080
tokenSolicitante =
tokenCredor =
tokenAdmin =
usuarioId =
credorId = 1
propostaId =
contratoId =
parcelaId =
pagamentoId =
notificacaoId =
```

Para todas as requisições com JSON:

```text
Body -> raw -> JSON
```

Para `/auth/register` e `/auth/login`:

```text
Authorization -> Type -> No Auth
```

Para os demais endpoints:

```text
Authorization -> Type -> Bearer Token
Token -> {{tokenSolicitante}}, {{tokenCredor}} ou {{tokenAdmin}}
```

Se quiser pular o cadastro manual, faça login com os usuários seedados em
`documentacao/usuarios-demo.md` e aproveite os tokens retornados por `/auth/login`.

## 1. Registrar Solicitante

```http
POST {{baseUrl}}/auth/register
```

Body:

```json
{
  "nome": "Davi Solicitante",
  "cpf": "529.982.247-25",
  "email": "solicitante@loanflow.com",
  "estadoCivil": "SOLTEIRO",
  "nacionalidade": "Brasileira",
  "profissao": "Analista",
  "dataNascimento": "1994-03-12",
  "telefone": "11999990000",
  "tipoDocumentoIdentidade": "RG",
  "documentoIdentidade": "12345678",
  "orgaoEmissor": "SSP-SP",
  "pessoaExpostaPoliticamente": false,
  "endereco": {
    "cep": "01001-000",
    "logradouro": "Praça da Sé",
    "numero": "100",
    "complemento": "Sala 4",
    "bairro": "Centro",
    "cidade": "São Paulo",
    "uf": "SP"
  },
  "senha": "Senha@123",
  "papel": "SOLICITANTE",
  "rendaMensal": 3000.00,
  "tipoOcupacao": "CLT",
  "saldoDisponivelSimulado": null,
  "contaBancaria": {
    "banco": "Banco do Brasil",
    "agencia": "1234",
    "numeroConta": "987654",
    "tipoConta": "CORRENTE",
    "chavePix": "solicitante@loanflow.com"
  }
}
```

Salvar `accessToken` em `tokenSolicitante`.

## 2. Registrar Credor

```http
POST {{baseUrl}}/auth/register
```

Body:

```json
{
  "nome": "Davi Credor",
  "cpf": "111.444.777-35",
  "email": "credor@loanflow.com",
  "estadoCivil": "CASADO",
  "nacionalidade": "Brasileira",
  "profissao": "Investidor",
  "dataNascimento": "1988-08-21",
  "telefone": "21988881111",
  "tipoDocumentoIdentidade": "CNH",
  "documentoIdentidade": "99887766",
  "orgaoEmissor": "DETRAN-RJ",
  "pessoaExpostaPoliticamente": false,
  "endereco": {
    "cep": "20040-020",
    "logradouro": "Rua da Assembleia",
    "numero": "50",
    "complemento": "Conjunto 502",
    "bairro": "Centro",
    "cidade": "Rio de Janeiro",
    "uf": "RJ"
  },
  "senha": "Senha@123",
  "papel": "CREDOR",
  "rendaMensal": null,
  "tipoOcupacao": null,
  "saldoDisponivelSimulado": 10000.00,
  "contaBancaria": {
    "banco": "Inter",
    "agencia": "5678",
    "numeroConta": "123456",
    "tipoConta": "CORRENTE",
    "chavePix": "credor@loanflow.com"
  }
}
```

Salvar `accessToken` em `tokenCredor`.

Se o banco estiver limpo, o `credorId` tende a ser `1`. Para confirmar no SQL Server:

```sql
SELECT id, usuario_id FROM credores;
```

## 3. Registrar Admin

```http
POST {{baseUrl}}/auth/register
```

Body:

```json
{
  "nome": "Davi Admin",
  "cpf": "390.533.447-05",
  "email": "admin@loanflow.com",
  "estadoCivil": "SOLTEIRO",
  "nacionalidade": "Brasileira",
  "profissao": "Administrador",
  "dataNascimento": "1986-05-14",
  "telefone": "31977771111",
  "tipoDocumentoIdentidade": "RG",
  "documentoIdentidade": "55443322",
  "orgaoEmissor": "SSP-MG",
  "pessoaExpostaPoliticamente": false,
  "endereco": {
    "cep": "30130-110",
    "logradouro": "Avenida Afonso Pena",
    "numero": "900",
    "complemento": "Sala 12",
    "bairro": "Centro",
    "cidade": "Belo Horizonte",
    "uf": "MG"
  },
  "senha": "Senha@123",
  "papel": "ADMIN",
  "rendaMensal": null,
  "tipoOcupacao": null,
  "saldoDisponivelSimulado": null
}
```

Salvar `accessToken` em `tokenAdmin`.

## 4. Ver Usuário Autenticado

```http
GET {{baseUrl}}/usuarios/me
Authorization: Bearer {{tokenSolicitante}}
```

Resultado esperado: dados do solicitante autenticado.

## 5. Criar Proposta

```http
POST {{baseUrl}}/propostas
Authorization: Bearer {{tokenSolicitante}}
```

Body:

```json
{
  "valorSolicitado": 1200.00,
  "taxaJuros": 5.00,
  "prazoMeses": 3,
  "categoriaFinalidade": "ESTUDO",
  "finalidade": "Compra de equipamentos para trabalho autônomo",
  "descricaoDetalhada": "Notebook, cadeira ergonômica e adaptação básica para ampliar a produção."
}
```

Salvar `id` em `propostaId`.

Resultado esperado:

```text
status = AGUARDANDO_ACEITE
dataExpiracao preenchida automaticamente pelo backend
```

## 6. Submeter Proposta

```http
POST {{baseUrl}}/propostas/{{propostaId}}/submeter
Authorization: Bearer {{tokenSolicitante}}
```

Sem body.

Resultado esperado:

```text
status = SUBMETIDA
```

## 7. Credor Lista Propostas para Análise

```http
GET {{baseUrl}}/propostas/analise
Authorization: Bearer {{tokenCredor}}
```

Resultado esperado: lista contendo a proposta submetida.

## 8. Credor Inicia Análise

```http
POST {{baseUrl}}/propostas/{{propostaId}}/iniciar-analise
Authorization: Bearer {{tokenCredor}}
```

Sem body.

Resultado esperado:

```text
status = EM_ANALISE
```

## 9. Credor Aprova Proposta

```http
POST {{baseUrl}}/propostas/{{propostaId}}/aprovar
Authorization: Bearer {{tokenCredor}}
```

Sem body.

Resultado esperado:

```text
status = APROVADA
```

## 10. Gerar Contrato

```http
POST {{baseUrl}}/contratos/proposta/{{propostaId}}/gerar
Authorization: Bearer {{tokenCredor}}
```

Sem body.

Salvar `id` em `contratoId`.

Resultado esperado:

```text
status = AGUARDANDO_ASSINATURAS
hashDocumento preenchido
pdfPath preenchido
```

## 11. Consultar Contrato

```http
GET {{baseUrl}}/contratos/{{contratoId}}
Authorization: Bearer {{tokenSolicitante}}
```

Resultado esperado: dados do contrato gerado.

## 12. Baixar PDF do Contrato

```http
GET {{baseUrl}}/contratos/{{contratoId}}/download
Authorization: Bearer {{tokenSolicitante}}
```

Resultado esperado: download de um PDF simples do contrato.

## 13. Solicitante Assina Contrato

```http
POST {{baseUrl}}/contratos/{{contratoId}}/assinar
Authorization: Bearer {{tokenSolicitante}}
```

Body:

```json
{
  "aceite": true
}
```

Resultado esperado: assinatura registrada.

## 14. Credor Assina Contrato

```http
POST {{baseUrl}}/contratos/{{contratoId}}/assinar
Authorization: Bearer {{tokenCredor}}
```

Body:

```json
{
  "aceite": true
}
```

Resultado esperado:

```text
contrato formalizado
proposta contratada
parcelas geradas automaticamente
```

## 15. Listar Parcelas do Contrato

```http
GET {{baseUrl}}/contratos/{{contratoId}}/parcelas
Authorization: Bearer {{tokenSolicitante}}
```

Salvar o `id` da primeira parcela em `parcelaId`.

Resultado esperado: 3 parcelas se a proposta foi criada com `prazoMeses = 3`.

## 16. Registrar Pagamento Manual

```http
POST {{baseUrl}}/parcelas/{{parcelaId}}/pagamentos
Authorization: Bearer {{tokenSolicitante}}
```

Body:

```json
{
  "valorPago": 400.00,
  "formaPagamento": "PIX_MANUAL",
  "comprovante": "Pagamento manual registrado para demonstração do TCC"
}
```

Resultado esperado:

```text
pagamento registrado
parcela PAGA ou PARCIALMENTE_PAGA
```

Salvar `id` em `pagamentoId` se quiser testar cancelamento de pagamento.

## 17. Listar Pagamentos da Parcela

```http
GET {{baseUrl}}/parcelas/{{parcelaId}}/pagamentos
Authorization: Bearer {{tokenSolicitante}}
```

Resultado esperado: lista com o pagamento registrado.

## 17.1. Cancelar Pagamento Manual

Use apenas se quiser testar estorno/cancelamento no protótipo.

```http
POST {{baseUrl}}/parcelas/{{parcelaId}}/pagamentos/{{pagamentoId}}/cancelar
Authorization: Bearer {{tokenSolicitante}}
```

Sem body.

Resultado esperado:

```text
pagamento CANCELADO
valorPagoAcumulado da parcela recalculado
```

## 18. Listar Notificações

```http
GET {{baseUrl}}/notificacoes
Authorization: Bearer {{tokenSolicitante}}
```

Se houver notificação, salve `id` em `notificacaoId`.

Filtro opcional:

```http
GET {{baseUrl}}/notificacoes?lida=false&tipo=CONTRATO
Authorization: Bearer {{tokenSolicitante}}
```

## 19. Marcar Notificação como Lida

```http
POST {{baseUrl}}/notificacoes/{{notificacaoId}}/lida
Authorization: Bearer {{tokenSolicitante}}
```

Sem body.

Resultado esperado:

```text
lida = true
```

## 20. Listar Auditorias

```http
GET {{baseUrl}}/admin/auditorias
Authorization: Bearer {{tokenAdmin}}
```

Resultado esperado: registros de eventos como:

```text
CRIAR
SUBMETER
INICIAR_ANALISE
APROVAR
GERAR_CONTRATO
ASSINAR
FORMALIZAR
GERAR_PARCELAS
REGISTRAR_PAGAMENTO
```

Filtro opcional:

```http
GET {{baseUrl}}/admin/auditorias?acao=ASSINAR&entidadeTipo=Contrato&entidadeId={{contratoId}}
Authorization: Bearer {{tokenAdmin}}
```

## 21. Consultar Dashboard Administrativo

```http
GET {{baseUrl}}/admin/dashboard
Authorization: Bearer {{tokenAdmin}}
```

Resultado esperado: totais simples de usuários, propostas, contratos, parcelas, pagamentos e auditorias.

## 22. Marcar Parcelas Vencidas Como Em Atraso

```http
POST {{baseUrl}}/admin/parcelas/marcar-atrasadas
Authorization: Bearer {{tokenAdmin}}
```

Sem body.

Resultado esperado: lista de parcelas que foram atualizadas para `EM_ATRASO`.

## 23. Atualizar Proposta em Rascunho

Este teste deve ser feito antes de submeter a proposta.

```http
PUT {{baseUrl}}/propostas/{{propostaId}}
Authorization: Bearer {{tokenSolicitante}}
Content-Type: application/json
```

Body:

```json
{
  "valorSolicitado": 1500.00,
  "taxaJuros": 5.00,
  "prazoMeses": 4,
  "categoriaFinalidade": "OUTRA",
  "finalidade": "Ajuste da proposta antes do envio",
  "descricaoDetalhada": "Revisão do valor solicitado depois de recalcular o custo total da necessidade."
}
```

Resultado esperado:

```text
status continua AGUARDANDO_ACEITE
dados atualizados
```

## 24. Bloquear e Reativar Usuário

Use com cuidado em ambiente de teste. Se bloquear o usuário que você está usando, ele não conseguirá mais autenticar.

```http
POST {{baseUrl}}/usuarios/{{usuarioId}}/bloquear
Authorization: Bearer {{tokenAdmin}}
```

```http
POST {{baseUrl}}/usuarios/{{usuarioId}}/reativar
Authorization: Bearer {{tokenAdmin}}
```

## Problemas Comuns

### 403 em /auth/register

Verifique se a aba `Authorization` está como:

```text
No Auth
```

Remova qualquer header manual:

```text
Authorization
```

### 400 com e-mail ou CPF duplicado

Troque o CPF e o e-mail do payload. O banco provavelmente já tem o registro.

### 404 em proposta, contrato ou parcela

Confira se você salvou corretamente:

```text
propostaId
contratoId
parcelaId
```

### 403 em endpoints protegidos

Confira se está usando o token do papel correto:

```text
SOLICITANTE -> criar/submeter proposta, assinar, pagar
CREDOR -> analisar, aprovar, gerar contrato, assinar
ADMIN -> auditorias
```
