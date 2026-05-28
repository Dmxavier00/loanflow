import BankAccountNotice from './BankAccountNotice';
import StatusBadge from './StatusBadge';
import {
  formatCreditScore,
  formatCurrency,
  formatDate,
  formatDateTime,
  formatLabel,
  formatProposalHeadline,
  formatProposalNumber
} from '../biblioteca/format';

export default function CreditorProposalDialog({
  open,
  proposal,
  busy,
  hasBankAccount,
  onClose,
  onAccept
}) {
  if (!open || !proposal) {
    return null;
  }

  const headline = formatProposalHeadline(proposal);
  const summary = proposal.finalidade || 'Sem resumo informado.';
  const detailedDescription = proposal.descricaoDetalhada || 'Sem descri\u00e7\u00e3o detalhada.';

  return (
    <div
      className="creditor-proposal-dialog-backdrop"
      role="presentation"
      onClick={(event) => {
        if (event.target === event.currentTarget && !busy) {
          onClose();
        }
      }}
    >
      <section
        className="creditor-proposal-dialog"
        role="dialog"
        aria-modal="true"
        aria-labelledby="creditor-proposal-dialog-title"
      >
        <header className="creditor-proposal-dialog-header">
          <div className="creditor-proposal-dialog-heading">
            <h2 id="creditor-proposal-dialog-title">{'Ordem pronta para contratação'}</h2>
          </div>
        </header>

        {!hasBankAccount ? (
          <BankAccountNotice
            show
            message={'Cadastre uma conta banc\u00e1ria em Minha conta antes de aceitar esta proposta.'}
          />
        ) : null}

        <dl className="creditor-proposal-dialog-card-grid" aria-label="Dados da proposta">
          <div className="creditor-proposal-dialog-info-card creditor-proposal-dialog-info-card-hero">
            <dt>Solicitante</dt>
            <dd>{headline}</dd>
          </div>
          <div className="creditor-proposal-dialog-info-card">
            <dt>Score</dt>
            <dd>{formatCreditScore(proposal.solicitanteScoreCredito)}</dd>
          </div>
          <div className="creditor-proposal-dialog-info-card">
            <dt>Risco</dt>
            <dd>
              <StatusBadge value={proposal.solicitanteNivelRisco} />
            </dd>
          </div>
          <div className="creditor-proposal-dialog-info-card">
            <dt>Status atual</dt>
            <dd>
              <StatusBadge value={proposal.status} />
            </dd>
          </div>
          <div className="creditor-proposal-dialog-info-card">
            <dt>Expira em</dt>
            <dd>{formatDate(proposal.dataExpiracao)}</dd>
          </div>
        </dl>

        <dl className="creditor-proposal-dialog-meta-grid" aria-label="Rastreio da proposta">
          <div className="creditor-proposal-dialog-info-card">
            <dt>Categoria</dt>
            <dd>{formatLabel(proposal.categoriaFinalidade)}</dd>
          </div>
          <div className="creditor-proposal-dialog-info-card">
            <dt>Criada em</dt>
            <dd>{formatDateTime(proposal.dataCriacao)}</dd>
          </div>
          <div className="creditor-proposal-dialog-info-card">
            <dt>{'N\u00ba da proposta'}</dt>
            <dd>{formatProposalNumber(proposal)}</dd>
          </div>
        </dl>

        <dl className="creditor-proposal-dialog-copy-grid" aria-label="Descrição da proposta">
          <div className="creditor-proposal-dialog-info-card creditor-proposal-dialog-info-card-wide">
            <dt>Resumo do pedido</dt>
            <dd>{summary}</dd>
          </div>
          <div className="creditor-proposal-dialog-info-card creditor-proposal-dialog-info-card-wide">
            <dt>{'Descri\u00e7\u00e3o detalhada'}</dt>
            <dd>{detailedDescription}</dd>
          </div>
        </dl>

        <dl className="creditor-proposal-dialog-financial-grid" aria-label="Condições financeiras da proposta">
          <div className="creditor-proposal-dialog-info-card">
            <dt>Valor solicitado</dt>
            <dd>{formatCurrency(proposal.valorSolicitado)}</dd>
          </div>
          <div className="creditor-proposal-dialog-info-card">
            <dt>Taxa de Juros</dt>
            <dd>{proposal.taxaJuros}%</dd>
          </div>
          <div className="creditor-proposal-dialog-info-card">
            <dt>Prazo</dt>
            <dd>{proposal.prazoMeses} meses</dd>
          </div>
          <div className="creditor-proposal-dialog-info-card creditor-proposal-dialog-info-card-featured">
            <dt>Total com juros</dt>
            <dd>{formatCurrency(proposal.valorTotalComJuros)}</dd>
          </div>
        </dl>

        <footer className="creditor-proposal-dialog-actions">
          <button
            type="button"
            className="secondary-button"
            onClick={onClose}
            disabled={busy}
          >
            Voltar
          </button>
          <button
            type="button"
            className="primary-button"
            onClick={onAccept}
            disabled={busy || !hasBankAccount}
          >
            {busy ? 'Formalizando...' : 'Aceitar e formalizar contrato'}
          </button>
        </footer>
      </section>
    </div>
  );
}
