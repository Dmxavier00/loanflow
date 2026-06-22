declare @admin_preservado_id bigint;

select @admin_preservado_id = id
from usuarios
where email = 'carla.admin.demo@loanflow.com'
  and papel = 'ADMIN';

if @admin_preservado_id is null
begin
    throw 51024, 'Admin preservado carla.admin.demo@loanflow.com nao encontrado.', 1;
end;

declare @admins_removidos table (
    id bigint not null primary key
);

insert into @admins_removidos (id)
select id
from usuarios
where papel = 'ADMIN'
  and email <> 'carla.admin.demo@loanflow.com';

delete n
from notificacoes n
join @admins_removidos ar on ar.id = n.usuario_id;

update auditorias
set usuario_id = null
where usuario_id in (select id from @admins_removidos);

update pagamentos
set usuario_registrador_id = @admin_preservado_id
where usuario_registrador_id in (select id from @admins_removidos);

update assinaturas_eletronicas
set usuario_id = @admin_preservado_id
where usuario_id in (select id from @admins_removidos);

update desafios_assinatura
set usuario_id = @admin_preservado_id
where usuario_id in (select id from @admins_removidos);

update eventos_assinatura
set usuario_id = null
where usuario_id in (select id from @admins_removidos);

delete cb
from contas_bancarias cb
join @admins_removidos ar on ar.id = cb.usuario_id;

delete a
from administradores a
join @admins_removidos ar on ar.id = a.usuario_id;

delete u
from usuarios u
join @admins_removidos ar on ar.id = u.id;
