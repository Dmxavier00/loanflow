update sc
set score_credito_simulado = case
    when sc.renda_mensal is null or sc.renda_mensal <= 0 then 0
    when pontuacao.valor < 25 then 25
    when pontuacao.valor > 95 then 95
    else pontuacao.valor
end
from solicitantes_credito sc
join usuarios u on u.id = sc.usuario_id
cross apply (
    select case
        when sc.renda_mensal >= 12000.00 then 82
        when sc.renda_mensal >= 8000.00 then 76
        when sc.renda_mensal >= 5000.00 then 68
        when sc.renda_mensal >= 3000.00 then 60
        when sc.renda_mensal >= 1500.00 then 48
        else 35
    end as valor
) base
cross apply (
    select case upper(coalesce(sc.tipo_ocupacao, ''))
        when 'SERVIDOR' then 8
        when 'CLT' then 4
        when 'EMPRESARIO' then 2
        when 'MEI' then -4
        when 'AUTONOMO' then -8
        else 0
    end as valor
) ocupacao
cross apply (
    select try_convert(int, right(replace(replace(coalesce(u.cpf, ''), '.', ''), '-', ''), 1)) as valor
) digito
cross apply (
    select case digito.valor
        when 0 then -26
        when 1 then -18
        when 2 then -10
        when 3 then -4
        when 4 then 0
        when 5 then 4
        when 6 then 8
        when 7 then 12
        when 8 then 16
        when 9 then 20
        else 0
    end as valor
) perfil
cross apply (
    select base.valor + ocupacao.valor + perfil.valor as valor
) pontuacao;
