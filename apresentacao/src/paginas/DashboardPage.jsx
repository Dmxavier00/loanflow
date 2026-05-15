import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import BankAccountNotice from '../componentes/BankAccountNotice';
import EmptyState from '../componentes/EmptyState';
import MessageBanner from '../componentes/MessageBanner';
import UiIcon from '../componentes/UiIcon';
import { useAuth } from '../contexto/AuthContext';
import { api } from '../biblioteca/api';
import {
  formatCurrency,
  formatDate,
  formatDateTime,
  formatLabel,
  formatProposalHeadline
} from '../biblioteca/format';

const SIGNATURE_STATUSES = ['AGUARDANDO_ASSINATURAS', 'ASSINADO_PARCIALMENTE'];
const OPEN_INSTALLMENT_STATUSES = ['ABERTA', 'PARCIALMENTE_PAGA', 'EM_ATRASO'];
const MONTH_LABELS = ['JAN', 'FEV', 'MAR', 'ABR', 'MAI', 'JUN', 'JUL', 'AGO', 'SET', 'OUT', 'NOV', 'DEZ'];
const TREND_CURRENCY_FORMATTER = new Intl.NumberFormat('pt-BR', {
  style: 'currency',
  currency: 'BRL',
  maximumFractionDigits: 0
});
const TREND_GROUP_OPTIONS = [
  { value: 'day', label: 'Dias' },
  { value: 'month', label: 'Meses' },
  { value: 'year', label: 'Anos' }
];
const TREND_WINDOW_OPTIONS = {
  day: [
    { value: 7, label: 'Últimos 7 dias' },
    { value: 15, label: 'Últimos 15 dias' },
    { value: 30, label: 'Últimos 30 dias' }
  ],
  month: [
    { value: 3, label: 'Últimos 3 meses' },
    { value: 6, label: 'Últimos 6 meses' },
    { value: 12, label: 'Últimos 12 meses' }
  ],
  year: [
    { value: 1, label: 'Último ano' },
    { value: 3, label: 'Últimos 3 anos' },
    { value: 5, label: 'Últimos 5 anos' }
  ]
};
const DEFAULT_TREND_WINDOW_BY_UNIT = {
  day: 7,
  month: 6,
  year: 1
};

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
  if (!value) {
    return 0;
  }

  const parsed = toDateValue(value)?.getTime() ?? 0;
  return Number.isNaN(parsed) ? 0 : parsed;
}

function sumValues(items, getter) {
  return items.reduce((sum, item) => sum + Number(getter(item) ?? 0), 0);
}

function getOutstandingInstallmentValue(installment) {
  const expected = Number(installment?.valorPrevisto ?? 0);
  const paid = Number(installment?.valorPagoAcumulado ?? 0);
  return Math.max(expected - paid, 0);
}

function getDaysUntil(value) {
  if (!value) {
    return null;
  }

  const today = new Date();
  today.setHours(0, 0, 0, 0);

  const target = toDateValue(value);
  if (!target || Number.isNaN(target.getTime())) {
    return null;
  }

  target.setHours(0, 0, 0, 0);
  return Math.round((target.getTime() - today.getTime()) / 86400000);
}

function formatRelativeDueDate(value) {
  const daysUntil = getDaysUntil(value);
  if (daysUntil === null) {
    return 'Sem data prevista';
  }

  if (daysUntil === 0) {
    return 'Vence hoje';
  }

  if (daysUntil === 1) {
    return 'Vence amanhã';
  }

  if (daysUntil > 1) {
    return `Vence em ${daysUntil} dias`;
  }

  if (daysUntil === -1) {
    return 'Venceu ontem';
  }

  return `Venceu há ${Math.abs(daysUntil)} dias`;
}

function getFirstName(name) {
  return name?.trim()?.split(/\s+/)[0] ?? 'Cliente';
}

function formatTrendMonth(date) {
  return `${MONTH_LABELS[date.getMonth()]}/${String(date.getFullYear()).slice(-2)}`;
}

function formatTrendDay(date) {
  return `${String(date.getDate()).padStart(2, '0')}/${String(date.getMonth() + 1).padStart(2, '0')}`;
}

function formatTrendYear(date) {
  return String(date.getFullYear());
}

function getMonthKey(date) {
  return `${date.getFullYear()}-${date.getMonth()}`;
}

function getDayKey(date) {
  return `${date.getFullYear()}-${date.getMonth()}-${date.getDate()}`;
}

function getYearKey(date) {
  return `${date.getFullYear()}`;
}

function getTrendKey(date, unit) {
  if (unit === 'day') {
    return getDayKey(date);
  }

  if (unit === 'year') {
    return getYearKey(date);
  }

  return getMonthKey(date);
}

function normalizeTrendDate(date, unit) {
  const normalized = new Date(date);
  normalized.setHours(0, 0, 0, 0);

  if (unit === 'year') {
    return new Date(normalized.getFullYear(), 0, 1);
  }

  if (unit === 'month') {
    return new Date(normalized.getFullYear(), normalized.getMonth(), 1);
  }

  return normalized;
}

function addTrendStep(date, unit, step) {
  if (unit === 'year') {
    return new Date(date.getFullYear() + step, 0, 1);
  }

  if (unit === 'month') {
    return new Date(date.getFullYear(), date.getMonth() + step, 1);
  }

  return new Date(date.getFullYear(), date.getMonth(), date.getDate() + step);
}

function buildTrendPeriods(unit, window) {
  const end = normalizeTrendDate(new Date(), unit);
  const start = addTrendStep(end, unit, -(window - 1));

  return Array.from({ length: window }, (_, index) => addTrendStep(start, unit, index));
}

