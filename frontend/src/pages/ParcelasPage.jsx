import { useEffect, useMemo, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import EmptyState from '../components/EmptyState';
import MessageBanner from '../components/MessageBanner';
import SectionCard from '../components/SectionCard';
import StatusBadge from '../components/StatusBadge';
import { useAuth } from '../context/AuthContext';
import { api } from '../lib/api';
import { formatCurrency, formatDate, formatDateTime, formatLabel } from '../lib/format';

const paymentFormDefaults = {
  valorPago: '',
  formaPagamento: 'PIX_MANUAL',
  comprovante: ''
};

const statusOptions = ['', 'ABERTA', 'PARCIALMENTE_PAGA', 'PAGA', 'EM_ATRASO'];

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
  const [payments, setPayments] = useState([]);
  const [paymentForm, setPaymentForm] = useState(paymentFormDefaults);
  const [error, setError] = useState('');
  const [feedback, setFeedback] = useState('');
  const [loading, setLoading] = useState(false);

  const canRegisterPayment = hasRole('SOLICITANTE') || hasRole('ADMIN');

  const selectedParcela = useMemo(
    () => parcelas.find((parcela) => parcela.id === selectedParcelaId) ?? null,
    [parcelas, selectedParcelaId]
  );

  const paymentSummary = useMemo(() => {
    return payments.reduce((sum, payment) => sum + Number(payment.valorPago ?? 0), 0);
  }, [payments]);

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

  const loadPayments = async (parcela, currentFilters = filters) => {
    const paymentList = await api.getPagamentosByParcela(token, parcela.id);
    setSelectedParcelaId(parcela.id);
    setPayments(paymentList);
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
        await loadPayments(nextSelectedParcela, currentFilters);
      } else {
        setSelectedParcelaId(null);
        setPayments([]);
        syncSearchParams(currentFilters, null);
      }
    } catch (loadError) {
      setError(loadError.message);
      setParcelas([]);
      setSelectedParcelaId(null);
      setPayments([]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadParcelas(filters, highlightedParcelaId);
  }, [token]);

  const handleFilterChange = (event) => {
    const { name, value } = event.target;
    setFilters((current) => ({ ...current, [name]: value }));
  };

  const handlePaymentFormChange = (event) => {
    const { name, value } = event.target;
    setPaymentForm((current) => ({ ...current, [name]: value }));
  };

  const handleSelectParcela = async (parcela) => {
    setError('');
    setFeedback('');

    try {
      await loadPayments(parcela);
    } catch (loadError) {
      setError(loadError.message);
    }
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

  const handleCancelPayment = async (paymentId) => {
    if (!selectedParcela) {
      return;
    }

    setError('');
    setFeedback('');

    try {
      await api.cancelPayment(token, selectedParcela.id, paymentId);
      setFeedback('Pagamento cancelado com sucesso.');
      await refreshCurrentSelection();
    } catch (actionError) {
      setError(actionError.message);
    }
  };

  return (
    <div className="page-stack">
      <SectionCard title="Filtros de parcelas" subtitle="Busque parcelas por contrato, status e vencimento.">
        <div className="filters-row">
          <label>
            Número do contrato
            <input
              name="numeroContrato"
              value={filters.numeroContrato}
              onChange={handleFilterChange}
              placeholder="Ex.: LF-123"
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
      </SectionCard>

      <MessageBanner type="error">{error}</MessageBanner>
      <MessageBanner type="success">{feedback}</MessageBanner>

      <SectionCard title="Parcelas acessíveis" subtitle="Selecione uma parcela para ver detalhes e pagamentos.">
        {parcelas.length ? (
          <div className="list-stack">
            {parcelas.map((parcela) => (
              <article
                key={parcela.id}
                className={`list-card${selectedParcelaId === parcela.id ? ' list-card-highlight' : ''}`}
              >
                <div>
                  <strong>{parcela.numeroContrato}</strong>
                  <p>Parcela {parcela.numero} com vencimento em {formatDate(parcela.dataVencimento)}</p>
                  <small>
                    Previsto {formatCurrency(parcela.valorPrevisto)} | Pago{' '}
                    {formatCurrency(parcela.valorPagoAcumulado)}
                  </small>
                </div>
                <div className="stack-actions">
                  <StatusBadge value={parcela.status} />
                  <button
                    type="button"
                    className={selectedParcelaId === parcela.id ? 'primary-button' : 'secondary-button'}
                    onClick={() => handleSelectParcela(parcela)}
                  >
                    {selectedParcelaId === parcela.id ? 'Selecionada' : 'Selecionar'}
                  </button>
                </div>
              </article>
            ))}
          </div>
        ) : (
          <EmptyState
            title="Nenhuma parcela encontrada."
            description="Depois da formalização do contrato, as parcelas aparecem aqui e podem ser refinadas pelos filtros."
          />
        )}
      </SectionCard>

      {selectedParcela ? (
        <>
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
          </SectionCard>

          {canRegisterPayment ? (
            <SectionCard title="Registrar pagamento" subtitle="Fluxo manual do MVP para demonstração do TCC.">
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
            </SectionCard>
          ) : null}

          <SectionCard title="Pagamentos registrados" subtitle={`Total registrado: ${formatCurrency(paymentSummary)}`}>
            {payments.length ? (
              <div className="list-stack">
                {payments.map((payment) => (
                  <article key={payment.id} className="list-card">
                    <div>
                      <strong>Pagamento #{payment.id}</strong>
                      <p>{formatCurrency(payment.valorPago)} via {payment.formaPagamento}</p>
                      <small>{formatDateTime(payment.dataHoraPagamento)}</small>
                    </div>
                    <div className="stack-actions">
                      <StatusBadge value={payment.status} />
                      {canRegisterPayment && payment.status !== 'CANCELADO' ? (
                        <button
                          type="button"
                          className="danger-button"
                          onClick={() => handleCancelPayment(payment.id)}
                        >
                          Cancelar
                        </button>
                      ) : null}
                    </div>
                  </article>
                ))}
              </div>
            ) : (
              <EmptyState
                title="Sem pagamentos nesta parcela."
                description="O registro manual de pagamentos aparece aqui para auditoria."
              />
            )}
          </SectionCard>
        </>
      ) : null}
    </div>
  );
}
