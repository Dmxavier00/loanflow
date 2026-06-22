import { Link } from 'react-router-dom';
import brandFull from '../recursos/loanflow-full.png';
import { useAuth } from '../contexto/AuthContext';
import { getAuthenticatedSecondaryAction } from '../biblioteca/bankAccountPolicy';

const heroStats = [
  { label: 'Análise inicial', value: 'até 48h' },
  { label: 'Fluxo digital', value: '100% centralizado' },
  { label: 'Perfis conectados', value: 'solicitante e credor' },
  { label: 'Operação monitorada', value: 'do pedido ao pagamento' }
];

const trustBarItems = [
  { label: 'Originação guiada', value: 'propostas com dados e contexto' },
  { label: 'Formalização contínua', value: 'aceite, contrato e parcelas' },
  { label: 'Visão financeira', value: 'parcelas, vencimentos e registros' },
  { label: 'Governança ativa', value: 'alertas, histórico e rastreabilidade' }
];

const solutionCards = [
  {
    tag: 'Origination',
    title: 'Entrada organizada para novas propostas',
    description:
      'Estruture pedidos de crédito com dados claros, documentação centralizada e trilha de decisão.'
  },
  {
    tag: 'Contratos',
    title: 'Formalização com menos atrito',
    description:
      'Conduza aceite, emissão e formalização de contratos em um fluxo contínuo para ganhar velocidade.'
  },
  {
    tag: 'Parcelas',
    title: 'Acompanhamento financeiro em um só painel',
    description:
      'Visualize vencimentos, pagamentos registrados e alertas operacionais sem trocar de sistema.'
  },
  {
    tag: 'Compliance',
    title: 'Segurança operacional para crescer com previsibilidade',
    description:
      'Unifique perfis, permissões e histórico de eventos para sustentar uma operação mais confiável.'
  }
];

const journeySteps = [
  {
    index: '01',
    title: 'Receba a demanda com contexto',
    description:
      'Cada solicitação nasce com os dados essenciais para acelerar a avaliação e reduzir retrabalho.'
  },
  {
    index: '02',
    title: 'Analise, negocie e formalize',
    description:
      'A relação entre solicitante e credor fica concentrada em um único fluxo operacional.'
  },
  {
    index: '03',
    title: 'Acompanhe o ciclo completo',
    description:
      'Contratos, parcelas e notificações seguem conectados para que a operação não perca ritmo.'
  }
];

const audienceCards = [
  {
    title: 'Para empresas que buscam crédito',
    points: [
      'Envio de propostas em um fluxo mais claro e orientado.',
      'Consulta de contratos e parcelas sem depender de processos paralelos.',
      'Mais previsibilidade sobre o status de cada etapa.'
    ]
  },
  {
    title: 'Para credores e parceiros financeiros',
    points: [
      'Fila organizada para avaliação das operações recebidas.',
      'Visão única de aceite, formalização e cobrança.',
      'Mais controle sobre comunicações e eventos da carteira.'
    ]
  }
];

const securityTopics = [
  'Controle de acesso por perfil',
  'Histórico operacional centralizado',
  'Contratos e pagamentos no mesmo fluxo',
  'Experiência pensada para decisão rápida'
];

