update solicitantes_credito
set score_credito_simulado = case
    when round(score_credito_simulado / 10.0, 0) > 100 then 100
    else cast(round(score_credito_simulado / 10.0, 0) as int)
end
where score_credito_simulado > 100;

alter table solicitantes_credito
add constraint ck_solicitantes_score_credito_percentual
check (score_credito_simulado is null or score_credito_simulado between 0 and 100);
