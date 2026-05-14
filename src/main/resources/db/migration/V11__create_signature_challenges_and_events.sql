create table desafios_assinatura (
    id uniqueidentifier not null primary key,
    contrato_id bigint not null,
    usuario_id bigint not null,
    codigo_hash nvarchar(128) null,
    metodo_autenticacao nvarchar(40) not null,
    status nvarchar(30) not null,
    ip_origem nvarchar(80) null,
    user_agent nvarchar(500) null,
    criado_em datetime2 not null,
    expira_em datetime2 not null,
    validado_em datetime2 null,
    consumido_em datetime2 null,
    tentativas int not null,
    constraint fk_desafios_assinatura_contrato foreign key (contrato_id) references contratos(id),
    constraint fk_desafios_assinatura_usuario foreign key (usuario_id) references usuarios(id)
);

create table eventos_assinatura (
    id bigint identity(1,1) not null primary key,
    contrato_id bigint not null,
    usuario_id bigint null,
    tipo nvarchar(40) not null,
    detalhes nvarchar(500) null,
    ip_origem nvarchar(80) null,
    user_agent nvarchar(500) null,
    data_hora datetime2 not null,
    constraint fk_eventos_assinatura_contrato foreign key (contrato_id) references contratos(id),
    constraint fk_eventos_assinatura_usuario foreign key (usuario_id) references usuarios(id)
);

alter table assinaturas_eletronicas
    add constraint fk_assinaturas_desafio foreign key (desafio_autenticacao_id) references desafios_assinatura(id);

create index ix_desafios_assinatura_contrato on desafios_assinatura(contrato_id);
create index ix_desafios_assinatura_usuario on desafios_assinatura(usuario_id);
create index ix_desafios_assinatura_status on desafios_assinatura(status);
create index ix_desafios_assinatura_expira_em on desafios_assinatura(expira_em);
create index ix_eventos_assinatura_contrato_data_hora on eventos_assinatura(contrato_id, data_hora);
create index ix_eventos_assinatura_usuario_data_hora on eventos_assinatura(usuario_id, data_hora);
