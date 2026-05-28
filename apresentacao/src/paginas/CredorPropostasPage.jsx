import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import BankAccountNotice from '../componentes/BankAccountNotice';
import EmptyState from '../componentes/EmptyState';
import MessageBanner from '../componentes/MessageBanner';
import SectionCard from '../componentes/SectionCard';
import StatusBadge from '../componentes/StatusBadge';
import { useAuth } from '../contexto/AuthContext';
import { api } from '../biblioteca/api';
import {
  formatCurrency,
  formatDate,
  formatLabel,
  formatProposalHeadline,
  formatProposalNumber
} from '../biblioteca/format';

function getCreditorWorkflowHint(status) {
  switch (status) {
    case 'ACEITA':
      return 'Fluxo legado: a proposta ainda aguarda a etapa seguinte.';
    case 'EM_ANALISE':
      return 'Fluxo legado: aprove ou rejeite a proposta para seguir.';
    case 'APROVADA':
      return 'Fluxo legado: formalize o contrato automaticamente.';
    case 'REJEITADA':
      return 'Fluxo encerrado: proposta rejeitada pelo credor.';
    case 'CONTRATADA':
      return 'Fluxo concluído: aceite registrado e contrato formalizado automaticamente.';
    case 'QUITADA':
      return 'Fluxo encerrado: contrato quitado por pagamento integral.';
    default:
      return '';
  }
}

function findProposalById(proposals, proposalId) {
  return proposals.find((proposal) => proposal.id === proposalId) ?? null;
}

const simulationDisclaimer =
  'Simulação acadêmica: os valores, percentuais e próximas etapas desta tela demonstram apenas o fluxo do protótipo, sem liquidação financeira real nem validação regulatória da operação.';

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
      `Proposta ${formatProposalNumber(selectedProposal)} aceita e contratada com sucesso.`
    );
  };

  const handleStartAnalysis = async (proposalId) => {
    await runAction(
      () => api.startProposalAnalysis(token, proposalId),
      `Análise iniciada para a proposta ${formatProposalNumber(findProposalById(acceptedProposals, proposalId) ?? { id: proposalId })}.`
    );
  };

  const handleApproveProposal = async (proposalId) => {
    await runAction(
      () => api.approveProposal(token, proposalId),
      `Proposta ${formatProposalNumber(findProposalById(acceptedProposals, proposalId) ?? { id: proposalId })} aprovada com sucesso.`
    );
  };

  const handleRejectProposal = async (proposalId) => {
    await runAction(
      () => api.rejectProposal(token, proposalId),
      `Proposta ${formatProposalNumber(findProposalById(acceptedProposals, proposalId) ?? { id: proposalId })} rejeitada com sucesso.`
    );
  };

  const handleGenerateContract = async (proposalId) => {
    await runAction(async () => {
      const contract = await api.generateContract(token, proposalId);
      window.localStorage.setItem('loanflow.lastContractId', String(contract.id));
      navigate(`/contratos?numeroContrato=${encodeURIComponent(contract.numeroContrato)}&contratoId=${contract.id}`);
    }, 'Contrato formalizado com sucesso.');
  };

  return (
    <div className="page-stack">
      <MessageBanner type="error">{error}</MessageBanner>
      <MessageBanner type="success">{feedback}</MessageBanner>
      <BankAccountNotice
        show={!hasBankAccount}
        message="Cadastre uma conta bancária no painel antes de aceitar propostas e formalizar contratos."
      />

      <SectionCard
        title="Propostas aguardando aceite"
        subtitle="Selecione uma proposta aberta para vinculá-la ao seu perfil e formalizar o contrato automaticamente."
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
                    Proposta {formatProposalNumber(proposal)} | {formatLabel(proposal.categoriaFinalidade)} |{' '}
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
        subtitle="Confira os dados da simulação antes de aceitar a proposta e formalizar o contrato."
        actions={
          <button
            type="button"
            className="primary-button"
            onClick={handleAcceptSelected}
            disabled={!selectedProposal || !hasBankAccount}
          >
            Aceitar e formalizar
          </button>
        }
      >
        <p className="helper-text">{simulationDisclaimer}</p>
        {selectedProposal ? (
          <dl className="detail-grid">
            <div>
              <dt>Solicitante</dt>
              <dd>{formatProposalHeadline(selectedProposal)}</dd>
            </div>
            <div>
              <dt>Código</dt>
              <dd>{formatProposalNumber(selectedProposal)}</dd>
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
              <dt>Taxa simulada</dt>
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
        subtitle="Propostas simuladas assumidas por você e o estágio operacional de cada uma."
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
                    Proposta {formatProposalNumber(proposal)} | {formatLabel(proposal.categoriaFinalidade)} |{' '}
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
                        Formalizar contrato
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
            description="As propostas assumidas por você passam a aparecer aqui com o estágio atual do contrato."
          />
        )}
      </SectionCard>
    </div>
  );
}
