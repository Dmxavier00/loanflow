declare @today date = cast(sysdatetime() as date);
declare @current_month date = datefromparts(year(@today), month(@today), 1);

declare @dashboard_contracts table (
    seed_ord int not null primary key,
    solicitante_email nvarchar(160) not null,
    credor_email nvarchar(160) not null,
    numero_proposta nvarchar(40) not null,
    numero_contrato nvarchar(40) not null,
    valor_solicitado decimal(18, 2) not null,
    taxa_juros decimal(8, 4) not null,
    prazo_meses int not null,
    finalidade nvarchar(500) not null,
    categoria_finalidade nvarchar(40) not null,
    descricao_detalhada nvarchar(2000) not null,
    start_month_offset int not null,
    parcelas_pagas int not null
);

insert into @dashboard_contracts (
    seed_ord,
    solicitante_email,
    credor_email,
    numero_proposta,
    numero_contrato,
    valor_solicitado,
    taxa_juros,
    prazo_meses,
    finalidade,
    categoria_finalidade,
    descricao_detalhada,
    start_month_offset,
    parcelas_pagas
)
values
    (
        1,
        'alice.solicitante.demo@loanflow.com',
        'bruno.credor.demo@loanflow.com',
        'PPT-DEMO-DASH-01',
        'CTR-DEMO-DASH-01',
        7200.00,
        8.5000,
        8,
        'Consolidacao de despesas medicas e familiares',
        'SAUDE',
        'Contrato demonstrativo usado para exibir pagamentos mensais no dashboard do solicitante, do credor e do administrador.',
        -5,
        6
    ),
    (
        2,
        'ana.paula.martins@loanflow.local',
        'patricia.s.monteiro@loanflow.local',
        'PPT-DEMO-DASH-02',
        'CTR-DEMO-DASH-02',
        9600.00,
        11.3000,
        10,
        'Reforma residencial com acompanhamento parcelado',
        'REFORMA',
        'Contrato demonstrativo com pagamentos distribuidos nos meses anteriores para dar volume ao grafico de evolucao financeira.',
        -4,
        4
    ),
    (
        3,
        'rafael.t.moura@loanflow.local',
        'vanessa.q.braga@loanflow.local',
        'PPT-DEMO-DASH-03',
        'CTR-DEMO-DASH-03',
        8400.00,
        12.4000,
        12,
        'Capital de giro para oficina local',
        'CAPITAL_DE_GIRO',
        'Contrato demonstrativo com carteira ativa e pagamentos recentes para enriquecer a visao administrativa.',
        -2,
        3
    );

update sc
set
    renda_mensal = case
        when sc.renda_mensal is null or sc.renda_mensal < perfil.renda_minima then perfil.renda_minima
        else sc.renda_mensal
    end,
    score_credito_simulado = case
        when sc.score_credito_simulado is null or sc.score_credito_simulado < perfil.score_minimo then perfil.score_minimo
        else sc.score_credito_simulado
    end
from solicitantes_credito sc
join usuarios u on u.id = sc.usuario_id
join (
    select 'alice.solicitante.demo@loanflow.com' as email, cast(5600.00 as decimal(18, 2)) as renda_minima, 76 as score_minimo
    union all
    select 'ana.paula.martins@loanflow.local', cast(6400.00 as decimal(18, 2)), 78
    union all
    select 'rafael.t.moura@loanflow.local', cast(7200.00 as decimal(18, 2)), 82
) perfil on perfil.email = u.email;

update cr
set
    saldo_disponivel_simulado = case
        when cr.saldo_disponivel_simulado < resumo.valor_total + 15000.00 then resumo.valor_total + 15000.00
        else cr.saldo_disponivel_simulado
    end,
    total_emprestado_simulado = case
        when cr.total_emprestado_simulado < resumo.valor_total then resumo.valor_total
        else cr.total_emprestado_simulado
    end,
    limite_operacoes = case when cr.limite_operacoes < 6 then 6 else cr.limite_operacoes end
from credores cr
join usuarios u on u.id = cr.usuario_id
join (
    select credor_email, sum(valor_solicitado) as valor_total
    from @dashboard_contracts
    group by credor_email
) resumo on resumo.credor_email = u.email;