function formatTrendLabel(date, unit) {
  if (unit === 'day') {
    return formatTrendDay(date);
  }

  if (unit === 'year') {
    return formatTrendYear(date);
  }

  return formatTrendMonth(date);
}

function getTrendLabelStep(totalPoints) {
  if (totalPoints <= 8) {
    return 1;
  }

  if (totalPoints <= 16) {
    return 2;
  }

  return Math.ceil(totalPoints / 6);
}

function getDateBadgeParts(value) {
  const date = toDateValue(value);
  if (!date || Number.isNaN(date.getTime())) {
    return { day: '--', month: 'SEM' };
  }

  return {
    day: String(date.getDate()).padStart(2, '0'),
    month: MONTH_LABELS[date.getMonth()]
  };
}

function getDueHeadline(value) {
  const daysUntil = getDaysUntil(value);
  if (daysUntil === null) {
    return 'Sem agenda';
  }

  if (daysUntil < 0) {
    return `Atrasada há ${Math.abs(daysUntil)} dia(s)`;
  }

  if (daysUntil === 0) {
    return 'Hoje';
  }

  if (daysUntil === 1) {
    return 'Amanhã';
  }

  return `Em ${daysUntil} dias`;
}

function getInstallmentPill(installment) {
  const daysUntil = getDaysUntil(installment?.dataVencimento);

  if (installment?.status === 'EM_ATRASO' || (daysUntil !== null && daysUntil < 0)) {
    return { label: 'Em atraso', tone: 'danger' };
  }

  if (daysUntil !== null && daysUntil <= 3) {
    return { label: 'Em breve', tone: 'warning' };
  }

  return { label: 'Agendada', tone: 'info' };
}

function getGenericPill(status) {
  if (!status) {
    return { label: 'Ativo', tone: 'info' };
  }

  if (['EM_ATRASO', 'REJEITADA', 'CANCELADA', 'CANCELADO', 'BLOQUEADO'].includes(status)) {
    return { label: formatLabel(status), tone: 'danger' };
  }

  if (
    ['AGUARDANDO_ASSINATURAS', 'ASSINADO_PARCIALMENTE', 'EM_ANALISE', 'AGUARDANDO_ACEITE'].includes(
      status
    )
  ) {
    return { label: formatLabel(status), tone: 'warning' };
  }

  if (['FORMALIZADO', 'PAGA', 'ACEITA', 'APROVADA', 'CONTRATADA'].includes(status)) {
    return { label: formatLabel(status), tone: 'success' };
  }

  return { label: formatLabel(status), tone: 'info' };
}

function buildTrendSeries(installments, unit, window) {
  const periods = buildTrendPeriods(unit, window);
  const paidBuckets = new Map(periods.map((period) => [getTrendKey(period, unit), 0]));

  installments.forEach((installment) => {
    const value = Number(installment.valorPagoAcumulado ?? 0);
    const date = toDateValue(installment.dataPagamento ?? installment.dataVencimento);

    if (!date || Number.isNaN(date.getTime()) || value <= 0) {
      return;
    }

    const key = getTrendKey(normalizeTrendDate(date, unit), unit);
    if (paidBuckets.has(key)) {
      paidBuckets.set(key, paidBuckets.get(key) + value);
    }
  });

  const hasPaidData = [...paidBuckets.values()].some((value) => value > 0);
  const buckets = new Map(periods.map((period) => [getTrendKey(period, unit), 0]));

  installments.forEach((installment) => {
    const value = Number(
      hasPaidData ? installment.valorPagoAcumulado ?? 0 : installment.valorPrevisto ?? 0
    );
    const referenceDate = toDateValue(
      hasPaidData ? installment.dataPagamento ?? installment.dataVencimento : installment.dataVencimento
    );

    if (!referenceDate || Number.isNaN(referenceDate.getTime()) || value <= 0) {
      return;
    }

    const key = getTrendKey(normalizeTrendDate(referenceDate, unit), unit);
    if (buckets.has(key)) {
      buckets.set(key, buckets.get(key) + value);
    }
  });

  let cumulative = 0;
  const points = periods.map((period) => {
    const key = getTrendKey(period, unit);
    const periodValue = buckets.get(key) ?? 0;
    cumulative += periodValue;

    return {
      label: formatTrendLabel(period, unit),
      value: cumulative
    };
  });

  return {
    points,
    isProjection: !hasPaidData
  };
}

function buildChartGeometry(points) {
  const width = 920;
  const height = 290;
  const padding = { top: 16, right: 18, bottom: 42, left: 52 };
  const chartWidth = width - padding.left - padding.right;
  const chartHeight = height - padding.top - padding.bottom;
  const maxPoint = Math.max(...points.map((point) => point.value), 0);
  const safeMax = maxPoint > 0 ? maxPoint : 1000;
  const xStep = points.length > 1 ? chartWidth / (points.length - 1) : 0;

  const coordinates = points.map((point, index) => {
    const x = padding.left + xStep * index;
    const ratio = point.value / safeMax;
    const y = padding.top + chartHeight - ratio * chartHeight;
    return { ...point, x, y };
  });

  const linePath = coordinates
    .map((point, index) => `${index === 0 ? 'M' : 'L'}${point.x} ${point.y}`)
    .join(' ');

  const areaPath = `${linePath} L${padding.left + chartWidth} ${padding.top + chartHeight} L${padding.left} ${padding.top + chartHeight} Z`;

  const ticks = Array.from({ length: 4 }, (_, index) => {
    const ratio = index / 3;
    const value = Math.round(safeMax * (1 - ratio));
    return {
      key: `tick-${index}`,
      value,
      label: TREND_CURRENCY_FORMATTER.format(value),
      y: padding.top + chartHeight * ratio
    };
  });

  return {
    width,
    height,
    padding,
    coordinates,
    ticks,
    linePath,
    areaPath
  };
}

