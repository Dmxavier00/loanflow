declare @now datetime2 = sysdatetime();
declare @senha_hash nvarchar(120) = '$2a$10$M5ULlPXkYedH.FBsNF2Kqemgb8G/pAzMBpfwJZdBK8AhDduvPHGaO';

declare @demo_users table (
    seed_ord int not null primary key,
    nome nvarchar(120) not null,
    email nvarchar(160) not null,
    cpf nvarchar(14) not null,
    papel nvarchar(30) not null
);

insert into @demo_users (seed_ord, nome, email, cpf, papel)
values
    (1, 'Carla Admin Demo', 'carla.admin.demo@loanflow.com', '10000000019', 'ADMIN'),
    (2, 'Bruno Credor Demo', 'bruno.credor.demo@loanflow.com', '10000791989', 'CREDOR'),
    (3, 'Alice Solicitante Demo', 'alice.solicitante.demo@loanflow.com', '10001583816', 'SOLICITANTE'),
    (4, 'Ana Paula Martins', 'ana.paula.martins@loanflow.local', '10002375753', 'SOLICITANTE'),
    (5, 'Bruno Henrique Lopes', 'bruno.h.lopes@loanflow.local', '10003167690', 'SOLICITANTE'),
    (6, 'Camila Rocha Nunes', 'camila.rocha.nunes@loanflow.local', '10003959520', 'SOLICITANTE'),
    (7, 'Diego Fernandes Alves', 'diego.f.alves@loanflow.local', '10004751400', 'SOLICITANTE'),
    (8, 'Fernanda Almeida Costa', 'fernanda.a.costa@loanflow.local', '10005543339', 'SOLICITANTE'),
    (9, 'Gustavo Ribeiro Lima', 'gustavo.r.lima@loanflow.local', '10006335276', 'SOLICITANTE'),
    (10, 'Juliana Barbosa Melo', 'juliana.b.melo@loanflow.local', '10007127103', 'SOLICITANTE'),
    (11, 'Leandro Carvalho Pinto', 'leandro.c.pinto@loanflow.local', '10007919042', 'SOLICITANTE'),
    (12, 'Mariana Duarte Gomes', 'mariana.d.gomes@loanflow.local', '10008710961', 'SOLICITANTE'),
    (13, 'Rafael Teixeira Moura', 'rafael.t.moura@loanflow.local', '10009502807', 'SOLICITANTE'),
    (14, 'Patricia Salles Monteiro', 'patricia.s.monteiro@loanflow.local', '10010294708', 'CREDOR'),
    (15, 'Ricardo Nogueira Campos', 'ricardo.n.campos@loanflow.local', '10011086637', 'CREDOR'),
    (16, 'Tatiane Farias Pinto', 'tatiane.f.pinto@loanflow.local', '10011878576', 'CREDOR'),
    (17, 'Eduardo Bastos Freire', 'eduardo.b.freire@loanflow.local', '10012670448', 'CREDOR'),
    (18, 'Luciana Azevedo Prado', 'luciana.a.prado@loanflow.local', '10013462385', 'CREDOR'),
    (19, 'Marcelo Vieira Cunha', 'marcelo.v.cunha@loanflow.local', '10014254212', 'CREDOR'),
    (20, 'Silvia Moreira Teles', 'silvia.m.teles@loanflow.local', '10015046150', 'CREDOR'),
    (21, 'Thiago Rezende Amaral', 'thiago.r.amaral@loanflow.local', '10015838099', 'CREDOR'),
    (22, 'Vanessa Queiroz Braga', 'vanessa.q.braga@loanflow.local', '10016629973', 'CREDOR'),
    (23, 'Walter Pires Andrade', 'walter.p.andrade@loanflow.local', '10017421845', 'CREDOR'),
    (24, 'Carla Menezes Duarte', 'carla.m.duarte@loanflow.local', '10018213782', 'ADMIN'),
    (25, 'Fabio Martins Queiroga', 'fabio.m.queiroga@loanflow.local', '10019005610', 'ADMIN'),
    (26, 'Helena Souto Barros', 'helena.s.barros@loanflow.local', '10019797532', 'ADMIN'),
    (27, 'Igor Almeida Tavares', 'igor.a.tavares@loanflow.local', '10020589450', 'ADMIN'),
    (28, 'Renata Borges Vidal', 'renata.b.vidal@loanflow.local', '10021381321', 'ADMIN');

