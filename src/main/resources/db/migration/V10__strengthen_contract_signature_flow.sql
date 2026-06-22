alter table contratos add hash_pdf_emitido nvarchar(128) null;
alter table contratos add data_ultima_verificacao_assinatura datetime2 null;
alter table contratos add motivo_expiracao nvarchar(240) null;

alter table assinaturas_eletronicas add hash_conteudo_aceito nvarchar(128) null;
alter table assinaturas_eletronicas add versao_termo_aceite nvarchar(40) null;
alter table assinaturas_eletronicas add metodo_autenticacao nvarchar(40) null;
alter table assinaturas_eletronicas add desafio_autenticacao_id uniqueidentifier null;
alter table assinaturas_eletronicas add assinatura_documento_valida bit null;