export default function HomePage() {
  const { isAuthenticated, user } = useAuth();

  const primaryAction = isAuthenticated
    ? { to: '/dashboard', label: 'Acessar painel' }
    : { to: '/cadastro', label: 'Abrir minha conta' };

  const secondaryAction = isAuthenticated
    ? getAuthenticatedSecondaryAction(user)
    : { to: '/login', label: 'Entrar' };

  return (
    <div className="home-page">
      <header className="landing-topbar">
        <Link to="/" className="landing-brand" aria-label="LoanFlow">
          <img className="landing-brand-image" src={brandFull} alt="LoanFlow" />
        </Link>

        <nav className="landing-nav" aria-label="Navegação principal">
          <a href="#solucoes">Soluções</a>
          <a href="#processo">Como funciona</a>
          <a href="#segmentos">Para quem é</a>
          <a href="#seguranca">Segurança</a>
        </nav>

        <div className="landing-actions">
          <Link to={secondaryAction.to} className="landing-login-link">
            {secondaryAction.label}
          </Link>
          <Link to={primaryAction.to} className="primary-button landing-primary-action">
            {primaryAction.label}
          </Link>
        </div>
      </header>

      <main className="landing-main">
        <section className="landing-hero">
          <div className="landing-hero-copy">
            <span className="eyebrow landing-eyebrow">LoanFlow para empresas</span>
            <h1>Crédito estruturado para negócios que precisam crescer com previsibilidade.</h1>
            <p>
              A LoanFlow conecta solicitantes, credores, contratos e pagamentos em uma jornada
              única, com linguagem institucional e experiência moderna para apresentar a sua
              empresa fictícia com mais consistência.
            </p>

            <div className="landing-hero-actions">
              <Link to={primaryAction.to} className="primary-button">
                {primaryAction.label}
              </Link>
              <a href="#solucoes" className="secondary-button landing-outline-action">
                Conhecer a plataforma
              </a>
            </div>

            <div className="landing-hero-note">
              <strong>{isAuthenticated ? `Bem-vindo de volta, ${user?.nome}.` : 'Jornada completa de ponta a ponta.'}</strong>
              <span>
                Da entrada da proposta ao acompanhamento das parcelas, tudo acontece em um único
                ambiente.
              </span>
            </div>
          </div>

          <div className="landing-hero-showcase">
            <article className="landing-highlight-card">
              <span className="landing-card-label">Destaque da operação</span>
              <h2>Mais clareza para vender crédito e mais confiança para executar.</h2>
              <p>
                Organize sua esteira comercial e operacional com uma home institucional forte e um
                painel transacional logo depois do acesso.
              </p>

              <div className="landing-chip-row">
                <span>Propostas guiadas</span>
                <span>Contratos digitais</span>
                <span>Alertas operacionais</span>
              </div>
            </article>

            <div className="landing-stat-grid">
              {heroStats.map((item) => (
                <article key={item.label} className="landing-stat-card">
                  <span>{item.label}</span>
                  <strong>{item.value}</strong>
                </article>
              ))}
            </div>
          </div>
        </section>

        <section className="landing-trustbar">
          {trustBarItems.map((item) => (
            <div key={item.label}>
              <span>{item.label}</span>
              <strong>{item.value}</strong>
            </div>
          ))}
        </section>

        <section className="landing-section" id="solucoes">
          <div className="landing-section-heading">
            <span className="eyebrow">Soluções essenciais</span>
            <h2>Uma vitrine institucional que prepara a entrada na plataforma operacional.</h2>
            <p>
              Inspirada em uma experiência corporativa de mercado, esta home apresenta sua marca
              antes do login e cria contexto para quem ainda está conhecendo a empresa.
            </p>
          </div>

          <div className="landing-solution-grid">
            {solutionCards.map((card) => (
              <article key={card.title} className="landing-feature-card">
                <span className="landing-feature-tag">{card.tag}</span>
                <h3>{card.title}</h3>
                <p>{card.description}</p>
              </article>
            ))}
          </div>
        </section>

        <section className="landing-section landing-process-section" id="processo">
          <div className="landing-process-copy">
            <span className="eyebrow">Como funciona</span>
            <h2>Uma jornada simples para explicar a proposta da sua empresa logo na chegada.</h2>
            <div className="landing-step-list">
              {journeySteps.map((step) => (
                <article key={step.index} className="landing-step-card">
                  <span>{step.index}</span>
                  <div>
                    <h3>{step.title}</h3>
                    <p>{step.description}</p>
                  </div>
                </article>
              ))}
            </div>
          </div>

          <aside className="landing-process-panel">
            <span className="landing-card-label">Visão executiva</span>
            <h3>Mais instituição na entrada, mais conversão no acesso.</h3>
            <p>
              A nova home ajuda a apresentar posicionamento, benefícios e fluxo antes de levar o
              usuário para autenticar e operar.
            </p>

            <div className="landing-panel-metrics">
              <div>
                <span>Experiência</span>
                <strong>Home + autenticação + painel</strong>
              </div>
              <div>
                <span>Tom de marca</span>
                <strong>Corporativo, confiável e moderno</strong>
              </div>
              <div>
                <span>Navegação</span>
                <strong>CTA direta para cadastro e login</strong>
              </div>
            </div>
          </aside>
        </section>

        <section className="landing-section" id="segmentos">
          <div className="landing-section-heading">
            <span className="eyebrow">Para quem é</span>
            <h2>LoanFlow conversa com quem precisa captar e com quem precisa decidir.</h2>
          </div>

          <div className="landing-audience-grid">
            {audienceCards.map((card) => (
              <article key={card.title} className="landing-audience-card">
                <h3>{card.title}</h3>
                <div className="landing-bullet-list">
                  {card.points.map((point) => (
                    <p key={point}>{point}</p>
                  ))}
                </div>
              </article>
            ))}
          </div>
        </section>

        <section className="landing-cta-banner" id="seguranca">
          <div className="landing-cta-copy">
            <span className="eyebrow landing-eyebrow">Segurança e governança</span>
            <h2>Uma fachada forte para gerar confiança antes do primeiro clique de acesso.</h2>
            <p>
              A home reforça a proposta de valor da LoanFlow e encaminha visitantes para o próximo
              passo com clareza, seja conhecer mais, entrar ou criar conta.
            </p>
          </div>

          <div className="landing-security-grid">
            {securityTopics.map((topic) => (
              <div key={topic} className="landing-security-chip">
                {topic}
              </div>
            ))}
            <Link to={primaryAction.to} className="primary-button landing-banner-button">
              {primaryAction.label}
            </Link>
          </div>
        </section>
      </main>
    </div>
  );
}
