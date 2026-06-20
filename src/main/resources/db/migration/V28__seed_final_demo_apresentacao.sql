declare @now datetime2 = sysdatetime();
declare @today date = cast(@now as date);
declare @current_month date = datefromparts(year(@today), month(@today), 1);
declare @senha_hash nvarchar(120) = '$2a$10$M5ULlPXkYedH.FBsNF2Kqemgb8G/pAzMBpfwJZdBK8AhDduvPHGaO';

declare @demo_users table (
    nome nvarchar(120) not null,
    email nvarchar(160) not null primary key,
    cpf nvarchar(14) not null,
    papel nvarchar(30) not null,
    estado_civil nvarchar(30) not null,
    nacionalidade nvarchar(60) not null,
    profissao nvarchar(120) not null,
    data_nascimento date not null,
    telefone nvarchar(20) not null,
    tipo_documento_identidade nvarchar(30) not null,
    documento_identidade nvarchar(40) not null,
    orgao_emissor nvarchar(20) not null,
    pessoa_exposta_politicamente bit not null,
    cep nvarchar(9) not null,
    logradouro nvarchar(160) not null,
    numero nvarchar(20) not null,
    complemento nvarchar(80) null,
    bairro nvarchar(80) not null,
    cidade nvarchar(80) not null,
    uf nvarchar(2) not null,
    renda_mensal decimal(18, 2) null,
    tipo_ocupacao nvarchar(80) null,
    score_credito_simulado int null,
    saldo_disponivel_simulado decimal(18, 2) null,
    total_emprestado_simulado decimal(18, 2) null,
    limite_operacoes int null,
    nivel_acesso nvarchar(50) null,
    setor_responsavel nvarchar(80) null,
    data_designacao date null,
    status_administrativo nvarchar(40) null,
    banco nvarchar(120) null,
    agencia nvarchar(20) null,
    numero_conta nvarchar(30) null,
    tipo_conta nvarchar(20) null,
    chave_pix nvarchar(120) null
);

