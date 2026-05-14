# Checklist de Smoke - Fase 1

Este roteiro foi pensado para o fechamento manual da Fase 1 do fluxo de assinatura.
Ele cobre os cenários mínimos de validação funcional sem exigir mudanças de código.

## Pré-requisitos

- API e SPA rodando com SQL Server e Flyway habilitados.
- Solicitante e credor com conta bancária cadastrada.
- Proposta aprovada e contrato já gerado.
- IDs do contrato e dos usuários disponíveis para consulta rápida.

## Configuração recomendada para agilizar os testes

- Para expiração de desafio, prefira subir a API com `SIGNATURE_CHALLENGE_TTL_MINUTES=1`.
- Para expiração de contrato, como o prazo normal é de 7 dias, use ajuste controlado no banco de testes.

Exemplo de subida local:

```powershell
$env:SIGNATURE_CHALLENGE_TTL_MINUTES="1"
powershell -ExecutionPolicy Bypass -File .\start-demo.ps1
```

## Cenário 1 - Iniciar desafio por senha

1. Entrar como signatário elegível no contrato.
2. Abrir a assinatura guiada.
3. Selecionar `Senha atual`.
4. Marcar o aceite explícito e iniciar o desafio.

Esperado:

- resposta `200`
- `metodo = REAUTENTICACAO_SENHA`
- `desafioId` preenchido
- `expiraEm` preenchido
- mensagem orientando a confirmação final

## Cenário 2 - Iniciar desafio por código temporário

1. Entrar como o outro signatário elegível.
2. Abrir a assinatura guiada.
3. Selecionar `Código temporário`.
4. Marcar o aceite explícito e iniciar o desafio.
5. Consultar a central de notificações do usuário.

Esperado:

- resposta `200`
- `metodo = CODIGO_ONE_TIME`
- `desafioId` preenchido
- `mascaraDestino` preenchida
- notificação `SISTEMA` contendo o código temporário do contrato

## Cenário 3 - Assinar com sucesso

1. Iniciar o desafio.
2. Confirmar com a credencial correta:
   - senha atual, quando o método for `REAUTENTICACAO_SENHA`
   - código temporário recebido, quando o método for `CODIGO_ONE_TIME`
3. Repetir com o segundo signatário.

Esperado:

- primeira assinatura muda o contrato para `ASSINADO_PARCIALMENTE`
- segunda assinatura muda o contrato para `FORMALIZADO`
- `dataFormalizacao` preenchida
- proposta vinculada em `CONTRATADA`
- parcelas geradas automaticamente

## Cenário 4 - Expirar desafio

Opção A, mais rápida:

```sql
UPDATE desafios_assinatura
SET expira_em = DATEADD(MINUTE, -1, SYSDATETIME())
WHERE id = '<desafioId>';
```

Opção B:

- subir a API com `SIGNATURE_CHALLENGE_TTL_MINUTES=1`
- aguardar o prazo acabar

Depois:

1. Tentar confirmar a assinatura com o `desafioId` expirado.

Esperado:

- erro de regra de negócio informando que o desafio expirou
- necessidade de gerar novo desafio para seguir

## Cenário 5 - Expirar contrato

Como o prazo normal é de 7 dias, force o vencimento em banco de teste:

```sql
UPDATE contratos
SET data_expiracao_assinatura = DATEADD(MINUTE, -1, SYSDATETIME())
WHERE id = <contratoId>;
```

Depois:

1. Consultar `GET /contratos/{id}` ou tentar iniciar/confirmar assinatura.

Esperado:

- contrato transicionado para `EXPIRADO`
- `motivoExpiracao` preenchido
- novas assinaturas bloqueadas

## Cenário 6 - Tentar assinatura repetida

1. Registrar uma assinatura válida para o usuário.
2. Tentar abrir outro fluxo de assinatura ou confirmar novamente para o mesmo contrato e usuário.

Esperado:

- bloqueio da nova tentativa
- mensagem de negócio informando que o usuário já assinou o contrato

## Evidências mínimas para guardar

- resposta do início do desafio com `desafioId`, `metodo` e `expiraEm`
- captura da notificação com código temporário
- resposta da primeira assinatura com contrato em `ASSINADO_PARCIALMENTE`
- resposta da segunda assinatura com contrato em `FORMALIZADO`
- resposta do cenário de desafio expirado
- resposta do cenário de contrato expirado
- resposta do cenário de assinatura repetida

## Observação sobre estados e eventos extras

O modelo interno mantém estados e eventos adicionais como `CONSUMIDO`, `DESAFIO_EXPIRADO` e `DESAFIO_CONSUMIDO`.
Isso é uma ampliação de rastreabilidade, não uma mudança de escopo do checklist funcional.
Externamente, o fluxo principal continua sendo:

1. iniciar desafio
2. validar credencial
3. registrar assinatura
4. formalizar ou expirar o contrato
