create table contas_bancarias (
    id bigint identity(1,1) not null primary key,
    usuario_id bigint not null,
    banco nvarchar(120) not null,
    agencia nvarchar(20) not null,
    numero_conta nvarchar(30) not null,
    tipo_conta nvarchar(20) not null,
    chave_pix nvarchar(120) null,
    data_cadastro datetime2 not null,
    data_atualizacao datetime2 not null,
    constraint uk_contas_bancarias_usuario unique (usuario_id),
    constraint fk_contas_bancarias_usuario foreign key (usuario_id) references usuarios(id)
);

create index ix_contas_bancarias_banco on contas_bancarias(banco);
