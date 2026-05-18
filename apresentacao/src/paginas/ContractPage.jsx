import { useEffect, useMemo, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import BankAccountNotice from '../componentes/BankAccountNotice';
import ContractSignatureDialog from '../componentes/ContractSignatureDialog';
import EmptyState from '../componentes/EmptyState';
import MessageBanner from '../componentes/MessageBanner';
import SectionCard from '../componentes/SectionCard';
import StatusBadge from '../componentes/StatusBadge';
import UiIcon from '../componentes/UiIcon';
import { useAuth } from '../contexto/AuthContext';
import { api } from '../biblioteca/api';
import { formatCurrency, formatDateTime, formatLabel } from '../biblioteca/format';

const statusOptions = ['', 'AGUARDANDO_ASSINATURAS', 'ASSINADO_PARCIALMENTE', 'EXPIRADO', 'FORMALIZADO', 'CANCELADO'];

const signatureStatuses = ['AGUARDANDO_ASSINATURAS', 'ASSINADO_PARCIALMENTE'];
const installmentOpenStatuses = ['ABERTA', 'PARCIALMENTE_PAGA', 'EM_ATRASO'];
const emptyFilters = {
  numeroContrato: '',
  status: '',
  finalidade: ''
};

const localDateFormatter = new Intl.DateTimeFormat('pt-BR', {
  dateStyle: 'short'
});

function sumValues(items, getter) {
  return items.reduce((sum, item) => sum + Number(getter(item) ?? 0), 0);
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
  if (status === 'FORMALIZADO') {
    return 'success';
  }

  if (status === 'CANCELADO' || status === 'EXPIRADO') {
    return 'danger';
  }

  if (signatureStatuses.includes(status)) {
    return 'warning';
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

function getContractStatusSummary(contract) {
  if (!contract) {
    return 'Selecione um contrato para abrir os detalhes operacionais.';
  }

  if (contract.status === 'FORMALIZADO') {
    return 'Contrato formalizado com sucesso. O foco agora passa a ser a agenda financeira e a leitura das parcelas.';
  }

  if (contract.status === 'CANCELADO') {
    return 'Fluxo encerrado antes da formalizacao. O documento segue disponivel para consulta e rastreabilidade.';
  }

  if (contract.status === 'EXPIRADO') {
    return contract.motivoExpiracao || 'O prazo de assinatura expirou e o contrato foi travado para novos aceites.';
  }

  if (contract.status === 'ASSINADO_PARCIALMENTE') {
    return 'Uma assinatura ja foi registrada. Falta concluir o aceite restante para formalizar o contrato.';
  }

  if (contract.status === 'AGUARDANDO_ASSINATURAS') {
    return 'Contrato emitido e pronto para coleta de aceite eletronico.';
  }

  return 'Contrato em acompanhamento no fluxo operacional da plataforma.';
}

function getContractNextStep(contract, installmentOverview) {
  if (!contract) {
    return 'Selecione um contrato';
  }

  if (contract.status === 'AGUARDANDO_ASSINATURAS') {
    return 'Validar identidade e coletar a primeira assinatura';
  }

  if (contract.status === 'ASSINADO_PARCIALMENTE') {
    return 'Concluir a assinatura restante';
  }

  if (contract.status === 'FORMALIZADO') {
    return installmentOverview.nextInstallment
      ? `Acompanhar a parcela ${installmentOverview.nextInstallment.numero}`
      : 'Acompanhar a carteira de parcelas';
  }

  if (contract.status === 'EXPIRADO') {
    return 'Prazo de assinatura encerrado';
  }

  if (contract.status === 'CANCELADO') {
    return 'Fluxo encerrado';
  }

  return 'Monitorar evolucao do contrato';
}

function getPermissionSummary({ selectedContract, canSignSelected, canCancelSelected, hasBankAccount }) {
  if (!selectedContract) {
    return 'Selecione um contrato para ver o que o seu perfil pode fazer.';
  }

  if (canSignSelected && !hasBankAccount) {
    return 'O aceite esta disponivel, mas a conta bancaria precisa ser cadastrada antes da assinatura.';
  }

  if (canSignSelected && canCancelSelected) {
    return 'Seu perfil pode reautenticar, assinar o documento ou encerrar o fluxo antes da formalizacao.';
  }

  if (canSignSelected) {
    return 'Sua assinatura e a proxima acao esperada para este contrato.';
  }

  if (canCancelSelected) {
    return 'Seu perfil pode cancelar o contrato caso o fluxo precise ser interrompido.';
  }

  if (selectedContract.status === 'FORMALIZADO') {
    return 'Nenhuma acao contratual pendente. O acompanhamento segue na agenda de parcelas.';
  }

  if (selectedContract.status === 'EXPIRADO') {
    return selectedContract.motivoExpiracao || 'O contrato expirou e agora fica disponivel apenas para consulta.';
  }

  if (selectedContract.status === 'CANCELADO') {
    return 'Contrato disponivel apenas para leitura e historico.';
  }

  return 'Nao ha operacoes liberadas para o seu perfil neste momento.';
}

function getPdfFileName(pdfPath) {
  if (!pdfPath) {
    return 'PDF nao disponivel';
  }

  const normalizedSegments = pdfPath.split(/[\\/]/);
  return normalizedSegments[normalizedSegments.length - 1] || pdfPath;
}

export default function ContractPage() {
  const { token, hasRole, hasBankAccount } = useAuth();
  const [searchParams, setSearchParams] = useSearchParams();

  const highlightedContractId =
    Number(searchParams.get('contratoId') || window.localStorage.getItem('loanflow.lastContractId') || 0) || null;
  const [filters, setFilters] = useState({
    ...emptyFilters,
    numeroContrato: searchParams.get('numeroContrato') || '',
    status: searchParams.get('status') || '',
    finalidade: searchParams.get('finalidade') || ''
  });
  const [contracts, setContracts] = useState([]);
  const [selectedContractId, setSelectedContractId] = useState(highlightedContractId);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [feedback, setFeedback] = useState('');
  const [contractInstallments, setContractInstallments] = useState([]);
  const [installmentsLoading, setInstallmentsLoading] = useState(false);
  const [installmentsError, setInstallmentsError] = useState('');
  const [signatureDialogOpen, setSignatureDialogOpen] = useState(false);
  const [signatureChallenge, setSignatureChallenge] = useState(null);
  const [signatureBusy, setSignatureBusy] = useState(false);

  const canSign = hasRole('SOLICITANTE') || hasRole('CREDOR');
  const canCancel = hasRole('CREDOR') || hasRole('ADMIN');

  const selectedContract = useMemo(
    () => contracts.find((contract) => contract.id === selectedContractId) ?? null,
    [contracts, selectedContractId]
  );

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
  const selectedContractSummary = selectedContract
    ? getContractStatusSummary(selectedContract)
    : 'Selecione um contrato para abrir os detalhes operacionais.';
  const canSignSelected = Boolean(selectedContract && canSign && signatureStatuses.includes(selectedContract.status));
  const canCancelSelected = Boolean(
    selectedContract && canCancel && !['FORMALIZADO', 'CANCELADO'].includes(selectedContract.status)
  );
  const activeFilterCount = Object.values(filters).filter((value) => String(value || '').trim()).length;
  const activeFilterLabel = activeFilterCount
    ? `${activeFilterCount} filtro${activeFilterCount === 1 ? '' : 's'} ativo${activeFilterCount === 1 ? '' : 's'}`
    : 'Visao completa';
  const nextStepLabel = getContractNextStep(selectedContract, installmentOverview);
  const permissionSummary = getPermissionSummary({
    selectedContract,
    canSignSelected,
    canCancelSelected,
    hasBankAccount
  });
  const selectedContractPurpose =
    selectedContract?.finalidade || 'Contrato gerado na plataforma para continuidade do fluxo operacional.';
  const selectedContractExpiryLabel = selectedContract?.dataExpiracaoAssinatura
    ? formatDateTime(selectedContract.dataExpiracaoAssinatura)
    : 'Sem prazo informado';
  const challengeExpiryLabel = signatureChallenge?.expiraEm ? formatDateTime(signatureChallenge.expiraEm) : null;
  const selectedContractFacts = selectedContract
    ? [
        {
          label: 'Proposta vinculada',
          value: `#${selectedContract.propostaId}`,
          icon: 'file'
        },
        {
          label: 'Gerado em',
          value: formatDateTime(selectedContract.dataGeracao),
          icon: 'clock'
        },
        {
          label: selectedContract.dataFormalizacao ? 'Formalizado em' : 'Prazo do aceite',
          value: selectedContract.dataFormalizacao
            ? formatDateTime(selectedContract.dataFormalizacao)
            : selectedContractExpiryLabel,
          icon: selectedContract.dataFormalizacao ? 'file-check' : 'signature'
        }
      ]
    : [];
  const contractFinanceHeadline = installmentsLoading
    ? 'Atualizando cronograma'
    : installmentOverview.nextInstallment
      ? `Parcela ${installmentOverview.nextInstallment.numero}`
      : installmentOverview.total
        ? `${installmentOverview.total} parcela(s)`
        : 'Sem agenda financeira';
  const contractFinanceDescription = installmentsLoading
    ? 'Carregando a agenda financeira deste contrato.'
    : installmentOverview.nextInstallment
      ? `${formatDueLabel(installmentOverview.nextInstallment.dataVencimento)} | ${formatCurrency(
          installmentOverview.nextInstallment.valorPrevisto
        )}`
      : installmentOverview.total
        ? `${installmentOverview.open} em aberto e ${installmentOverview.overdue} em atraso.`
        : 'As parcelas aparecem aqui quando o contrato entrar na etapa financeira.';

  const resetSignatureFlow = () => {
    setSignatureChallenge(null);
    setSignatureBusy(false);
  };

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

  const loadContracts = async (
    currentFilters = filters,
    preferredContractId = selectedContractId ?? highlightedContractId
  ) => {
    setLoading(true);
    setError('');

    try {
      const data = await api.searchContracts(token, {
        numeroContrato: currentFilters.numeroContrato || undefined,
        status: currentFilters.status || undefined,
        finalidade: currentFilters.finalidade || undefined
      });
      setContracts(data);

      const nextSelectedId = data.find((contract) => contract.id === preferredContractId)?.id ?? data[0]?.id ?? null;
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

  useEffect(() => {
    setSignatureDialogOpen(false);
    resetSignatureFlow();
  }, [selectedContract?.id, selectedContract?.status]);

  const handleFilterChange = (event) => {
    const { name, value } = event.target;
    setFilters((current) => ({ ...current, [name]: value }));
  };

  const handleSelectContract = (contractId) => {
    setSelectedContractId(contractId);
    window.localStorage.setItem('loanflow.lastContractId', String(contractId));
    syncSearchParams(filters, contractId);
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

  const handleOpenSignatureDialog = () => {
    setError('');
    setFeedback('');
    setSignatureDialogOpen(true);
  };

  const handleCloseSignatureDialog = () => {
    if (signatureBusy) {
      return;
    }
    setSignatureDialogOpen(false);
    resetSignatureFlow();
  };

  const handleStartSignatureChallenge = async (method) => {
    if (!selectedContract?.id) {
      return;
    }

    setError('');
    setFeedback('');
    setSignatureBusy(true);

    try {
      const challenge = await api.startContractSignatureChallenge(token, selectedContract.id, {
        metodo: method
      });
      setSignatureChallenge(challenge);
      setFeedback(challenge.mensagem || 'Desafio iniciado para a assinatura.');
    } catch (challengeError) {
      setError(challengeError.message);
    } finally {
      setSignatureBusy(false);
    }
  };

  const handleConfirmSignature = async (codigo) => {
    if (!selectedContract?.id || !signatureChallenge?.desafioId) {
      return;
    }

    setError('');
    setFeedback('');
    setSignatureBusy(true);

    try {
      await api.signContract(token, selectedContract.id, {
        aceite: true,
        desafioId: signatureChallenge.desafioId,
        codigo
      });
      setFeedback('Assinatura registrada.');
      setSignatureDialogOpen(false);
      resetSignatureFlow();
      await loadContracts(filters, selectedContract?.id ?? selectedContractId);
    } catch (signError) {
      setError(signError.message);
    } finally {
      setSignatureBusy(false);
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
      <header className="account-page-header contract-page-header">
        <h1>Contratos</h1>
        <p>Busque um contrato, confira o status e resolva a proxima acao sem trocar de contexto.</p>
      </header>

      <div className="contracts-feedback-stack">
        <MessageBanner type="error">{error}</MessageBanner>
        <MessageBanner type="success">{feedback}</MessageBanner>
        <BankAccountNotice
          show={canSign && !hasBankAccount}
          message="Cadastre uma conta bancaria no painel antes de assinar contratos."
        />
      </div>

      <SectionCard
        title="Filtros"
        subtitle="Refine a carteira por numero, status ou finalidade."
        actions={<span className="contract-utility-badge">{activeFilterLabel}</span>}
      >
        <div className="contract-filter-stack">
          <label>
            Numero do contrato
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
        <div className="form-actions contract-filter-actions">
          <button type="button" className="primary-button" onClick={() => loadContracts(filters, selectedContractId)}>
            <UiIcon name="stack" size={16} />
            {loading ? 'Carregando...' : 'Atualizar lista'}
          </button>
          <button type="button" className="secondary-button" onClick={handleResetFilters}>
            Limpar filtros
          </button>
        </div>
        <p className="helper-text contract-filter-note">
          {loading ? 'Atualizando a lista de contratos agora.' : 'A mesma filtragem alimenta a lista e o detalhe abaixo.'}
        </p>
      </SectionCard>

      <SectionCard
        title="Contratos"
        subtitle="Selecione um item para abrir os detalhes abaixo."
        actions={<span className="contract-utility-badge">{contracts.length} item(ns)</span>}
      >
        {loading && !contracts.length ? (
          <p className="helper-text">Carregando contratos...</p>
        ) : contracts.length ? (
          <div className="list-stack contract-list-stack">
            {contracts.map((contract) => (
              <article
                key={contract.id}
                className={`list-card contract-list-card${
                  selectedContractId === contract.id ? ' list-card-highlight contract-list-card-active' : ''
                }`}
              >
                <div className="contract-list-card-main">
                  <div className="contract-list-card-topline">
                    <strong>{contract.numeroContrato}</strong>
                    <StatusBadge value={contract.status} />
                  </div>
                  <p className="contract-list-card-purpose">
                    {contract.finalidade || 'Sem finalidade informada para este contrato.'}
                  </p>
                  <div className="contract-list-card-meta">
                    <span>
                      <UiIcon name="file" size={15} />
                      Proposta #{contract.propostaId}
                    </span>
                    <span>
                      <UiIcon name="clock" size={15} />
                      {contract.dataFormalizacao
                        ? `Formalizado em ${formatDateTime(contract.dataFormalizacao)}`
                        : `Gerado em ${formatDateTime(contract.dataGeracao)}`}
                    </span>
                  </div>
                </div>

                <div className="stack-actions contract-list-card-actions">
                  <button
                    type="button"
                    className={selectedContractId === contract.id ? 'primary-button' : 'secondary-button'}
                    onClick={() => handleSelectContract(contract.id)}
                  >
                    {selectedContractId === contract.id ? 'Selecionado' : 'Abrir'}
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
        <div className="contract-detail-stack">
          <section className={`contract-summary-board contract-tone-${selectedContractTone}`}>
            <div className="contract-summary-board-header">
              <div className="contract-summary-board-copy">
                <span className="contract-detail-label">Contrato selecionado</span>
                <h2>{selectedContract.numeroContrato}</h2>
                <p>{selectedContractPurpose}</p>
              </div>
              <StatusBadge value={selectedContract.status} />
            </div>

            <div className="contract-fact-grid">
              {selectedContractFacts.map((item) => (
                <article key={item.label} className="contract-fact-card">
                  <span className="contract-fact-icon">
                    <UiIcon name={item.icon} size={16} />
                  </span>
                  <div className="contract-fact-copy">
                    <span>{item.label}</span>
                    <strong>{item.value}</strong>
                  </div>
                </article>
              ))}
            </div>

            <p className="contract-spotlight-summary">{selectedContractSummary}</p>

            <div className="contract-command-actions">
              <button type="button" className="secondary-button" onClick={handleDownload}>
                <UiIcon name="file" size={16} />
                Baixar PDF
              </button>

              <Link className="secondary-button" to={`/parcelas?numeroContrato=${encodeURIComponent(selectedContract.numeroContrato)}`}>
                <UiIcon name="stack" size={16} />
                Ver parcelas
              </Link>

              {canCancelSelected ? (
                <button
                  type="button"
                  className="danger-button"
                  onClick={() => runAction(() => api.cancelContract(token, selectedContract.id), 'Contrato cancelado.')}
                >
                  <UiIcon name="ban" size={16} />
                  Cancelar contrato
                </button>
              ) : null}
            </div>
          </section>

          <SectionCard title="Resumo" subtitle="Dados principais e contexto do contrato em foco.">
            <dl className="detail-grid contract-detail-grid">
              <div>
                <dt>Status atual</dt>
                <dd>{formatLabel(selectedContract.status)}</dd>
              </div>
              <div>
                <dt>Proxima acao</dt>
                <dd>{nextStepLabel}</dd>
              </div>
              <div>
                <dt>{selectedContract.dataFormalizacao ? 'Formalizado em' : 'Prazo do aceite'}</dt>
                <dd>{selectedContract.dataFormalizacao ? formatDateTime(selectedContract.dataFormalizacao) : selectedContractExpiryLabel}</dd>
              </div>
              <div>
                <dt>Arquivo emitido</dt>
                <dd>{getPdfFileName(selectedContract.pdfPath)}</dd>
              </div>
              <div className="detail-span-2">
                <dt>Finalidade</dt>
                <dd>{selectedContractPurpose}</dd>
              </div>
              <div className="detail-span-2">
                <dt>Seu perfil agora</dt>
                <dd>{permissionSummary}</dd>
              </div>
            </dl>
          </SectionCard>

          <SectionCard title="Assinaturas" subtitle="Acompanhe o aceite eletronico e o proximo passo deste contrato.">
            {canSignSelected ? (
              <div className="contract-signature-shell">
                <div className="contract-signature-head">
                  <div>
                    <span className="contract-detail-label">Fluxo protegido de assinatura</span>
                    <strong>Abra o desafio guiado antes do aceite final</strong>
                  </div>
                  {signatureChallenge ? <span className="contract-signature-chip">Desafio ativo ate {challengeExpiryLabel}</span> : null}
                </div>

                <p className="helper-text">
                  O aceite acontece em duas etapas: iniciar o desafio e confirmar a assinatura com senha atual ou codigo
                  temporario, sempre dentro do prazo do contrato.
                </p>

                <div className="contract-signature-actions">
                  <button
                    type="button"
                    className="primary-button"
                    onClick={handleOpenSignatureDialog}
                    disabled={signatureBusy || !hasBankAccount}
                  >
                    <UiIcon name="signature" size={16} />
                    {signatureChallenge ? 'Continuar desafio' : 'Abrir assinatura guiada'}
                  </button>
                </div>

                {signatureChallenge ? (
                  <p className="helper-text contract-signature-helper">
                    Desafio iniciado. Finalize o aceite ate {challengeExpiryLabel}.
                  </p>
                ) : null}
              </div>
            ) : (
              <div className="contract-signature-readonly">
                <article className="contract-document-card contract-document-card-soft">
                  <span className="contract-detail-label">Situacao da assinatura</span>
                  <strong>{nextStepLabel}</strong>
                  <p className="helper-text">{permissionSummary}</p>
                  {selectedContract.status === 'ASSINADO_PARCIALMENTE' ? (
                    <p className="helper-text">
                      Uma assinatura ja foi concluida. Falta o aceite restante para formalizar o contrato.
                    </p>
                  ) : null}
                </article>
              </div>
            )}
          </SectionCard>

          <SectionCard title="Documento" subtitle="Arquivo emitido e hash do contrato.">
            <div className="contract-document-shell">
              <article className="contract-document-card contract-document-card-feature">
                <div className="contract-document-head">
                  <div>
                    <span className="contract-detail-label">Arquivo emitido</span>
                    <strong>{getPdfFileName(selectedContract.pdfPath)}</strong>
                    <p className="helper-text">PDF autenticado disponivel para download e conferencia no fluxo atual.</p>
                  </div>
                  <div className="contract-document-actions">
                    <button type="button" className="secondary-button" onClick={handleDownload}>
                      <UiIcon name="file" size={16} />
                      Baixar PDF
                    </button>
                    <button
                      type="button"
                      className="secondary-button"
                      onClick={handleCopyHash}
                      disabled={!selectedContract.hashDocumento}
                    >
                      <UiIcon name="shield-check" size={16} />
                      Copiar hash
                    </button>
                  </div>
                </div>
                <div className="contract-document-hash">
                  <span className="contract-detail-label">Hash do documento</span>
                  <code className="code-box contract-code-box">
                    {selectedContract.hashDocumento || 'Hash indisponivel para este contrato.'}
                  </code>
                </div>
                <p className="helper-text">
                  Esse identificador preserva a referencia do conteudo gerado na emissao do contrato.
                </p>
              </article>
            </div>
          </SectionCard>

          <SectionCard
            title="Parcelas"
            subtitle={
              selectedContract.status === 'FORMALIZADO'
                ? 'Cronograma financeiro vinculado ao contrato formalizado.'
                : 'As parcelas aparecem com mais destaque quando o contrato entra na etapa financeira.'
            }
            actions={
              <span className="contract-utility-badge">
                {installmentsLoading ? 'Atualizando agenda' : `${installmentOverview.total} parcela(s)`}
              </span>
            }
          >
            <div className="contract-finance-shell">
              <article className="contract-document-card contract-document-card-soft">
                <span className="contract-detail-label">Situacao da carteira</span>
                <strong>{contractFinanceHeadline}</strong>
                <p>{contractFinanceDescription}</p>
              </article>

              <div className="contract-finance-metrics">
                <article className="contract-finance-metric">
                  <span>Total previsto</span>
                  <strong>{installmentsLoading ? '--' : formatCurrency(installmentOverview.totalValue)}</strong>
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
                <p className="helper-text">Carregando cronograma de parcelas...</p>
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
                  description="As parcelas aparecem aqui assim que o contrato avancar para a etapa financeira."
                />
              )}

              <div className="contract-panel-footer">
                <Link className="secondary-button" to={`/parcelas?numeroContrato=${encodeURIComponent(selectedContract.numeroContrato)}`}>
                  <UiIcon name="stack" size={16} />
                  Abrir cronograma completo
                </Link>
              </div>
            </div>
          </SectionCard>
        </div>
      ) : (
        <SectionCard title="Selecione um contrato" subtitle="Escolha um item da lista para abrir os detalhes abaixo.">
          <EmptyState
            title="Nenhum contrato em foco."
            description="Assim que voce selecionar um contrato, o resumo, o documento e as parcelas aparecem aqui."
          />
        </SectionCard>
      )}

      <ContractSignatureDialog
        open={signatureDialogOpen}
        contract={selectedContract}
        busy={signatureBusy}
        challenge={signatureChallenge}
        hasBankAccount={hasBankAccount}
        onClose={handleCloseSignatureDialog}
        onStartChallenge={handleStartSignatureChallenge}
        onConfirmSignature={handleConfirmSignature}
      />
    </div>
  );
}
