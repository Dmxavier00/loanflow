import { useEffect, useMemo, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import BankAccountNotice from '../componentes/BankAccountNotice';
import CreditorProposalDialog from '../componentes/CreditorProposalDialog';
import EmptyState from '../componentes/EmptyState';
import MessageBanner from '../componentes/MessageBanner';
import SectionCard from '../componentes/SectionCard';
import StatusBadge from '../componentes/StatusBadge';
import UiIcon from '../componentes/UiIcon';
import { useAuth } from '../contexto/AuthContext';
import { api } from '../biblioteca/api';
import {
  formatCreditScore,
  formatCurrency,
  formatDate,
  formatDateTime,
  formatLabel,
  formatProposalHeadline,
  formatProposalNumber,
  formatRiskLevel
} from '../biblioteca/format';

const interestRateOptions = Array.from({ length: 21 }, (_, index) => {
  const taxa = index + 5;

  return {
    value: String(taxa),
    label: `${taxa}%`
  };
});

const installmentOptions = Array.from({ length: 12 }, (_, index) => {
  const parcelas = index + 1;

  return {
    value: String(parcelas),
    label: `${parcelas} ${parcelas === 1 ? 'parcela' : 'parcelas'}`
  };
});

const purposeCategoryOptions = [
  { value: 'CAPITAL_DE_GIRO', label: 'Capital de giro' },
  { value: 'REFORMA', label: 'Reforma' },
  { value: 'QUITACAO_DE_DIVIDAS', label: 'Quitação de dívidas' },
  { value: 'EMERGENCIA', label: 'Emergência' },
  { value: 'ESTUDO', label: 'Estudo' },
  { value: 'SAUDE', label: 'Saúde' },
  { value: 'OUTRA', label: 'Outra' }
];

const defaultInterestRate = interestRateOptions[0].value;
const defaultPurposeCategory = purposeCategoryOptions[0].value;
const emptyForm = {
  valorSolicitado: '',
  taxaJuros: defaultInterestRate,
  prazoMeses: '3',
  categoriaFinalidade: defaultPurposeCategory,
  finalidade: '',
  descricaoDetalhada: ''
};

const statusOptions = [
  '',
  'AGUARDANDO_ACEITE',
  'ACEITA',
  'RASCUNHO',
  'SUBMETIDA',
  'EM_ANALISE',
  'APROVADA',
  'REJEITADA',
  'CANCELADA',
  'CONTRATADA',
  'QUITADA',
  'EXPIRADA'
];

const creditorOrderStatusOptions = [
  '',
  'AGUARDANDO_ACEITE',
  'ACEITA'
];

const editableStatuses = ['RASCUNHO', 'AGUARDANDO_ACEITE'];
const cancelableStatuses = ['RASCUNHO', 'AGUARDANDO_ACEITE', 'SUBMETIDA', 'EM_ANALISE'];
const creditorOrdersPerPage = 6;
const normalizeInterestRate = (value) => {
  if (value === null || value === undefined || value === '') {
    return defaultInterestRate;
  }

  const normalized = String(Number(value));
  return interestRateOptions.some((option) => option.value === normalized) ? normalized : '';
};

const getProposalPipelineLabel = (proposal) => {
  if (proposal.contratoStatus === 'QUITADO' || proposal.status === 'QUITADA') {
    return 'Contrato quitado';
  }

  if (proposal.status === 'CONTRATADA') {
    return 'Contrato ativo em acompanhamento';
  }

  if (proposal.status === 'REJEITADA') {
    return 'Encerrada por rejeição do credor';
  }

  if (proposal.credorId) {
    return 'Em acompanhamento por um credor';
  }

  if (proposal.status === 'RASCUNHO') {
    return 'Ainda não entrou na fila de aceite';
  }

  if (proposal.status === 'CANCELADA') {
    return 'Encerrada antes do aceite';
  }

  if (proposal.status === 'EXPIRADA') {
    return 'Prazo encerrado sem aceite';
  }

  return 'Disponível para aceite de um credor';
};

const getProposalStatusValue = (proposal) => (
  proposal?.contratoStatus === 'QUITADO' ? 'QUITADA' : proposal?.status
);

export default function PropostasPage() {
  const { token, hasRole, hasBankAccount } = useAuth();
  const [searchParams] = useSearchParams();
  const focusId = searchParams.get('focusId');

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [feedback, setFeedback] = useState('');
  const [form, setForm] = useState(emptyForm);
  const [editingId, setEditingId] = useState(null);
  const [myProposals, setMyProposals] = useState([]);
  const [pendingProposals, setPendingProposals] = useState([]);
  const [acceptingSelectedProposal, setAcceptingSelectedProposal] = useState(false);
  const [creditorOrderPage, setCreditorOrderPage] = useState(1);
  const [selectedProposalId, setSelectedProposalId] = useState(null);
  const [creditorOrderFilters, setCreditorOrderFilters] = useState({
    status: '',
    categoriaFinalidade: '',
    finalidade: ''
  });
  const [appliedCreditorOrderFilters, setAppliedCreditorOrderFilters] = useState({
    status: '',
    categoriaFinalidade: '',
    finalidade: ''
  });
  const isSolicitante = hasRole('SOLICITANTE');
  const isCredor = hasRole('CREDOR');
  const highlightedId = focusId ? Number(focusId) : null;

  const proposalBeingEdited = useMemo(
    () => myProposals.find((proposal) => proposal.id === editingId) ?? null,
    [myProposals, editingId]
  );

  const selectedProposal = useMemo(
    () => pendingProposals.find((proposal) => proposal.id === selectedProposalId) ?? null,
    [pendingProposals, selectedProposalId]
  );

  const filteredPendingProposals = useMemo(() => {
    const normalizedText = appliedCreditorOrderFilters.finalidade.trim().toLowerCase();

    return pendingProposals.filter((proposal) => {
      const matchesStatus = !appliedCreditorOrderFilters.status || proposal.status === appliedCreditorOrderFilters.status;
      const matchesCategory =
        !appliedCreditorOrderFilters.categoriaFinalidade ||
        proposal.categoriaFinalidade === appliedCreditorOrderFilters.categoriaFinalidade;
      const matchesText =
        !normalizedText ||
        proposal.finalidade?.toLowerCase().includes(normalizedText) ||
        proposal.descricaoDetalhada?.toLowerCase().includes(normalizedText);

      return matchesStatus && matchesCategory && matchesText;
    });
  }, [appliedCreditorOrderFilters, pendingProposals]);

  const totalCreditorOrderPages = Math.max(1, Math.ceil(filteredPendingProposals.length / creditorOrdersPerPage));
  const activeCreditorOrderPage = Math.min(creditorOrderPage, totalCreditorOrderPages);

  const visiblePendingProposals = useMemo(() => {
    const startIndex = (activeCreditorOrderPage - 1) * creditorOrdersPerPage;
    return filteredPendingProposals.slice(startIndex, startIndex + creditorOrdersPerPage);
  }, [activeCreditorOrderPage, filteredPendingProposals]);

  const loadData = async () => {
    setLoading(true);
    setError('');

    try {
      const requests = [];

      if (isSolicitante) {
        requests.push(api.getMyProposals(token));
      }

      if (isCredor) {
        requests.push(api.getPendingAcceptanceProposals(token));
      }

      const responses = await Promise.all(requests);
      let cursor = 0;

      if (isSolicitante) {
        setMyProposals(responses[cursor] ?? []);
        cursor += 1;
      } else {
        setMyProposals([]);
      }

      if (isCredor) {
        const nextPending = responses[cursor] ?? [];
        cursor += 1;

        setPendingProposals(nextPending);
        setSelectedProposalId((currentId) =>
          nextPending.some((proposal) => proposal.id === currentId) ? currentId : null
        );
      } else {
        setPendingProposals([]);
        setSelectedProposalId(null);
      }
    } catch (loadError) {
      setError(loadError.message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, [token, isSolicitante, isCredor]);

  useEffect(() => {
    if (creditorOrderPage > totalCreditorOrderPages) {
      setCreditorOrderPage(totalCreditorOrderPages);
    }
  }, [creditorOrderPage, totalCreditorOrderPages]);

  const populateFormForEdit = (proposal) => {
    setEditingId(proposal.id);
    setForm({
      valorSolicitado: proposal.valorSolicitado ?? '',
      taxaJuros: normalizeInterestRate(proposal.taxaJuros),
      prazoMeses: String(proposal.prazoMeses ?? 3),
      categoriaFinalidade: proposal.categoriaFinalidade ?? defaultPurposeCategory,
      finalidade: proposal.finalidade ?? '',
      descricaoDetalhada: proposal.descricaoDetalhada ?? ''
    });
  };

  const resetForm = () => {
    setEditingId(null);
    setForm(emptyForm);
  };

  const handleFormChange = (event) => {
    const { name, value } = event.target;
    setForm((current) => ({ ...current, [name]: value }));
  };

  const handleCreditorOrderFilterChange = (event) => {
    const { name, value } = event.target;
    setCreditorOrderFilters((current) => ({ ...current, [name]: value }));
  };

  const handleApplyCreditorOrderFilters = () => {
    setAppliedCreditorOrderFilters(creditorOrderFilters);
    setCreditorOrderPage(1);
  };

  const buildPayload = () => ({
    valorSolicitado: Number(form.valorSolicitado),
    taxaJuros: Number(form.taxaJuros),
    prazoMeses: Number(form.prazoMeses),
    categoriaFinalidade: form.categoriaFinalidade,
    finalidade: form.finalidade.trim(),
    descricaoDetalhada: form.descricaoDetalhada.trim()
  });

  const runAction = async (action, successMessage) => {
    setError('');
    setFeedback('');

    try {
      await action();
      setFeedback(successMessage);
      await loadData();
    } catch (actionError) {
      setError(actionError.message);
    }
  };

  const handleSaveProposal = async (event) => {
    event.preventDefault();

    await runAction(async () => {
      if (editingId) {
        await api.updateProposal(token, editingId, buildPayload());
      } else {
        const created = await api.createProposal(token, buildPayload());
        window.localStorage.setItem('loanflow.lastProposalId', String(created.id));
      }
      resetForm();
    }, editingId ? 'Proposta atualizada com sucesso.' : 'Proposta criada e aguardando aceite de um credor.');
  };

  const handleAcceptSelected = async () => {
    if (!selectedProposal) {
      setError('Selecione uma proposta antes de aceitar.');
      return;
    }

    setAcceptingSelectedProposal(true);

    try {
      await runAction(
        () => api.acceptProposal(token, selectedProposal.id),
        `Proposta ${formatProposalNumber(selectedProposal)} aceita e contratada com sucesso.`
      );
    } finally {
      setAcceptingSelectedProposal(false);
    }
  };

  return (
    <div className="page-stack">
      <MessageBanner type="error">{error}</MessageBanner>
      <MessageBanner type="success">{feedback}</MessageBanner>
      <BankAccountNotice
        show={(isSolicitante || isCredor) && !hasBankAccount}
        message={
          isCredor
            ? 'Cadastre uma conta bancária em Minha conta antes de aceitar e formalizar propostas.'
            : 'Cadastre uma conta bancária em Minha conta antes de criar ou editar propostas.'
        }
      />

      {isSolicitante ? (
        <SectionCard
          className="proposal-entry-card"
          title={editingId ? `Editar proposta #${editingId}` : 'Nova proposta'}
          subtitle="Crie uma proposta simulada sem escolher credor. A validade é automática por 7 dias e a proposta entra na fila aguardando aceite."
        >
          <form className="form-grid compact-proposal-form" onSubmit={handleSaveProposal}>
            <label className="proposal-field-amount">
              Valor solicitado
              <div className="currency-input-shell">
                <span aria-hidden="true">R$</span>
                <input
                  type="number"
                  min="0.01"
                  step="0.01"
                  name="valorSolicitado"
                  value={form.valorSolicitado}
                  onChange={handleFormChange}
                  required
                />
              </div>
            </label>
            <label className="proposal-field-rate">
              Taxa simulada
              <select
                name="taxaJuros"
                value={form.taxaJuros}
                onChange={handleFormChange}
                required
              >
                <option value="" disabled>
                  Selecione uma taxa
                </option>
                {interestRateOptions.map((option) => (
                  <option key={option.value} value={option.value}>
                    {option.label}
                  </option>
                ))}
              </select>
            </label>
            <label className="proposal-field-term">
              Parcelas
              <select
                name="prazoMeses"
                value={form.prazoMeses}
                onChange={handleFormChange}
                required
              >
                {installmentOptions.map((option) => (
                  <option key={option.value} value={option.value}>
                    {option.label}
                  </option>
                ))}
              </select>
            </label>
            <label className="proposal-field-category">
              Categoria
              <select
                name="categoriaFinalidade"
                value={form.categoriaFinalidade}
                onChange={handleFormChange}
                required
              >
                {purposeCategoryOptions.map((option) => (
                  <option key={option.value} value={option.value}>
                    {option.label}
                  </option>
                ))}
              </select>
            </label>
            <label className="form-span-2 proposal-field-split proposal-field-summary">
              Resumo do pedido
              <input
                type="text"
                name="finalidade"
                value={form.finalidade}
                onChange={handleFormChange}
                maxLength="160"
                required
              />
            </label>
            <label className="form-span-2 proposal-field-split proposal-field-details">
              Descrição detalhada
              <textarea
                name="descricaoDetalhada"
                rows="2"
                value={form.descricaoDetalhada}
                onChange={handleFormChange}
                maxLength="2000"
                required
              />
            </label>
            <div className="form-actions form-span-2">
              <button type="submit" className="primary-button" disabled={!hasBankAccount}>
                {editingId ? 'Salvar alterações' : 'Criar proposta'}
              </button>
              <button type="button" className="danger-button" onClick={resetForm}>
                Limpar dados
              </button>
              {proposalBeingEdited ? <StatusBadge value={proposalBeingEdited.status} /> : null}
            </div>
          </form>
        </SectionCard>
      ) : null}

      {isSolicitante ? (
        <SectionCard
          title="Minhas propostas"
          subtitle="Acompanhe suas propostas simuladas abertas, aceitas e encerradas."
        >
          {loading ? (
            <p className="helper-text">Carregando propostas...</p>
          ) : myProposals.length ? (
            <div className="list-stack">
              {myProposals.map((proposal) => (
                <article
                  key={proposal.id}
                  className={`borrower-proposal-card${highlightedId === proposal.id ? ' is-highlighted' : ''}`}
                >
                  <div className="borrower-proposal-card-head">
                    <div className="borrower-proposal-card-copy">
                      <div className="borrower-proposal-card-topline">
                        <span className="borrower-proposal-card-id">Proposta {formatProposalNumber(proposal)}</span>
                        {getProposalPipelineLabel(proposal) ? (
                          <span className="borrower-proposal-card-pipeline">{getProposalPipelineLabel(proposal)}</span>
                        ) : null}
                      </div>
                      <h3>{proposal.finalidade}</h3>
                      <p>{proposal.descricaoDetalhada}</p>
                    </div>
                    <div className="borrower-proposal-card-actions">
                      <StatusBadge value={getProposalStatusValue(proposal)} />
                      <div className="inline-button-group">
                        {editableStatuses.includes(proposal.status) ? (
                          <button
                            type="button"
                            className="secondary-button"
                            onClick={() => populateFormForEdit(proposal)}
                            disabled={!hasBankAccount}
                          >
                            Editar
                          </button>
                        ) : null}

                        {cancelableStatuses.includes(proposal.status) ? (
                          <button
                            type="button"
                            className="danger-button"
                            onClick={() => runAction(() => api.cancelProposal(token, proposal.id), 'Proposta cancelada.')}
                          >
                            Cancelar
                          </button>
                        ) : null}
                      </div>
                    </div>
                  </div>
                  <dl className="borrower-proposal-detail-grid">
                    <div>
                      <dt>Categoria</dt>
                      <dd>{formatLabel(proposal.categoriaFinalidade)}</dd>
                    </div>
                    <div>
                      <dt>Valor solicitado</dt>
                      <dd>{formatCurrency(proposal.valorSolicitado)}</dd>
                    </div>
                    <div>
                      <dt>Taxa simulada</dt>
                      <dd>{proposal.taxaJuros}%</dd>
                    </div>
                    <div>
                      <dt>Total com juros</dt>
                      <dd>{formatCurrency(proposal.valorTotalComJuros)}</dd>
                    </div>
                    <div>
                      <dt>Prazo</dt>
                      <dd>{proposal.prazoMeses} meses</dd>
                    </div>
                    <div>
                      <dt>Criada em</dt>
                      <dd>{formatDateTime(proposal.dataCriacao)}</dd>
                    </div>
                    <div>
                      <dt>Expira em</dt>
                      <dd>{formatDate(proposal.dataExpiracao)}</dd>
                    </div>
                    {proposal.numeroContrato ? (
                      <div>
                        <dt>Contrato</dt>
                        <dd>{proposal.numeroContrato}</dd>
                      </div>
                    ) : null}
                    {getProposalPipelineLabel(proposal) ? (
                      <div className="borrower-proposal-detail-wide">
                        <dt>Fluxo atual</dt>
                        <dd>{getProposalPipelineLabel(proposal)}</dd>
                      </div>
                    ) : null}
                  </dl>
                </article>
              ))}
            </div>
          ) : (
            <EmptyState
              title="Nenhuma proposta encontrada."
              description="Crie uma proposta para entrar na fila de aceite dos credores."
            />
          )}
        </SectionCard>
      ) : null}

      {isCredor ? (
        <SectionCard
          className="creditor-order-section"
          title="Ordens aguardando aceite"
        >
          <div className="filters-row">
            <label>
              Status
              <select
                name="status"
                value={creditorOrderFilters.status}
                onChange={handleCreditorOrderFilterChange}
              >
                {creditorOrderStatusOptions.map((status) => (
                  <option key={status || 'all'} value={status}>
                    {status ? formatLabel(status) : 'Todos'}
                  </option>
                ))}
              </select>
            </label>
            <label>
              Categoria
              <select
                name="categoriaFinalidade"
                value={creditorOrderFilters.categoriaFinalidade}
                onChange={handleCreditorOrderFilterChange}
              >
                <option value="">Todas</option>
                {purposeCategoryOptions.map((option) => (
                  <option key={option.value} value={option.value}>
                    {option.label}
                  </option>
                ))}
              </select>
            </label>
            <label>
              Texto
              <input
                name="finalidade"
                value={creditorOrderFilters.finalidade}
                onChange={handleCreditorOrderFilterChange}
                placeholder="Resumo ou descrição"
              />
            </label>
          </div>
          <div className="form-actions">
            <button
              type="button"
              className="primary-button"
              onClick={handleApplyCreditorOrderFilters}
            >
              Aplicar filtros
            </button>
          </div>

          {loading ? (
            <p className="helper-text">Carregando propostas aguardando aceite...</p>
          ) : filteredPendingProposals.length ? (
            <div className="creditor-order-results">
              <div className="list-stack">
                {visiblePendingProposals.map((proposal) => (
                  <article
                    key={proposal.id}
                    className={`list-card creditor-order-card${selectedProposalId === proposal.id ? ' list-card-highlight' : ''}`}
                  >
                    <div>
                      <strong>{formatProposalHeadline(proposal)}</strong>
                      <p>{proposal.finalidade}</p>
                      <small>
                        Proposta {formatProposalNumber(proposal)} | {formatLabel(proposal.categoriaFinalidade)} |{' '}
                        {formatCurrency(proposal.valorSolicitado)} | Score{' '}
                        {formatCreditScore(proposal.solicitanteScoreCredito)} |{' '}
                        {formatRiskLevel(proposal.solicitanteNivelRisco)} | {proposal.prazoMeses} meses | Expira em{' '}
                        {formatDate(proposal.dataExpiracao)}
                      </small>
                    </div>
                    <div className="stack-actions">
                      <StatusBadge value={proposal.status} />
                      <button
                        type="button"
                        className={selectedProposalId === proposal.id ? 'primary-button' : 'secondary-button'}
                        onClick={() => setSelectedProposalId(proposal.id)}
                      >
                        {selectedProposalId === proposal.id ? 'Selecionada' : 'Selecionar'}
                      </button>
                    </div>
                  </article>
                ))}
              </div>

              <div className="creditor-order-pagination">
                <div className="creditor-order-pagination-copy">
                  <strong>
                    Página {activeCreditorOrderPage} de {totalCreditorOrderPages}
                  </strong>
                  <span>
                    Mostrando{' '}
                    {Math.min((activeCreditorOrderPage - 1) * creditorOrdersPerPage + 1, filteredPendingProposals.length)} a{' '}
                    {Math.min(activeCreditorOrderPage * creditorOrdersPerPage, filteredPendingProposals.length)} de{' '}
                    {filteredPendingProposals.length} propostas
                  </span>
                </div>
                <div className="creditor-order-pagination-actions">
                  <button
                    type="button"
                    className="secondary-button"
                    onClick={() => setCreditorOrderPage((currentPage) => Math.max(1, currentPage - 1))}
                    disabled={activeCreditorOrderPage === 1}
                  >
                    <span className="creditor-order-pagination-icon creditor-order-pagination-icon-left" aria-hidden="true">
                      <UiIcon name="arrow-right" size={16} />
                    </span>
                    Anterior
                  </button>
                  <button
                    type="button"
                    className="secondary-button"
                    onClick={() =>
                      setCreditorOrderPage((currentPage) => Math.min(totalCreditorOrderPages, currentPage + 1))
                    }
                    disabled={activeCreditorOrderPage === totalCreditorOrderPages}
                  >
                    Próxima
                    <UiIcon name="arrow-right" size={16} />
                  </button>
                </div>
              </div>
            </div>
          ) : (
            <EmptyState
              title={
                pendingProposals.length
                  ? 'Nenhuma ordem corresponde aos filtros atuais.'
                  : 'Nenhuma proposta aguardando aceite.'
              }
              description={
                pendingProposals.length
                  ? 'Ajuste os filtros para visualizar outras ordens disponíveis.'
                  : 'Quando um solicitante criar uma proposta, ela aparecerá aqui para os credores.'
              }
            />
          )}
        </SectionCard>
      ) : null}

      <CreditorProposalDialog
        open={isCredor && Boolean(selectedProposal)}
        proposal={selectedProposal}
        busy={acceptingSelectedProposal}
        hasBankAccount={hasBankAccount}
        onClose={() => setSelectedProposalId(null)}
        onAccept={handleAcceptSelected}
      />
    </div>
  );
}
