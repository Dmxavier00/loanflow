import { useEffect, useMemo, useState } from 'react';
import BankAccountNotice from './BankAccountNotice';
import UiIcon from './UiIcon';
import { formatDateTime, formatLabel } from '../biblioteca/format';

const methodOptions = [
  {
    value: 'REAUTENTICACAO_SENHA',
    icon: 'shield-check',
    title: 'Senha atual',
    description: 'Valide o aceite com a mesma senha usada para entrar na sua conta.'
  },
  {
    value: 'CODIGO_ONE_TIME',
    icon: 'bell',
    title: 'Código temporário',
    description: 'Gere um código único, entregue pela central de notificações, para confirmar a assinatura.'
  }
];

function resolveCredentialLabel(method) {
  return method === 'CODIGO_ONE_TIME' ? 'Código temporário' : 'Senha atual';
}

function resolveCredentialPlaceholder(method) {
  return method === 'CODIGO_ONE_TIME'
    ? 'Digite o código recebido na central de notificações'
    : 'Digite sua senha atual para concluir o aceite';
}

export default function ContractSignatureDialog({
  open,
  contract,
  busy,
  challenge,
  hasBankAccount,
  onClose,
  onStartChallenge,
  onConfirmSignature
}) {
  const [accepted, setAccepted] = useState(false);
  const [method, setMethod] = useState('REAUTENTICACAO_SENHA');
  const [credential, setCredential] = useState('');

  const activeMethod = challenge?.metodo ?? method;
  const challengeExpiryLabel = challenge?.expiraEm ? formatDateTime(challenge.expiraEm) : null;
  const destinationLabel = challenge?.mascaraDestino || null;

  useEffect(() => {
    if (!open) {
      return;
    }
    setAccepted(Boolean(challenge?.desafioId));
    setMethod(challenge?.metodo ?? 'REAUTENTICACAO_SENHA');
    setCredential('');
  }, [open, challenge?.desafioId, challenge?.metodo, contract?.id]);

  const selectedMethod = useMemo(
    () => methodOptions.find((option) => option.value === activeMethod) ?? methodOptions[0],
    [activeMethod]
  );

  if (!open || !contract) {
    return null;
  }

  const handleStart = async () => {
    if (!hasBankAccount || busy || !accepted) {
      return;
    }
    await onStartChallenge(method);
  };

  const handleConfirm = async () => {
    if (!hasBankAccount || busy || !challenge?.desafioId || !credential.trim()) {
      return;
    }
    await onConfirmSignature(credential);
  };

  return (
    <div
      className="contract-signature-dialog-backdrop"
      role="presentation"
      onClick={(event) => {
        if (event.target === event.currentTarget && !busy) {
          onClose();
        }
      }}
    >
      <section
        className="contract-signature-dialog"
        role="dialog"
        aria-modal="true"
        aria-labelledby="contract-signature-dialog-title"
      >
        <header className="contract-signature-dialog-header">
          <div>
            <span className="contract-detail-label">Assinatura eletrônica protegida</span>
            <h2 id="contract-signature-dialog-title">Confirmar aceite do contrato {contract.numeroContrato}</h2>
            <p className="helper-text">
              O aceite final depende de um desafio curto. Revise o prazo, escolha o método e conclua a validação antes da
              expiração.
            </p>
          </div>

          <button type="button" className="contract-signature-dialog-close" onClick={onClose} disabled={busy}>
            <UiIcon name="x" size={18} />
            Fechar
          </button>
        </header>

        <div className="contract-signature-dialog-meta">
          <span className="contract-signature-chip">
            <UiIcon name="clock" size={14} />
            Prazo geral até {formatDateTime(contract.dataExpiracaoAssinatura)}
          </span>
          <span className="contract-signature-chip">{formatLabel(contract.status)}</span>
          {challengeExpiryLabel ? <span className="contract-signature-chip">Desafio ativo até {challengeExpiryLabel}</span> : null}
        </div>

        {!hasBankAccount ? (
          <BankAccountNotice show message="Cadastre uma conta bancária em Minha conta antes de iniciar ou concluir a assinatura." />
        ) : null}

        <label className="contract-signature-acceptance">
          <input
            type="checkbox"
            checked={accepted}
            onChange={(event) => setAccepted(event.target.checked)}
            disabled={busy}
          />
          <span>
            Confirmo que revisei o contrato e desejo registrar o aceite eletrônico desta operação no LoanFlow.
          </span>
        </label>

        <div className="contract-signature-method-grid">
          {methodOptions.map((option) => {
            const selected = method === option.value;
            const lockedByChallenge = Boolean(challenge?.desafioId && challenge?.metodo !== option.value);
            return (
              <button
                key={option.value}
                type="button"
                className={`contract-signature-method-card${selected ? ' is-selected' : ''}`}
                onClick={() => setMethod(option.value)}
                disabled={busy || Boolean(challenge?.desafioId) || lockedByChallenge}
              >
                <UiIcon name={option.icon} size={18} />
                <strong>{option.title}</strong>
                <span>{option.description}</span>
              </button>
            );
          })}
        </div>

        <div className="contract-signature-dialog-stage">
          <div className="contract-signature-head">
            <div>
              <span className="contract-detail-label">
                {challenge?.desafioId ? 'Confirmação final do aceite' : 'Etapa 1: iniciar desafio'}
              </span>
              <strong>
                {challenge?.desafioId
                  ? `Use ${resolveCredentialLabel(activeMethod).toLowerCase()} para concluir a assinatura`
                  : `Preparar desafio por ${selectedMethod.title.toLowerCase()}`}
              </strong>
            </div>
            <span className="contract-signature-chip">{selectedMethod.title}</span>
          </div>

          <p className="helper-text contract-signature-helper">
            {challenge?.mensagem
              ? challenge.mensagem
              : selectedMethod.value === 'CODIGO_ONE_TIME'
                ? 'Ao iniciar o desafio, um código temporário será disponibilizado na central de notificações e associado ao destino mascarado do seu cadastro.'
                : 'Ao iniciar o desafio, você liberará a confirmação final por senha dentro de alguns minutos.'}
          </p>

          {destinationLabel ? (
            <p className="helper-text contract-signature-helper">
              Destino mascarado vinculado ao desafio: <strong>{destinationLabel}</strong>
            </p>
          ) : null}

          {challenge?.desafioId ? (
            <div className="contract-signature-form">
              <label>
                {resolveCredentialLabel(activeMethod)}
                <input
                  type={activeMethod === 'CODIGO_ONE_TIME' ? 'text' : 'password'}
                  value={credential}
                  onChange={(event) => setCredential(event.target.value)}
                  placeholder={resolveCredentialPlaceholder(activeMethod)}
                  disabled={busy || !hasBankAccount}
                />
              </label>
            </div>
          ) : null}
        </div>

        <footer className="contract-signature-actions">
          <button type="button" className="secondary-button" onClick={onClose} disabled={busy}>
            Cancelar
          </button>

          {!challenge?.desafioId ? (
            <button
              type="button"
              className="primary-button"
              onClick={handleStart}
              disabled={busy || !hasBankAccount || !accepted}
            >
              <UiIcon name="shield-check" size={16} />
              {busy ? 'Iniciando desafio...' : 'Iniciar desafio'}
            </button>
          ) : (
            <button
              type="button"
              className="primary-button"
              onClick={handleConfirm}
              disabled={busy || !hasBankAccount || !accepted || !credential.trim()}
            >
              <UiIcon name="signature" size={16} />
              {busy ? 'Confirmando...' : 'Confirmar assinatura'}
            </button>
          )}
        </footer>
      </section>
    </div>
  );
}
