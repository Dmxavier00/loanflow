import { useEffect, useMemo, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import BankAccountNotice from '../components/BankAccountNotice';
import EmptyState from '../components/EmptyState';
import MessageBanner from '../components/MessageBanner';
import SectionCard from '../components/SectionCard';
import StatusBadge from '../components/StatusBadge';
import { useAuth } from '../context/AuthContext';
import { api } from '../lib/api';
import { formatCurrency, formatDate, formatLabel, formatProposalHeadline } from '../lib/format';

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
  { value: 'QUITACAO_DE_DIVIDAS', label: 'Quitacao de dividas' },
  { value: 'EMERGENCIA', label: 'Emergencia' },
  { value: 'ESTUDO', label: 'Estudo' },
  { value: 'SAUDE', label: 'Saude' },
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
  'EXPIRADA'
];

const creditorOrderStatusOptions = [
  '',
  'AGUARDANDO_ACEITE',
  'ACEITA'
];

const editableStatuses = ['RASCUNHO', 'AGUARDANDO_ACEITE'];
const cancelableStatuses = ['RASCUNHO', 'AGUARDANDO_ACEITE', 'SUBMETIDA', 'EM_ANALISE'];

const normalizeInterestRate = (value) => {
  if (value === null || value === undefined || value === '') {
    return defaultInterestRate;
  }

  const normalized = String(Number(value));
  return interestRateOptions.some((option) => option.value === normalized) ? normalized : '';
};

