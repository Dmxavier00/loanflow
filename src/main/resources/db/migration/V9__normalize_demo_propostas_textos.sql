declare @demo_textos table (
    solicitante_email nvarchar(160) not null,
    finalidade_antiga nvarchar(160) not null,
    finalidade_nova nvarchar(160) not null,
    descricao_nova nvarchar(2000) not null,
    primary key (solicitante_email, finalidade_antiga)
);

insert into @demo_textos (
    solicitante_email,
    finalidade_antiga,
    finalidade_nova,
    descricao_nova
)
values
    (
        'alice.solicitante.demo@loanflow.com',
        'Tratamento odontologico com implante e exames',
        'Tratamento odontológico com implante e exames',
        'Solicita crédito para cobrir implante dentário, radiografias e retorno clínico, mantendo parcelas dentro de uma faixa compatível com a renda declarada.'
    ),
    (
        'ana.paula.martins@loanflow.local',
        'Reforma da cozinha e troca do piso ceramico',
        'Reforma da cozinha e troca do piso cerâmico',
        'Pretende corrigir infiltração, substituir bancada e trocar o revestimento da cozinha de um apartamento antigo em Santos.'
    ),
    (
        'bruno.h.lopes@loanflow.local',
        'Quitacao de cartao e cheque especial em parcela unica',
        'Quitação de cartão e cheque especial em parcela única',
        'Busca consolidar saldo rotativo de cartão e cheque especial em uma única parcela mensal mais previsível.'
    ),
    (
        'camila.rocha.nunes@loanflow.local',
        'Curso de pos-graduacao e licencas de software',
        'Curso de pós-graduação e licenças de software',
        'Vai usar o valor para matrícula de pós-graduação em análise de dados, compra de livros e assinatura anual de ferramentas de estudo.'
    ),
    (
        'diego.f.alves@loanflow.local',
        'Troca de embreagem e pneus do carro de trabalho',
        'Troca de embreagem e pneus do carro de trabalho',
        'Precisa reparar o veículo usado para visitas comerciais, incluindo embreagem, dois pneus e alinhamento.'
    ),
    (
        'fernanda.a.costa@loanflow.local',
        'Capital de giro para ampliar producao de marmitas fitness',
        'Capital de giro para ampliar produção de marmitas fitness',
        'Pretendia comprar freezer vertical, embalagens e reforçar estoque de proteínas para vender marmitas sob encomenda.'
    ),
    (
        'gustavo.r.lima@loanflow.local',
        'Reparo do telhado e impermeabilizacao da laje',
        'Reparo do telhado e impermeabilização da laje',
        'O valor seria usado para trocar telhas quebradas, refazer rufos e aplicar manta impermeabilizante antes do período de chuva.'
    ),
    (
        'juliana.b.melo@loanflow.local',
        'Montagem de estudio fotografico domestico',
        'Montagem de estúdio fotográfico doméstico',
        'Deseja adquirir câmera usada, lente retrato, dois pontos de luz e fundo infinito para ampliar renda com ensaios em casa.'
    ),
    (
        'leandro.c.pinto@loanflow.local',
        'Quitacao de parcelas atrasadas e reorganizacao do orcamento',
        'Quitação de parcelas atrasadas e reorganização do orçamento',
        'A proposta serviria para liquidar duas parcelas atrasadas e substituir custos altos por um cronograma único e previsível.'
    ),
    (
        'mariana.d.gomes@loanflow.local',
        'Semestre final da faculdade e material didatico',
        'Semestre final da faculdade e material didático',
        'Quer cobrir mensalidades do semestre final, taxa de conclusão e compra de um notebook básico para o TCC.'
    ),
    (
        'rafael.t.moura@loanflow.local',
        'Compra de ferramentas para oficina de bicicletas',
        'Compra de ferramentas para oficina de bicicletas',
        'Vai investir em bancada, jogo de ferramentas, compressor pequeno e peças de giro rápido para ampliar atendimentos locais.'
    );

update p
set
    p.finalidade = d.finalidade_nova,
    p.descricao_detalhada = d.descricao_nova
from propostas p
join solicitantes_credito s on s.id = p.solicitante_id
join usuarios u on u.id = s.usuario_id
join @demo_textos d on d.solicitante_email = u.email
where p.finalidade = d.finalidade_antiga;
