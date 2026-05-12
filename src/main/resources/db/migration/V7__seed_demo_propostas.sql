declare @now datetime2 = sysdatetime();

declare @demo_propostas table (
    solicitante_email nvarchar(160) not null primary key,
    credor_email nvarchar(160) null,
    valor_solicitado decimal(18, 2) not null,
    taxa_juros decimal(8, 4) not null,
    prazo_meses int not null,
    finalidade nvarchar(160) not null,
    categoria_finalidade nvarchar(40) not null,
    descricao_detalhada nvarchar(2000) not null,
    status nvarchar(40) not null,
    dias_desde_criacao int not null,
    dias_desde_atualizacao int not null,
    dias_para_expiracao int not null
);

insert into @demo_propostas (
    solicitante_email,
    credor_email,
    valor_solicitado,
    taxa_juros,
    prazo_meses,
    finalidade,
    categoria_finalidade,
    descricao_detalhada,
    status,
    dias_desde_criacao,
    dias_desde_atualizacao,
    dias_para_expiracao
)
values
    (
        'alice.solicitante.demo@loanflow.com',
        null,
        4800.00,
        8.9000,
        6,
        'Tratamento odontologico com implante e exames',
        'SAUDE',
        'Solicita credito para cobrir implante dentario, radiografias e retorno clinico, mantendo parcelas dentro de uma faixa compativel com a renda declarada.',
        'AGUARDANDO_ACEITE',
        -2,
        -1,
        5
    ),
    (
        'ana.paula.martins@loanflow.local',
        'patricia.s.monteiro@loanflow.local',
        9200.00,
        13.2000,
        12,
        'Reforma da cozinha e troca do piso ceramico',
        'REFORMA',
        'Pretende corrigir infiltracao, substituir bancada e trocar o revestimento da cozinha de um apartamento antigo em Santos.',
        'ACEITA',
        -6,
        -3,
        4
    ),
    (
        'bruno.h.lopes@loanflow.local',
        'ricardo.n.campos@loanflow.local',
        6800.00,
        10.5000,
        8,
        'Quitacao de cartao e cheque especial em parcela unica',
        'QUITACAO_DE_DIVIDAS',
        'Busca consolidar saldo rotativo de cartao e cheque especial em uma unica parcela mensal mais previsivel.',
        'APROVADA',
        -8,
        -2,
        3
    ),
    (
        'camila.rocha.nunes@loanflow.local',
        'tatiane.f.pinto@loanflow.local',
        5400.00,
        7.9000,
        6,
        'Curso de pos-graduacao e licencas de software',
        'ESTUDO',
        'Vai usar o valor para matricula de pos-graduacao em analise de dados, compra de livros e assinatura anual de ferramentas de estudo.',
        'EM_ANALISE',
        -5,
        -1,
        6
    ),
    (
        'diego.f.alves@loanflow.local',
        'eduardo.b.freire@loanflow.local',
        3100.00,
        11.4000,
        4,
        'Troca de embreagem e pneus do carro de trabalho',
        'EMERGENCIA',
        'Precisa reparar o veiculo usado para visitas comerciais, incluindo embreagem, dois pneus e alinhamento.',
        'SUBMETIDA',
        -3,
        -1,
        4
    ),
    (
        'fernanda.a.costa@loanflow.local',
        'luciana.a.prado@loanflow.local',
        7900.00,
        15.8000,
        10,
        'Capital de giro para ampliar producao de marmitas fitness',
        'CAPITAL_DE_GIRO',
        'Pretendia comprar freezer vertical, embalagens e reforcar estoque de proteinas para vender marmitas sob encomenda.',
        'REJEITADA',
        -9,
        -2,
        2
    ),
    (
        'gustavo.r.lima@loanflow.local',
        null,
        4500.00,
        9.7000,
        5,
        'Reparo do telhado e impermeabilizacao da laje',
        'REFORMA',
        'O valor seria usado para trocar telhas quebradas, refazer rufos e aplicar manta impermeabilizante antes do periodo de chuva.',
        'CANCELADA',
        -7,
        -1,
        5
    ),
    (
        'juliana.b.melo@loanflow.local',
        null,
        8700.00,
        12.6000,
        9,
        'Montagem de estudio fotografico domestico',
        'OUTRA',
        'Deseja adquirir camera usada, lente retrato, dois pontos de luz e fundo infinito para ampliar renda com ensaios em casa.',
        'AGUARDANDO_ACEITE',
        -1,
        -1,
        7
    ),
    (
        'leandro.c.pinto@loanflow.local',
        null,
        5200.00,
        10.9000,
        7,
        'Quitacao de parcelas atrasadas e reorganizacao do orcamento',
        'QUITACAO_DE_DIVIDAS',
        'A proposta serviria para liquidar duas parcelas atrasadas e substituir custos altos por um cronograma unico e previsivel.',
        'EXPIRADA',
        -10,
        -8,
        -1
    ),
    (
        'mariana.d.gomes@loanflow.local',
        null,
        7600.00,
        8.4000,
        10,
        'Semestre final da faculdade e material didatico',
        'ESTUDO',
        'Quer cobrir mensalidades do semestre final, taxa de conclusao e compra de um notebook basico para o TCC.',
        'AGUARDANDO_ACEITE',
        -2,
        0,
        6
    ),
    (
        'rafael.t.moura@loanflow.local',
        'vanessa.q.braga@loanflow.local',
        9800.00,
        14.1000,
        12,
        'Compra de ferramentas para oficina de bicicletas',
        'CAPITAL_DE_GIRO',
        'Vai investir em bancada, jogo de ferramentas, compressor pequeno e pecas de giro rapido para ampliar atendimentos locais.',
        'ACEITA',
        -4,
        -1,
        5
    );

update p
set
    credor_id = c.id,
    valor_solicitado = d.valor_solicitado,
    taxa_juros = d.taxa_juros,
    prazo_meses = d.prazo_meses,
    finalidade = d.finalidade,
    categoria_finalidade = d.categoria_finalidade,
    descricao_detalhada = d.descricao_detalhada,
    status = d.status,
    data_criacao = dateadd(day, d.dias_desde_criacao, @now),
    data_atualizacao = dateadd(day, d.dias_desde_atualizacao, @now),
    data_expiracao = cast(dateadd(day, d.dias_para_expiracao, @now) as date)
from propostas p
join solicitantes_credito s on s.id = p.solicitante_id
join usuarios su on su.id = s.usuario_id
join @demo_propostas d on d.solicitante_email = su.email
left join usuarios cu on cu.email = d.credor_email
left join credores c on c.usuario_id = cu.id
where p.finalidade = d.finalidade;

insert into propostas (
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
    s.id,
    c.id,
    d.valor_solicitado,
    d.taxa_juros,
    d.prazo_meses,
    d.finalidade,
    d.categoria_finalidade,
    d.descricao_detalhada,
    d.status,
    dateadd(day, d.dias_desde_criacao, @now),
    dateadd(day, d.dias_desde_atualizacao, @now),
    cast(dateadd(day, d.dias_para_expiracao, @now) as date)
from @demo_propostas d
join usuarios su on su.email = d.solicitante_email
join solicitantes_credito s on s.usuario_id = su.id
left join usuarios cu on cu.email = d.credor_email
left join credores c on c.usuario_id = cu.id
where not exists (
    select 1
    from propostas p
    where p.solicitante_id = s.id
      and p.finalidade = d.finalidade
);