export default function PropostasPage() {
  const { token, hasRole, hasBankAccount } = useAuth();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const focusId = searchParams.get('focusId');

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [feedback, setFeedback] = useState('');
  const [form, setForm] = useState(emptyForm);
  const [editingId, setEditingId] = useState(null);
  const [myProposals, setMyProposals] = useState([]);
  const [pendingProposals, setPendingProposals] = useState([]);
  const [acceptedProposals, setAcceptedProposals] = useState([]);
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
        requests.push(api.getAcceptedProposals(token));
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
        const nextAccepted = responses[cursor] ?? [];
        cursor += 1;

        setPendingProposals(nextPending);
        setAcceptedProposals(nextAccepted);
        setSelectedProposalId((currentId) =>
          nextPending.some((proposal) => proposal.id === currentId) ? currentId : null
        );
      } else {
        setPendingProposals([]);
        setAcceptedProposals([]);
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

    await runAction(
      () => api.acceptProposal(token, selectedProposal.id),
      `Proposta #${selectedProposal.id} aceita com sucesso.`
    );
  };

  const handleGenerateContract = async (proposalId) => {
    await runAction(async () => {
      const contract = await api.generateContract(token, proposalId);
      window.localStorage.setItem('loanflow.lastContractId', String(contract.id));
      navigate(`/contratos?numeroContrato=${encodeURIComponent(contract.numeroContrato)}&contratoId=${contract.id}`);
    }, 'Contrato gerado com sucesso.');
  };

  return (
    <div className="page-stack">
      <MessageBanner type="error">{error}</MessageBanner>
      <MessageBanner type="success">{feedback}</MessageBanner>
      <BankAccountNotice
        show={(isSolicitante || isCredor) && !hasBankAccount}
        message={
          isCredor
            ? 'Cadastre uma conta bancaria em Minha conta antes de aceitar propostas ou gerar contratos.'
            : 'Cadastre uma conta bancaria em Minha conta antes de criar ou editar propostas.'
        }
      />

      {isSolicitante ? (
        <SectionCard
          title={editingId ? `Editar proposta #${editingId}` : 'Nova proposta'}
          subtitle="Crie uma proposta sem escolher credor. A validade e automatica por 7 dias e a proposta entra na fila aguardando aceite."
        >
          <form className="form-grid" onSubmit={handleSaveProposal}>
            <label>
              Valor solicitado
              <input
                type="number"
                min="0.01"
                step="0.01"
                name="valorSolicitado"
                value={form.valorSolicitado}
                onChange={handleFormChange}
                required
              />
            </label>
            <label>
              Taxa de juros
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
            <label>
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
            <label>
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
            <label className="form-span-2">
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
            <label className="form-span-2">
              Descricao detalhada
              <textarea
                name="descricaoDetalhada"
                rows="4"
                value={form.descricaoDetalhada}
                onChange={handleFormChange}
                maxLength="2000"
                required
              />
            </label>

            <div className="form-actions form-span-2">
              <button type="submit" className="primary-button" disabled={!hasBankAccount}>
                {editingId ? 'Salvar alteracoes' : 'Criar proposta'}
              </button>
              <button type="button" className="danger-button" onClick={resetForm}>
                Limpar dados
              </button>
              {proposalBeingEdited ? <StatusBadge value={proposalBeingEdited.status} /> : null}
            </div>
            <p className="helper-text form-span-2">
              A expiracao da proposta e renovada automaticamente por 7 dias sempre que ela for criada ou editada.
            </p>
          </form>
        </SectionCard>
      ) : null}

      {isSolicitante ? (
        <SectionCard title="Minhas propostas" subtitle="Acompanhe suas propostas abertas, aceitas e encerradas.">
          {loading ? (
            <p className="helper-text">Carregando propostas...</p>
          ) : myProposals.length ? (
            <div className="list-stack">
              {myProposals.map((proposal) => (
                <article
                  key={proposal.id}
                  className={`list-card${highlightedId === proposal.id ? ' list-card-highlight' : ''}`}
                >
                  <div>
                    <strong>Proposta #{proposal.id}</strong>
                    <p>{proposal.finalidade}</p>
                    <small>
                      {formatLabel(proposal.categoriaFinalidade)} | {formatCurrency(proposal.valorSolicitado)} | Expira em{' '}
                      {formatDate(proposal.dataExpiracao)}
                    </small>
                  </div>
                  <div className="stack-actions">
                    <StatusBadge value={proposal.status} />
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
          title="Ativos do credor"
          subtitle="Aqui voce acompanha a fila de aceite e as propostas que ja estao na sua carteira."
        >
          <div className="metrics-grid metrics-grid-compact">
            <article className="metric-card metric-card-compact metric-gold">
              <span>Aguardando aceite</span>
              <strong>{pendingProposals.length}</strong>
            </article>
            <article className="metric-card metric-card-compact metric-green">
              <span>Ja aceitas</span>
              <strong>{acceptedProposals.length}</strong>
            </article>
          </div>
        </SectionCard>
      ) : null}

      {isCredor ? (
        <SectionCard
          title="Ordens aguardando aceite"
          subtitle="Selecione uma proposta aberta para assumir a operacao como credor."
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
                placeholder="Resumo ou descricao"
              />
            </label>
          </div>
          <div className="form-actions">
            <button
              type="button"
              className="primary-button"
              onClick={() => setAppliedCreditorOrderFilters(creditorOrderFilters)}
            >
              Aplicar filtros
            </button>
          </div>

          {loading ? (
            <p className="helper-text">Carregando propostas aguardando aceite...</p>
          ) : filteredPendingProposals.length ? (
            <div className="list-stack">
              {filteredPendingProposals.map((proposal) => (
                <article
                  key={proposal.id}
                  className={`list-card${selectedProposalId === proposal.id ? ' list-card-highlight' : ''}`}
                >
                  <div>
                    <strong>{formatProposalHeadline(proposal)}</strong>
                    <p>{proposal.finalidade}</p>
                    <small>
                      Proposta #{proposal.id} | {formatLabel(proposal.categoriaFinalidade)} |{' '}
                      {formatCurrency(proposal.valorSolicitado)} | {proposal.prazoMeses} meses | Expira em{' '}
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
          ) : (
            <EmptyState
              title={
                pendingProposals.length
                  ? 'Nenhuma ordem corresponde aos filtros atuais.'
                  : 'Nenhuma proposta aguardando aceite.'
              }
              description={
                pendingProposals.length
                  ? 'Ajuste os filtros para visualizar outras ordens disponiveis.'
                  : 'Quando um solicitante criar uma proposta, ela aparecera aqui para os credores.'
              }
            />
          )}
        </SectionCard>
      ) : null}

      {isCredor ? (
        <SectionCard
          title="Ordem selecionada"
          subtitle="Confira os dados antes de aceitar a proposta."
          actions={
            <button
              type="button"
              className="primary-button"
              onClick={handleAcceptSelected}
              disabled={!selectedProposal || !hasBankAccount}
            >
              Aceitar proposta
            </button>
          }
        >
          {selectedProposal ? (
            <dl className="detail-grid">
              <div>
                <dt>Solicitante</dt>
                <dd>{formatProposalHeadline(selectedProposal)}</dd>
              </div>
              <div>
                <dt>Codigo</dt>
                <dd>#{selectedProposal.id}</dd>
              </div>
              <div>
                <dt>Status atual</dt>
                <dd>
                  <StatusBadge value={selectedProposal.status} />
                </dd>
              </div>
              <div>
                <dt>Valor solicitado</dt>
                <dd>{formatCurrency(selectedProposal.valorSolicitado)}</dd>
              </div>
              <div>
                <dt>Taxa de juros</dt>
                <dd>{selectedProposal.taxaJuros}%</dd>
              </div>
              <div>
                <dt>Prazo</dt>
                <dd>{selectedProposal.prazoMeses} meses</dd>
              </div>
              <div>
                <dt>Expiracao</dt>
                <dd>{formatDate(selectedProposal.dataExpiracao)}</dd>
              </div>
              <div>
                <dt>Categoria</dt>
                <dd>{formatLabel(selectedProposal.categoriaFinalidade)}</dd>
              </div>
              <div className="detail-span-2">
                <dt>Resumo do pedido</dt>
                <dd>{selectedProposal.finalidade}</dd>
              </div>
              <div className="detail-span-2">
                <dt>Descricao detalhada</dt>
                <dd>{selectedProposal.descricaoDetalhada}</dd>
              </div>
            </dl>
          ) : (
            <EmptyState
              title="Nenhuma proposta selecionada."
              description="Escolha uma proposta na lista acima para liberar o aceite."
            />
          )}
        </SectionCard>
      ) : null}

      {isCredor ? (
        <SectionCard title="Propostas aceitas por voce" subtitle="Historico das propostas ja assumidas pelo seu credor.">
          {loading ? (
            <p className="helper-text">Carregando propostas aceitas...</p>
          ) : acceptedProposals.length ? (
            <div className="list-stack">
              {acceptedProposals.map((proposal) => (
                <article key={proposal.id} className="list-card">
                  <div>
                    <strong>{formatProposalHeadline(proposal)}</strong>
                    <p>{proposal.finalidade}</p>
                    <small>
                      Proposta #{proposal.id} | {formatLabel(proposal.categoriaFinalidade)} |{' '}
                      {formatCurrency(proposal.valorSolicitado)} | {proposal.prazoMeses} meses
                    </small>
                  </div>
                  <div className="stack-actions">
                    <StatusBadge value={proposal.status} />
                    {['ACEITA', 'APROVADA'].includes(proposal.status) ? (
                      <button
                        type="button"
                        className="primary-button"
                        onClick={() => handleGenerateContract(proposal.id)}
                        disabled={!hasBankAccount}
                      >
                        Gerar contrato
                      </button>
                    ) : null}
                  </div>
                </article>
              ))}
            </div>
          ) : (
            <EmptyState
              title="Voce ainda nao aceitou propostas."
              description="As propostas aceitas nesta tela aparecerao aqui."
            />
          )}
        </SectionCard>
      ) : null}

    </div>
  );
}
