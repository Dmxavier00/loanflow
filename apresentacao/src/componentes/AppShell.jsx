import { Link, NavLink, Outlet, useLocation } from 'react-router-dom';
import UiIcon from './UiIcon';
import { useAuth } from '../contexto/AuthContext';
import { getAuthenticatedSecondaryAction } from '../biblioteca/bankAccountPolicy';
import { roleSummary } from '../biblioteca/format';
import brandFull from '../recursos/loanflow-full.png';

export default function AppShell() {
  const { user, logout, hasRole } = useAuth();
  const location = useLocation();
  const isSolicitante = hasRole('SOLICITANTE');
  const isCredor = hasRole('CREDOR');
  const isAccountPage = location.pathname === '/minha-conta';

  const navigation = [
    {
      to: '/dashboard',
      label: 'Dashboard',
      caption: 'Visão geral da operação',
      icon: 'home'
    },
    {
      to: '/minha-conta',
      label: 'Minha conta',
      caption: 'Perfil e dados bancários',
      icon: 'id-card'
    },
    {
      to: '/solicitacoes',
      label: 'Solicitações',
      caption: isCredor ? 'Propostas e ordens do credor' : 'Propostas e rascunhos',
      icon: 'file'
    },
    {
      to: '/contratos',
      label: 'Contratos',
      caption: 'Formalização e download',
      icon: 'file-check'
    },
    {
      to: '/parcelas',
      label: 'Parcelas',
      caption: 'Vencimentos e pagamentos',
      icon: 'wallet'
    },
    {
      to: '/alertas',
      label: 'Alertas',
      caption: 'Eventos e notificações',
      icon: 'bell'
    }
  ];

  const topNavigation = [
    { to: '/solicitacoes', label: 'Mercado' },
    { to: '/contratos', label: 'Contratos' },
    { to: '/parcelas', label: 'Fluxo' },
    { to: '/alertas', label: 'Alertas' }
  ];

  const initials = (user?.nome ?? 'Loan Flow')
    .split(' ')
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0])
    .join('')
    .toUpperCase();
  const topbarAction = getAuthenticatedSecondaryAction(user);

  const shellClasses = [
    'app-shell',
    isSolicitante ? 'solicitante-shell-view' : '',
    isAccountPage ? 'account-shell-view' : ''
  ]
    .filter(Boolean)
    .join(' ');

  return (
    <div className={shellClasses}>
      <header className="app-topbar">
        <div className="topbar-brand">
          <Link to="/dashboard" className="topbar-brand-link" aria-label="LoanFlow">
            <img className="brand-logo-image" src={brandFull} alt="LoanFlow" />
          </Link>
        </div>

        <nav className="topbar-nav" aria-label="Atalhos principais">
          {topNavigation.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              className={({ isActive }) => `topbar-link${isActive ? ' is-active' : ''}`}
            >
              {item.label}
            </NavLink>
          ))}
        </nav>

        <div className="topbar-actions">
          <Link to={topbarAction.to} className="topbar-cta">
            {topbarAction.label}
          </Link>
          <Link to="/minha-conta" className="topbar-chip">
            Minha conta
          </Link>
          <Link to="/minha-conta" className="topbar-profile-pill">
            <span className="topbar-avatar">{initials}</span>
            <div>
              <strong>{user?.nome}</strong>
              <small>{roleSummary(user?.papel)}</small>
            </div>
            <UiIcon name="chevron-down" size={16} className="topbar-profile-chevron" />
          </Link>
        </div>
      </header>

      <div className="shell-body">
        <aside className="sidebar">
          <div className="sidebar-header">
            <div className="sidebar-brand">
              <span className="sidebar-label sidebar-brand-label">Workspace financeiro</span>
            </div>
            <div className="sidebar-profile">
              <strong>{user?.nome}</strong>
              <span>{roleSummary(user?.papel)}</span>
              <small>{user?.email}</small>
            </div>
          </div>

          <nav className="sidebar-nav" aria-label="Principal">
            {navigation.map((item) => (
              <NavLink
                key={item.to}
                to={item.to}
                className={({ isActive }) => `nav-link${isActive ? ' is-active' : ''}`}
              >
                <span className="nav-glyph" aria-hidden="true">
                  <UiIcon name={item.icon} size={18} />
                </span>
                <span className="nav-copy">
                  <strong>{item.label}</strong>
                  <small>{item.caption}</small>
                </span>
              </NavLink>
            ))}
          </nav>

          <button type="button" className="secondary-button sidebar-button" onClick={logout}>
            <UiIcon name="logout" size={18} />
            Encerrar sessão
          </button>
        </aside>

        <div className="shell-main">
          <main className="page-content">
            <Outlet />
          </main>
        </div>
      </div>
    </div>
  );
}
