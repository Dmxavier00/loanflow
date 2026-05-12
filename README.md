# LoanFlow

Back-end Spring Boot para um protótipo acadêmico de plataforma web de microcrédito P2P com assinatura eletrônica simulada de contratos.

O foco do projeto é demonstrar o fluxo funcional do TCC: cadastro de usuários, autenticação JWT, proposta de crédito, análise por credor, geração de contrato, aceite eletrônico, hash do documento, parcelas, pagamento manual e trilha de auditoria.

## Stack

- Java 17
- Spring Boot 4
- Spring Security
- JWT
- Spring Data JPA
- SQL Server
- Flyway
- Maven

## Escopo do Protótipo

Este projeto não implementa operação financeira real. Para manter o TCC executável, ficam fora do escopo:

- PIX automático
- integração bancária
- bureaus de crédito
- KYC regulatório completo
- antifraude avançado
- assinatura ICP-Brasil
- certificado A1/A3

A assinatura eletrônica do protótipo é feita por aceite explícito de usuário autenticado, com hash do contrato e registro em auditoria.

## Arquitetura

O projeto usa uma arquitetura modular simples por domínio:

```text
src/main/java/com/api/loanflow
|-- auth
|-- security
|-- shared
|-- usuario
|-- proposta
|-- contrato
|-- parcela
|-- pagamento
|-- notificacao
|-- auditoria
`-- admin
```

Dentro dos módulos principais:

```text
api                     Controllers e DTOs
application             Services e casos de uso
domain                  Entidades JPA e enums
infrastructure          Persistência e detalhes técnicos
infrastructure/persistence
                        Repositories Spring Data JPA
```

O pacote `shared` guarda recursos transversais, como exceções e serviços de hash. O pacote `security` concentra JWT, filtro de autenticação e configuração do Spring Security.

## Banco de Dados

Configuração principal em:

```text
src/main/resources/application.properties
```

Configuração atual de desenvolvimento:

```properties
spring.datasource.url=jdbc:sqlserver://localhost:1433;databaseName=loanflow;encrypt=false;trustServerCertificate=true
spring.datasource.username=loanflow_user
spring.datasource.password=Loanflow@12345
```

Script inicial do schema:

```text
src/main/resources/db/migration/V1__create_core_schema.sql
```

Crie o banco e usuário no SQL Server antes de subir a aplicação:

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

Ao iniciar a aplicação, o Flyway aplica as migrations automaticamente.

A base de desenvolvimento também recebe usuários demo pela migration `V5__seed_demo_users.sql`. Consulte `docs/usuarios-demo.md` para a lista de e-mails e a senha padrão `Senha@123`.

## Como Rodar

Compile e rode os testes:

```powershell
cmd /c mvnw.cmd test
```

Suba a aplicação:

```powershell
cmd /c mvnw.cmd spring-boot:run
```

Aplicação local:

```text
http://localhost:8080
```

Se a porta estiver ocupada:

```powershell
netstat -ano | findstr :8080
Stop-Process -Id <PID> -Force
```

## Front-end React

O repositório agora inclui um front-end React em:

```text
frontend/
```

Escopo inicial da interface:

- login com JWT
- dashboard
- propostas
- contrato
- parcelas e pagamentos
- notificações

Tecnologias do front:

- React 18
- React Router
- Vite
- fetch nativo para consumo da API

Arquivo de configuração do endpoint da API:

```text
frontend/.env.example
```

Exemplo:

```properties
VITE_API_BASE_URL=http://localhost:8080
```

Para rodar o front localmente:

```powershell
cd frontend
npm install
npm run dev
```

Endereço esperado do front em desenvolvimento:

```text
http://localhost:5173
```

## Fluxo Principal

1. Cadastrar solicitante.
2. Cadastrar credor.
3. Cadastrar administrador.
4. Fazer login e salvar os tokens JWT.
5. Solicitante cria proposta.
6. Solicitante submete proposta.
7. Credor inicia análise.
8. Credor aprova proposta.
9. Credor gera contrato.
10. Solicitante assina contrato.
11. Credor assina contrato.
12. Sistema formaliza contrato e gera parcelas.
13. Solicitante registra pagamento manual.
14. Sistema registra auditoria do fluxo.

## Minha Recomendação Objetiva Implementada

Para deixar o protótipo mais completo sem virar um produto financeiro real, foram priorizados estes pontos:

- Atualização de perfil do usuário autenticado.
- Listagem administrativa de usuários.
- Identificadores de perfil na resposta de login e cadastro: `solicitanteId`, `credorId` e `administradorId`.
- Atualização de proposta enquanto ainda está em `RASCUNHO`.
- Consulta de propostas por status, respeitando permissão do usuário autenticado.
- Cancelamento de proposta antes da contratação.
- Cancelamento de contrato antes da formalização.
- Cancelamento de pagamento manual, com recalculo do valor pago da parcela.
- Filtros simples para notificações e auditorias.
- Bloqueio e reativação de usuários pelo administrador.
- Dashboard administrativo básico.
- Marcação manual de parcelas vencidas como `EM_ATRASO`.
- Respostas JSON para erros de autenticação e autorização.
- CORS preparado para o front-end React local em `http://localhost:5173`.

