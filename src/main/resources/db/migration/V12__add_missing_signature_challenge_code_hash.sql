declare @schema_name sysname = schema_name();
declare @qualified_table nvarchar(300) = quotename(@schema_name) + '.desafios_assinatura';

if col_length(@qualified_table, 'codigo_hash') is null
begin
    exec('alter table ' + @qualified_table + ' add codigo_hash nvarchar(128) null;');
end;
