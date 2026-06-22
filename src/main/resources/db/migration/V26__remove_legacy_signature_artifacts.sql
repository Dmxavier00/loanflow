drop table if exists dbo.assinaturas_eletronicas;
drop table if exists dbo.eventos_assinatura;
drop table if exists dbo.desafios_assinatura;

if col_length('dbo.contratos', 'data_expiracao_assinatura') is not null
    alter table dbo.contratos drop column data_expiracao_assinatura;

if col_length('dbo.contratos', 'data_ultima_verificacao_assinatura') is not null
    alter table dbo.contratos drop column data_ultima_verificacao_assinatura;

if col_length('dbo.contratos', 'motivo_expiracao') is not null
    alter table dbo.contratos drop column motivo_expiracao;
