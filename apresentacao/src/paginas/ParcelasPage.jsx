import { useEffect, useMemo, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import EmptyState from '../componentes/EmptyState';
import MessageBanner from '../componentes/MessageBanner';
import MetricCard from '../componentes/MetricCard';
import SectionCard from '../componentes/SectionCard';
import StatusBadge from '../componentes/StatusBadge';
import UiIcon from '../componentes/UiIcon';
import { useAuth } from '../contexto/AuthContext';
import { api } from '../biblioteca/api';
import { formatCurrency, formatDate, formatLabel } from '../biblioteca/format';

const paymentFormDefaults = {
  valorPago: '',
  formaPagamento: 'PIX_MANUAL',
  comprovante: ''
};

const statusOptions = ['', 'ABERTA', 'PARCIALMENTE_PAGA', 'PAGA', 'EM_ATRASO'];
const borrowerParcelasPerPage = 12;

function toAmount(value) {
  return Number(value ?? 0);
}

function isParcelaFullyPaid(parcela) {
  const valorPrevisto = toAmount(parcela.valorPrevisto);
  const valorPagoAcumulado = toAmount(parcela.valorPagoAcumulado);

  return parcela.status === 'PAGA' || (valorPrevisto > 0 && valorPagoAcumulado >= valorPrevisto);
}

function compareParcelas(left, right) {
  const numeroDifference = Number(left.numero ?? 0) - Number(right.numero ?? 0);
  if (numeroDifference !== 0) {
    return numeroDifference;
  }

  return new Date(left.dataVencimento ?? 0).getTime() - new Date(right.dataVencimento ?? 0).getTime();
}

function buildContractSummaries(parcelas) {
  const groupedContracts = new Map();

  parcelas.forEach((parcela, index) => {
    const numeroContrato = parcela.numeroContrato || `Contrato-${parcela.id}`;
    const currentContract = groupedContracts.get(numeroContrato) ?? {
      numeroContrato,
      parcelas: [],
      totalParcelas: 0,
      parcelasPagas: 0,
      valorTotalReceber: 0,
      valorRecebido: 0,
      sortOrder: index
    };

    currentContract.parcelas.push(parcela);
    currentContract.totalParcelas += 1;
    currentContract.valorTotalReceber += toAmount(parcela.valorPrevisto);
    currentContract.valorRecebido += toAmount(parcela.valorPagoAcumulado);

    if (isParcelaFullyPaid(parcela)) {
      currentContract.parcelasPagas += 1;
    }

    groupedContracts.set(numeroContrato, currentContract);
  });

  return Array.from(groupedContracts.values())
    .map((contract) => {
      const orderedParcelas = [...contract.parcelas].sort(compareParcelas);
      const parcelasRestantes = Math.max(contract.totalParcelas - contract.parcelasPagas, 0);

      return {
        ...contract,
        parcelas: orderedParcelas,
        parcelasRestantes,
        valorRestanteReceber: Math.max(contract.valorTotalReceber - contract.valorRecebido, 0),
        proximaParcela: orderedParcelas.find((parcela) => !isParcelaFullyPaid(parcela)) ?? null
      };
    })
    .sort((left, right) => left.sortOrder - right.sortOrder);
}

function pickPreferredContractParcela(contract, selectedParcelaId) {
  if (!contract) {
    return null;
  }

  return (
    contract.parcelas.find((parcela) => parcela.id === selectedParcelaId) ??
    contract.parcelas.find((parcela) => !isParcelaFullyPaid(parcela)) ??
    contract.parcelas[0] ??
    null
  );
}

function getPageForParcela(parcelasList, parcelaId) {
  const parcelaIndex = parcelasList.findIndex((parcela) => parcela.id === parcelaId);

  if (parcelaIndex < 0) {
    return 1;
  }

  return Math.floor(parcelaIndex / borrowerParcelasPerPage) + 1;
}

export default function ParcelasPage() {
  const { token, hasRole } = useAuth();
  const [searchParams, setSearchParams] = useSearchParams();
  const highlightedParcelaId = Number(searchParams.get('parcelaId') || 0) || null;

  const [filters, setFilters] = useState({
    numeroContrato: searchParams.get('numeroContrato') || '',
    status: searchParams.get('status') || '',
    dataVencimentoInicio: searchParams.get('dataVencimentoInicio') || '',
    dataVencimentoFim: searchParams.get('dataVencimentoFim') || ''
  });
  const [parcelas, setParcelas] = useState([]);
  const [selectedParcelaId, setSelectedParcelaId] = useState(highlightedParcelaId);
  const [paymentForm, setPaymentForm] = useState(paymentFormDefaults);
  const [error, setError] = useState('');
  const [feedback, setFeedback] = useState('');
  const [loading, setLoading] = useState(false);
  const [borrowerParcelaPage, setBorrowerParcelaPage] = useState(1);

  const isCredor = hasRole('CREDOR');
  const hasPaymentRole = hasRole('SOLICITANTE') || hasRole('ADMIN');

  const selectedParcela = useMemo(
    () => parcelas.find((parcela) => parcela.id === selectedParcelaId) ?? null,
    [parcelas, selectedParcelaId]
  );

  const canRegisterPayment = hasPaymentRole && selectedParcela?.status === 'ABERTA';

  const contractSummaries = useMemo(() => buildContractSummaries(parcelas), [parcelas]);

  const totalBorrowerParcelaPages = Math.max(1, Math.ceil(parcelas.length / borrowerParcelasPerPage));
  const activeBorrowerParcelaPage = Math.min(borrowerParcelaPage, totalBorrowerParcelaPages);

  const visibleBorrowerParcelas = useMemo(() => {
    const startIndex = (activeBorrowerParcelaPage - 1) * borrowerParcelasPerPage;

    return parcelas.slice(startIndex, startIndex + borrowerParcelasPerPage);
  }, [activeBorrowerParcelaPage, parcelas]);

  const selectedContractNumber = selectedParcela?.numeroContrato ?? contractSummaries[0]?.numeroContrato ?? null;

  const selectedContract = useMemo(
    () => contractSummaries.find((contract) => contract.numeroContrato === selectedContractNumber) ?? null,
    [contractSummaries, selectedContractNumber]
  );

  const creditorPortfolioSummary = useMemo(() => {
    return contractSummaries.reduce(
      (summary, contract) => ({
        totalContratos: summary.totalContratos + 1,
        totalParcelas: summary.totalParcelas + contract.totalParcelas,
        totalParcelasPagas: summary.totalParcelasPagas + contract.parcelasPagas,
        valorTotalReceber: summary.valorTotalReceber + contract.valorTotalReceber,
        valorRecebido: summary.valorRecebido + contract.valorRecebido,
        valorRestanteReceber: summary.valorRestanteReceber + contract.valorRestanteReceber
      }),
      {
        totalContratos: 0,
        totalParcelas: 0,
        totalParcelasPagas: 0,
        valorTotalReceber: 0,
        valorRecebido: 0,
        valorRestanteReceber: 0
      }
    );
  }, [contractSummaries]);

  const syncSearchParams = (nextFilters, parcelaId = null) => {
    const next = new URLSearchParams();
    if (nextFilters.numeroContrato) {
      next.set('numeroContrato', nextFilters.numeroContrato);
    }
    if (nextFilters.status) {
      next.set('status', nextFilters.status);
    }
    if (nextFilters.dataVencimentoInicio) {
      next.set('dataVencimentoInicio', nextFilters.dataVencimentoInicio);
    }
    if (nextFilters.dataVencimentoFim) {
      next.set('dataVencimentoFim', nextFilters.dataVencimentoFim);
    }
    if (parcelaId) {
      next.set('parcelaId', String(parcelaId));
    }
    setSearchParams(next);
  };

  const selectParcela = (parcela, currentFilters = filters) => {
    setSelectedParcelaId(parcela.id);
    syncSearchParams(currentFilters, parcela.id);
  };

  const loadParcelas = async (currentFilters = filters, preferredParcelaId = selectedParcelaId ?? highlightedParcelaId) => {
    setLoading(true);
    setError('');

    try {
      const data = await api.searchParcelas(token, {
        numeroContrato: currentFilters.numeroContrato || undefined,
        status: currentFilters.status || undefined,
        dataVencimentoInicio: currentFilters.dataVencimentoInicio || undefined,
        dataVencimentoFim: currentFilters.dataVencimentoFim || undefined
      });

      setParcelas(data);

      const nextSelectedParcela =
        data.find((parcela) => parcela.id === preferredParcelaId) ??
        data[0] ??
        null;

      if (nextSelectedParcela) {
        selectParcela(nextSelectedParcela, currentFilters);
        setBorrowerParcelaPage(getPageForParcela(data, nextSelectedParcela.id));
      } else {
        setSelectedParcelaId(null);
        setBorrowerParcelaPage(1);
        syncSearchParams(currentFilters, null);
      }
    } catch (loadError) {
      setError(loadError.message);
      setParcelas([]);
      setSelectedParcelaId(null);
      setBorrowerParcelaPage(1);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadParcelas(filters, highlightedParcelaId);
  }, [token]);

  useEffect(() => {
    if (borrowerParcelaPage > totalBorrowerParcelaPages) {
      setBorrowerParcelaPage(totalBorrowerParcelaPages);
    }
  }, [borrowerParcelaPage, totalBorrowerParcelaPages]);

  const handleFilterChange = (event) => {
    const { name, value } = event.target;
    setFilters((current) => ({ ...current, [name]: value }));
  };

  const handlePaymentFormChange = (event) => {
    const { name, value } = event.target;
    setPaymentForm((current) => ({ ...current, [name]: value }));
  };

  const handleSelectParcela = (parcela) => {
    setError('');
    setFeedback('');
    selectParcela(parcela);
  };

  const handleSelectContract = (numeroContrato) => {
    const contract = contractSummaries.find((item) => item.numeroContrato === numeroContrato);
    const parcela = pickPreferredContractParcela(contract, selectedParcelaId);

    if (!parcela) {
      return;
    }

    handleSelectParcela(parcela);
  };

  const refreshCurrentSelection = async () => {
    await loadParcelas(filters, selectedParcelaId);
  };

  const handleRegisterPayment = async (event) => {
    event.preventDefault();
    if (!selectedParcela) {
      setError('Selecione uma parcela para registrar o pagamento.');
      return;
    }

    setError('');
    setFeedback('');

    try {
      await api.registerPayment(token, selectedParcela.id, {
        valorPago: Number(paymentForm.valorPago),
        formaPagamento: paymentForm.formaPagamento,
        comprovante: paymentForm.comprovante
      });
      setPaymentForm(paymentFormDefaults);
      setFeedback('Pagamento registrado com sucesso.');
      await refreshCurrentSelection();
    } catch (actionError) {
      setError(actionError.message);
    }
  };

  const handleParcelaCardKeyDown = (event, parcela) => {
    if (event.key !== 'Enter' && event.key !== ' ') {
      return;
    }

    event.preventDefault();
    handleSelectParcela(parcela);
  };

  const renderFilterControls = () => (
    <>
      <div className="filters-row">
        <label>
          Número do contrato
          <input
            name="numeroContrato"
            value={filters.numeroContrato}
            onChange={handleFilterChange}
            placeholder="Ex.: LF-2026-000123"
          />
        </label>
        <label>
          Status
          <select name="status" value={filters.status} onChange={handleFilterChange}>
            {statusOptions.map((status) => (
              <option key={status || 'all'} value={status}>
                {status ? formatLabel(status) : 'Todos'}
              </option>
            ))}
          </select>
        </label>
        <label>
          Vencimento inicial
          <input
            type="date"
            name="dataVencimentoInicio"
            value={filters.dataVencimentoInicio}
            onChange={handleFilterChange}
          />
        </label>
        <label>
          Vencimento final
          <input
            type="date"
            name="dataVencimentoFim"
            value={filters.dataVencimentoFim}
            onChange={handleFilterChange}
          />
        </label>
      </div>
      <div className="form-actions">
        <button type="button" className="primary-button" onClick={() => loadParcelas(filters, selectedParcelaId)}>
          {loading ? 'Carregando...' : 'Filtrar parcelas'}
        </button>
      </div>
    </>
  );

  const renderParcelaList = (parcelasList) => (
    <div className="list-stack parcelas-grid">
      {parcelasList.map((parcela) => {
        const isSelected = selectedParcelaId === parcela.id;

        return (
          <article
            key={parcela.id}
            className={`list-card installment-card${isSelected ? ' list-card-highlight' : ''}`}
            role="button"
            tabIndex={0}
            aria-pressed={isSelected}
            onClick={() => handleSelectParcela(parcela)}
            onKeyDown={(event) => handleParcelaCardKeyDown(event, parcela)}
          >
            <div className="installment-card-head">
              <strong>{`${parcela.numeroContrato} · Parcela ${parcela.numero}`}</strong>
              <StatusBadge value={parcela.status} />
            </div>
            <p className="installment-card-due">Vencimento em {formatDate(parcela.dataVencimento)}</p>
            <div className="installment-card-values">
              <div>
                <span>Previsto</span>
                <strong>{formatCurrency(parcela.valorPrevisto)}</strong>
              </div>
              <div>
                <span>Pago</span>
                <strong>{formatCurrency(parcela.valorPagoAcumulado)}</strong>
              </div>
            </div>
          </article>
        );
      })}
    </div>
  );

  const renderBorrowerPagination = () => {
    if (parcelas.length <= borrowerParcelasPerPage) {
      return null;
    }

    return (
      <div className="creditor-order-pagination">
        <div className="creditor-order-pagination-copy">
          <strong>
            Página {activeBorrowerParcelaPage} de {totalBorrowerParcelaPages}
          </strong>
          <span>
            Mostrando{' '}
            {Math.min((activeBorrowerParcelaPage - 1) * borrowerParcelasPerPage + 1, parcelas.length)} a{' '}
            {Math.min(activeBorrowerParcelaPage * borrowerParcelasPerPage, parcelas.length)} de {parcelas.length}{' '}
            parcelas
          </span>
        </div>
        <div className="creditor-order-pagination-actions">
          <button
            type="button"
            className="secondary-button"
            onClick={() => setBorrowerParcelaPage((currentPage) => Math.max(1, currentPage - 1))}
            disabled={activeBorrowerParcelaPage === 1}
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
              setBorrowerParcelaPage((currentPage) => Math.min(totalBorrowerParcelaPages, currentPage + 1))
            }
            disabled={activeBorrowerParcelaPage === totalBorrowerParcelaPages}
          >
            Próxima
            <UiIcon name="arrow-right" size={16} />
          </button>
        </div>
      </div>
    );
  };

  return (
    <div className="page-stack">
      {isCredor ? (
        <SectionCard
          className="creditor-payments-summary-card"
          title="Carteira do credor"
        >
            {contractSummaries.length ? (
              <div className="metrics-grid creditor-payments-portfolio-grid">
                <MetricCard label="Total de contratos" value={creditorPortfolioSummary.totalContratos} />
                <MetricCard
                  label="Valor total a receber"
                  value={formatCurrency(creditorPortfolioSummary.valorTotalReceber)}
                accent="gold"
              />
              <MetricCard
                label="Valor já recebido"
                value={formatCurrency(creditorPortfolioSummary.valorRecebido)}
                accent="green"
              />
              <MetricCard
                label="Saldo a receber"
                value={formatCurrency(creditorPortfolioSummary.valorRestanteReceber)}
                accent="teal"
              />
            </div>
          ) : (
            <EmptyState
              title="Nenhum contrato encontrado."
              description="Quando o credor tiver contratos com parcelas acessíveis, o resumo financeiro aparece aqui."
            />
          )}
        </SectionCard>
      ) : null}

      {isCredor ? (
        <SectionCard title="Filtros de recebimento">
          {renderFilterControls()}
        </SectionCard>
      ) : null}

      {isCredor ? (
        <>
          <MessageBanner type="error">{error}</MessageBanner>
          <MessageBanner type="success">{feedback}</MessageBanner>

          {contractSummaries.length ? (
            <SectionCard title="Recebimento por contrato">
              <div className="creditor-payments-contract-grid">
                {contractSummaries.map((contract) => {
                  const isSelected = contract.numeroContrato === selectedContractNumber;

                  return (
                    <article
                      key={contract.numeroContrato}
                      className={`creditor-payments-contract-card${isSelected ? ' list-card-highlight' : ''}`}
                    >
                      <div className="creditor-payments-contract-head">
                        <div className="creditor-payments-contract-copy">
                          <strong>{contract.numeroContrato}</strong>
                          <p>
                            {contract.parcelasPagas} de {contract.totalParcelas} parcelas já foram recebidas
                          </p>
                          <div className="creditor-payments-contract-meta">
                            <span>{contract.parcelasRestantes} parcelas ainda em aberto</span>
                            <span>
                              {contract.proximaParcela
                                ? `Próximo vencimento em ${formatDate(contract.proximaParcela.dataVencimento)}`
                                : 'Todas as parcelas deste contrato já foram liquidadas'}
                            </span>
                          </div>
                        </div>
                        <button
                          type="button"
                          className={isSelected ? 'primary-button' : 'secondary-button'}
                          onClick={() => handleSelectContract(contract.numeroContrato)}
                        >
                          {isSelected ? 'Contrato selecionado' : 'Ver parcelas'}
                        </button>
                      </div>

                      <div className="creditor-payments-contract-metrics">
                        <div className="creditor-payments-contract-metric">
                          <span>Parcelas pagas</span>
                          <strong>{contract.parcelasPagas}</strong>
                        </div>
                        <div className="creditor-payments-contract-metric">
                          <span>Parcelas restantes</span>
                          <strong>{contract.parcelasRestantes}</strong>
                        </div>
                        <div className="creditor-payments-contract-metric">
                          <span>Total a receber</span>
                          <strong>{formatCurrency(contract.valorTotalReceber)}</strong>
                        </div>
                        <div className="creditor-payments-contract-metric">
                          <span>Já recebido</span>
                          <strong>{formatCurrency(contract.valorRecebido)}</strong>
                        </div>
                        <div className="creditor-payments-contract-metric is-highlight">
                          <span>Falta receber</span>
                          <strong>{formatCurrency(contract.valorRestanteReceber)}</strong>
                        </div>
                      </div>
                    </article>
                  );
                })}
              </div>
            </SectionCard>
          ) : null}

          {selectedContract ? (
            <SectionCard
              title={`Parcelas do contrato ${selectedContract.numeroContrato}`}
              subtitle="Selecione uma parcela para ver o detalhe financeiro dela."
            >
              <div className="creditor-payments-contract-metrics">
                <div className="creditor-payments-contract-metric">
                  <span>Parcelas pagas</span>
                  <strong>{selectedContract.parcelasPagas}</strong>
                </div>
                <div className="creditor-payments-contract-metric">
                  <span>Parcelas restantes</span>
                  <strong>{selectedContract.parcelasRestantes}</strong>
                </div>
                <div className="creditor-payments-contract-metric">
                  <span>Total a receber</span>
                  <strong>{formatCurrency(selectedContract.valorTotalReceber)}</strong>
                </div>
                <div className="creditor-payments-contract-metric">
                  <span>Já recebido</span>
                  <strong>{formatCurrency(selectedContract.valorRecebido)}</strong>
                </div>
                <div className="creditor-payments-contract-metric is-highlight">
                  <span>Falta receber</span>
                  <strong>{formatCurrency(selectedContract.valorRestanteReceber)}</strong>
                </div>
              </div>

              <div className="creditor-payments-subsection-head">
                <strong>Parcelas do contrato</strong>
                <p>Acompanhe o andamento de cada parcela e escolha uma delas para abrir o detalhe.</p>
              </div>

              {renderParcelaList(selectedContract.parcelas)}
            </SectionCard>
          ) : null}
        </>
      ) : (
        <SectionCard title="Filtros de parcelas">
          {renderFilterControls()}
          <MessageBanner type="error">{error}</MessageBanner>
          <MessageBanner type="success">{feedback}</MessageBanner>

          <div className="installments-filtered-list">
            {parcelas.length ? (
              <>
                {renderParcelaList(visibleBorrowerParcelas)}
                {renderBorrowerPagination()}
              </>
            ) : (
              <EmptyState
                title="Nenhuma parcela encontrada."
                description="Depois da formalização do contrato, as parcelas aparecem aqui e podem ser refinadas pelos filtros."
              />
            )}
          </div>
        </SectionCard>
      )}

      {selectedParcela ? (
        <SectionCard
          title={`${selectedParcela.numeroContrato} · Parcela ${selectedParcela.numero}`}
          subtitle={`Vencimento em ${formatDate(selectedParcela.dataVencimento)}`}
        >
          <div className="detail-grid">
            <div>
              <dt>Status</dt>
              <dd>
                <StatusBadge value={selectedParcela.status} />
              </dd>
            </div>
            <div>
              <dt>Contrato</dt>
              <dd>{selectedParcela.numeroContrato}</dd>
            </div>
            <div>
              <dt>Valor previsto</dt>
              <dd>{formatCurrency(selectedParcela.valorPrevisto)}</dd>
            </div>
            <div>
              <dt>Valor pago</dt>
              <dd>{formatCurrency(selectedParcela.valorPagoAcumulado)}</dd>
            </div>
          </div>

          {canRegisterPayment ? (
            <div className="installment-payment-form">
              <div className="installment-payment-form-head">
                <h3>Registrar pagamento</h3>
              </div>
              <form className="form-grid" onSubmit={handleRegisterPayment}>
                <label>
                  Valor pago
                  <input
                    type="number"
                    min="0.01"
                    step="0.01"
                    name="valorPago"
                    value={paymentForm.valorPago}
                    onChange={handlePaymentFormChange}
                    required
                  />
                </label>
                <label>
                  Forma de pagamento
                  <select
                    name="formaPagamento"
                    value={paymentForm.formaPagamento}
                    onChange={handlePaymentFormChange}
                  >
                    <option value="PIX_MANUAL">PIX manual</option>
                    <option value="TRANSFERENCIA_SIMULADA">Transferência simulada</option>
                    <option value="DINHEIRO">Dinheiro</option>
                    <option value="OUTRO">Outro</option>
                  </select>
                </label>
                <label className="form-span-2">
                  Comprovante
                  <textarea
                    rows="3"
                    name="comprovante"
                    value={paymentForm.comprovante}
                    onChange={handlePaymentFormChange}
                    placeholder="Descrição resumida do comprovante"
                  />
                </label>
                <div className="form-actions form-span-2">
                  <button type="submit" className="primary-button">
                    Registrar pagamento
                  </button>
                </div>
              </form>
            </div>
          ) : null}
        </SectionCard>
      ) : null}
    </div>
  );
}
