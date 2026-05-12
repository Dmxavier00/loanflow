import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import BankAccountNotice from '../components/BankAccountNotice';
import EmptyState from '../components/EmptyState';
import MessageBanner from '../components/MessageBanner';
import SectionCard from '../components/SectionCard';
import StatusBadge from '../components/StatusBadge';
import { useAuth } from '../context/AuthContext';
import { api } from '../lib/api';
import { formatCurrency, formatDate, formatLabel, formatProposalHeadline } from '../lib/format';

function getCreditorWorkflowHint(status) {
  switch (status) {
    case 'ACEITA':
      return 'Próximo passo: iniciar a análise da proposta.';
    case 'EM_ANALISE':
      return 'Próximo passo: aprovar ou rejeitar a proposta.';
    case 'APROVADA':
      return 'Próximo passo: gerar o contrato.';
    case 'REJEITADA':
      return 'Fluxo encerrado: proposta rejeitada pelo credor.';
    case 'CONTRATADA':
      return 'Fluxo concluído: contrato gerado e formalizado.';
    default:
      return '';
  }
}

export default function CredorPropostasPage() {
  const { token, hasBankAccount } = useAuth();
  const navigate = useNavigate();
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [feedback, setFeedback] = useState('');
  const [pendingProposals, setPendingProposals] = useState([]);
  const [acceptedProposals, setAcceptedProposals] = useState([]);
  const [selectedProposalId, setSelectedProposalId] = useState(null);

  const selectedProposal = useMemo(
    () => pendingProposals.find((proposal) => proposal.id === selectedProposalId) ?? null,
    [pendingProposals, selectedProposalId]
  );

  const loadData = async () => {
    setLoading(true);
    setError('');

    try {
      const [pending, accepted] = await Promise.all([
        api.getPendingAcceptanceProposals(token),
        api.getAcceptedProposals(token)
      ]);
      setPendingProposals(pending ?? []);
      setAcceptedProposals(accepted ?? []);
      setSelectedProposalId((currentId) =>
        (pending ?? []).some((proposal) => proposal.id === currentId) ? currentId : null
      );
    } catch (loadError) {
      setError(loadError.message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, [token]);

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

  const handleStartAnalysis = async (proposalId) => {
    await runAction(
      () => api.startProposalAnalysis(token, proposalId),
      `Analise iniciada para a proposta #${proposalId}.`
    );
  };

  const handleApproveProposal = async (proposalId) => {
    await runAction(
      () => api.approveProposal(token, proposalId),
      `Proposta #${proposalId} aprovada com sucesso.`
    );
  };

  const handleRejectProposal = async (proposalId) => {
    await runAction(
      () => api.rejectProposal(token, proposalId),
      `Proposta #${proposalId} rejeitada com sucesso.`
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
        show={!hasBankAccount}
        message="Cadastre uma conta bancária no painel antes de aceitar propostas, iniciar análises, aprovar operações ou gerar contratos."
      />

      <SectionCard
        title="Propostas aguardando aceite"
        subtitle="Selecione uma proposta aberta para vinculá-la ao seu perfil e mover o fluxo para análise."
      >
        {loading ? (
          <p className="helper-text">Carregando propostas aguardando aceite...</p>
        ) : pendingProposals.length ? (
          <div className="list-stack">
            {pendingProposals.map((proposal) => (
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
            title="Nenhuma proposta aguardando aceite."
            description="Quando um solicitante criar uma proposta, ela aparecerá aqui para os credores."
          />
        )}
      </SectionCard>

      <SectionCard
        title="Proposta selecionada"
        subtitle="Confira os dados antes de aceitar a proposta e assumir a análise."
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
              <dt>Código</dt>
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
              <dt>Expiração</dt>
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
              <dt>Descrição detalhada</dt>
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

      <SectionCard
        title="Carteira do credor"
        subtitle="Propostas assumidas por você e o próximo passo operacional de cada uma."
      >
        {loading ? (
          <p className="helper-text">Carregando carteira do credor...</p>
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
                  {getCreditorWorkflowHint(proposal.status) ? (
                    <p className="helper-text">{getCreditorWorkflowHint(proposal.status)}</p>
                  ) : null}
                </div>
                <div className="stack-actions">
                  <StatusBadge value={proposal.status} />
                  <div className="inline-button-group">
                    {proposal.status === 'ACEITA' ? (
                      <button
                        type="button"
                        className="secondary-button"
                        onClick={() => handleStartAnalysis(proposal.id)}
                        disabled={!hasBankAccount}
                      >
                        Iniciar análise
                      </button>
                    ) : null}
                    {proposal.status === 'EM_ANALISE' ? (
                      <>
                        <button
                          type="button"
                          className="primary-button"
                          onClick={() => handleApproveProposal(proposal.id)}
                          disabled={!hasBankAccount}
                        >
                          Aprovar
                        </button>
                        <button
                          type="button"
                          className="danger-button"
                          onClick={() => handleRejectProposal(proposal.id)}
                        >
                          Rejeitar
                        </button>
                      </>
                    ) : null}
                    {proposal.status === 'APROVADA' ? (
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
                </div>
              </article>
            ))}
          </div>
        ) : (
          <EmptyState
            title="Sua carteira de propostas está vazia."
            description="As propostas aceitas por você passam a aparecer aqui para análise, decisão e contrato."
          />
        )}
      </SectionCard>
    </div>
  );
}
