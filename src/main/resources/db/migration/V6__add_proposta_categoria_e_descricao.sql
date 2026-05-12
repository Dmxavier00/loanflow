if col_length('propostas', 'categoria_finalidade') is null
begin
    alter table propostas add categoria_finalidade nvarchar(40) null;
end;

if col_length('propostas', 'descricao_detalhada') is null
begin
    alter table propostas add descricao_detalhada nvarchar(2000) null;
end;

exec sp_executesql N'
    update propostas
    set categoria_finalidade = ''OUTRA''
    where categoria_finalidade is null;

    update propostas
    set descricao_detalhada = finalidade
    where descricao_detalhada is null;
';

exec sp_executesql N'
    alter table propostas alter column categoria_finalidade nvarchar(40) not null;
    alter table propostas alter column descricao_detalhada nvarchar(2000) not null;
';