insert into @demo_users (
    nome,
    email,
    cpf,
    papel,
    estado_civil,
    nacionalidade,
    profissao,
    data_nascimento,
    telefone,
    tipo_documento_identidade,
    documento_identidade,
    orgao_emissor,
    pessoa_exposta_politicamente,
    cep,
    logradouro,
    numero,
    complemento,
    bairro,
    cidade,
    uf,
    renda_mensal,
    tipo_ocupacao,
    score_credito_simulado,
    saldo_disponivel_simulado,
    total_emprestado_simulado,
    limite_operacoes,
    nivel_acesso,
    setor_responsavel,
    data_designacao,
    status_administrativo,
    banco,
    agencia,
    numero_conta,
    tipo_conta,
    chave_pix
)
values
    (
        'Carla Admin Demo',
        'carla.admin.demo@loanflow.com',
        '10000000019',
        'ADMIN',
        'CASADO',
        'Brasileira',
        'Coordenador Operacional',
        '1986-04-12',
        '11990000001',
        'RG',
        '51000001',
        'SSP-SP',
        0,
        '01001-000',
        'Praca da Se',
        '100',
        'Sala 12',
        'Centro',
        'Sao Paulo',
        'SP',
        null,
        null,
        null,
        null,
        null,
        null,
        'ADMIN',
        'OPERACOES',
        dateadd(day, -120, @today),
        'ATIVO',
        null,
        null,
        null,
        null,
        null
    ),
    (
        'Bruno Credor Demo',
        'bruno.credor.demo@loanflow.com',
        '10000791989',
        'CREDOR',
        'SOLTEIRO',
        'Brasileira',
        'Investidor Independente',
        '1983-06-18',
        '11990000002',
        'RG',
        '51000002',
        'SSP-SP',
        0,
        '01310-100',
        'Avenida Paulista',
        '1200',
        'Conjunto 44',
        'Bela Vista',
        'Sao Paulo',
        'SP',
        null,
        null,
        null,
        120000.00,
        42000.00,
        12,
        null,
        null,
        null,
        null,
        'Banco do Brasil',
        '1001',
        '1000001',
        'CORRENTE',
        'bruno.credor.demo@loanflow.com'
    ),
    (
        'Alice Solicitante Demo',
        'alice.solicitante.demo@loanflow.com',
        '10001583816',
        'SOLICITANTE',
        'SOLTEIRO',
        'Brasileira',
        'Analista Financeiro',
        '1992-03-22',
        '11990000003',
        'RG',
        '51000003',
        'SSP-SP',
        0,
        '11010-010',
        'Rua do Comercio',
        '210',
        'Apto 32',
        'Centro',
        'Santos',
        'SP',
        6400.00,
        'CLT',
        82,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        'Nubank',
        '0001',
        '2000001',
        'PAGAMENTO',
        'alice.solicitante.demo@loanflow.com'
    ),
    (
        'Ana Paula Martins',
        'ana.paula.martins@loanflow.local',
        '10002375753',
        'SOLICITANTE',
        'CASADO',
        'Brasileira',
        'Microempreendedora',
        '1988-11-09',
        '11990000004',
        'CNH',
        '51000004',
        'DETRAN-SP',
        0,
        '11045-200',
        'Rua Alexandre Martins',
        '88',
        null,
        'Aparecida',
        'Santos',
        'SP',
        7200.00,
        'MEI',
        86,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        'Santander',
        '2020',
        '2000002',
        'CORRENTE',
        'ana.paula.martins@loanflow.local'
    ),
    (
        'Patricia Salles Monteiro',
        'patricia.s.monteiro@loanflow.local',
        '10010294708',
        'CREDOR',
        'UNIAO_ESTAVEL',
        'Brasileira',
        'Consultora Patrimonial',
        '1981-08-15',
        '11990000005',
        'RG',
        '51000005',
        'SSP-SP',
        0,
        '13025-010',
        'Rua Coronel Quirino',
        '415',
        'Sala 8',
        'Cambui',
        'Campinas',
        'SP',
        null,
        null,
        null,
        160000.00,
        58000.00,
        14,
        null,
        null,
        null,
        null,
        'Inter',
        '3030',
        '3000001',
        'CORRENTE',
        'patricia.s.monteiro@loanflow.local'
    ),
    (
        'Camila Rocha Nunes',
        'camila.rocha.nunes@loanflow.local',
        '10003959520',
        'SOLICITANTE',
        'SOLTEIRO',
        'Brasileira',
        'Tecnica em Enfermagem',
        '1990-09-03',
        '11990000006',
        'RG',
        '51000006',
        'SSP-SP',
        0,
        '11030-010',
        'Avenida Conselheiro Nebias',
        '745',
        null,
        'Boqueirao',
        'Santos',
        'SP',
        5200.00,
        'CLT',
        78,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        'Bradesco',
        '4040',
        '2000003',
        'CORRENTE',
        'camila.rocha.nunes@loanflow.local'
    ),
    (
        'Rafael Teixeira Moura',
        'rafael.t.moura@loanflow.local',
        '10009502807',
        'SOLICITANTE',
        'CASADO',
        'Brasileira',
        'Dono de Oficina',
        '1985-02-27',
        '11990000007',
        'CNH',
        '51000007',
        'DETRAN-SP',
        0,
        '11060-001',
        'Avenida Ana Costa',
        '301',
        'Loja 2',
        'Gonzaga',
        'Santos',
        'SP',
        8100.00,
        'EMPRESARIO',
        84,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        'C6 Bank',
        '5050',
        '2000004',
        'CORRENTE',
        'rafael.t.moura@loanflow.local'
    ),
    (
        'Vanessa Queiroz Braga',
        'vanessa.q.braga@loanflow.local',
        '10016629973',
        'CREDOR',
        'SOLTEIRO',
        'Brasileira',
        'Empresaria',
        '1979-12-01',
        '11990000008',
        'RG',
        '51000008',
        'SSP-SP',
        0,
        '13010-111',
        'Avenida Francisco Glicerio',
        '920',
        'Conjunto 18',
        'Centro',
        'Campinas',
        'SP',
        null,
        null,
        null,
        135000.00,
        36000.00,
        10,
        null,
        null,
        null,
        null,
        'Banco Safra',
        '6060',
        '3000002',
        'CORRENTE',
        'vanessa.q.braga@loanflow.local'
    );

update u
set
    nome = d.nome,
    cpf = d.cpf,
    senha_hash = @senha_hash,
    status = 'ATIVO',
    papel = d.papel,
    estado_civil = d.estado_civil,
    nacionalidade = d.nacionalidade,
    profissao = d.profissao,
    data_nascimento = d.data_nascimento,
    telefone = d.telefone,
    tipo_documento_identidade = d.tipo_documento_identidade,
    documento_identidade = d.documento_identidade,
    orgao_emissor = d.orgao_emissor,
    pessoa_exposta_politicamente = d.pessoa_exposta_politicamente,
    cep = d.cep,
    logradouro = d.logradouro,
    numero = d.numero,
    complemento = d.complemento,
    bairro = d.bairro,
    cidade = d.cidade,
    uf = d.uf,
    data_atualizacao = @now
from usuarios u
join @demo_users d on d.email = u.email;

insert into usuarios (
    nome,
    cpf,
    email,
    senha_hash,
    status,
    data_cadastro,
    data_atualizacao,
    papel,
    estado_civil,
    nacionalidade,
    profissao,
    data_nascimento,
    telefone,
    tipo_documento_identidade,
    documento_identidade,
    orgao_emissor,
    pessoa_exposta_politicamente,
    cep,
    logradouro,
    numero,
    complemento,
    bairro,
    cidade,
    uf
)
select
    d.nome,
    d.cpf,
    d.email,
    @senha_hash,
    'ATIVO',
    @now,
    @now,
    d.papel,
    d.estado_civil,
    d.nacionalidade,
    d.profissao,
    d.data_nascimento,
    d.telefone,
    d.tipo_documento_identidade,
    d.documento_identidade,
    d.orgao_emissor,
    d.pessoa_exposta_politicamente,
    d.cep,
    d.logradouro,
    d.numero,
    d.complemento,
    d.bairro,
    d.cidade,
    d.uf
