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
const purposeCategoryOptions = [
  { value: 'CAPITAL_DE_GIRO', label: 'Capital de giro' },
  { value: 'REFORMA', label: 'Reforma' },
  { value: 'QUITACAO_DE_DIVIDAS', label: 'Quitacao de dividas' },
  { value: 'EMERGENCIA', label: 'Emergencia' },
  { value: 'ESTUDO', label: 'Estudo' },
  { value: 'SAUDE', label: 'Saude' },
  { value: 'OUTRA', label: 'Outra' }
];

const signatureStatuses = ['AGUARDANDO_ASSINATURAS', 'ASSINADO_PARCIALMENTE'];
const installmentOpenStatuses = ['ABERTA', 'PARCIALMENTE_PAGA', 'EM_ATRASO'];
const contractsPerPage = 6;
const emptyFilters = {
  numeroContrato: '',
  status: '',
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

function getContractListMeta(contract) {
  const lifecycleLabel = contract.dataFormalizacao
    ? `Formalizado em ${formatDateTime(contract.dataFormalizacao)}`
    : contract.dataExpiracaoAssinatura
      ? `Aceite ate ${formatDateTime(contract.dataExpiracaoAssinatura)}`
      : `Gerado em ${formatDateTime(contract.dataGeracao)}`;

  return `Proposta #${contract.propostaId} | ${formatCurrency(contract.valorTotalComJuros)} | ${lifecycleLabel}`;
}

function ContractDetailsDialog({
  open,
  contract,
  tone,
  nextStepLabel,
  permissionSummary,
  purpose,
  expiryLabel,
  documentName,
  canSignSelected,
  canCancelSelected,
  signatureChallenge,
  challengeExpiryLabel,
  signatureBusy,
  hasBankAccount,
  financeHeadline,
  financeDescription,
  installmentOverview,
  installmentsLoading,
  installmentsError,
  contractTotalWithInterest,
  onClose,
  onDownload,
  onCopyHash,
  onOpenSignatureDialog,
  onCancelContract
}) {
  if (!open || !contract) {
    return null;
  }

  return (
    <div
      className="contract-details-dialog-backdrop"
      role="presentation"
      onClick={(event) => {
        if (event.target === event.currentTarget && !signatureBusy) {
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
            <h2 id="contract-details-dialog-title">Detalhes operacionais do contrato</h2>
            <p>Revise o documento, acompanhe o cronograma e execute a proxima acao sem sair da carteira.</p>
          </div>

          <button
            type="button"
            className="contract-details-dialog-close"
            onClick={onClose}
            disabled={signatureBusy}
          >
            <UiIcon name="x" size={18} />
            Fechar
          </button>
        </header>

        <div className="contract-details-dialog-body">
          <section className={`contract-selection-panel contract-tone-${tone}`}>
            <div className="contract-selection-header">
              <div className="contract-selection-copy">
                <span className="contract-detail-label">Visao geral do contrato</span>
                <h2>{contract.numeroContrato}</h2>
                <p>{purpose}</p>
              </div>
              <div className="contract-selection-status">
                <StatusBadge value={contract.status} />
                <span className="contract-utility-badge">Proposta #{contract.propostaId}</span>
              </div>
            </div>

            <dl className="detail-grid contract-selection-grid">
              <div>
                <dt>Status do fluxo</dt>
                <dd>{formatLabel(contract.status)}</dd>
              </div>
              <div>
                <dt>Gerado em</dt>
                <dd>{formatDateTime(contract.dataGeracao)}</dd>
              </div>
              <div>
                <dt>{contract.dataFormalizacao ? 'Formalizado em' : 'Prazo do aceite'}</dt>
                <dd>
                  {contract.dataFormalizacao
                    ? formatDateTime(contract.dataFormalizacao)
                    : expiryLabel}
                </dd>
              </div>
              <div className="detail-span-2">
                <dt>Proxima acao esperada</dt>
                <dd>{nextStepLabel}</dd>
              </div>
            </dl>

            <div className="contract-command-actions">
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
          </section>

          <div className="contract-detail-panels">
            <article className="contract-document-card contract-document-card-soft">
              <span className="contract-detail-label">Assinaturas</span>
              {canSignSelected ? (
                <div className="contract-signature-shell">
                  <div className="contract-signature-head">
                    <div>
                      <strong>Abra o desafio guiado antes do aceite final</strong>
                      <p className="helper-text">
                        O aceite acontece em duas etapas: iniciar o desafio e confirmar a assinatura com senha atual
                        ou codigo temporario, sempre dentro do prazo do contrato.
                      </p>
                    </div>
                    {signatureChallenge ? (
                      <span className="contract-signature-chip">Desafio ativo ate {challengeExpiryLabel}</span>
                    ) : null}
                  </div>

                  <div className="contract-signature-actions">
                    <button
                      type="button"
                      className="primary-button"
                      onClick={onOpenSignatureDialog}
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
                  <strong>Assinatura sem acao pendente para este perfil</strong>
                  <p className="helper-text">{permissionSummary}</p>
                  {contract.status === 'ASSINADO_PARCIALMENTE' ? (
                    <p className="helper-text">
                      Uma assinatura ja foi concluida. Falta o aceite restante para formalizar o contrato.
                    </p>
                  ) : null}
                </div>
              )}
            </article>

            <article className="contract-document-card contract-document-card-soft">
              <span className="contract-detail-label">Documento</span>
              <strong>{documentName}</strong>
              <p className="helper-text">
                PDF autenticado disponivel para download e conferencia durante o acompanhamento do contrato.
              </p>
              <code className="code-box contract-inline-code">
                {contract.hashDocumento || 'Hash indisponivel para este contrato.'}
              </code>
            </article>

            <section className="contract-document-card contract-document-card-soft contract-finance-card">
              <div className="contract-finance-card-head">
                <div>
                  <span className="contract-detail-label">Parcelas</span>
                  <strong>{financeHeadline}</strong>
                  <p>{financeDescription}</p>
                </div>
                <span className="contract-utility-badge">
                  {installmentsLoading ? 'Atualizando agenda' : `${installmentOverview.total} parcela(s)`}
                </span>
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
        </div>
      </section>
    </div>
  );
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
  const [signatureDialogOpen, setSignatureDialogOpen] = useState(false);
  const [signatureChallenge, setSignatureChallenge] = useState(null);
  const [signatureBusy, setSignatureBusy] = useState(false);

  const canSign = hasRole('SOLICITANTE') || hasRole('CREDOR');
  const canCancel = hasRole('CREDOR') || hasRole('ADMIN');

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
  const canSignSelected = Boolean(selectedContract && canSign && signatureStatuses.includes(selectedContract.status));
  const canCancelSelected = Boolean(
    selectedContract && canCancel && !['FORMALIZADO', 'CANCELADO'].includes(selectedContract.status)
  );
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
  const selectedContractDocumentName = selectedContract?.pdfPath || 'PDF nao disponivel';
  const challengeExpiryLabel = signatureChallenge?.expiraEm ? formatDateTime(signatureChallenge.expiraEm) : null;
  const contractFinanceHeadline = installmentsLoading
    ? 'Atualizando cronograma'
    : installmentOverview.nextInstallment
      ? `Parcela ${installmentOverview.nextInstallment.numero}`
      : installmentOverview.total
        ? `${installmentOverview.total} parcela(s)`
        : 'Sem agenda financeira';
  const contractTotalWithInterest =
    installmentOverview.totalValue > 0
      ? installmentOverview.totalValue
      : selectedContract?.valorTotalComJuros;
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
        status: currentFilters.status || undefined,
        categoriaFinalidade: currentFilters.categoriaFinalidade || undefined
      });
      setContracts(data);

      const nextSelectedId = data.find((contract) => contract.id === preferredContractId)?.id ?? null;
      setSelectedContractId(nextSelectedId);
      setContractPage(getPageForContract(data, nextSelectedId));
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
    if (signatureBusy) {
      return;
    }
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
      <div className="contracts-feedback-stack">
        <MessageBanner type="error">{error}</MessageBanner>
        <MessageBanner type="success">{feedback}</MessageBanner>
        <BankAccountNotice
          show={canSign && !hasBankAccount}
          message="Cadastre uma conta bancaria no painel antes de assinar contratos."
        />
      </div>

      <SectionCard
        className="creditor-order-section contract-catalog-section"
        title="Contratos em carteira"
        subtitle="Selecione um contrato para revisar documento, assinatura e acompanhamento financeiro."
      >
        <div className="filters-row">
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

                return (
                  <article
                    key={contract.id}
                    className={`list-card creditor-order-card${isContractSelected ? ' list-card-highlight' : ''}`}
                  >
                    <div>
                      <strong>{contract.numeroContrato}</strong>
                      <p>{contract.finalidade || 'Sem finalidade informada para este contrato.'}</p>
                      <small>{getContractListMeta(contract)}</small>
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
        nextStepLabel={nextStepLabel}
        permissionSummary={permissionSummary}
        purpose={selectedContractPurpose}
        expiryLabel={selectedContractExpiryLabel}
        documentName={selectedContractDocumentName}
        canSignSelected={canSignSelected}
        canCancelSelected={canCancelSelected}
        signatureChallenge={signatureChallenge}
        challengeExpiryLabel={challengeExpiryLabel}
        signatureBusy={signatureBusy}
        hasBankAccount={hasBankAccount}
        financeHeadline={contractFinanceHeadline}
        financeDescription={contractFinanceDescription}
        installmentOverview={installmentOverview}
        installmentsLoading={installmentsLoading}
        installmentsError={installmentsError}
        contractTotalWithInterest={contractTotalWithInterest}
        onClose={handleCloseDetailsDialog}
        onDownload={handleDownload}
        onCopyHash={handleCopyHash}
        onOpenSignatureDialog={handleOpenSignatureDialog}
        onCancelContract={() => runAction(() => api.cancelContract(token, selectedContract.id), 'Contrato cancelado.')}
      />

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
