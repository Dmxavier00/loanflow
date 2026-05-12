import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import EmptyState from '../componentes/EmptyState';
import MessageBanner from '../componentes/MessageBanner';
import SectionCard from '../componentes/SectionCard';
import StatusBadge from '../componentes/StatusBadge';
import { useAuth } from '../contexto/AuthContext';
import { api } from '../biblioteca/api';
import { formatDateTime } from '../biblioteca/format';

const filterDefaults = {
  lida: 'all',
  tipo: ''
};

export default function NotificationsPage() {
  const { token } = useAuth();
  const navigate = useNavigate();
  const [filters, setFilters] = useState(filterDefaults);
  const [notifications, setNotifications] = useState([]);
  const [error, setError] = useState('');
  const [feedback, setFeedback] = useState('');
  const [loading, setLoading] = useState(true);

  const loadNotifications = async (currentFilters = filters) => {
    setLoading(true);
    setError('');

    try {
      const params = {
        lida:
          currentFilters.lida === 'all'
            ? undefined
            : currentFilters.lida === 'true'
              ? true
              : false,
        tipo: currentFilters.tipo || undefined
      };
      const data = await api.getNotifications(token, params);
      setNotifications(data);
    } catch (loadError) {
      setError(loadError.message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadNotifications();
  }, [token]);

  const handleFilterChange = (event) => {
    const { name, value } = event.target;
    const nextFilters = { ...filters, [name]: value };
    setFilters(nextFilters);
    loadNotifications(nextFilters);
  };

  const handleMarkAsRead = async (notificationId) => {
    setError('');
    setFeedback('');

    try {
      await api.markNotificationAsRead(token, notificationId);
      setFeedback('Notificação marcada como lida.');
      await loadNotifications();
    } catch (actionError) {
      setError(actionError.message);
    }
  };

  const openReference = (notification) => {
    if (!notification.referenciaTipo || !notification.referenciaId) {
      return;
    }

    if (notification.referenciaTipo === 'Contrato') {
      navigate(`/contratos?contratoId=${notification.referenciaId}`);
      return;
    }

    if (notification.referenciaTipo === 'Proposta') {
      navigate(`/solicitacoes?focusId=${notification.referenciaId}`);
      return;
    }

    if (notification.referenciaTipo === 'Parcela') {
      navigate(`/parcelas?parcelaId=${notification.referenciaId}`);
    }
  };

  return (
    <div className="page-stack">
      <SectionCard title="Filtros de notificação" subtitle="Refine por leitura e tipo.">
        <div className="filters-row">
          <label>
            Leitura
            <select name="lida" value={filters.lida} onChange={handleFilterChange}>
              <option value="all">Todas</option>
              <option value="false">Não lidas</option>
              <option value="true">Lidas</option>
            </select>
          </label>

          <label>
            Tipo
            <select name="tipo" value={filters.tipo} onChange={handleFilterChange}>
              <option value="">Todos</option>
              <option value="PROPOSTA">Proposta</option>
              <option value="CONTRATO">Contrato</option>
              <option value="PARCELA">Parcela</option>
              <option value="PAGAMENTO">Pagamento</option>
              <option value="SISTEMA">Sistema</option>
            </select>
          </label>
        </div>
      </SectionCard>

      <MessageBanner type="error">{error}</MessageBanner>
      <MessageBanner type="success">{feedback}</MessageBanner>

      <SectionCard title="Central de notificações" subtitle="Eventos do fluxo transacional do backend.">
        {loading ? (
          <p className="helper-text">Carregando notificações...</p>
        ) : notifications.length ? (
          <div className="list-stack">
            {notifications.map((notification) => (
              <article key={notification.id} className="list-card">
                <div>
                  <strong>{notification.mensagem}</strong>
                  <p>
                    Referência: {notification.referenciaTipo ?? 'Sem referência'}
                    {notification.referenciaId ? ` #${notification.referenciaId}` : ''}
                  </p>
                  <small>{formatDateTime(notification.dataEnvio)}</small>
                </div>
                <div className="stack-actions">
                  <StatusBadge value={notification.tipo} />
                  <div className="inline-button-group">
                    {!notification.lida ? (
                      <button
                        type="button"
                        className="secondary-button"
                        onClick={() => handleMarkAsRead(notification.id)}
                      >
                        Marcar como lida
                      </button>
                    ) : null}
                    {notification.referenciaId ? (
                      <button type="button" className="primary-button" onClick={() => openReference(notification)}>
                        Abrir referência
                      </button>
                    ) : null}
                  </div>
                </div>
              </article>
            ))}
          </div>
        ) : (
          <EmptyState
            title="Nenhuma notificação encontrada."
            description="Altere os filtros ou avance nos fluxos para gerar novos eventos."
          />
        )}
      </SectionCard>
    </div>
  );
}
