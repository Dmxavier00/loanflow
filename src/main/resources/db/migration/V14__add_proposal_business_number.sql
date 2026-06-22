if col_length('propostas', 'numero_proposta') is null
begin
    alter table propostas add numero_proposta nvarchar(40) null;
end;

exec sp_executesql N'
    update propostas
    set numero_proposta = concat(
        ''PR-'',
        year(data_criacao),
        ''-'',
        right(replicate(''0'', 6) + cast(id as varchar(20)), 6)
    )
    where numero_proposta is null;
';

exec sp_executesql N'
    alter table propostas alter column numero_proposta nvarchar(40) not null;
';

if not exists (
    select 1
    from sys.key_constraints
    where [name] = 'uk_propostas_numero'
      and [parent_object_id] = object_id('propostas')
)
begin
    alter table propostas add constraint uk_propostas_numero unique (numero_proposta);
end;
