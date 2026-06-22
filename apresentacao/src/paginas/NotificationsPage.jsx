import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import EmptyState from '../componentes/EmptyState';
import MessageBanner from '../componentes/MessageBanner';
import SectionCard from '../componentes/SectionCard';
import UiIcon from '../componentes/UiIcon';
import { useAuth } from '../contexto/AuthContext';
import { api } from '../biblioteca/api';
import { formatDateTime } from '../biblioteca/format';

const filterDefaults = {
  lida: 'all',
  tipo: ''
};

const notificationsPerPage = 10;

function getNotificationTimestamp(notification) {
  return new Date(notification.dataEnvio).getTime() || 0;
}

export default function NotificationsPage() {
  const { token } = useAuth();
  const navigate = useNavigate();
  const [filters, setFilters] = useState(filterDefaults);
  const [notifications, setNotifications] = useState([]);
  const [notificationPage, setNotificationPage] = useState(1);
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
    setNotificationPage(1);
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

  const handleNotificationCardClick = (notification) => {
    if (!notification.lida) {
      handleMarkAsRead(notification.id);
    }
  };

  const handleNotificationCardKeyDown = (event, notification) => {
    if (notification.lida || (event.key !== 'Enter' && event.key !== ' ')) {
      return;
    }

    event.preventDefault();
    handleMarkAsRead(notification.id);
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

  const orderedNotifications = [...notifications].sort((current, next) => {
    const dateOrder = getNotificationTimestamp(next) - getNotificationTimestamp(current);

    if (dateOrder !== 0) {
      return dateOrder;
    }

    return (next.id ?? 0) - (current.id ?? 0);
  });
  const totalNotificationPages = Math.max(1, Math.ceil(orderedNotifications.length / notificationsPerPage));
  const activeNotificationPage = Math.min(notificationPage, totalNotificationPages);
  const notificationStartIndex = (activeNotificationPage - 1) * notificationsPerPage;
  const visibleNotifications = orderedNotifications.slice(
    notificationStartIndex,
    notificationStartIndex + notificationsPerPage
  );

  return (
    <div className="page-stack">
      <SectionCard title="Central de notificações">
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

        <MessageBanner type="error">{error}</MessageBanner>
        <MessageBanner type="success">{feedback}</MessageBanner>

        {loading ? (
          <p className="helper-text">Carregando notificações...</p>
        ) : notifications.length ? (
          <div className="creditor-order-results">
            <div className="list-stack">
              {visibleNotifications.map((notification) => (
                <article
                  key={notification.id}
                  className={`list-card notification-card ${notification.lida ? 'is-read' : 'is-unread'}`}
                  role={!notification.lida ? 'button' : undefined}
                  tabIndex={!notification.lida ? 0 : undefined}
                  aria-label={!notification.lida ? `Marcar notificacao ${notification.id} como lida` : undefined}
                  onClick={() => handleNotificationCardClick(notification)}
                  onKeyDown={(event) => handleNotificationCardKeyDown(event, notification)}
                >
                  <div>
                    <strong>{notification.mensagem}</strong>
                    <p>
                      Referência: {notification.referenciaTipo ?? 'Sem referência'}
                      {notification.referenciaId ? ` #${notification.referenciaId}` : ''}
                    </p>
                    <small>{formatDateTime(notification.dataEnvio)}</small>
                  </div>
                  <div className="stack-actions">
                    <span className={`notification-read-note ${notification.lida ? 'is-read' : 'is-unread'}`}>
                      {notification.lida ? 'Lida' : 'Não lida'}
                    </span>
                    {notification.referenciaId ? (
                      <div className="inline-button-group">
                        <button
                          type="button"
                          className="primary-button"
                          onClick={(event) => {
                            event.stopPropagation();
                            openReference(notification);
                          }}
                        >
                          Abrir referência
                        </button>
                      </div>
                    ) : null}
                  </div>
                </article>
              ))}
            </div>

            <div className="creditor-order-pagination">
              <div className="creditor-order-pagination-copy">
                <strong>
                  Página {activeNotificationPage} de {totalNotificationPages}
                </strong>
                <span>
                  Mostrando {Math.min(notificationStartIndex + 1, orderedNotifications.length)} a{' '}
                  {Math.min(notificationStartIndex + notificationsPerPage, orderedNotifications.length)} de{' '}
                  {orderedNotifications.length} notificações
                </span>
              </div>
              <div className="creditor-order-pagination-actions">
                <button
                  type="button"
                  className="secondary-button"
                  onClick={() => setNotificationPage((currentPage) => Math.max(1, currentPage - 1))}
                  disabled={activeNotificationPage === 1}
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
                    setNotificationPage((currentPage) => Math.min(totalNotificationPages, currentPage + 1))
                  }
                  disabled={activeNotificationPage === totalNotificationPages}
                >
                  Próxima
                  <UiIcon name="arrow-right" size={16} />
                </button>
              </div>
            </div>
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