function DashboardTrendChart({ points, isProjection, projectionNote, ariaLabel }) {
  const geometry = buildChartGeometry(points);
  const xLabelStep = getTrendLabelStep(geometry.coordinates.length);

  return (
    <div className="dashboard-reference-chart-shell">
      {isProjection ? <p className="dashboard-reference-chart-note">{projectionNote}</p> : null}

      <svg
        className="dashboard-reference-chart"
        viewBox={`0 0 ${geometry.width} ${geometry.height}`}
        preserveAspectRatio="none"
        role="img"
        aria-label={ariaLabel}
      >
        <defs>
          <linearGradient id="dashboardReferenceArea" x1="0" x2="0" y1="0" y2="1">
            <stop offset="0%" stopColor="#265aff" stopOpacity="0.22" />
            <stop offset="100%" stopColor="#265aff" stopOpacity="0.04" />
          </linearGradient>
        </defs>

        {geometry.ticks.map((tick) => (
          <g key={tick.key}>
            <line
              x1={geometry.padding.left}
              y1={tick.y}
              x2={geometry.width - geometry.padding.right}
              y2={tick.y}
              className="dashboard-reference-chart-grid"
            />
            <text
              x={geometry.padding.left - 10}
              y={tick.y + 4}
              textAnchor="end"
              className="dashboard-reference-chart-y-label"
            >
              {tick.label}
            </text>
          </g>
        ))}

        <path d={geometry.areaPath} fill="url(#dashboardReferenceArea)" />
        <path d={geometry.linePath} className="dashboard-reference-chart-line" />

        {geometry.coordinates.map((point, index) => (
          <g key={point.label}>
            <circle
              cx={point.x}
              cy={point.y}
              r="5.5"
              className="dashboard-reference-chart-dot-ring"
            />
            <circle cx={point.x} cy={point.y} r="3" className="dashboard-reference-chart-dot" />
            {index % xLabelStep === 0 || index === geometry.coordinates.length - 1 ? (
              <text
                x={point.x}
                y={geometry.height - 12}
                textAnchor="middle"
                className="dashboard-reference-chart-x-label"
              >
                {point.label}
              </text>
            ) : null}
          </g>
        ))}
      </svg>
    </div>
  );
}

function getBorrowerStatusPill(overdueCount, pendingSignatureCount, openCount) {
  if (overdueCount) {
    return {
      label: `${overdueCount} atraso(s) exigem atenção`,
      tone: 'danger'
    };
  }

  if (pendingSignatureCount) {
    return {
      label: `${pendingSignatureCount} assinatura(s) pendente(s)`,
      tone: 'warning'
    };
  }

  if (openCount) {
    return {
      label: 'Fluxo em dia',
      tone: 'success'
    };
  }

  return {
    label: 'Sem parcelas abertas',
    tone: 'info'
  };
}

