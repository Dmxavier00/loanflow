# LoanFlow

Protótipo acadêmico de uma plataforma web de microcrédito P2P para TCC, com API Spring Boot, interface React e fluxo completo de proposta, contrato, formalização automática, acompanhamento de parcelas, pagamentos, notificações, administração e auditoria.

## Visão Geral

O projeto foi construído para demonstrar um fluxo funcional de concessão de crédito entre pessoas, sem depender de integração bancária real. O objetivo é cobrir a jornada principal da operação:

- cadastro e autenticação com JWT
- perfis de `SOLICITANTE`, `CREDOR` e `ADMIN`
- criação, análise e aprovação de propostas
- geração de contrato em PDF com hash do documento
- formalização automática do contrato quando o credor aceita a proposta
- acompanhamento de parcelas dentro do painel de contratos
- registro manual de pagamentos
- notificações por usuário e trilha de auditoria
- dashboard, administração e auditoria para o perfil `ADMIN`

## Enquadramento Acadêmico

O `LoanFlow` é um protótipo didático para TCC. Isso significa que:

- taxas, encargos, parcelas e contratos exibidos no sistema são parâmetros de simulação acadêmica
- o projeto não afirma conformidade automática com Bacen, STJ, Lei da Usura ou qualquer enquadramento regulatório aplicável a operações reais
- qualquer uso fora do contexto acadêmico exigiria revisão jurídica e regulatória específica, além de integrações e controles que não fazem parte deste MVP

## Stack

### Back-end

- Java 17
- Spring Boot 4
- Spring Security
- Spring Data JPA
- Bean Validation
- Flyway
- SQL Server
- JWT
- Maven

### Front-end

- React 18
- React Router
- Vite
- Fetch API nativa

### Testes

- JUnit 5
- Spring Boot Test
- Spring Security Test
- H2 para testes automatizados

## Estrutura do Projeto

```text
loanflow/
|-- src/main/java/com/api/loanflow
|   |-- administracao
|   |-- auditoria
|   |-- autenticacao
|   |-- contrato
|   |-- notificacao
|   |-- pagamento
|   |-- parcela
|   |-- proposta
|   |-- seguranca
|   |-- compartilhado
|   `-- usuario
|-- src/main/resources/db/migration
|-- src/test
|-- apresentacao
|-- documentacao
`-- roteiros
```

Organização interna dos módulos principais:

```text
api                     Controllers e DTOs
aplicacao               Casos de uso e serviços
dominio                 Entidades e enums
infraestrutura          Persistência e detalhes técnicos
infraestrutura/persistencia
                        Repositories Spring Data
```

## Funcionalidades Entregues

- autenticação com JWT via `/auth/register` e `/auth/login`
- gestão do usuário autenticado em `/usuarios/me`
- cadastro e manutenção de conta bancária
- consulta de credores para composição do fluxo
- criação, edição, submissão, aceite, análise, aprovação e cancelamento de propostas
- geração, consulta, formalização automática e download de contratos
- consulta de parcelas dentro do painel de contratos e por API
- registro e cancelamento de pagamentos manuais
- notificações por usuário, incluindo eventos de pagamento e quitação
- dashboard, administração de usuários e auditoria administrativa
- busca de endereço por CEP
- SPA React servida pelo mesmo projeto

## Pré-requisitos

- Java 17
- SQL Server local acessível na porta `1433`
- PowerShell
- Windows para uso direto dos scripts `.ps1`

Observação:
o front-end pode usar o Node local versionado em `.tools/node/...` quando esse diretório existir. Os scripts de `apresentacao/` já cuidam disso, e a API continua subindo mesmo se o build automático da SPA não puder ser executado.

## Banco de Dados

Configuração atual da API em [src/main/resources/application.properties](src/main/resources/application.properties):

```properties
spring.datasource.url=${LOANFLOW_DB_URL:jdbc:sqlserver://localhost:1433;databaseName=loanflow;encrypt=false;trustServerCertificate=true}
spring.datasource.username=${LOANFLOW_DB_USERNAME:loanflow_user}
spring.datasource.password=${LOANFLOW_DB_PASSWORD:Loanflow@12345}
loanflow.jwt.secret=${JWT_SECRET:loanflow-demo-secret-change-me-loanflow-demo-secret-change-me}
```

Os valores acima ja trazem defaults locais compativeis com os scripts de demo. Com isso, o `Run Java` da IDE sobe a API sem exigir comando auxiliar.

