update contratos
set status = 'FORMALIZADO',
    data_formalizacao = coalesce(data_formalizacao, data_geracao)
where status in ('AGUARDANDO_ASSINATURAS', 'ASSINADO_PARCIALMENTE');

update propostas
set status = 'CONTRATADA'
where id in (
    select proposta_id
    from contratos
    where status = 'FORMALIZADO'
      and data_formalizacao is not null
);
