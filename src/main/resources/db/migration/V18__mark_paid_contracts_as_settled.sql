update contratos
set status = 'QUITADO'
where status = 'FORMALIZADO'
  and exists (
    select 1
    from parcelas
    where parcelas.contrato_id = contratos.id
  )
  and not exists (
    select 1
    from parcelas
    where parcelas.contrato_id = contratos.id
      and parcelas.status <> 'PAGA'
  );

update propostas
set status = 'QUITADA'
where status = 'CONTRATADA'
  and exists (
    select 1
    from contratos
    where contratos.proposta_id = propostas.id
      and contratos.status = 'QUITADO'
  );
