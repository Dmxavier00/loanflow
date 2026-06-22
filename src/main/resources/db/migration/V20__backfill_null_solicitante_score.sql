update solicitantes_credito
set score_credito_simulado = case
    when renda_mensal is null or renda_mensal <= 0 then 0
    when renda_mensal >= 5000.00 then 85
    when renda_mensal >= 3000.00 then 75
    when renda_mensal >= 1500.00 then 55
    else 35
end
where score_credito_simulado is null;