function DashboardBorrowerCommitmentPanel({
  loading,
  totalOutstandingInstallmentValue,
  totalOverdueOutstandingValue,
  totalPaidInstallmentValue,
  paidInstallmentProgress,
  openInstallments,
  overdueInstallments,
  paidInstallments,
  installments,
  nextInstallments,
  contractsAwaitingSignature,
  myProposals,
  notifications
}) {
  const nextInstallment = nextInstallments[0] ?? null;
  const safeProgress = Math.max(0, Math.min(paidInstallmentProgress, 100));
  const statusPill = getBorrowerStatusPill(
    overdueInstallments.length,
    contractsAwaitingSignature.length,
    openInstallments.length
  );
  const borrowerFacts = [
    {
      icon: 'stack',
      label: 'Parcelas abertas',
      value: `${openInstallments.length}`,
      helper: openInstallments.length
        ? `${formatCurrency(totalOutstandingInstallmentValue)} ainda pendentes`
        : 'Nenhum valor pendente'
    },
    {
      icon: 'currency',
      label: 'Já pago',
      value: formatCurrency(totalPaidInstallmentValue),
      helper: paidInstallments.length
        ? `${paidInstallments.length} parcela(s) com pagamento`
        : 'Sem pagamentos registrados'
    },
    {
      icon: 'calendar',
      label: 'Em atraso',
      value: `${overdueInstallments.length}`,
      helper: overdueInstallments.length
        ? `${formatCurrency(totalOverdueOutstandingValue)} exigem regularização`
        : 'Seu fluxo está sem atrasos'
    },
    {
      icon: 'bell',
      label: 'Alertas novos',
      value: `${notifications.length}`,
      helper: notifications.length
        ? 'Há atualizações esperando leitura'
        : 'Nenhuma atualização nova'
    }
  ];
  const borrowerActions = [
    {
      to: '/parcelas',
      icon: 'calendar',
      eyebrow: 'Próximo vencimento',
      title: nextInstallment
        ? `Parcela ${nextInstallment.numero} · ${nextInstallment.numeroContrato}`
        : 'Sem pagamentos agendados',
      description: nextInstallment
        ? `${formatRelativeDueDate(nextInstallment.dataVencimento)} · ${formatCurrency(
            getOutstandingInstallmentValue(nextInstallment)
          )}`
        : 'Quando novas parcelas forem geradas, elas aparecem aqui.'
    },
    {
      to: '/contratos',
      icon: 'signature',
      eyebrow: 'Assinaturas',
      title: contractsAwaitingSignature.length
        ? `${contractsAwaitingSignature.length} contrato(s) aguardando você`
        : 'Nenhuma assinatura pendente',
      description: contractsAwaitingSignature.length
        ? 'Revise seus contratos para liberar a próxima etapa.'
        : 'Seu fluxo contratual está em dia no momento.'
    },
    {
      to: '/solicitacoes',
      icon: 'file',
      eyebrow: 'Propostas',
      title: myProposals.length
        ? `${myProposals.length} proposta(s) em acompanhamento`
        : 'Sem propostas em andamento',
      description: myProposals.length
        ? 'Acompanhe atualização de análise, aceite e contratação.'
        : 'Novas solicitações aparecem aqui quando você movimentar o mercado.'
    }
  ];

  return (
    <section className="dashboard-reference-panel dashboard-borrower-panel">
      <div className="dashboard-reference-panel-head">
        <div className="dashboard-reference-panel-title">
          <span className="dashboard-reference-panel-icon" aria-hidden="true">
            <UiIcon name="wallet" />
          </span>
          <div>
            <h2>Seu compromisso atual</h2>
            <p>Veja o que ainda falta pagar, o que já avançou e onde agir agora.</p>
          </div>
        </div>
      </div>

      {loading ? (
        <p className="helper-text">Carregando panorama do solicitante...</p>
      ) : (
        <>
          <div className="dashboard-borrower-hero">
            <article className="dashboard-borrower-spotlight">
              <span className={`dashboard-reference-pill tone-${statusPill.tone}`}>
                {statusPill.label}
              </span>
              <strong>{formatCurrency(totalOutstandingInstallmentValue)}</strong>
              <p>
                {openInstallments.length
                  ? 'Este é o valor que ainda falta sair do seu fluxo atual de parcelas.'
                  : 'Você não tem valores pendentes no momento.'}
              </p>

              <div className="dashboard-borrower-meta-card">
                <span>Próxima saída prevista</span>
                <strong>
                  {nextInstallment
                    ? `${formatDate(nextInstallment.dataVencimento)} · ${formatCurrency(
                        getOutstandingInstallmentValue(nextInstallment)
                      )}`
                    : 'Sem vencimento próximo'}
                </strong>
              </div>

              <div className="dashboard-borrower-progress">
                <div className="dashboard-borrower-progress-head">
                  <span>Cronograma concluído</span>
                  <strong>
                    {paidInstallments.length} de {installments.length} parcela(s)
                  </strong>
                </div>
                <div className="dashboard-borrower-progress-track" aria-hidden="true">
                  <span style={{ width: `${safeProgress}%` }} />
                </div>
                <small>{safeProgress}% do seu cronograma já avançou.</small>
              </div>
            </article>

            <div className="dashboard-borrower-fact-grid">
              {borrowerFacts.map((fact) => (
                <article key={fact.label} className="dashboard-borrower-fact-card">
                  <div className="dashboard-borrower-fact-top">
                    <span>{fact.label}</span>
                    <UiIcon name={fact.icon} size={18} />
                  </div>
                  <strong>{fact.value}</strong>
                  <p>{fact.helper}</p>
                </article>
              ))}
            </div>
          </div>

          <div className="dashboard-borrower-step-grid">
            {borrowerActions.map((action) => (
              <Link key={action.eyebrow} to={action.to} className="dashboard-borrower-step-card">
                <span className="dashboard-borrower-step-icon" aria-hidden="true">
                  <UiIcon name={action.icon} />
                </span>
                <div className="dashboard-borrower-step-copy">
                  <span>{action.eyebrow}</span>
                  <strong>{action.title}</strong>
                  <p>{action.description}</p>
                </div>
                <span className="dashboard-borrower-step-arrow" aria-hidden="true">
                  <UiIcon name="arrow-right" size={16} />
                </span>
              </Link>
            ))}
          </div>
        </>
      )}
    </section>
  );
}

function DashboardListPanel({ panel, loading, emptyTitle, emptyDescription }) {
  return (
    <section className="dashboard-reference-panel">
      <div className="dashboard-reference-panel-head">
        <div className="dashboard-reference-panel-title">
          <span className="dashboard-reference-panel-icon" aria-hidden="true">
            <UiIcon name={panel.icon} />
          </span>
          <div>
            <h2>{panel.title}</h2>
            <p>{panel.description}</p>
          </div>
        </div>

        {panel.actionButton ? (
          <button
            type="button"
            className="secondary-button dashboard-reference-inline-button"
            onClick={panel.actionButton.onClick}
          >
            {panel.actionButton.label}
          </button>
        ) : null}
      </div>

      {loading ? (
        <p className="helper-text">Carregando painel...</p>
      ) : panel.items.length ? (
        <div className="dashboard-reference-list">
          {panel.items.map((item) =>
            item.kind === 'schedule' ? (
              <Link key={item.key} to={item.to} className="dashboard-reference-schedule-item">
                <div className="dashboard-reference-date-badge">
                  <strong>{item.day}</strong>
                  <span>{item.month}</span>
                </div>

                <div className="dashboard-reference-item-copy">
                  <strong>{item.title}</strong>
                  <p>{item.subtitle}</p>
                </div>

                <div className="dashboard-reference-item-meta">
                  <strong>{item.value}</strong>
                  <span className={`dashboard-reference-pill tone-${item.pill.tone}`}>
                    {item.pill.label}
                  </span>
                </div>
              </Link>
            ) : (
              <Link key={item.key} to={item.to} className="dashboard-reference-resource-item">
                <div className="dashboard-reference-resource-icon" aria-hidden="true">
                  <UiIcon name={item.icon ?? 'file'} />
                </div>

                <div className="dashboard-reference-item-copy">
                  <strong>{item.title}</strong>
                  <p>{item.subtitle}</p>
                </div>

                <div className="dashboard-reference-resource-meta">
                  <span className={`dashboard-reference-pill tone-${item.pill.tone}`}>
                    {item.pill.label}
                  </span>
                  <UiIcon name="arrow-right" size={16} />
                </div>
              </Link>
            )
          )}
        </div>
      ) : (
        <EmptyState title={emptyTitle} description={emptyDescription} />
      )}

      {panel.footerTo ? (
        <Link to={panel.footerTo} className="dashboard-reference-footer-link">
          <span>{panel.footerLabel}</span>
          <UiIcon name="arrow-right" size={16} />
        </Link>
      ) : null}
    </section>
  );
}