Se quiser usar outra senha de banco ou outra secret JWT, sobrescreva pelas variaveis abaixo:

```powershell
$env:LOANFLOW_DB_PASSWORD="sua-senha-do-sql-server"
$env:JWT_SECRET="uma-chave-com-pelo-menos-32-bytes-para-hmac"
```

Criação rápida da base local:

```sql
CREATE DATABASE loanflow;
GO

CREATE LOGIN loanflow_user WITH PASSWORD = 'Loanflow@12345';
GO

USE loanflow;
GO

CREATE USER loanflow_user FOR LOGIN loanflow_user;
GO

ALTER ROLE db_owner ADD MEMBER loanflow_user;
GO
```

Se preferir outra senha no SQL Server, tudo bem. Nesse caso, defina `LOANFLOW_DB_PASSWORD` antes de subir a API ou ajuste a propriedade equivalente.

As migrations ficam em [src/main/resources/db/migration](src/main/resources/db/migration) e são aplicadas automaticamente na subida da aplicação.

Observação:
os testes automatizados não dependem de SQL Server. Eles usam H2 em memória via [src/test/resources/application-test.properties](src/test/resources/application-test.properties).

## Como Executar

### 1. Rodar os testes do back-end

```powershell
cmd /c mvnw.cmd test
```

### 2. Rodar só a API

```powershell
cmd /c mvnw.cmd spring-boot:run
```

API disponível em:

```text
http://localhost:8080
```

### 2.1 Rodar a demo do TCC com um comando

Para a apresentacao, prefira usar o script da raiz do projeto:

```powershell
powershell -ExecutionPolicy Bypass -File .\start-demo.ps1
```

O script:

- abre uma nova janela so para a API, preservando a janela atual para voce
- aguarda a SPA responder e abre o navegador na rota `/login`
- usa o [run-demo.ps1](run-demo.ps1) por baixo para aplicar valores de demonstracao de banco e `JWT_SECRET`
- aceita sobrescrita por parametro, por exemplo `-DbPassword "sua-senha"` ou `-Port 8081`
- oferece `-DryRun` para conferir a configuracao sem iniciar a API

Se preferir subir tudo na mesma janela do terminal, ainda pode usar:

```powershell
powershell -ExecutionPolicy Bypass -File .\run-demo.ps1
```

Para encerrar a demo e liberar a porta antes de uma nova tentativa:

```powershell
powershell -ExecutionPolicy Bypass -File .\stop-demo.ps1
```

### 3. Rodar front e back separadamente no desenvolvimento

Back-end:

```powershell
cmd /c mvnw.cmd spring-boot:run
```

Front-end:

```powershell
powershell -ExecutionPolicy Bypass -File .\apresentacao\run-dev.ps1
```

SPA em desenvolvimento:

```text
http://localhost:5173
```

### 4. Gerar build do front manualmente

```powershell
powershell -ExecutionPolicy Bypass -File .\apresentacao\run-build.ps1
```

### 5. Testar regras isoladas do front

```powershell
powershell -ExecutionPolicy Bypass -File .\apresentacao\run-test.ps1
```

## Integração da SPA com a API

O projeto já possui integração entre o back-end e a SPA:

- ao iniciar a aplicação, `LoanflowApplication` chama um bootstrap que verifica se o build do front precisa ser atualizado
- quando necessário e possível, o back-end executa `npm run build` dentro de `apresentacao/`
- os arquivos de `apresentacao/dist` são servidos pela própria aplicação Spring Boot
- rotas navegáveis da SPA como `/dashboard`, `/solicitacoes`, `/contratos`, `/alertas`, `/administracao` e `/auditoria` fazem forward para `index.html`

A rota antiga `/parcelas` é mantida apenas como compatibilidade e redireciona para `/contratos?view=parcelas`, onde o painel de parcelas fica embutido no contexto do contrato.

Se o `npm`/Node não estiver disponível, ou se o build automático falhar, a API continua subindo normalmente. Nesse cenário, apenas a SPA embutida pode ficar desatualizada ou indisponível até um build manual.

Para desativar o build automático do front na subida da API:

```powershell
$env:LOANFLOW_FRONTEND_AUTO_BUILD="false"
cmd /c mvnw.cmd spring-boot:run
```

ou:

```powershell
cmd /c mvnw.cmd "-Dspring-boot.run.jvmArguments=-Dloanflow.frontend.auto-build=false" spring-boot:run
```

## Configuração do Front-end

Arquivo de exemplo:

[apresentacao/.env.example](apresentacao/.env.example)

