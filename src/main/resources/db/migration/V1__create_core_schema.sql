create table usuarios (
    id bigint identity(1,1) not null primary key,
    nome nvarchar(120) not null,
    cpf nvarchar(14) not null,
    email nvarchar(160) not null,
    senha_hash nvarchar(120) not null,
    status nvarchar(30) not null,
    data_cadastro datetime2 not null,
    data_atualizacao datetime2 not null,
    constraint uk_usuarios_cpf unique (cpf),
    constraint uk_usuarios_email unique (email)
);

create table usuario_papeis (
    usuario_id bigint not null,
    papel nvarchar(30) not null,
    constraint fk_usuario_papeis_usuario foreign key (usuario_id) references usuarios(id),
    constraint uk_usuario_papeis unique (usuario_id, papel)
);

create table solicitantes_credito (
    id bigint identity(1,1) not null primary key,
    usuario_id bigint not null,
    renda_mensal decimal(18,2) null,
    tipo_ocupacao nvarchar(80) null,
    score_credito_simulado int null,
    constraint uk_solicitantes_usuario unique (usuario_id),
    constraint fk_solicitantes_usuario foreign key (usuario_id) references usuarios(id)
);

create table credores (
    id bigint identity(1,1) not null primary key,
    usuario_id bigint not null,
    saldo_disponivel_simulado decimal(18,2) not null,
    total_emprestado_simulado decimal(18,2) not null,
    limite_operacoes int not null,
    constraint uk_credores_usuario unique (usuario_id),
    constraint fk_credores_usuario foreign key (usuario_id) references usuarios(id)
);

create table administradores (
    id bigint identity(1,1) not null primary key,
    usuario_id bigint not null,
    nivel_acesso nvarchar(50) null,
    setor_responsavel nvarchar(80) null,
    data_designacao date null,
    status_administrativo nvarchar(40) null,
    constraint uk_administradores_usuario unique (usuario_id),
    constraint fk_administradores_usuario foreign key (usuario_id) references usuarios(id)
);

create table propostas (
    id bigint identity(1,1) not null primary key,
    solicitante_id bigint not null,
    credor_id bigint null,
    valor_solicitado decimal(18,2) not null,
    taxa_juros decimal(8,4) not null,
    prazo_meses int not null,
    finalidade nvarchar(500) not null,
    status nvarchar(40) not null,
    data_criacao datetime2 not null,
    data_atualizacao datetime2 not null,
    data_expiracao date not null,
    constraint fk_propostas_solicitante foreign key (solicitante_id) references solicitantes_credito(id),
    constraint fk_propostas_credor foreign key (credor_id) references credores(id)
);

create table contratos (
    id bigint identity(1,1) not null primary key,
    proposta_id bigint not null,
    numero_contrato nvarchar(40) not null,
    status nvarchar(40) not null,
    conteudo_snapshot nvarchar(max) not null,
    pdf_path nvarchar(500) null,
    hash_documento nvarchar(128) not null,
    data_geracao datetime2 not null,
    data_expiracao_assinatura datetime2 null,
    data_formalizacao datetime2 null,
    constraint uk_contratos_proposta unique (proposta_id),
    constraint uk_contratos_numero unique (numero_contrato),
    constraint fk_contratos_proposta foreign key (proposta_id) references propostas(id)
);

create table assinaturas_eletronicas (
    id bigint identity(1,1) not null primary key,
    contrato_id bigint not null,
    usuario_id bigint not null,
    papel_signatario nvarchar(30) not null,
    tipo_aceite nvarchar(50) not null,
    hash_assinatura nvarchar(128) not null,
    ip_origem nvarchar(80) null,
    user_agent nvarchar(500) null,
    registro_temporal datetime2 not null,
    valida bit not null,
    constraint uk_assinatura_contrato_usuario unique (contrato_id, usuario_id),
    constraint fk_assinaturas_contrato foreign key (contrato_id) references contratos(id),
    constraint fk_assinaturas_usuario foreign key (usuario_id) references usuarios(id)
);

create table parcelas (
    id bigint identity(1,1) not null primary key,
    contrato_id bigint not null,
    numero int not null,
    valor_previsto decimal(18,2) not null,
    valor_pago_acumulado decimal(18,2) not null,
    data_vencimento date not null,
    status nvarchar(40) not null,
    constraint uk_parcela_contrato_numero unique (contrato_id, numero),
    constraint fk_parcelas_contrato foreign key (contrato_id) references contratos(id)
);

create table pagamentos (
    id bigint identity(1,1) not null primary key,
    parcela_id bigint not null,
    usuario_registrador_id bigint not null,
    valor_pago decimal(18,2) not null,
    data_hora_pagamento datetime2 not null,
    forma_pagamento nvarchar(40) not null,
    status nvarchar(40) not null,
    comprovante nvarchar(500) null,
    constraint fk_pagamentos_parcela foreign key (parcela_id) references parcelas(id),
    constraint fk_pagamentos_usuario foreign key (usuario_registrador_id) references usuarios(id)
);

create table notificacoes (
    id bigint identity(1,1) not null primary key,
    usuario_id bigint not null,
    tipo nvarchar(40) not null,
    mensagem nvarchar(500) not null,
    data_envio datetime2 not null,
    lida bit not null,
    referencia_tipo nvarchar(80) null,
    referencia_id bigint null,
    constraint fk_notificacoes_usuario foreign key (usuario_id) references usuarios(id)
);

create table auditorias (
    id bigint identity(1,1) not null primary key,
    usuario_id bigint null,
    acao nvarchar(50) not null,
    entidade_tipo nvarchar(80) not null,
    entidade_id bigint null,
    data_hora datetime2 not null,
    ip_origem nvarchar(80) null,
    detalhes nvarchar(max) null,
    constraint fk_auditorias_usuario foreign key (usuario_id) references usuarios(id)
);

create index ix_propostas_status on propostas(status);
create index ix_contratos_status on contratos(status);
create index ix_parcelas_status on parcelas(status);
create index ix_parcelas_data_vencimento on parcelas(data_vencimento);
create index ix_auditorias_entidade on auditorias(entidade_tipo, entidade_id);
