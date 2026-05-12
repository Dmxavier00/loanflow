if object_id('usuario_papeis', 'U') is not null
and exists (
    select up.usuario_id
    from usuario_papeis up
    group by up.usuario_id
    having count(*) > 1
)
begin
    throw 51000, 'Nao foi possivel migrar usuario_papeis: existem usuarios com mais de um papel cadastrado.', 1;
end;

if col_length('usuarios', 'papel') is null
begin
    alter table usuarios add papel nvarchar(30) null;
end;

if object_id('usuario_papeis', 'U') is not null
begin
    exec sp_executesql N'
        update u
        set papel = up.papel
        from usuarios u
        join usuario_papeis up on up.usuario_id = u.id
        where u.papel is null;
    ';
end;

declare @usuarios_sem_papel int;
set @usuarios_sem_papel = 0;

exec sp_executesql
    N'select @qt = count(*) from usuarios where papel is null;',
    N'@qt int output',
    @qt = @usuarios_sem_papel output;

if @usuarios_sem_papel > 0
begin
    throw 51001, 'Nao foi possivel migrar usuario_papeis: existem usuarios sem papel associado.', 1;
end;

exec sp_executesql N'alter table usuarios alter column papel nvarchar(30) not null;';

if object_id('usuario_papeis', 'U') is not null
begin
    drop table usuario_papeis;
end;