declare @demo_users_enriched table (
    seed_ord int not null primary key,
    nome nvarchar(120) not null,
    email nvarchar(160) not null,
    cpf nvarchar(14) not null,
    papel nvarchar(30) not null,
    senha_hash nvarchar(120) not null,
    status nvarchar(30) not null,
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

insert into @demo_users_enriched (
    seed_ord,
    nome,
    email,
    cpf,
    papel,
    senha_hash,
    status,
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
select
    d.seed_ord,
    d.nome,
    d.email,
    d.cpf,
    d.papel,
    @senha_hash,
    'ATIVO',
    case
        when d.seed_ord % 5 = 0 then 'CASADO'
        when d.seed_ord % 3 = 0 then 'UNIAO_ESTAVEL'
        else 'SOLTEIRO'
    end,
    'Brasileira',
    case d.papel
        when 'SOLICITANTE' then 'Analista Financeiro'
        when 'CREDOR' then 'Investidor Independente'
        else 'Coordenador Operacional'
    end,
    datefromparts(1984 + (d.seed_ord % 12), 1 + (d.seed_ord % 12), 1 + ((d.seed_ord * 3) % 28)),
    concat('1199', right(concat('0000', d.seed_ord), 4), right(concat('0000', d.seed_ord + 4200), 4)),
    'RG',
    right(concat('00000000', d.seed_ord * 731), 8),
    'SSP-SP',
    cast(0 as bit),
    concat('0100', ((d.seed_ord - 1) % 9) + 1, '-', right(concat('000', d.seed_ord), 3)),
    concat('Rua LoanFlow Demo ', d.seed_ord),
    cast(100 + d.seed_ord as nvarchar(20)),
    case when d.seed_ord % 4 = 0 then concat('Sala ', d.seed_ord) else null end,
    'Centro',
    case
        when d.papel = 'CREDOR' then 'Campinas'
        when d.papel = 'ADMIN' then 'Sao Paulo'
        else 'Santos'
    end,
    'SP',
    case when d.papel = 'SOLICITANTE' then cast(3000 + (d.seed_ord * 175) as decimal(18, 2)) end,
    case when d.papel = 'SOLICITANTE' then case when d.seed_ord % 2 = 0 then 'CLT' else 'AUTONOMO' end end,
    case when d.papel = 'SOLICITANTE' then 640 + (d.seed_ord % 90) end,
    case when d.papel = 'CREDOR' then cast(25000 + (d.seed_ord * 2500) as decimal(18, 2)) end,
    case when d.papel = 'CREDOR' then cast((d.seed_ord % 4) * 1500 as decimal(18, 2)) end,
    case when d.papel = 'CREDOR' then 10 + (d.seed_ord % 4) end,
    case when d.papel = 'ADMIN' then 'ADMIN' end,
    case when d.papel = 'ADMIN' then 'OPERACOES' end,
    case when d.papel = 'ADMIN' then dateadd(day, -d.seed_ord, cast(@now as date)) end,
    case when d.papel = 'ADMIN' then 'ATIVO' end,
    case
        when d.papel in ('SOLICITANTE', 'CREDOR') then case d.seed_ord % 6
            when 0 then 'Banco do Brasil'
            when 1 then 'Bradesco'
            when 2 then 'Santander'
            when 3 then 'Nubank'
            when 4 then 'Inter'
            else 'C6 Bank'
        end
    end,
    case when d.papel in ('SOLICITANTE', 'CREDOR') then right(concat('0000', 1000 + d.seed_ord), 4) end,
    case when d.papel in ('SOLICITANTE', 'CREDOR') then concat('10', right(concat('000000', d.seed_ord * 37), 6)) end,
    case
        when d.papel in ('SOLICITANTE', 'CREDOR') then case d.seed_ord % 3
            when 0 then 'PAGAMENTO'
            when 1 then 'CORRENTE'
            else 'POUPANCA'
        end
    end,
    case when d.papel in ('SOLICITANTE', 'CREDOR') then d.email end
from @demo_users d;

update u
set
    nome = d.nome,
    cpf = d.cpf,
    email = d.email,
    senha_hash = d.senha_hash,
    status = d.status,
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
join @demo_users_enriched d on d.email = u.email;

insert into usuarios (
    nome,
    cpf,
    email,
    senha_hash,
    status,
    data_cadastro,
    data_atualizacao,
    estado_civil,
    nacionalidade,
    profissao,
    cep,
    logradouro,
    numero,
    complemento,
    bairro,
    cidade,
    uf,
    data_nascimento,
    telefone,
    tipo_documento_identidade,
    documento_identidade,
    orgao_emissor,
    pessoa_exposta_politicamente
)
select
    d.nome,
    d.cpf,
    d.email,
    d.senha_hash,
    d.status,
    @now,
    @now,
    d.estado_civil,
    d.nacionalidade,
    d.profissao,
    d.cep,
    d.logradouro,
    d.numero,
    d.complemento,
    d.bairro,
    d.cidade,
    d.uf,
    d.data_nascimento,
    d.telefone,
    d.tipo_documento_identidade,
    d.documento_identidade,
    d.orgao_emissor,
    d.pessoa_exposta_politicamente
from @demo_users_enriched d
where not exists (
    select 1
    from usuarios u
    where u.email = d.email
);

insert into usuario_papeis (usuario_id, papel)
select
    u.id,
    d.papel
from usuarios u
join @demo_users_enriched d on d.email = u.email
where not exists (
    select 1
    from usuario_papeis up
    where up.usuario_id = u.id
      and up.papel = d.papel
);

update s
set
    renda_mensal = d.renda_mensal,
    tipo_ocupacao = d.tipo_ocupacao,
    score_credito_simulado = d.score_credito_simulado
from solicitantes_credito s
join usuarios u on u.id = s.usuario_id
join @demo_users_enriched d on d.email = u.email
where d.papel = 'SOLICITANTE';

insert into solicitantes_credito (usuario_id, renda_mensal, tipo_ocupacao, score_credito_simulado)
select
    u.id,
    d.renda_mensal,
    d.tipo_ocupacao,
    d.score_credito_simulado
from usuarios u
join @demo_users_enriched d on d.email = u.email
where d.papel = 'SOLICITANTE'
  and not exists (
      select 1
      from solicitantes_credito s
      where s.usuario_id = u.id
  );

update c
set
    saldo_disponivel_simulado = d.saldo_disponivel_simulado,
    total_emprestado_simulado = d.total_emprestado_simulado,
    limite_operacoes = d.limite_operacoes
from credores c
join usuarios u on u.id = c.usuario_id
join @demo_users_enriched d on d.email = u.email
where d.papel = 'CREDOR';

insert into credores (usuario_id, saldo_disponivel_simulado, total_emprestado_simulado, limite_operacoes)
select
    u.id,
    d.saldo_disponivel_simulado,
    d.total_emprestado_simulado,
    d.limite_operacoes
from usuarios u
join @demo_users_enriched d on d.email = u.email
where d.papel = 'CREDOR'
  and not exists (
      select 1
      from credores c
      where c.usuario_id = u.id
  );

update a
set
    nivel_acesso = d.nivel_acesso,
    setor_responsavel = d.setor_responsavel,
    data_designacao = d.data_designacao,
    status_administrativo = d.status_administrativo
from administradores a
join usuarios u on u.id = a.usuario_id
join @demo_users_enriched d on d.email = u.email
where d.papel = 'ADMIN';

insert into administradores (usuario_id, nivel_acesso, setor_responsavel, data_designacao, status_administrativo)
select
    u.id,
    d.nivel_acesso,
    d.setor_responsavel,
    d.data_designacao,
    d.status_administrativo
from usuarios u
join @demo_users_enriched d on d.email = u.email
where d.papel = 'ADMIN'
  and not exists (
      select 1
      from administradores a
      where a.usuario_id = u.id
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
join @demo_users_enriched d on d.email = u.email
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
from usuarios u
join @demo_users_enriched d on d.email = u.email
where d.papel in ('SOLICITANTE', 'CREDOR')
  and not exists (
      select 1
      from contas_bancarias cb
      where cb.usuario_id = u.id
  );