Conteúdo:

```properties
VITE_API_BASE_URL=http://localhost:8080
```

## Variáveis e Propriedades Úteis

- `LOANFLOW_DB_URL`
- `LOANFLOW_DB_USERNAME`
- `LOANFLOW_DB_PASSWORD`
- `JWT_SECRET`
- `JWT_EXPIRATION_MINUTES`
- `CONTRACT_STORAGE_PATH`
- `LOANFLOW_FRONTEND_AUTO_BUILD`

## Fluxo Principal do Protótipo

1. Usuário se cadastra como solicitante, credor ou administrador.
2. O solicitante cria uma proposta de crédito.
3. A proposta é disponibilizada para aceite do credor.
4. O credor aceita a oportunidade.
5. O sistema valida a operação, gera o contrato em PDF e formaliza o contrato automaticamente.
6. As parcelas são criadas e passam a compor a agenda financeira da operação.
7. O solicitante registra pagamentos manuais.
8. O sistema gera notificações e registra auditoria dos eventos.

## Endpoints Principais

### Auth

```text
POST /auth/register
POST /auth/login
```

### Endereços

```text
GET /enderecos/cep/{cep}
```

### Usuários

```text
GET /usuarios/me
GET /usuarios/me/conta-bancaria
PUT /usuarios/me
PUT /usuarios/me/conta-bancaria
PUT /usuarios/me/senha
PUT /usuarios/me/dados-financeiros
GET /usuarios
GET /usuarios/credores
POST /usuarios/{id}/bloquear
POST /usuarios/{id}/reativar
```

### Propostas

```text
POST /propostas
GET /propostas
GET /propostas/minhas
GET /propostas/analise
GET /propostas/aguardando-aceite
GET /propostas/aceitas
GET /propostas/{id}
GET /propostas/status/{status}
PUT /propostas/{id}
POST /propostas/{id}/submeter
POST /propostas/{id}/iniciar-analise
POST /propostas/{id}/aceitar
POST /propostas/{id}/aprovar
POST /propostas/{id}/cancelar
```

### Contratos

```text
POST /contratos/proposta/{propostaId}/gerar
GET /contratos
GET /contratos/{id}
GET /contratos/{id}/download
```

### Parcelas e Pagamentos

```text
GET /parcelas
GET /parcelas/{id}
GET /contratos/{contratoId}/parcelas
POST /parcelas/{parcelaId}/pagamentos
GET /parcelas/{parcelaId}/pagamentos
POST /parcelas/{parcelaId}/pagamentos/{pagamentoId}/cancelar
POST /admin/parcelas/marcar-atrasadas
```

### Notificações, Auditoria e Admin

```text
GET /notificacoes
POST /notificacoes/{id}/lida
GET /admin/auditorias
GET /admin/dashboard
```

## Usuários e Massa de Demonstração

Usuários seedados para demonstração estão documentados em:

[documentacao/usuarios-demo.md](documentacao/usuarios-demo.md)

Senha padrão dos usuários de demonstração:

```text
Senha@123
```

## Scripts Úteis

Scripts de apoio em [roteiros](roteiros):

- `seed-random-batch.ps1`: cria massa randômica de solicitantes, credores, propostas e contratos
- `register-partial-payments.ps1`: registra pagamentos parciais com base no lote criado

Artefatos gerados por esses scripts ficam em `artefatos/` e não são versionados.

## Documentação Auxiliar

- [documentacao/anotacoes-apresentacao.md](documentacao/anotacoes-apresentacao.md)
- [documentacao/roteiro-postman.md](documentacao/roteiro-postman.md)
- [documentacao/loanflow-api.http](documentacao/loanflow-api.http)

## Limitações do Protótipo

Este repositório não implementa operação financeira real. Estão fora do escopo do MVP:

- PIX automático e conciliação bancária
- integração com instituições financeiras
- bureaus de crédito
- KYC regulatório completo
- antifraude avançado
- assinatura ICP-Brasil
- recuperação de senha por e-mail real
- notificações por e-mail, SMS ou push

## Observações para Demonstração

- `/auth/register` e `/auth/login` não exigem token
- os demais endpoints usam `Authorization: Bearer <token>`
- pagamentos são manuais e simulados
- o PDF de contrato é gerado localmente
- a formalização principal do fluxo acontece automaticamente no aceite do credor
- o PDF do contrato mantém hash e trilha de auditoria para reforçar a rastreabilidade
- eventos relevantes ficam registrados na auditoria