## O Que Pode Ficar Para Depois

Estes pontos continuam fora do MVP imediato porque aumentam escopo sem serem necessários para demonstrar o TCC:

- Integração bancária, PIX automático ou conciliação real.
- KYC regulatório, bureau de crédito e antifraude avançado.
- Assinatura digital ICP-Brasil, certificado A1/A3 ou carimbo do tempo oficial.
- Recuperação de senha por e-mail real.
- Upload real de comprovantes em storage.
- Notificação por e-mail, SMS ou push.
- Dashboard financeiro detalhado.
- Tela administrativa completa.
- Motor de score de crédito real.
- Regras complexas de multa, juros de mora e renegociação.
- Testes automatizados de todos os fluxos REST.

O roteiro detalhado para Postman está em:

```text
docs/roteiro-postman.md
```

Também existe um arquivo `.http` para uso com REST Client no VS Code:

```text
docs/loanflow-api.http
```

## Endpoints Principais

### Auth

```text
POST /auth/register
POST /auth/login
```

### Usuários

```text
GET /usuarios/me
PUT /usuarios/me
GET /usuarios
POST /usuarios/{id}/bloquear
POST /usuarios/{id}/reativar
```

### Propostas

```text
POST /propostas
GET /propostas/minhas
GET /propostas/analise
GET /propostas/status/{status}
GET /propostas/{id}
PUT /propostas/{id}
POST /propostas/{id}/submeter
POST /propostas/{id}/iniciar-analise
POST /propostas/{id}/aprovar
POST /propostas/{id}/rejeitar
POST /propostas/{id}/cancelar
```

### Contratos

```text
POST /contratos/proposta/{propostaId}/gerar
GET /contratos/{id}
GET /contratos/{id}/download
POST /contratos/{id}/assinar
POST /contratos/{id}/cancelar
```

### Parcelas e Pagamentos

```text
GET /contratos/{contratoId}/parcelas
GET /parcelas/{id}
POST /parcelas/{parcelaId}/pagamentos
GET /parcelas/{parcelaId}/pagamentos
POST /parcelas/{parcelaId}/pagamentos/{pagamentoId}/cancelar
POST /admin/parcelas/marcar-atrasadas
```

### Notificações e Auditoria

```text
GET /notificacoes
GET /notificacoes?lida=false&tipo=CONTRATO
POST /notificacoes/{id}/lida
GET /admin/auditorias
GET /admin/auditorias?acao=ASSINAR&entidadeTipo=Contrato&entidadeId=1
GET /admin/dashboard
```

## Observações para Demonstração

- Use `No Auth` no Postman para `/auth/register` e `/auth/login`.
- Use `Authorization: Bearer <token>` nos demais endpoints.
- O pagamento é manual/simulado.
- O contrato em PDF é gerado localmente.
- A assinatura é um aceite eletrônico autenticado.
- A auditoria registra eventos importantes do fluxo.