update p
set
    solicitante_id = sc.id,
    credor_id = cr.id,
    valor_solicitado = dc.valor_solicitado,
    taxa_juros = dc.taxa_juros,
    prazo_meses = dc.prazo_meses,
    finalidade = dc.finalidade,
    categoria_finalidade = dc.categoria_finalidade,
    descricao_detalhada = dc.descricao_detalhada,
    status = 'CONTRATADA',
    data_criacao = dateadd(day, 2, cast(dateadd(month, dc.start_month_offset - 1, @current_month) as datetime2)),
    data_atualizacao = dateadd(day, 3, cast(dateadd(month, dc.start_month_offset - 1, @current_month) as datetime2)),
    data_expiracao = dateadd(day, 12, dateadd(month, dc.start_month_offset - 1, @current_month))
from propostas p
join @dashboard_contracts dc on dc.numero_proposta = p.numero_proposta
join usuarios su on su.email = dc.solicitante_email
join solicitantes_credito sc on sc.usuario_id = su.id
join usuarios cu on cu.email = dc.credor_email
join credores cr on cr.usuario_id = cu.id;

insert into propostas (
    numero_proposta,
    solicitante_id,
    credor_id,
    valor_solicitado,
    taxa_juros,
    prazo_meses,
    finalidade,
    categoria_finalidade,
    descricao_detalhada,
    status,
    data_criacao,
    data_atualizacao,
    data_expiracao
)
select
    dc.numero_proposta,
    sc.id,
    cr.id,
    dc.valor_solicitado,
    dc.taxa_juros,
    dc.prazo_meses,
    dc.finalidade,
    dc.categoria_finalidade,
    dc.descricao_detalhada,
    'CONTRATADA',
    dateadd(day, 2, cast(dateadd(month, dc.start_month_offset - 1, @current_month) as datetime2)),
    dateadd(day, 3, cast(dateadd(month, dc.start_month_offset - 1, @current_month) as datetime2)),
    dateadd(day, 12, dateadd(month, dc.start_month_offset - 1, @current_month))
from @dashboard_contracts dc
join usuarios su on su.email = dc.solicitante_email
join solicitantes_credito sc on sc.usuario_id = su.id
join usuarios cu on cu.email = dc.credor_email
join credores cr on cr.usuario_id = cu.id
where not exists (
    select 1
    from propostas p
    where p.numero_proposta = dc.numero_proposta
);

update co
set
    numero_contrato = dc.numero_contrato,
    status = 'FORMALIZADO',
    conteudo_snapshot = concat('Contrato demonstrativo para o dashboard: ', dc.numero_contrato),
    pdf_path = null,
    hash_documento = convert(nvarchar(128), hashbytes('SHA2_256', dc.numero_contrato), 2),
    hash_pdf_emitido = convert(nvarchar(128), hashbytes('SHA2_256', concat(dc.numero_contrato, '-pdf')), 2),
    data_geracao = dateadd(day, 4, cast(dateadd(month, dc.start_month_offset - 1, @current_month) as datetime2)),
    data_formalizacao = dateadd(day, 5, cast(dateadd(month, dc.start_month_offset - 1, @current_month) as datetime2))
from contratos co
join propostas p on p.id = co.proposta_id
join @dashboard_contracts dc on dc.numero_proposta = p.numero_proposta;

insert into contratos (
    proposta_id,
    numero_contrato,
    status,
    conteudo_snapshot,
    pdf_path,
    hash_documento,
    hash_pdf_emitido,
    data_geracao,
    data_formalizacao
)
select
    p.id,
    dc.numero_contrato,
    'FORMALIZADO',
    concat('Contrato demonstrativo para o dashboard: ', dc.numero_contrato),
    null,
    convert(nvarchar(128), hashbytes('SHA2_256', dc.numero_contrato), 2),
    convert(nvarchar(128), hashbytes('SHA2_256', concat(dc.numero_contrato, '-pdf')), 2),
    dateadd(day, 4, cast(dateadd(month, dc.start_month_offset - 1, @current_month) as datetime2)),
    dateadd(day, 5, cast(dateadd(month, dc.start_month_offset - 1, @current_month) as datetime2))
from @dashboard_contracts dc
join propostas p on p.numero_proposta = dc.numero_proposta
where not exists (
    select 1
    from contratos co
    where co.numero_contrato = dc.numero_contrato
);

declare @numbers table (numero int not null primary key);

insert into @numbers (numero)
values (1), (2), (3), (4), (5), (6), (7), (8), (9), (10), (11), (12);