from @demo_users d
where not exists (
    select 1
    from usuarios u
    where u.email = d.email
);

update sc
set
    renda_mensal = d.renda_mensal,
    tipo_ocupacao = d.tipo_ocupacao,
    score_credito_simulado = d.score_credito_simulado
from solicitantes_credito sc
join usuarios u on u.id = sc.usuario_id
join @demo_users d on d.email = u.email
where d.papel = 'SOLICITANTE';

insert into solicitantes_credito (usuario_id, renda_mensal, tipo_ocupacao, score_credito_simulado)
select
    u.id,
    d.renda_mensal,
    d.tipo_ocupacao,
    d.score_credito_simulado
from @demo_users d
join usuarios u on u.email = d.email
where d.papel = 'SOLICITANTE'
  and not exists (
      select 1
      from solicitantes_credito sc
      where sc.usuario_id = u.id
  );

update cr
set
    saldo_disponivel_simulado = d.saldo_disponivel_simulado,
    total_emprestado_simulado = d.total_emprestado_simulado,
    limite_operacoes = d.limite_operacoes
from credores cr
join usuarios u on u.id = cr.usuario_id
join @demo_users d on d.email = u.email
where d.papel = 'CREDOR';

insert into credores (usuario_id, saldo_disponivel_simulado, total_emprestado_simulado, limite_operacoes)
select
    u.id,
    d.saldo_disponivel_simulado,
    d.total_emprestado_simulado,
    d.limite_operacoes
from @demo_users d
join usuarios u on u.email = d.email
where d.papel = 'CREDOR'
  and not exists (
      select 1
      from credores cr
      where cr.usuario_id = u.id
  );

update ad
set
    nivel_acesso = d.nivel_acesso,
    setor_responsavel = d.setor_responsavel,
    data_designacao = d.data_designacao,
    status_administrativo = d.status_administrativo
from administradores ad
join usuarios u on u.id = ad.usuario_id
join @demo_users d on d.email = u.email
where d.papel = 'ADMIN';

insert into administradores (usuario_id, nivel_acesso, setor_responsavel, data_designacao, status_administrativo)
select
    u.id,
    d.nivel_acesso,
    d.setor_responsavel,
    d.data_designacao,
    d.status_administrativo
from @demo_users d
join usuarios u on u.email = d.email
where d.papel = 'ADMIN'
  and not exists (
      select 1
      from administradores ad
      where ad.usuario_id = u.id
  );

update cb
set
    banco = d.banco,
    agencia = d.agencia,
    numero_conta = d.numero_conta,
    tipo_conta = d.tipo_conta,
    chave_pix = d.chave_pix,
    data_atualizacao = @now
from contas_bancarias cb
join usuarios u on u.id = cb.usuario_id
join @demo_users d on d.email = u.email
where d.papel in ('SOLICITANTE', 'CREDOR');

insert into contas_bancarias (
    usuario_id,
    banco,
    agencia,
    numero_conta,
    tipo_conta,
    chave_pix,
    data_cadastro,
    data_atualizacao
)
select
    u.id,
    d.banco,
    d.agencia,
    d.numero_conta,
    d.tipo_conta,
    d.chave_pix,
    @now,
    @now
from @demo_users d
join usuarios u on u.email = d.email
where d.papel in ('SOLICITANTE', 'CREDOR')
  and not exists (
      select 1
      from contas_bancarias cb
      where cb.usuario_id = u.id
  );

declare @demo_propostas table (
    numero_proposta nvarchar(40) not null primary key,
    solicitante_email nvarchar(160) not null,
    credor_email nvarchar(160) null,
    valor_solicitado decimal(18, 2) not null,
    taxa_juros decimal(8, 4) not null,
    prazo_meses int not null,
    finalidade nvarchar(500) not null,
    categoria_finalidade nvarchar(40) not null,
    descricao_detalhada nvarchar(2000) not null,
    status nvarchar(40) not null,
    data_criacao datetime2 not null,
    data_atualizacao datetime2 not null,
    data_expiracao date not null
);