export default function DashboardPage() {
  return <DashboardOverviewPage />;
}

function DashboardOverviewPage() {
  const { token, user, hasRole, updateUser, requiresBankAccount } = useAuth();
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [panelNotice, setPanelNotice] = useState('');
  const [dashboard, setDashboard] = useState(null);
  const [bankAccount, setBankAccount] = useState(null);
  const [notifications, setNotifications] = useState([]);
  const [contracts, setContracts] = useState([]);
  const [installments, setInstallments] = useState([]);
  const [myProposals, setMyProposals] = useState([]);
  const [analysisProposals, setAnalysisProposals] = useState([]);
  const [adminActionMessage, setAdminActionMessage] = useState('');
  const [adminActionType, setAdminActionType] = useState('info');
  const [trendUnit, setTrendUnit] = useState('month');
  const [trendWindow, setTrendWindow] = useState(6);

  const isSolicitante = hasRole('SOLICITANTE');
  const isAdmin = hasRole('ADMIN');
  const isCredor = hasRole('CREDOR');
  const canCustomizeTrendGranularity = isCredor;
  const resolvedTrendUnit = isCredor ? trendUnit : 'month';
  const trendWindowOptions =
    TREND_WINDOW_OPTIONS[resolvedTrendUnit] ?? TREND_WINDOW_OPTIONS.month;
  const resolvedTrendWindow = trendWindowOptions.some((option) => option.value === trendWindow)
    ? trendWindow
    : DEFAULT_TREND_WINDOW_BY_UNIT[resolvedTrendUnit];

  const loadData = async () => {
    setLoading(true);
    setError('');
    setPanelNotice('');

    const requests = [
      api.getMe(token),
      api.getMyBankAccount(token).catch((requestError) => {
        if (requestError.status === 404) {
          return null;
        }

        throw requestError;
      }),
      api.getNotifications(token, { lida: false }),
      api.searchContracts(token, {}),
      api.searchParcelas(token, {})
    ];

    if (isAdmin) {
      requests.push(api.getAdminDashboard(token));
    }

    if (isSolicitante) {
      requests.push(api.getMyProposals(token));
    }

    if (isCredor) {
      requests.push(api.getPendingAcceptanceProposals(token));
    }

    try {
      const results = await Promise.allSettled(requests);
      const failedSections = [];
      let cursor = 0;

      const meResult = results[cursor];
      cursor += 1;
      if (meResult.status === 'rejected') {
        throw meResult.reason;
      }
      updateUser(meResult.value ?? user);

      const bankAccountResult = results[cursor];
      cursor += 1;
      if (bankAccountResult.status === 'fulfilled') {
        setBankAccount(bankAccountResult.value ?? null);
      } else {
        setBankAccount(null);
        failedSections.push('conta');
      }

      const notificationsResult = results[cursor];
      cursor += 1;
      if (notificationsResult.status === 'fulfilled') {
        setNotifications(notificationsResult.value ?? []);
      } else {
        setNotifications([]);
        failedSections.push('alertas');
      }

      const contractsResult = results[cursor];
      cursor += 1;
      if (contractsResult.status === 'fulfilled') {
        setContracts(contractsResult.value ?? []);
      } else {
        setContracts([]);
        failedSections.push('contratos');
      }

      const installmentsResult = results[cursor];
      cursor += 1;
      if (installmentsResult.status === 'fulfilled') {
        setInstallments(installmentsResult.value ?? []);
      } else {
        setInstallments([]);
        failedSections.push('parcelas');
      }

      if (isAdmin) {
        const adminResult = results[cursor];
        cursor += 1;
        if (adminResult?.status === 'fulfilled') {
          setDashboard(adminResult.value ?? null);
        } else {
          setDashboard(null);
          failedSections.push('indicadores administrativos');
        }
      } else {
        setDashboard(null);
      }

      if (isSolicitante) {
        const proposalsResult = results[cursor];
        cursor += 1;
        if (proposalsResult?.status === 'fulfilled') {
          setMyProposals(proposalsResult.value ?? []);
        } else {
          setMyProposals([]);
          failedSections.push('propostas');
        }
      } else {
        setMyProposals([]);
      }

      if (isCredor) {
        const analysisResult = results[cursor];
        if (analysisResult?.status === 'fulfilled') {
          setAnalysisProposals(analysisResult.value ?? []);
        } else {
          setAnalysisProposals([]);
          failedSections.push('fila de aceite');
        }
      } else {
        setAnalysisProposals([]);
      }

      if (failedSections.length) {
        setPanelNotice(
          `Alguns blocos do painel podem estar incompletos agora: ${failedSections.join(', ')}.`
        );
      }
    } catch (loadError) {
      setError(loadError.message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, [token, isAdmin, isSolicitante, isCredor]);

  const activeContracts = contracts.filter((contract) => contract.status !== 'CANCELADO');
  const contractsAwaitingSignature = contracts.filter((contract) =>
    SIGNATURE_STATUSES.includes(contract.status)
  );
  const openInstallments = installments.filter((installment) =>
    OPEN_INSTALLMENT_STATUSES.includes(installment.status)
  );
  const overdueInstallments = installments.filter(
    (installment) => installment.status === 'EM_ATRASO'
  );
  const paidInstallments = installments.filter(
    (installment) =>
      installment.status === 'PAGA' || Number(installment.valorPagoAcumulado ?? 0) > 0
  );

  const proposalFeed = isCredor ? analysisProposals : myProposals;
  const nextInstallments = [...openInstallments]
    .sort((left, right) => toTimeValue(left.dataVencimento) - toTimeValue(right.dataVencimento))
    .slice(0, 3);
  const recentContracts = [...contracts]
    .sort(
      (left, right) =>
        toTimeValue(right.dataFormalizacao ?? right.dataGeracao) -
        toTimeValue(left.dataFormalizacao ?? left.dataGeracao)
    )
    .slice(0, 3);
  const proposalRadar = [...proposalFeed]
    .sort(
      (left, right) =>
        toTimeValue(left.dataExpiracao ?? left.dataCriacao) -
        toTimeValue(right.dataExpiracao ?? right.dataCriacao)
    )
    .slice(0, 3);

  const totalProposalValue = sumValues(myProposals, (proposal) => proposal.valorSolicitado);
  const totalAnalysisValue = sumValues(analysisProposals, (proposal) => proposal.valorSolicitado);
  const totalOpenInstallmentValue = sumValues(
    openInstallments,
    (installment) => installment.valorPrevisto
  );
  const totalOutstandingInstallmentValue = sumValues(
    openInstallments,
    (installment) => getOutstandingInstallmentValue(installment)
  );
  const totalOverdueOutstandingValue = sumValues(
    overdueInstallments,
    (installment) => getOutstandingInstallmentValue(installment)
  );
  const totalPaidInstallmentValue = sumValues(
    installments,
    (installment) => installment.valorPagoAcumulado
  );
  const paidInstallmentProgress = installments.length
    ? Math.round((paidInstallments.length / installments.length) * 100)
    : 0;

  const heroDescription = isAdmin
    ? 'Aqui está o resumo da operação da plataforma.'
    : isCredor
      ? 'Aqui está a leitura rápida da sua carteira de crédito.'
      : 'Aqui está o resumo da sua jornada financeira.';

  const summaryCards = isAdmin
    ? [
        {
          icon: 'currency',
          label: 'Usuários monitorados',
          value: `${dashboard?.totalUsuarios ?? 0}`,
          helper: 'base ativa'
        },
        {
          icon: 'calendar',
          label: 'Propostas aprovadas',
          value: `${dashboard?.propostasAprovadas ?? 0}`,
          helper: 'carteira pronta'
        },
        {
          icon: 'stack',
          label: 'Parcelas abertas',
          value: `${dashboard?.parcelasAbertas ?? openInstallments.length}`,
          helper: 'fluxo em andamento'
        },
        {
          icon: 'chart',
          label: 'Pagamentos registrados',
          value: `${dashboard?.pagamentosRegistrados ?? 0}`,
          helper: 'histórico financeiro'
        }
      ]
    : isCredor
      ? [
          {
            icon: 'currency',
            label: 'Volume em análise',
            value: formatCurrency(totalAnalysisValue),
            helper: `${analysisProposals.length} proposta(s) na fila`
          },
          {
            icon: 'calendar',
            label: 'Próxima decisão',
            value: proposalRadar[0]
              ? getDueHeadline(proposalRadar[0].dataExpiracao ?? proposalRadar[0].dataCriacao)
              : 'Sem fila',
            helper: proposalRadar[0]
              ? formatDate(proposalRadar[0].dataExpiracao ?? proposalRadar[0].dataCriacao)
              : 'Nenhuma proposta pendente'
          },
          {
            icon: 'stack',
            label: 'Contratos ativos',
            value: `${activeContracts.length}`,
            helper: `${contractsAwaitingSignature.length} em assinatura`
          },
          {
            icon: 'chart',
            label: 'Recebimento previsto',
            value: formatCurrency(totalOpenInstallmentValue),
            helper: 'parcelas em aberto'
          }
        ]
      : [
          {
            icon: 'currency',
            label: 'Valor solicitado',
            value: formatCurrency(totalProposalValue),
            helper: myProposals.length
              ? `${myProposals.length} proposta(s) em andamento`
              : 'Sem propostas ativas'
          },
          {
            icon: 'calendar',
            label: 'Próximo vencimento',
            value: nextInstallments[0] ? getDueHeadline(nextInstallments[0].dataVencimento) : 'Sem agenda',
            helper: nextInstallments[0]
              ? formatDate(nextInstallments[0].dataVencimento)
              : 'Nenhuma parcela aberta'
          },
          {
            icon: 'stack',
            label: 'Parcelas pagas',
            value: `${paidInstallments.length} de ${installments.length}`,
            helper: installments.length
              ? `${paidInstallmentProgress}% concluído`
              : 'Sem parcelas registradas'
          },
          {
            icon: 'chart',
            label: 'Valor pago até agora',
            value: formatCurrency(totalPaidInstallmentValue),
            helper: 'total de pagamentos'
          }
        ];

  const trendSeries = buildTrendSeries(installments, resolvedTrendUnit, resolvedTrendWindow);
  const trendPanelContent = isCredor
    ? {
        title: 'Evolução dos recebimentos',
        description: 'Acompanhe quanto dinheiro entrou na sua carteira ao longo do tempo.',
        projectionNote:
          'Sem recebimentos suficientes no período. A linha mostra a agenda prevista dos valores a receber.',
        loadingLabel: 'Carregando evolução dos recebimentos...',
        ariaLabel: 'Gráfico de evolução dos recebimentos'
      }
    : {
        title: 'Evolução dos pagamentos',
        description: 'Acompanhe o progresso financeiro ao longo do tempo.',
        projectionNote:
          'Sem pagamentos suficientes no período. A linha mostra a agenda prevista da carteira.',
        loadingLabel: 'Carregando evolução financeira...',
        ariaLabel: 'Gráfico de evolução financeira'
      };

  const primaryPanel = isAdmin
    ? {
        icon: 'calendar',
        title: 'Parcelas que pedem atenção',
        description: 'Itens abertos e vencidos para tratamento rápido.',
        footerTo: '/parcelas',
        footerLabel: 'Ver fluxo financeiro',
        actionButton: {
          label: 'Sincronizar atrasos',
          onClick: handleMarkOverdue
        },
        items: [...overdueInstallments, ...nextInstallments]
          .sort((left, right) => toTimeValue(left.dataVencimento) - toTimeValue(right.dataVencimento))
          .slice(0, 3)
          .map((installment) => {
            const badge = getDateBadgeParts(installment.dataVencimento);
            return {
              kind: 'schedule',
              key: `installment-${installment.id}`,
              to: `/parcelas?parcelaId=${installment.id}`,
              day: badge.day,
              month: badge.month,
              title: `${installment.numeroContrato} · Parcela ${installment.numero}`,
              subtitle: formatRelativeDueDate(installment.dataVencimento),
              value: formatCurrency(installment.valorPrevisto),
              pill: getInstallmentPill(installment)
            };
          })
      }
    : isCredor
      ? {
          icon: 'file',
          title: 'Propostas para análise',
          description: 'Pedidos aguardando decisão ou revisão.',
          footerTo: '/solicitacoes',
          footerLabel: 'Ver todas as propostas',
          items: proposalRadar.map((proposal) => ({
            kind: 'resource',
            key: `proposal-${proposal.id}`,
            to: `/solicitacoes?focusId=${proposal.id}`,
            icon: 'file',
            title: formatProposalHeadline(proposal),
            subtitle: `${proposal.finalidade || 'Sem resumo'} · ${formatCurrency(
              proposal.valorSolicitado
            )}`,
            pill: getGenericPill(proposal.status)
          }))
        }
      : {
          icon: 'calendar',
          title: 'Próximas parcelas',
          description: 'Fique de olho nos seus próximos vencimentos.',
          footerTo: '/parcelas',
          footerLabel: 'Ver todas as parcelas',
          items: nextInstallments.map((installment) => {
            const badge = getDateBadgeParts(installment.dataVencimento);
            return {
              kind: 'schedule',
              key: `installment-${installment.id}`,
              to: `/parcelas?parcelaId=${installment.id}`,
              day: badge.day,
              month: badge.month,
              title: `Parcela ${installment.numero}`,
              subtitle: `Contrato ${installment.numeroContrato}`,
              value: formatCurrency(installment.valorPrevisto),
              pill: getInstallmentPill(installment)
            };
          })
        };

  const secondaryPanel = isAdmin
    ? {
        icon: 'signature',
        title: 'Assinaturas pendentes',
        description: 'Contratos aguardando fechamento ou formalização.',
        footerTo: '/contratos',
        footerLabel: 'Ver contratos',
        items: (contractsAwaitingSignature.length ? contractsAwaitingSignature : recentContracts)
          .slice(0, 3)
          .map((contract) => ({
            kind: 'resource',
            key: `contract-${contract.id}`,
            to: `/contratos?contratoId=${contract.id}`,
            icon: 'signature',
            title: contract.finalidade || 'Contrato de empréstimo',
            subtitle: `${contract.numeroContrato} · ${formatDateTime(
              contract.dataFormalizacao ?? contract.dataGeracao
            )}`,
            pill: getGenericPill(contract.status)
          }))
      }
    : isCredor
      ? {
          icon: 'signature',
          title: 'Assinaturas pendentes',
          description: 'Contratos aguardando conclusão no fluxo de crédito.',
          footerTo: '/contratos',
          footerLabel: 'Ver todas as pendências',
          items: (contractsAwaitingSignature.length ? contractsAwaitingSignature : recentContracts)
            .slice(0, 3)
            .map((contract) => ({
              kind: 'resource',
              key: `contract-${contract.id}`,
              to: `/contratos?contratoId=${contract.id}`,
              icon: 'signature',
              title: contract.finalidade || 'Contrato de empréstimo',
              subtitle: `${contract.numeroContrato} · ${formatDateTime(
                contract.dataFormalizacao ?? contract.dataGeracao
              )}`,
              pill: getGenericPill(contract.status)
            }))
        }
      : {
          icon: 'signature',
          title: 'Assinaturas pendentes',
          description: 'Contratos aguardando sua assinatura.',
          footerTo: '/contratos',
          footerLabel: 'Ver todas as pendências',
          items: (contractsAwaitingSignature.length ? contractsAwaitingSignature : recentContracts)
            .slice(0, 3)
            .map((contract) => ({
              kind: 'resource',
              key: `contract-${contract.id}`,
              to: `/contratos?contratoId=${contract.id}`,
              icon: 'file',
              title: contract.finalidade || 'Contrato de empréstimo',
              subtitle: `${contract.numeroContrato} · ${formatDateTime(
                contract.dataFormalizacao ?? contract.dataGeracao
              )}`,
              pill: getGenericPill(contract.status)
            }))
        };

  const greetingName = getFirstName(user?.nome);

  async function handleMarkOverdue() {
    setAdminActionMessage('');

    try {
      const updated = await api.markOverdueInstallments(token);
      setAdminActionType('success');
      setAdminActionMessage(`${updated.length} parcela(s) foram marcadas como em atraso.`);
      await loadData();
    } catch (actionError) {
      setAdminActionType('error');
      setAdminActionMessage(actionError.message);
    }
  }

  function handleTrendUnitChange(event) {
    const nextUnit = event.target.value;
    setTrendUnit(nextUnit);
    setTrendWindow(DEFAULT_TREND_WINDOW_BY_UNIT[nextUnit] ?? DEFAULT_TREND_WINDOW_BY_UNIT.month);
  }

  return (
    <div className="page-stack dashboard-page dashboard-reference-page">
      <MessageBanner type="error">{error}</MessageBanner>
      <BankAccountNotice
        show={!loading && requiresBankAccount && !bankAccount}
        message="Cadastre uma conta bancária em Minha conta para liberar propostas, aceite e assinatura."
      />
      <MessageBanner type="info">{panelNotice}</MessageBanner>
      <MessageBanner type={adminActionType}>{adminActionMessage}</MessageBanner>

      <section className="dashboard-reference-header">
        <div className="dashboard-reference-header-copy">
          <h1>Bem-vinda, {greetingName}</h1>
          <p>{heroDescription}</p>
        </div>
      </section>

      <section className="dashboard-reference-stat-grid">
        {summaryCards.map((card) => (
          <article key={card.label} className="dashboard-reference-stat-card">
            <div className="dashboard-reference-stat-icon" aria-hidden="true">
              <UiIcon name={card.icon} size={24} />
            </div>

            <div className="dashboard-reference-stat-copy">
              <span>{card.label}</span>
              <strong>{loading ? '--' : card.value}</strong>
              <small>{loading ? 'Atualizando dados...' : card.helper}</small>
            </div>
          </article>
        ))}
      </section>

      {isSolicitante ? (
        <DashboardBorrowerCommitmentPanel
          loading={loading}
          totalOutstandingInstallmentValue={totalOutstandingInstallmentValue}
          totalOverdueOutstandingValue={totalOverdueOutstandingValue}
          totalPaidInstallmentValue={totalPaidInstallmentValue}
          paidInstallmentProgress={paidInstallmentProgress}
          openInstallments={openInstallments}
          overdueInstallments={overdueInstallments}
          paidInstallments={paidInstallments}
          installments={installments}
          nextInstallments={nextInstallments}
          contractsAwaitingSignature={contractsAwaitingSignature}
          myProposals={myProposals}
          notifications={notifications}
        />
      ) : (
        <section className="dashboard-reference-panel dashboard-reference-chart-panel">
          <div className="dashboard-reference-panel-head">
            <div className="dashboard-reference-panel-title">
              <span className="dashboard-reference-panel-icon" aria-hidden="true">
                <UiIcon name="chart" />
              </span>
              <div>
                <h2>{trendPanelContent.title}</h2>
                <p>{trendPanelContent.description}</p>
              </div>
            </div>

            <div className="dashboard-reference-filter-row">
              {canCustomizeTrendGranularity ? (
                <label className="dashboard-reference-filter-field">
                  <span>Visualização</span>
                  <select
                    className="dashboard-reference-select"
                    value={trendUnit}
                    onChange={handleTrendUnitChange}
                  >
                    {TREND_GROUP_OPTIONS.map((option) => (
                      <option key={option.value} value={option.value}>
                        {option.label}
                      </option>
                    ))}
                  </select>
                </label>
              ) : null}

              <label className="dashboard-reference-filter-field">
                <span>Período</span>
                <select
                  className="dashboard-reference-select"
                  value={resolvedTrendWindow}
                  onChange={(event) => setTrendWindow(Number(event.target.value))}
                >
                  {trendWindowOptions.map((option) => (
                    <option key={`${resolvedTrendUnit}-${option.value}`} value={option.value}>
                      {option.label}
                    </option>
                  ))}
                </select>
              </label>
            </div>
          </div>

          {loading ? (
            <p className="helper-text">{trendPanelContent.loadingLabel}</p>
          ) : (
            <DashboardTrendChart
              points={trendSeries.points}
              isProjection={trendSeries.isProjection}
              projectionNote={trendPanelContent.projectionNote}
              ariaLabel={trendPanelContent.ariaLabel}
            />
          )}
        </section>
      )}

      <div className="dashboard-reference-bottom-grid">
        <DashboardListPanel
          panel={primaryPanel}
          loading={loading}
          emptyTitle={
            isCredor
              ? 'Sem propostas em fila.'
              : isAdmin
                ? 'Sem parcelas para tratar.'
                : 'Sem parcelas programadas.'
          }
          emptyDescription={
            isCredor
              ? 'Quando novas ordens chegarem, elas aparecerão aqui.'
              : isAdmin
                ? 'O fluxo financeiro volta a aparecer aqui assim que houver movimento.'
                : 'Quando houver novos vencimentos, eles aparecerão aqui.'
          }
        />

        <DashboardListPanel
          panel={secondaryPanel}
          loading={loading}
          emptyTitle="Sem pendências agora."
          emptyDescription="Novos contratos, alertas ou assinaturas aparecerão aqui assim que avançarem no fluxo."
        />
      </div>
    </div>
  );
}
