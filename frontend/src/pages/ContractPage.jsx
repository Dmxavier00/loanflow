import { useEffect, useMemo, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import BankAccountNotice from '../components/BankAccountNotice';
import EmptyState from '../components/EmptyState';
import MessageBanner from '../components/MessageBanner';
import SectionCard from '../components/SectionCard';
import StatusBadge from '../components/StatusBadge';
import { useAuth } from '../context/AuthContext';
import { api } from '../lib/api';
import { formatDateTime, formatLabel } from '../lib/format';

const statusOptions = [
  '',
  'AGUARDANDO_ASSINATURAS',
  'ASSINADO_PARCIALMENTE',
  'FORMALIZADO',
  'CANCELADO'
];

export default function ContractPage() {
  const { token, hasRole, hasBankAccount } = useAuth();
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();

  const highlightedContractId =
    Number(searchParams.get('contratoId') || window.localStorage.getItem('loanflow.lastContractId') || 0) || null;
  const [filters, setFilters] = useState({
    numeroContrato: searchParams.get('numeroContrato') || '',
    status: searchParams.get('status') || '',
    finalidade: searchParams.get('finalidade') || ''
  });
  const [contracts, setContracts] = useState([]);
  const [selectedContractId, setSelectedContractId] = useState(highlightedContractId);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [feedback, setFeedback] = useState('');

  const canSign = hasRole('SOLICITANTE') || hasRole('CREDOR');
  const canCancel = hasRole('CREDOR') || hasRole('ADMIN');

  const selectedContract = useMemo(
    () => contracts.find((contract) => contract.id === selectedContractId) ?? null,
    [contracts, selectedContractId]
  );

  const syncSearchParams = (nextFilters, contractId = null) => {
    const next = new URLSearchParams();
    if (nextFilters.numeroContrato) {
      next.set('numeroContrato', nextFilters.numeroContrato);
    }
    if (nextFilters.status) {
      next.set('status', nextFilters.status);
    }
    if (nextFilters.finalidade) {
      next.set('finalidade', nextFilters.finalidade);
    }
    if (contractId) {
      next.set('contratoId', String(contractId));
    }
    setSearchParams(next);
  };

  const loadContracts = async (currentFilters = filters, preferredContractId = selectedContractId ?? highlightedContractId) => {
    setLoading(true);
    setError('');

    try {
      const data = await api.searchContracts(token, {
        numeroContrato: currentFilters.numeroContrato || undefined,
        status: currentFilters.status || undefined,
        finalidade: currentFilters.finalidade || undefined
      });
      setContracts(data);

      const nextSelectedId =
        data.find((contract) => contract.id === preferredContractId)?.id ??
        data[0]?.id ??
        null;
      setSelectedContractId(nextSelectedId);
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

  const handleFilterChange = (event) => {
    const { name, value } = event.target;
    setFilters((current) => ({ ...current, [name]: value }));
  };

  const handleSelectContract = (contractId) => {
    setSelectedContractId(contractId);
    window.localStorage.setItem('loanflow.lastContractId', String(contractId));
    syncSearchParams(filters, contractId);
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
      link.download = `contrato-${selectedContract.id}.pdf`;
      link.click();
      window.URL.revokeObjectURL(blobUrl);
    } catch (downloadError) {
      setError(downloadError.message);
    }
  };

  return (
    <div className="page-stack">
      <SectionCard title="Filtros de contratos" subtitle="Busque por número do contrato, status ou finalidade.">
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
            Finalidade
            <input
              name="finalidade"
              value={filters.finalidade}
              onChange={handleFilterChange}
              placeholder="Ex.: capital de giro"
            />
          </label>
        </div>
        <div className="form-actions">
          <button type="button" className="primary-button" onClick={() => loadContracts(filters, selectedContractId)}>
            {loading ? 'Carregando...' : 'Filtrar contratos'}
          </button>
        </div>
      </SectionCard>

      <MessageBanner type="error">{error}</MessageBanner>
      <MessageBanner type="success">{feedback}</MessageBanner>
      <BankAccountNotice
        show={canSign && !hasBankAccount}
        message="Cadastre uma conta bancária no painel antes de assinar contratos."
      />

      <SectionCard title="Contratos acessíveis" subtitle="Selecione um contrato da lista para ver detalhes e ações.">
        {contracts.length ? (
          <div className="list-stack">
            {contracts.map((contract) => (
              <article
                key={contract.id}
                className={`list-card${selectedContractId === contract.id ? ' list-card-highlight' : ''}`}
              >
                <div>
                  <strong>{contract.numeroContrato}</strong>
                  <p>{contract.finalidade || 'Sem finalidade informada'}</p>
                  <small>
                    Proposta #{contract.propostaId} | Gerado em {formatDateTime(contract.dataGeracao)}
                  </small>
                </div>
                <div className="stack-actions">
                  <StatusBadge value={contract.status} />
                  <button
                    type="button"
                    className={selectedContractId === contract.id ? 'primary-button' : 'secondary-button'}
                    onClick={() => handleSelectContract(contract.id)}
                  >
                    {selectedContractId === contract.id ? 'Selecionado' : 'Selecionar'}
                  </button>
                </div>
              </article>
            ))}
          </div>
        ) : (
          <EmptyState
            title="Nenhum contrato encontrado."
            description="Ajuste os filtros para localizar contratos vinculados ao seu perfil."
          />
        )}
      </SectionCard>

      {selectedContract ? (
        <>
          <SectionCard
            title={selectedContract.numeroContrato}
            subtitle={selectedContract.finalidade || 'Contrato gerado na plataforma'}
            actions={
              <div className="inline-button-group">
                <button type="button" className="secondary-button" onClick={handleDownload}>
                  Baixar PDF
                </button>
                <button
                  type="button"
                  className="secondary-button"
                  onClick={() => navigate(`/parcelas?numeroContrato=${encodeURIComponent(selectedContract.numeroContrato)}`)}
                >
                  Ver parcelas
                </button>
              </div>
            }
          >
            <div className="detail-grid">
              <div>
                <dt>Status</dt>
                <dd>
                  <StatusBadge value={selectedContract.status} />
                </dd>
              </div>
              <div>
                <dt>Proposta vinculada</dt>
                <dd>#{selectedContract.propostaId}</dd>
              </div>
              <div>
                <dt>Data de geração</dt>
                <dd>{formatDateTime(selectedContract.dataGeracao)}</dd>
              </div>
              <div>
                <dt>Formalização</dt>
                <dd>{formatDateTime(selectedContract.dataFormalizacao)}</dd>
              </div>
              <div className="detail-span-2">
                <dt>Hash do documento</dt>
                <dd className="code-box">{selectedContract.hashDocumento}</dd>
              </div>
              <div className="detail-span-2">
                <dt>Arquivo PDF</dt>
                <dd>{selectedContract.pdfPath}</dd>
              </div>
            </div>
          </SectionCard>

          <SectionCard title="Ações do contrato" subtitle="Operações permitidas para o perfil autenticado.">
            <div className="action-strip">
              {canSign &&
              ['AGUARDANDO_ASSINATURAS', 'ASSINADO_PARCIALMENTE'].includes(selectedContract.status) ? (
                <button
                  type="button"
                  className="primary-button"
                  onClick={() =>
                    runAction(() => api.signContract(token, selectedContract.id), 'Assinatura registrada.')
                  }
                  disabled={!hasBankAccount}
                >
                  Assinar contrato
                </button>
              ) : null}

              {canCancel && !['FORMALIZADO', 'CANCELADO'].includes(selectedContract.status) ? (
                <button
                  type="button"
                  className="danger-button"
                  onClick={() =>
                    runAction(() => api.cancelContract(token, selectedContract.id), 'Contrato cancelado.')
                  }
                >
                  Cancelar contrato
                </button>
              ) : null}
            </div>

            <p className="helper-text">
              Status atual: <strong>{formatLabel(selectedContract.status)}</strong>. Depois da formalização, o
              próximo passo é acompanhar as parcelas.
            </p>
          </SectionCard>
        </>
      ) : null}
    </div>
  );
}
