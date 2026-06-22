import { useEffect, useMemo, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import EmptyState from '../componentes/EmptyState';
import MessageBanner from '../componentes/MessageBanner';
import SectionCard from '../componentes/SectionCard';
import StatusBadge from '../componentes/StatusBadge';
import UiIcon from '../componentes/UiIcon';
import { useAuth } from '../contexto/AuthContext';
import { api } from '../biblioteca/api';
import { formatCreditScore, formatCurrency, formatDateTime, formatLabel, formatProposalNumber } from '../biblioteca/format';

const visibleContractStatus = 'FORMALIZADO';
const purposeCategoryOptions = [
  { value: 'CAPITAL_DE_GIRO', label: 'Capital de giro' },
  { value: 'REFORMA', label: 'Reforma' },
  { value: 'QUITACAO_DE_DIVIDAS', label: 'Quitacao de dividas' },
  { value: 'EMERGENCIA', label: 'Emergencia' },
  { value: 'ESTUDO', label: 'Estudo' },
  { value: 'SAUDE', label: 'Saude' },
  { value: 'OUTRA', label: 'Outra' }
];

const installmentOpenStatuses = ['ABERTA', 'PARCIALMENTE_PAGA', 'EM_ATRASO'];
const contractsPerPage = 6;
const emptyFilters = {
  numeroContrato: '',
  nomeContraparte: '',
  categoriaFinalidade: ''
};

const localDateFormatter = new Intl.DateTimeFormat('pt-BR', {
  dateStyle: 'short'
});

function sumValues(items, getter) {
  return items.reduce((sum, item) => sum + Number(getter(item) ?? 0), 0);
}

function getPageForContract(contracts, contractId) {
  if (!contractId) {
    return 1;
  }

  const contractIndex = contracts.findIndex((contract) => contract.id === contractId);
  return contractIndex >= 0 ? Math.floor(contractIndex / contractsPerPage) + 1 : 1;
}

function toDateValue(value) {
  if (!value) {
    return null;
  }

  if (typeof value === 'string' && /^\d{4}-\d{2}-\d{2}$/.test(value)) {
    const [year, month, day] = value.split('-').map(Number);
    return new Date(year, month - 1, day);
  }

  return new Date(value);
}

function toTimeValue(value) {
  const parsed = toDateValue(value)?.getTime() ?? 0;
  return Number.isNaN(parsed) ? 0 : parsed;
}

function formatLocalDate(value) {
  const parsed = toDateValue(value);
  if (!parsed || Number.isNaN(parsed.getTime())) {
    return '-';
  }

  return localDateFormatter.format(parsed);
}

function formatCpf(value) {
  if (!value) {
    return '-';
  }

  const digits = value.toString().replace(/\D/g, '');
  if (digits.length !== 11) {
    return value;
  }

  return `${digits.slice(0, 3)}.${digits.slice(3, 6)}.${digits.slice(6, 9)}-${digits.slice(9)}`;
}

function getDaysUntil(value) {
  const parsed = toDateValue(value);
  if (!parsed || Number.isNaN(parsed.getTime())) {
    return null;
  }

  const today = new Date();
  today.setHours(0, 0, 0, 0);

  parsed.setHours(0, 0, 0, 0);
  return Math.round((parsed.getTime() - today.getTime()) / 86400000);
}

function formatDueLabel(value) {
  const daysUntil = getDaysUntil(value);
  if (daysUntil === null) {
    return 'Sem vencimento previsto';
  }

  if (daysUntil === 0) {
    return 'Vence hoje';
  }

  if (daysUntil === 1) {
    return 'Vence amanha';
  }

  if (daysUntil > 1) {
    return `Vence em ${daysUntil} dias`;
  }

  if (daysUntil === -1) {
    return 'Venceu ontem';
  }

  return `Venceu ha ${Math.abs(daysUntil)} dias`;
}

function getContractStatusTone(status) {
  if (status === 'FORMALIZADO' || status === 'QUITADO') {
    return 'success';
  }

  if (status === 'CANCELADO' || status === 'EXPIRADO') {
    return 'danger';
  }

  return 'info';
}

function getInstallmentPill(installment) {
  if (!installment) {
    return { label: 'Sem agenda', tone: 'info' };
  }

  if (installment.status === 'PAGA') {
    return { label: 'Liquidada', tone: 'success' };
  }

  if (installment.status === 'EM_ATRASO') {
    return { label: 'Em atraso', tone: 'danger' };
  }

  if (installment.status === 'PARCIALMENTE_PAGA') {
    return { label: 'Pagamento parcial', tone: 'warning' };
  }

  if ((getDaysUntil(installment.dataVencimento) ?? 999) <= 3) {
    return { label: 'Proxima da data', tone: 'warning' };
  }

  return { label: 'Programada', tone: 'info' };
}

function getContractListMeta(contract) {
  const lifecycleLabel = contract.dataFormalizacao
    ? `Formalizado em ${formatDateTime(contract.dataFormalizacao)}`
    : `Gerado em ${formatDateTime(contract.dataGeracao)}`;

  return `Proposta ${formatProposalNumber(contract)} | ${formatCurrency(contract.valorTotalComJuros)} | ${lifecycleLabel}`;
}

function getCounterpartySummary(contract, { isCredor, isSolicitante }) {
  if (isCredor) {
    return contract.solicitanteNome ? `Solicitante: ${contract.solicitanteNome}` : '';
  }

  if (isSolicitante) {
    return contract.credorNome ? `Credor: ${contract.credorNome}` : '';
  }

  return [
    contract.solicitanteNome && `Solicitante: ${contract.solicitanteNome}`,
    contract.credorNome && `Credor: ${contract.credorNome}`
  ]
    .filter(Boolean)
    .join(' | ');
}

function ContractDetailsDialog({
  open,
  contract,
  tone,
  canCancelSelected,
  installmentOverview,
  installmentsLoading,
  installmentsError,
  contractTotalWithInterest,
  onClose,
  onDownload,
  onCopyHash,
  onCancelContract
}) {
  if (!open || !contract) {
    return null;
  }

  const lifecycleLabel = contract.dataFormalizacao ? 'Formalizado em' : 'Gerado em';
  const lifecycleValue = contract.dataFormalizacao ? formatDateTime(contract.dataFormalizacao) : formatDateTime(contract.dataGeracao);
  const proposalNumber = formatProposalNumber(contract);
  const nextInstallment = installmentOverview.nextInstallment;
  const hasInstallments = installmentOverview.total > 0;
  const installmentsTotalLabel = installmentsLoading
    ? '--'
    : hasInstallments
      ? `${installmentOverview.total} parcela(s)`
      : 'Nao gerado';
  const nextInstallmentLabel = installmentsLoading
    ? '--'
    : nextInstallment
      ? `Parcela ${nextInstallment.numero}`
      : hasInstallments
        ? '-'
        : 'Cronograma pendente';
  const nextInstallmentMeta = installmentsLoading
    ? 'Atualizando'
    : nextInstallment
      ? `${formatCurrency(nextInstallment.valorPrevisto)} | ${formatLocalDate(nextInstallment.dataVencimento)}`
      : hasInstallments
        ? 'Sem parcela aberta'
        : 'Parcelas ainda nao geradas';

  return (
    <div
      className="contract-details-dialog-backdrop"
      role="presentation"
      onClick={(event) => {
        if (event.target === event.currentTarget) {
          onClose();
        }
      }}
    >
      <section
        className="contract-details-dialog"
        role="dialog"
        aria-modal="true"
        aria-labelledby="contract-details-dialog-title"
      >
        <header className="contract-details-dialog-header">
          <div className="contract-details-dialog-heading">
            <span className="contract-detail-label">Carteira de contratos</span>
            <div className="contract-details-dialog-title-row">
              <h2 id="contract-details-dialog-title">{contract.numeroContrato}</h2>
              <StatusBadge value={contract.status} />
            </div>
          </div>

          <div className="contract-details-dialog-header-actions">
            <button
              type="button"
              className="contract-details-dialog-close"
              onClick={onClose}
            >
              <UiIcon name="x" size={18} />
              Fechar
            </button>
          </div>
        </header>

        <div className="contract-details-dialog-body">
          <article className={`contract-selection-panel contract-tone-${tone} contract-simple-panel`}>
            <div className="contract-simple-toolbar">
              <div className="contract-simple-title">
                <span className="contract-detail-label">Contrato selecionado</span>
                <strong>{contract.finalidade || 'Sem finalidade informada'}</strong>
              </div>

              <div className="contract-simple-actions">
                <button type="button" className="secondary-button" onClick={onDownload}>
                  <UiIcon name="file" size={16} />
                  Baixar PDF
                </button>

                <button
                  type="button"
                  className="secondary-button"
                  onClick={onCopyHash}
                  disabled={!contract.hashDocumento}
                >
                  <UiIcon name="shield-check" size={16} />
                  Copiar hash
                </button>

                {canCancelSelected ? (
                  <button
                    type="button"
                    className="danger-button"
                    onClick={onCancelContract}
                  >
                    <UiIcon name="ban" size={16} />
                    Cancelar contrato
                  </button>
                ) : null}
              </div>
            </div>

            <div className="contract-simple-stats">
              <article className="contract-simple-stat">
                <span>Proposta</span>
                <strong>{proposalNumber}</strong>
              </article>
              <article className="contract-simple-stat">
                <span>{lifecycleLabel}</span>
                <strong>{lifecycleValue}</strong>
              </article>
              <article className="contract-simple-stat">
                <span>Proxima parcela</span>
                <strong>{nextInstallmentLabel}</strong>
                <small>{nextInstallmentMeta}</small>
              </article>
            </div>

            <div className="contract-simple-grid">
              <section className="contract-simple-section contract-simple-section-wide contract-parties-section">
                <span className="contract-detail-label">Partes</span>
                <div className="contract-party-grid">
                  <article className="contract-party-card">
                    <div className="contract-party-card-head">
                      <span className="contract-party-icon">
                        <UiIcon name="user" size={18} />
                      </span>
                      <div>
                        <span>Solicitante</span>
                        <strong>{contract.solicitanteNome || '-'}</strong>
                      </div>
                    </div>

                    <dl className="contract-party-dl">
                      <div>
                        <dt>CPF</dt>
                        <dd>{formatCpf(contract.solicitanteCpf)}</dd>
                      </div>
                      <div>
                        <dt>E-mail</dt>
                        <dd>{contract.solicitanteEmail || '-'}</dd>
                      </div>
                      <div>
                        <dt>Telefone</dt>
                        <dd>{contract.solicitanteTelefone || '-'}</dd>
                      </div>
                      <div>
                        <dt>Renda mensal</dt>
                        <dd>{formatCurrency(contract.solicitanteRendaMensal)}</dd>
                      </div>
                      <div>
                        <dt>Score</dt>
                        <dd>{formatCreditScore(contract.solicitanteScoreCredito)}</dd>
                      </div>
                      <div>
                        <dt>Risco</dt>
                        <dd>{formatLabel(contract.solicitanteNivelRisco)}</dd>
                      </div>
                    </dl>
                  </article>

                  <article className="contract-party-card">
                    <div className="contract-party-card-head">
                      <span className="contract-party-icon">
                        <UiIcon name="bank" size={18} />
                      </span>
                      <div>
                        <span>Credor</span>
                        <strong>{contract.credorNome || '-'}</strong>
                      </div>
                    </div>

                    <dl className="contract-party-dl">
                      <div>
                        <dt>CPF</dt>
                        <dd>{formatCpf(contract.credorCpf)}</dd>
                      </div>
                      <div>
                        <dt>E-mail</dt>
                        <dd>{contract.credorEmail || '-'}</dd>
                      </div>
                      <div>
                        <dt>Telefone</dt>
                        <dd>{contract.credorTelefone || '-'}</dd>
                      </div>
                      <div>
                        <dt>Banco</dt>
                        <dd>{contract.credorBanco || '-'}</dd>
                      </div>
                      <div>
                        <dt>Total emprestado</dt>
                        <dd>{formatCurrency(contract.credorTotalEmprestadoSimulado)}</dd>
                      </div>
                      <div>
                        <dt>Chave Pix</dt>
                        <dd>{contract.credorChavePix || '-'}</dd>
                      </div>
                    </dl>
                  </article>
                </div>
              </section>

              <section className="contract-simple-section contract-simple-section-wide contract-financial-section">
                <div className="contract-simple-section-head">
                  <span className="contract-detail-label">Financeiro</span>
                </div>

                <div className="contract-finance-metrics">
                  <article className="contract-finance-metric">
                    <span>Total com juros</span>
                    <strong>{formatCurrency(contractTotalWithInterest)}</strong>
                  </article>
                  <article className="contract-finance-metric">
                    <span>Pago ate agora</span>
                    <strong>{installmentsLoading ? '--' : formatCurrency(installmentOverview.paidValue)}</strong>
                  </article>
                  <article className="contract-finance-metric">
                    <span>Em aberto</span>
                    <strong>{installmentsLoading ? '--' : installmentOverview.open}</strong>
                  </article>
                  <article className="contract-finance-metric">
                    <span>Em atraso</span>
                    <strong>{installmentsLoading ? '--' : installmentOverview.overdue}</strong>
                  </article>
                </div>

                {installmentsError ? <p className="helper-text">{installmentsError}</p> : null}

                {installmentsLoading ? (
                  <p className="helper-text">Carregando parcelas...</p>
                ) : installmentOverview.preview.length ? (
                  <div className="contract-finance-feed">
                    {installmentOverview.preview.map((installment) => {
                      const pill = getInstallmentPill(installment);

                      return (
                        <article key={installment.id} className="contract-installment-item">
                          <div className="contract-installment-copy">
                            <strong>Parcela {installment.numero}</strong>
                            <p>{formatDueLabel(installment.dataVencimento)}</p>
                          </div>
                          <div className="contract-installment-meta">
                            <span className={`contract-pill tone-${pill.tone}`}>{pill.label}</span>
                            <strong>{formatCurrency(installment.valorPrevisto)}</strong>
                            <small>{formatLocalDate(installment.dataVencimento)}</small>
                          </div>
                        </article>
                      );
                    })}
                  </div>
                ) : (
                  <EmptyState
                    title="Sem parcelas carregadas."
                    description="Sem registros de parcelas para este contrato."
                  />
                )}

                <div className="contract-panel-footer">
                  <Link
                    className="secondary-button"
                    to={`/parcelas?numeroContrato=${encodeURIComponent(contract.numeroContrato)}`}
                  >
                    <UiIcon name="stack" size={16} />
                    Abrir cronograma completo
                  </Link>
                </div>
              </section>

            </div>
          </article>
        </div>
      </section>
    </div>
  );
}

export default function ContractPage() {
  const { token, hasRole } = useAuth();
  const [searchParams, setSearchParams] = useSearchParams();

  const highlightedContractId =
    Number(searchParams.get('contratoId') || window.localStorage.getItem('loanflow.lastContractId') || 0) || null;
  const [filters, setFilters] = useState({
    ...emptyFilters,
    numeroContrato: searchParams.get('numeroContrato') || '',
    nomeContraparte: searchParams.get('nomeContraparte') || '',
    categoriaFinalidade: searchParams.get('categoriaFinalidade') || ''
  });
  const [contracts, setContracts] = useState([]);
  const [contractPage, setContractPage] = useState(1);
  const [selectedContractId, setSelectedContractId] = useState(highlightedContractId);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [feedback, setFeedback] = useState('');
  const [contractInstallments, setContractInstallments] = useState([]);
  const [installmentsLoading, setInstallmentsLoading] = useState(false);
  const [installmentsError, setInstallmentsError] = useState('');
  const [detailsDialogOpen, setDetailsDialogOpen] = useState(false);

  const canCancel = hasRole('CREDOR') || hasRole('ADMIN');
  const isCredor = hasRole('CREDOR');
  const isSolicitante = hasRole('SOLICITANTE');
  const showCounterpartyFilter = isCredor || isSolicitante;
  const counterpartyFilterLabel = isCredor ? 'Nome do solicitante' : 'Nome do credor';
  const counterpartyFilterPlaceholder = isCredor ? 'Ex.: Maria Silva' : 'Ex.: Credor Financeiro';

  const selectedContract = useMemo(
    () => contracts.find((contract) => contract.id === selectedContractId) ?? null,
    [contracts, selectedContractId]
  );

  const totalContractPages = Math.max(1, Math.ceil(contracts.length / contractsPerPage));
  const activeContractPage = Math.min(contractPage, totalContractPages);
  const visibleContracts = useMemo(() => {
    const startIndex = (activeContractPage - 1) * contractsPerPage;
    return contracts.slice(startIndex, startIndex + contractsPerPage);
  }, [activeContractPage, contracts]);

  useEffect(() => {
    if (!selectedContract?.id) {
      setContractInstallments([]);
      setInstallmentsError('');
      return;
    }

    let isActive = true;

    async function loadInstallments() {
      setInstallmentsLoading(true);
      setInstallmentsError('');

      try {
        const data = await api.getParcelasByContrato(token, selectedContract.id);
        if (isActive) {
          setContractInstallments(data ?? []);
        }
      } catch (loadError) {
        if (isActive) {
          setContractInstallments([]);
          setInstallmentsError(loadError.message);
        }
      } finally {
        if (isActive) {
          setInstallmentsLoading(false);
        }
      }
    }

    loadInstallments();

    return () => {
      isActive = false;
    };
  }, [selectedContract?.id, token]);

  const installmentOverview = useMemo(() => {
    const paid = contractInstallments.filter((installment) => installment.status === 'PAGA');
    const overdue = contractInstallments.filter((installment) => installment.status === 'EM_ATRASO');
    const open = contractInstallments.filter((installment) => installmentOpenStatuses.includes(installment.status));
    const nextInstallment =
      [...open].sort((left, right) => toTimeValue(left.dataVencimento) - toTimeValue(right.dataVencimento))[0] ?? null;
    const previewSource = open.length ? open : contractInstallments;
    const preview = [...previewSource]
      .sort((left, right) => toTimeValue(left.dataVencimento) - toTimeValue(right.dataVencimento))
      .slice(0, 4);

    return {
      total: contractInstallments.length,
      paid: paid.length,
      open: open.length,
      overdue: overdue.length,
      totalValue: sumValues(contractInstallments, (installment) => installment.valorPrevisto),
      paidValue: sumValues(contractInstallments, (installment) => installment.valorPagoAcumulado),
      nextInstallment,
      preview
    };
  }, [contractInstallments]);

  const selectedContractTone = selectedContract ? getContractStatusTone(selectedContract.status) : 'info';
  const canCancelSelected = Boolean(
    selectedContract && canCancel && !['FORMALIZADO', 'QUITADO', 'CANCELADO'].includes(selectedContract.status)
  );
  const contractTotalWithInterest =
    installmentOverview.totalValue > 0
      ? installmentOverview.totalValue
      : selectedContract?.valorTotalComJuros;
  const syncSearchParams = (nextFilters, contractId = null) => {
    const next = new URLSearchParams();
    if (nextFilters.numeroContrato) {
      next.set('numeroContrato', nextFilters.numeroContrato);
    }
    if (nextFilters.nomeContraparte) {
      next.set('nomeContraparte', nextFilters.nomeContraparte);
    }
    if (nextFilters.categoriaFinalidade) {
      next.set('categoriaFinalidade', nextFilters.categoriaFinalidade);
    }
    if (contractId) {
      next.set('contratoId', String(contractId));
    }
    setSearchParams(next);
  };

  const loadContracts = async (
    currentFilters = filters,
    preferredContractId = selectedContractId ?? highlightedContractId
  ) => {
    setLoading(true);
    setError('');

    try {
      const data = await api.searchContracts(token, {
        numeroContrato: currentFilters.numeroContrato || undefined,
        nomeContraparte: currentFilters.nomeContraparte || undefined,
        status: visibleContractStatus,
        categoriaFinalidade: currentFilters.categoriaFinalidade || undefined
      });
      const formalizedContracts = (data ?? []).filter((contract) => contract.status === visibleContractStatus);
      setContracts(formalizedContracts);

      const nextSelectedId = formalizedContracts.find((contract) => contract.id === preferredContractId)?.id ?? null;
      setSelectedContractId(nextSelectedId);
      setContractPage(getPageForContract(formalizedContracts, nextSelectedId));
      if (nextSelectedId) {
        window.localStorage.setItem('loanflow.lastContractId', String(nextSelectedId));
      }
      syncSearchParams(currentFilters, nextSelectedId);
    } catch (loadError) {
      setError(loadError.message);
      setContracts([]);
      setSelectedContractId(null);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadContracts(filters, highlightedContractId);
  }, [token]);

  useEffect(() => {
    if (detailsDialogOpen && !selectedContract) {
      setDetailsDialogOpen(false);
    }
  }, [detailsDialogOpen, selectedContract]);

  useEffect(() => {
    if (contractPage > totalContractPages) {
      setContractPage(totalContractPages);
    }
  }, [contractPage, totalContractPages]);

  const handleFilterChange = (event) => {
    const { name, value } = event.target;
    setFilters((current) => ({ ...current, [name]: value }));
  };

  const handleSelectContract = (contractId) => {
    setSelectedContractId(contractId);
    setContractPage(getPageForContract(contracts, contractId));
    window.localStorage.setItem('loanflow.lastContractId', String(contractId));
    syncSearchParams(filters, contractId);
    setDetailsDialogOpen(true);
  };

  const handleCloseDetailsDialog = () => {
    setDetailsDialogOpen(false);
  };

  const handleResetFilters = () => {
    setFilters(emptyFilters);
    loadContracts(emptyFilters, selectedContractId ?? highlightedContractId);
  };

  const runAction = async (action, successMessage) => {
    setError('');
    setFeedback('');

    try {
      await action();
      setFeedback(successMessage);
      await loadContracts(filters, selectedContract?.id ?? selectedContractId);
    } catch (actionError) {
      setError(actionError.message);
    }
  };

  const handleDownload = async () => {
    if (!selectedContract?.id) {
      return;
    }

    try {
      const blob = await api.downloadContract(token, selectedContract.id);
      const blobUrl = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = blobUrl;
      link.download = `${selectedContract.numeroContrato || `contrato-${selectedContract.id}`}.pdf`;
      link.click();
      window.URL.revokeObjectURL(blobUrl);
    } catch (downloadError) {
      setError(downloadError.message);
    }
  };

  const handleCopyHash = async () => {
    if (!selectedContract?.hashDocumento) {
      return;
    }

    try {
      if (!window.navigator?.clipboard?.writeText) {
        throw new Error('Copie o hash manualmente neste navegador.');
      }

      await window.navigator.clipboard.writeText(selectedContract.hashDocumento);
      setFeedback('Hash do contrato copiado.');
    } catch (copyError) {
      setError(copyError.message);
    }
  };

  return (
    <div className="page-stack contract-page contract-page-vertical">
      <div className="contracts-feedback-stack">
        <MessageBanner type="error">{error}</MessageBanner>
        <MessageBanner type="success">{feedback}</MessageBanner>
      </div>

      <SectionCard
        className="creditor-order-section contract-catalog-section"
        title="Contratos em carteira"
      >
        <div className="filters-row">
          <label>
            Numero do contrato
            <input
              name="numeroContrato"
              value={filters.numeroContrato}
              onChange={handleFilterChange}
              placeholder="Ex.: LF-2026-000123"
            />
          </label>
          {showCounterpartyFilter ? (
            <label>
              {counterpartyFilterLabel}
              <input
                name="nomeContraparte"
                value={filters.nomeContraparte}
                onChange={handleFilterChange}
                placeholder={counterpartyFilterPlaceholder}
              />
            </label>
          ) : null}
          <label>
            Categoria
            <select
              name="categoriaFinalidade"
              value={filters.categoriaFinalidade}
              onChange={handleFilterChange}
            >
              <option value="">Todas</option>
              {purposeCategoryOptions.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
          </label>
        </div>
        <div className="form-actions">
          <button type="button" className="primary-button" onClick={() => loadContracts(filters, selectedContractId)}>
            {loading ? 'Carregando...' : 'Aplicar filtros'}
          </button>
          <button type="button" className="secondary-button" onClick={handleResetFilters}>
            Limpar filtros
          </button>
        </div>

        {loading && !contracts.length ? (
          <p className="helper-text">Carregando contratos...</p>
        ) : contracts.length ? (
          <div className="creditor-order-results">
            <div className="list-stack">
              {visibleContracts.map((contract) => {
                const isContractSelected = selectedContractId === contract.id && detailsDialogOpen;
                const counterpartySummary = getCounterpartySummary(contract, { isCredor, isSolicitante });
                const contractListSummary = [counterpartySummary, getContractListMeta(contract)]
                  .filter(Boolean)
                  .join(' | ');

                return (
                  <article
                    key={contract.id}
                    className={`list-card creditor-order-card${isContractSelected ? ' list-card-highlight' : ''}`}
                  >
                    <div>
                      <strong>{contract.numeroContrato}</strong>
                      <p>{contract.finalidade || 'Sem finalidade informada para este contrato.'}</p>
                      <small>{contractListSummary}</small>
                    </div>

                    <div className="stack-actions">
                      <StatusBadge value={contract.status} />
                      <button
                        type="button"
                        className={isContractSelected ? 'primary-button' : 'secondary-button'}
                        onClick={() => handleSelectContract(contract.id)}
                      >
                        {isContractSelected ? 'Selecionado' : 'Ver detalhes'}
                      </button>
                    </div>
                  </article>
                );
              })}
            </div>

            <div className="creditor-order-pagination">
              <div className="creditor-order-pagination-copy">
                <strong>
                  Pagina {activeContractPage} de {totalContractPages}
                </strong>
                <span>
                  Mostrando {Math.min((activeContractPage - 1) * contractsPerPage + 1, contracts.length)} a{' '}
                  {Math.min(activeContractPage * contractsPerPage, contracts.length)} de {contracts.length} contratos
                </span>
              </div>
              <div className="creditor-order-pagination-actions">
                <button
                  type="button"
                  className="secondary-button"
                  onClick={() => setContractPage((currentPage) => Math.max(1, currentPage - 1))}
                  disabled={activeContractPage === 1}
                >
                  <span className="creditor-order-pagination-icon creditor-order-pagination-icon-left" aria-hidden="true">
                    <UiIcon name="arrow-right" size={16} />
                  </span>
                  Anterior
                </button>
                <button
                  type="button"
                  className="secondary-button"
                  onClick={() => setContractPage((currentPage) => Math.min(totalContractPages, currentPage + 1))}
                  disabled={activeContractPage === totalContractPages}
                >
                  Proxima
                  <UiIcon name="arrow-right" size={16} />
                </button>
              </div>
            </div>
          </div>
        ) : (
          <EmptyState
            title="Nenhum contrato encontrado."
            description="Ajuste os filtros para localizar contratos vinculados ao seu perfil."
          />
        )}
      </SectionCard>

      <ContractDetailsDialog
        open={detailsDialogOpen}
        contract={selectedContract}
        tone={selectedContractTone}
        canCancelSelected={canCancelSelected}
        installmentOverview={installmentOverview}
        installmentsLoading={installmentsLoading}
        installmentsError={installmentsError}
        contractTotalWithInterest={contractTotalWithInterest}
        onClose={handleCloseDetailsDialog}
        onDownload={handleDownload}
        onCopyHash={handleCopyHash}
        onCancelContract={() => runAction(() => api.cancelContract(token, selectedContract.id), 'Contrato cancelado.')}
      />
    </div>
  );
}
