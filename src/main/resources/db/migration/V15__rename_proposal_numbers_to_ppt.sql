exec sp_executesql N'
    update propostas
    set numero_proposta = concat(
        ''PPT-'',
        year(data_criacao),
        ''-'',
        right(replicate(''0'', 6) + cast(id as varchar(20)), 6)
    )
    where numero_proposta not like ''PPT-%'';
';