insert into @demo_propostas (
    numero_proposta,
    solicitante_email,
    credor_email,
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
values
    (
        'PPT-DEMO-FINAL-01',
        'alice.solicitante.demo@loanflow.com',
        'bruno.credor.demo@loanflow.com',
        7200.00,
        8.0000,
        6,
        'Tratamento odontologico e reserva familiar',
        'SAUDE',
        'Cenario final de apresentacao com contrato formalizado, parcelas pagas, parcela em atraso e parcelas abertas.',
        'CONTRATADA',
        dateadd(day, -75, @now),
        dateadd(day, -3, @now),
        dateadd(day, 20, @today)
    ),
    (
        'PPT-DEMO-FINAL-02',
        'ana.paula.martins@loanflow.local',
        'patricia.s.monteiro@loanflow.local',
        4800.00,
        6.5000,
        4,
        'Compra de equipamentos para producao local',
        'CAPITAL_DE_GIRO',
        'Cenario final de apresentacao com contrato quitado e historico completo de pagamentos.',
        'QUITADA',
        dateadd(day, -150, @now),
        dateadd(day, -18, @now),
        dateadd(day, -10, @today)
    ),
    (
        'PPT-DEMO-FINAL-03',
        'camila.rocha.nunes@loanflow.local',
        null,
        3900.00,
        7.2000,
        5,
        'Curso de especializacao profissional',
        'ESTUDO',
        'Proposta disponivel para aceite do credor durante a demonstracao.',
        'AGUARDANDO_ACEITE',
        dateadd(day, -2, @now),
        dateadd(day, -1, @now),
        dateadd(day, 6, @today)
    ),
    (
        'PPT-DEMO-FINAL-04',
        'rafael.t.moura@loanflow.local',
        'vanessa.q.braga@loanflow.local',
        8400.00,
        9.4000,
        8,
        'Ferramentas para oficina de bicicletas',
        'CAPITAL_DE_GIRO',
        'Proposta em analise para demonstrar o acompanhamento operacional antes da formalizacao.',
        'EM_ANALISE',
        dateadd(day, -8, @now),
        dateadd(day, -1, @now),
        dateadd(day, 5, @today)
    );

update p
set
    solicitante_id = sc.id,
    credor_id = cr.id,
    valor_solicitado = dp.valor_solicitado,
    taxa_juros = dp.taxa_juros,
    prazo_meses = dp.prazo_meses,
    finalidade = dp.finalidade,
    categoria_finalidade = dp.categoria_finalidade,
    descricao_detalhada = dp.descricao_detalhada,
    status = dp.status,
    data_criacao = dp.data_criacao,
    data_atualizacao = dp.data_atualizacao,
    data_expiracao = dp.data_expiracao
from propostas p
join @demo_propostas dp on dp.numero_proposta = p.numero_proposta
join usuarios su on su.email = dp.solicitante_email
join solicitantes_credito sc on sc.usuario_id = su.id
left join usuarios cu on cu.email = dp.credor_email
left join credores cr on cr.usuario_id = cu.id;

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
    dp.numero_proposta,
    sc.id,
    cr.id,
    dp.valor_solicitado,
    dp.taxa_juros,
    dp.prazo_meses,
    dp.finalidade,
    dp.categoria_finalidade,
    dp.descricao_detalhada,
    dp.status,
    dp.data_criacao,
    dp.data_atualizacao,
    dp.data_expiracao
from @demo_propostas dp
join usuarios su on su.email = dp.solicitante_email
join solicitantes_credito sc on sc.usuario_id = su.id
left join usuarios cu on cu.email = dp.credor_email
left join credores cr on cr.usuario_id = cu.id
where not exists (
    select 1
    from propostas p
    where p.numero_proposta = dp.numero_proposta
);

declare @demo_contratos table (
    numero_contrato nvarchar(40) not null primary key,
    numero_proposta nvarchar(40) not null,
    status nvarchar(40) not null,
    conteudo_snapshot nvarchar(max) not null,
    data_geracao datetime2 not null,
    data_formalizacao datetime2 not null
);

insert into @demo_contratos (
    numero_contrato,
    numero_proposta,
    status,
    conteudo_snapshot,
    data_geracao,
    data_formalizacao
)
values
    (
        'CTR-DEMO-FINAL-01',
        'PPT-DEMO-FINAL-01',
        'FORMALIZADO',
        'Contrato demonstrativo final com parcelas pagas, uma parcela em atraso e agenda futura aberta.',
        dateadd(day, -70, @now),
        dateadd(day, -69, @now)
    ),
    (
        'CTR-DEMO-FINAL-02',
        'PPT-DEMO-FINAL-02',
        'QUITADO',
        'Contrato demonstrativo final quitado com historico completo de pagamentos.',
        dateadd(day, -145, @now),
        dateadd(day, -144, @now)
    );

update co
set
    proposta_id = p.id,
    numero_contrato = dc.numero_contrato,
    status = dc.status,
    conteudo_snapshot = dc.conteudo_snapshot,
    pdf_path = null,
    hash_documento = convert(nvarchar(128), hashbytes('SHA2_256', concat(dc.numero_contrato, '-conteudo')), 2),
    hash_pdf_emitido = convert(nvarchar(128), hashbytes('SHA2_256', concat(dc.numero_contrato, '-pdf')), 2),
    data_geracao = dc.data_geracao,
    data_formalizacao = dc.data_formalizacao
from contratos co
join propostas p on p.id = co.proposta_id
join @demo_contratos dc on dc.numero_proposta = p.numero_proposta;

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
    dc.status,
    dc.conteudo_snapshot,
    null,
    convert(nvarchar(128), hashbytes('SHA2_256', concat(dc.numero_contrato, '-conteudo')), 2),
    convert(nvarchar(128), hashbytes('SHA2_256', concat(dc.numero_contrato, '-pdf')), 2),
    dc.data_geracao,
    dc.data_formalizacao
from @demo_contratos dc
join propostas p on p.numero_proposta = dc.numero_proposta
where not exists (
    select 1
    from contratos co
    where co.proposta_id = p.id
       or co.numero_contrato = dc.numero_contrato
);

declare @demo_parcelas table (
    numero_contrato nvarchar(40) not null,
    numero int not null,
    valor_previsto decimal(18, 2) not null,
    valor_pago_acumulado decimal(18, 2) not null,
    data_vencimento date not null,
    status nvarchar(40) not null,
    primary key (numero_contrato, numero)
);

insert into @demo_parcelas (
    numero_contrato,
    numero,
    valor_previsto,
    valor_pago_acumulado,
    data_vencimento,
    status
)
values
    ('CTR-DEMO-FINAL-01', 1, 1296.00, 1296.00, dateadd(day, 5, dateadd(month, -2, @current_month)), 'PAGA'),
    ('CTR-DEMO-FINAL-01', 2, 1296.00, 1296.00, dateadd(day, 5, dateadd(month, -1, @current_month)), 'PAGA'),
    ('CTR-DEMO-FINAL-01', 3, 1296.00, 0.00, dateadd(day, 5, @current_month), 'EM_ATRASO'),
    ('CTR-DEMO-FINAL-01', 4, 1296.00, 0.00, dateadd(day, 5, dateadd(month, 1, @current_month)), 'ABERTA'),
    ('CTR-DEMO-FINAL-01', 5, 1296.00, 0.00, dateadd(day, 5, dateadd(month, 2, @current_month)), 'ABERTA'),
    ('CTR-DEMO-FINAL-01', 6, 1296.00, 0.00, dateadd(day, 5, dateadd(month, 3, @current_month)), 'ABERTA'),
    ('CTR-DEMO-FINAL-02', 1, 1278.00, 1278.00, dateadd(day, 10, dateadd(month, -4, @current_month)), 'PAGA'),
    ('CTR-DEMO-FINAL-02', 2, 1278.00, 1278.00, dateadd(day, 10, dateadd(month, -3, @current_month)), 'PAGA'),
    ('CTR-DEMO-FINAL-02', 3, 1278.00, 1278.00, dateadd(day, 10, dateadd(month, -2, @current_month)), 'PAGA'),
    ('CTR-DEMO-FINAL-02', 4, 1278.00, 1278.00, dateadd(day, 10, dateadd(month, -1, @current_month)), 'PAGA');

update pa
set
    valor_previsto = dp.valor_previsto,
    valor_pago_acumulado = dp.valor_pago_acumulado,
    data_vencimento = dp.data_vencimento,
    status = dp.status
from parcelas pa
join contratos co on co.id = pa.contrato_id
join @demo_parcelas dp on dp.numero_contrato = co.numero_contrato
    and dp.numero = pa.numero;

insert into parcelas (
    contrato_id,
    numero,
    valor_previsto,
    valor_pago_acumulado,
    data_vencimento,
    status
)
select
    co.id,
    dp.numero,
    dp.valor_previsto,
    dp.valor_pago_acumulado,
    dp.data_vencimento,
    dp.status
from @demo_parcelas dp
join contratos co on co.numero_contrato = dp.numero_contrato
where not exists (
    select 1
    from parcelas pa
    where pa.contrato_id = co.id
      and pa.numero = dp.numero
);

declare @demo_pagamentos table (
    numero_contrato nvarchar(40) not null,
    numero_parcela int not null,
    comprovante nvarchar(500) not null primary key,
    data_hora_pagamento datetime2 not null
);

insert into @demo_pagamentos (
    numero_contrato,
    numero_parcela,
    comprovante,
    data_hora_pagamento
)
select
    dp.numero_contrato,
    dp.numero,
    concat('DEMO-FINAL-', dp.numero_contrato, '-P', right(concat('00', dp.numero), 2)),
    dateadd(hour, 10, cast(dp.data_vencimento as datetime2))
from @demo_parcelas dp
where dp.status = 'PAGA';

update pg
set
    parcela_id = pa.id,
    usuario_registrador_id = su.id,
    valor_pago = pa.valor_previsto,
    data_hora_pagamento = dpg.data_hora_pagamento,
    forma_pagamento = 'PIX_MANUAL',
    status = 'REGISTRADO',
    comprovante = dpg.comprovante
from pagamentos pg
join @demo_pagamentos dpg on dpg.comprovante = pg.comprovante
join contratos co on co.numero_contrato = dpg.numero_contrato
join parcelas pa on pa.contrato_id = co.id and pa.numero = dpg.numero_parcela
join propostas pr on pr.id = co.proposta_id
join solicitantes_credito sc on sc.id = pr.solicitante_id
join usuarios su on su.id = sc.usuario_id;

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
    su.id,
    pa.valor_previsto,
    dpg.data_hora_pagamento,
    'PIX_MANUAL',
    'REGISTRADO',
    dpg.comprovante
from @demo_pagamentos dpg
join contratos co on co.numero_contrato = dpg.numero_contrato
join parcelas pa on pa.contrato_id = co.id and pa.numero = dpg.numero_parcela
join propostas pr on pr.id = co.proposta_id
join solicitantes_credito sc on sc.id = pr.solicitante_id
join usuarios su on su.id = sc.usuario_id
where not exists (
    select 1
    from pagamentos pg
    where pg.comprovante = dpg.comprovante
);

update co
set status = dc.status
from contratos co
join @demo_contratos dc on dc.numero_contrato = co.numero_contrato;

update p
set status = dp.status
from propostas p
join @demo_propostas dp on dp.numero_proposta = p.numero_proposta;

;with proposta_eventos as (
    select
        su.id as usuario_id,
        p.id as referencia_id,
        'PROPOSTA' as tipo,
        case p.numero_proposta
            when 'PPT-DEMO-FINAL-01' then 'Sua proposta foi aceita e contratada automaticamente.'
            when 'PPT-DEMO-FINAL-02' then 'Sua proposta foi quitada apos pagamento integral.'
            when 'PPT-DEMO-FINAL-03' then 'Sua proposta esta disponivel para aceite dos credores.'
            when 'PPT-DEMO-FINAL-04' then 'Sua proposta esta em analise pelo credor.'
        end as mensagem,
        dateadd(hour, -6, @now) as data_envio
    from propostas p
    join solicitantes_credito sc on sc.id = p.solicitante_id
    join usuarios su on su.id = sc.usuario_id
    where p.numero_proposta in (
        'PPT-DEMO-FINAL-01',
        'PPT-DEMO-FINAL-02',
        'PPT-DEMO-FINAL-03',
        'PPT-DEMO-FINAL-04'
    )

    union all

    select
        cu.id as usuario_id,
        p.id as referencia_id,
        'PROPOSTA' as tipo,
        case p.numero_proposta
            when 'PPT-DEMO-FINAL-01' then 'Proposta aceita e formalizada automaticamente.'
            when 'PPT-DEMO-FINAL-02' then 'Contrato quitado apos pagamento integral.'
            when 'PPT-DEMO-FINAL-04' then 'Proposta em analise aguardando decisao do credor.'
        end as mensagem,
        dateadd(hour, -5, @now) as data_envio
    from propostas p
    join credores cr on cr.id = p.credor_id
    join usuarios cu on cu.id = cr.usuario_id
    where p.numero_proposta in (
        'PPT-DEMO-FINAL-01',
        'PPT-DEMO-FINAL-02',
        'PPT-DEMO-FINAL-04'
    )
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
    pe.tipo,
    pe.mensagem,
    pe.data_envio,
    0,
    'Proposta',
    pe.referencia_id
from proposta_eventos pe
where pe.mensagem is not null
  and not exists (
      select 1
      from notificacoes n
      where n.usuario_id = pe.usuario_id
        and n.tipo = pe.tipo
        and n.referencia_tipo = 'Proposta'
        and n.referencia_id = pe.referencia_id
        and n.mensagem = pe.mensagem
  );

;with pagamento_destinatarios as (
    select pg.id as pagamento_id, su.id as usuario_id
    from pagamentos pg
    join @demo_pagamentos dpg on dpg.comprovante = pg.comprovante
    join parcelas pa on pa.id = pg.parcela_id
    join contratos co on co.id = pa.contrato_id
    join propostas pr on pr.id = co.proposta_id
    join solicitantes_credito sc on sc.id = pr.solicitante_id
    join usuarios su on su.id = sc.usuario_id

    union

    select pg.id as pagamento_id, cu.id as usuario_id
    from pagamentos pg
    join @demo_pagamentos dpg on dpg.comprovante = pg.comprovante
    join parcelas pa on pa.id = pg.parcela_id
    join contratos co on co.id = pa.contrato_id
    join propostas pr on pr.id = co.proposta_id
    join credores cr on cr.id = pr.credor_id
    join usuarios cu on cu.id = cr.usuario_id

    union

    select pg.id as pagamento_id, au.id as usuario_id
    from pagamentos pg
    join @demo_pagamentos dpg on dpg.comprovante = pg.comprovante
    join usuarios au on au.email = 'carla.admin.demo@loanflow.com'
),
pagamento_eventos as (
    select
        pd.usuario_id,
        pa.id as parcela_id,
        concat('Pagamento registrado na parcela ', pa.numero, ' do contrato ', co.numero_contrato, '.') as mensagem,
        pg.data_hora_pagamento as data_envio
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

;with contrato_destinatarios as (
    select co.id as contrato_id, su.id as usuario_id
    from contratos co
    join propostas pr on pr.id = co.proposta_id
    join solicitantes_credito sc on sc.id = pr.solicitante_id
    join usuarios su on su.id = sc.usuario_id
    where co.numero_contrato in ('CTR-DEMO-FINAL-01', 'CTR-DEMO-FINAL-02')

    union

    select co.id as contrato_id, cu.id as usuario_id
    from contratos co
    join propostas pr on pr.id = co.proposta_id
    join credores cr on cr.id = pr.credor_id
    join usuarios cu on cu.id = cr.usuario_id
    where co.numero_contrato in ('CTR-DEMO-FINAL-01', 'CTR-DEMO-FINAL-02')

    union

    select co.id as contrato_id, au.id as usuario_id
    from contratos co
    join usuarios au on au.email = 'carla.admin.demo@loanflow.com'
    where co.numero_contrato in ('CTR-DEMO-FINAL-01', 'CTR-DEMO-FINAL-02')
),
contrato_eventos as (
    select
        cd.usuario_id,
        co.id as contrato_id,
        case co.numero_contrato
            when 'CTR-DEMO-FINAL-01' then 'Contrato formalizado automaticamente e disponivel para consulta.'
            when 'CTR-DEMO-FINAL-02' then 'Contrato quitado apos pagamento integral.'
        end as mensagem,
        coalesce(co.data_formalizacao, co.data_geracao, @now) as data_envio
    from contratos co
    join contrato_destinatarios cd on cd.contrato_id = co.id
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
where ce.mensagem is not null
  and not exists (
      select 1
      from notificacoes n
      where n.usuario_id = ce.usuario_id
        and n.tipo = 'CONTRATO'
        and n.referencia_tipo = 'Contrato'
        and n.referencia_id = ce.contrato_id
        and n.mensagem = ce.mensagem
  );

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
    u.id,
    'SISTEMA',
    'Base de apresentacao final preparada com usuarios, propostas, contratos, parcelas, pagamentos, notificacoes e auditoria.',
    @now,
    0,
    'Sistema',
    null
from usuarios u
where u.email = 'carla.admin.demo@loanflow.com'
  and not exists (
      select 1
      from notificacoes n
      where n.usuario_id = u.id
        and n.tipo = 'SISTEMA'
        and n.referencia_tipo = 'Sistema'
        and n.mensagem = 'Base de apresentacao final preparada com usuarios, propostas, contratos, parcelas, pagamentos, notificacoes e auditoria.'
  );

declare @auditoria_plan table (
    usuario_email nvarchar(160) not null,
    acao nvarchar(50) not null,
    entidade_tipo nvarchar(80) not null,
    entidade_chave nvarchar(500) not null,
    detalhes nvarchar(max) not null,
    data_hora datetime2 not null,
    ip_origem nvarchar(80) not null
);

insert into @auditoria_plan (
    usuario_email,
    acao,
    entidade_tipo,
    entidade_chave,
    detalhes,
    data_hora,
    ip_origem
)
values
    ('alice.solicitante.demo@loanflow.com', 'CRIAR', 'Proposta', 'PPT-DEMO-FINAL-01', 'Proposta criada para cenario final de apresentacao.', dateadd(day, -75, @now), '127.0.0.1'),
    ('bruno.credor.demo@loanflow.com', 'ACEITAR', 'Proposta', 'PPT-DEMO-FINAL-01', 'Credor aceitou a proposta no cenario final.', dateadd(day, -70, @now), '127.0.0.1'),
    ('bruno.credor.demo@loanflow.com', 'GERAR_CONTRATO', 'Contrato', 'CTR-DEMO-FINAL-01', 'Contrato gerado com hash SHA-256 no cenario final.', dateadd(day, -70, @now), '127.0.0.1'),
    ('bruno.credor.demo@loanflow.com', 'FORMALIZAR', 'Contrato', 'CTR-DEMO-FINAL-01', 'Contrato formalizado automaticamente apos aceite do credor.', dateadd(day, -69, @now), '127.0.0.1'),
    ('bruno.credor.demo@loanflow.com', 'GERAR_PARCELAS', 'Contrato', 'CTR-DEMO-FINAL-01', 'Parcelas geradas para o contrato final.', dateadd(day, -69, @now), '127.0.0.1'),
    ('alice.solicitante.demo@loanflow.com', 'REGISTRAR_PAGAMENTO', 'Parcela', 'CTR-DEMO-FINAL-01:1', 'Pagamento manual registrado na parcela 1.', dateadd(day, -50, @now), '127.0.0.1'),
    ('alice.solicitante.demo@loanflow.com', 'REGISTRAR_PAGAMENTO', 'Parcela', 'CTR-DEMO-FINAL-01:2', 'Pagamento manual registrado na parcela 2.', dateadd(day, -20, @now), '127.0.0.1'),
    ('ana.paula.martins@loanflow.local', 'CRIAR', 'Proposta', 'PPT-DEMO-FINAL-02', 'Proposta criada para cenario de contrato quitado.', dateadd(day, -150, @now), '127.0.0.1'),
    ('patricia.s.monteiro@loanflow.local', 'ACEITAR', 'Proposta', 'PPT-DEMO-FINAL-02', 'Credor aceitou a proposta quitada no cenario final.', dateadd(day, -145, @now), '127.0.0.1'),
    ('patricia.s.monteiro@loanflow.local', 'FORMALIZAR', 'Contrato', 'CTR-DEMO-FINAL-02', 'Contrato formalizado automaticamente apos aceite do credor.', dateadd(day, -144, @now), '127.0.0.1'),
    ('ana.paula.martins@loanflow.local', 'REGISTRAR_PAGAMENTO', 'Parcela', 'CTR-DEMO-FINAL-02:1', 'Pagamento manual registrado na parcela 1.', dateadd(day, -120, @now), '127.0.0.1'),
    ('ana.paula.martins@loanflow.local', 'REGISTRAR_PAGAMENTO', 'Parcela', 'CTR-DEMO-FINAL-02:2', 'Pagamento manual registrado na parcela 2.', dateadd(day, -90, @now), '127.0.0.1'),
    ('ana.paula.martins@loanflow.local', 'REGISTRAR_PAGAMENTO', 'Parcela', 'CTR-DEMO-FINAL-02:3', 'Pagamento manual registrado na parcela 3.', dateadd(day, -60, @now), '127.0.0.1'),
    ('ana.paula.martins@loanflow.local', 'REGISTRAR_PAGAMENTO', 'Parcela', 'CTR-DEMO-FINAL-02:4', 'Pagamento manual registrado na parcela 4.', dateadd(day, -30, @now), '127.0.0.1'),
    ('ana.paula.martins@loanflow.local', 'ATUALIZAR', 'Contrato', 'CTR-DEMO-FINAL-02', 'Contrato quitado apos pagamento integral.', dateadd(day, -29, @now), '127.0.0.1'),
    ('camila.rocha.nunes@loanflow.local', 'CRIAR', 'Proposta', 'PPT-DEMO-FINAL-03', 'Proposta criada aguardando aceite de credor.', dateadd(day, -2, @now), '127.0.0.1'),
    ('rafael.t.moura@loanflow.local', 'CRIAR', 'Proposta', 'PPT-DEMO-FINAL-04', 'Proposta criada para analise operacional.', dateadd(day, -8, @now), '127.0.0.1'),
    ('vanessa.q.braga@loanflow.local', 'INICIAR_ANALISE', 'Proposta', 'PPT-DEMO-FINAL-04', 'Credor iniciou analise da proposta.', dateadd(day, -1, @now), '127.0.0.1'),
    ('carla.admin.demo@loanflow.com', 'ATUALIZAR', 'Usuario', 'carla.admin.demo@loanflow.com', 'Base final de apresentacao revisada pelo administrador.', @now, '127.0.0.1');

insert into auditorias (
    usuario_id,
    acao,
    entidade_tipo,
    entidade_id,
    data_hora,
    ip_origem,
    detalhes
)
select
    u.id,
    ap.acao,
    ap.entidade_tipo,
    case ap.entidade_tipo
        when 'Usuario' then eu.id
        when 'Proposta' then ep.id
        when 'Contrato' then ec.id
        when 'Parcela' then epa.id
        else null
    end,
    ap.data_hora,
    ap.ip_origem,
    ap.detalhes
from @auditoria_plan ap
join usuarios u on u.email = ap.usuario_email
left join usuarios eu on ap.entidade_tipo = 'Usuario'
    and eu.email = ap.entidade_chave
left join propostas ep on ap.entidade_tipo = 'Proposta'
    and ep.numero_proposta = ap.entidade_chave
left join contratos ec on ap.entidade_tipo = 'Contrato'
    and ec.numero_contrato = ap.entidade_chave
left join contratos ecpa on ap.entidade_tipo = 'Parcela'
    and ecpa.numero_contrato = left(ap.entidade_chave, charindex(':', ap.entidade_chave + ':') - 1)
left join parcelas epa on ap.entidade_tipo = 'Parcela'
    and epa.contrato_id = ecpa.id
    and epa.numero = try_convert(int, substring(ap.entidade_chave, charindex(':', ap.entidade_chave + ':') + 1, 10))
where not exists (
    select 1
    from auditorias a
    where a.usuario_id = u.id
      and a.acao = ap.acao
      and a.entidade_tipo = ap.entidade_tipo
      and (
          a.entidade_id = case ap.entidade_tipo
              when 'Usuario' then eu.id
              when 'Proposta' then ep.id
              when 'Contrato' then ec.id
              when 'Parcela' then epa.id
              else null
          end
          or (a.entidade_id is null and case ap.entidade_tipo
              when 'Usuario' then eu.id
              when 'Proposta' then ep.id
              when 'Contrato' then ec.id
              when 'Parcela' then epa.id
              else null
          end is null)
      )
      and a.detalhes = ap.detalhes
);
