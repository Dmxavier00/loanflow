with parcelas_parciais as (
    select id
    from parcelas
    where status = 'PARCIALMENTE_PAGA'
       or (valor_pago_acumulado > 0 and valor_pago_acumulado < valor_previsto)
)
update pagamentos
set status = 'CANCELADO'
where status <> 'CANCELADO'
  and parcela_id in (select id from parcelas_parciais);

with parcelas_parciais as (
    select id
    from parcelas
    where status = 'PARCIALMENTE_PAGA'
       or (valor_pago_acumulado > 0 and valor_pago_acumulado < valor_previsto)
)
update parcelas
set
    valor_pago_acumulado = 0.00,
    status = case
        when data_vencimento < cast(getdate() as date) then 'EM_ATRASO'
        else 'ABERTA'
    end
where id in (select id from parcelas_parciais);