declare @dashboard_installments table (
    contrato_id bigint not null,
    numero_contrato nvarchar(40) not null,
    numero int not null,
    valor_previsto decimal(18, 2) not null,
    valor_pago_acumulado decimal(18, 2) not null,
    data_vencimento date not null,
    status nvarchar(40) not null,
    comprovante nvarchar(500) not null,
    primary key (contrato_id, numero)
);

insert into @dashboard_installments (
    contrato_id,
    numero_contrato,
    numero,
    valor_previsto,
    valor_pago_acumulado,
    data_vencimento,
    status,
    comprovante
)
select
    co.id,
    dc.numero_contrato,
    n.numero,
    parcela.valor_previsto,
    case when n.numero <= dc.parcelas_pagas then parcela.valor_previsto else 0.00 end,
    parcela.data_vencimento,
    case
        when n.numero <= dc.parcelas_pagas then 'PAGA'
        when parcela.data_vencimento < @today then 'EM_ATRASO'
        else 'ABERTA'
    end,
    concat('DEMO-DASH-', dc.numero_contrato, '-P', right(concat('00', n.numero), 2))
from @dashboard_contracts dc
join contratos co on co.numero_contrato = dc.numero_contrato
join @numbers n on n.numero <= dc.prazo_meses
cross apply (
    select cast(round(dc.valor_solicitado * (1 + (dc.taxa_juros / cast(100.0000 as decimal(18, 4)))), 2) as decimal(18, 2)) as valor_total
) total
cross apply (
    select cast(round(total.valor_total / dc.prazo_meses, 2, 1) as decimal(18, 2)) as valor_base
) base
cross apply (
    select
        cast(
            case
                when n.numero = dc.prazo_meses then total.valor_total - (base.valor_base * (dc.prazo_meses - 1))
                else base.valor_base
            end as decimal(18, 2)
        ) as valor_previsto,
        dateadd(day, 4, dateadd(month, dc.start_month_offset + n.numero - 1, @current_month)) as data_vencimento
) parcela;

update pa
set
    valor_previsto = di.valor_previsto,
    valor_pago_acumulado = di.valor_pago_acumulado,
    data_vencimento = di.data_vencimento,
    status = di.status
from parcelas pa
join @dashboard_installments di on di.contrato_id = pa.contrato_id and di.numero = pa.numero;

insert into parcelas (
    contrato_id,
    numero,
    valor_previsto,
    valor_pago_acumulado,
    data_vencimento,
    status
)
select
    di.contrato_id,
    di.numero,
    di.valor_previsto,
    di.valor_pago_acumulado,
    di.data_vencimento,
    di.status
from @dashboard_installments di
where not exists (
    select 1
    from parcelas pa
    where pa.contrato_id = di.contrato_id
      and pa.numero = di.numero
);

update pg
set
    parcela_id = pa.id,
    usuario_registrador_id = sc.usuario_id,
    valor_pago = di.valor_previsto,
    data_hora_pagamento = dateadd(hour, 10, cast(di.data_vencimento as datetime2)),
    forma_pagamento = 'PIX_MANUAL',
    status = 'REGISTRADO'
from pagamentos pg
join @dashboard_installments di on di.comprovante = pg.comprovante
join parcelas pa on pa.contrato_id = di.contrato_id and pa.numero = di.numero
join contratos co on co.id = pa.contrato_id
join propostas pr on pr.id = co.proposta_id
join solicitantes_credito sc on sc.id = pr.solicitante_id
where di.status = 'PAGA';

insert into pagamentos (
    parcela_id,
    usuario_registrador_id,
    valor_pago,
    data_hora_pagamento,
    forma_pagamento,
    status,
    comprovante
)
select
    pa.id,
    sc.usuario_id,
    di.valor_previsto,
    dateadd(hour, 10, cast(di.data_vencimento as datetime2)),
    'PIX_MANUAL',
    'REGISTRADO',
    di.comprovante
from @dashboard_installments di
join parcelas pa on pa.contrato_id = di.contrato_id and pa.numero = di.numero
join contratos co on co.id = pa.contrato_id
join propostas pr on pr.id = co.proposta_id
join solicitantes_credito sc on sc.id = pr.solicitante_id
where di.status = 'PAGA'
  and not exists (
      select 1
      from pagamentos pg
      where pg.comprovante = di.comprovante
  );
