with pagamento_destinatarios as (
    select pg.id as pagamento_id, sc.usuario_id
    from pagamentos pg
    join parcelas pa on pa.id = pg.parcela_id
    join contratos co on co.id = pa.contrato_id
    join propostas pr on pr.id = co.proposta_id
    join solicitantes_credito sc on sc.id = pr.solicitante_id
    where pg.status <> 'CANCELADO'

    union

    select pg.id as pagamento_id, cr.usuario_id
    from pagamentos pg
    join parcelas pa on pa.id = pg.parcela_id
    join contratos co on co.id = pa.contrato_id
    join propostas pr on pr.id = co.proposta_id
    join credores cr on cr.id = pr.credor_id
    where pg.status <> 'CANCELADO'

    union

    select pg.id as pagamento_id, u.id as usuario_id
    from pagamentos pg
    cross join usuarios u
    where pg.status <> 'CANCELADO'
      and u.papel = 'ADMIN'
),
pagamento_eventos as (
    select
        pd.usuario_id,
        pa.id as parcela_id,
        concat('Pagamento registrado na parcela ', pa.numero, ' do contrato ', co.numero_contrato, '.') as mensagem,
        coalesce(pg.data_hora_pagamento, sysdatetime()) as data_envio
    from pagamentos pg
    join parcelas pa on pa.id = pg.parcela_id
    join contratos co on co.id = pa.contrato_id
    join pagamento_destinatarios pd on pd.pagamento_id = pg.id
    where pg.status <> 'CANCELADO'
)
insert into notificacoes (
    usuario_id,
    tipo,
    mensagem,
    data_envio,
    lida,
    referencia_tipo,
    referencia_id
)
select
    pe.usuario_id,
    'PAGAMENTO',
    pe.mensagem,
    pe.data_envio,
    0,
    'Parcela',
    pe.parcela_id
from pagamento_eventos pe
where not exists (
    select 1
    from notificacoes n
    where n.usuario_id = pe.usuario_id
      and n.tipo = 'PAGAMENTO'
      and n.referencia_tipo = 'Parcela'
      and n.referencia_id = pe.parcela_id
      and n.mensagem = pe.mensagem
);

with contrato_destinatarios as (
    select co.id as contrato_id, sc.usuario_id
    from contratos co
    join propostas pr on pr.id = co.proposta_id
    join solicitantes_credito sc on sc.id = pr.solicitante_id
    where co.status = 'QUITADO'

    union

    select co.id as contrato_id, cr.usuario_id
    from contratos co
    join propostas pr on pr.id = co.proposta_id
    join credores cr on cr.id = pr.credor_id
    where co.status = 'QUITADO'

    union

    select co.id as contrato_id, u.id as usuario_id
    from contratos co
    cross join usuarios u
    where co.status = 'QUITADO'
      and u.papel = 'ADMIN'
),
contrato_eventos as (
    select
        cd.usuario_id,
        co.id as contrato_id,
        'Contrato quitado apos pagamento integral.' as mensagem,
        coalesce(max(pg.data_hora_pagamento), co.data_formalizacao, co.data_geracao, sysdatetime()) as data_envio
    from contratos co
    join contrato_destinatarios cd on cd.contrato_id = co.id
    left join parcelas pa on pa.contrato_id = co.id
    left join pagamentos pg on pg.parcela_id = pa.id and pg.status <> 'CANCELADO'
    where co.status = 'QUITADO'
    group by cd.usuario_id, co.id, co.data_formalizacao, co.data_geracao
)
insert into notificacoes (
    usuario_id,
    tipo,
    mensagem,
    data_envio,
    lida,
    referencia_tipo,
    referencia_id
)
select
    ce.usuario_id,
    'CONTRATO',
    ce.mensagem,
    ce.data_envio,
    0,
    'Contrato',
    ce.contrato_id
from contrato_eventos ce
where not exists (
    select 1
    from notificacoes n
    where n.usuario_id = ce.usuario_id
      and n.tipo = 'CONTRATO'
      and n.referencia_tipo = 'Contrato'
      and n.referencia_id = ce.contrato_id
      and n.mensagem = ce.mensagem
);
