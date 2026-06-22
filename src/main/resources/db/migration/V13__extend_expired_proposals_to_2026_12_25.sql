declare @hoje date = cast(sysdatetime() as date);
declare @nova_data_expiracao date = '2026-12-25';

update propostas
set
    data_expiracao = @nova_data_expiracao,
    data_atualizacao = sysdatetime(),
    status = case
        when status = 'EXPIRADA' and credor_id is null then 'AGUARDANDO_ACEITE'
        when status = 'EXPIRADA' and credor_id is not null then 'ACEITA'
        else status
    end
where data_expiracao < @hoje
  and status in (
      'RASCUNHO',
      'AGUARDANDO_ACEITE',
      'SUBMETIDA',
      'ACEITA',
      'EM_ANALISE',
      'APROVADA',
      'EXPIRADA'
  );
