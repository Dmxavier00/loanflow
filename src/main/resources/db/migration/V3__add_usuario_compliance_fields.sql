alter table usuarios add data_nascimento date null;
alter table usuarios add telefone nvarchar(20) null;
alter table usuarios add tipo_documento_identidade nvarchar(30) null;
alter table usuarios add documento_identidade nvarchar(40) null;
alter table usuarios add orgao_emissor nvarchar(20) null;
alter table usuarios add pessoa_exposta_politicamente bit not null constraint df_usuarios_pessoa_exposta_politicamente default 0;
