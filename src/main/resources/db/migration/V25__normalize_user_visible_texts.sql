update notificacoes
set mensagem = N'Contrato quitado após pagamento integral.'
where mensagem = N'Contrato quitado apos pagamento integral.';

update auditorias
set detalhes = N'Contrato reaberto após cancelamento de pagamento.'
where detalhes = N'Contrato reaberto apos cancelamento de pagamento.';

update auditorias
set detalhes = N'Contrato quitado após pagamento integral.'
where detalhes = N'Contrato quitado apos pagamento integral.';

update auditorias
set detalhes = N'Contrato formalizado automaticamente após o aceite do credor.'
where detalhes = N'Contrato formalizado automaticamente apos o aceite do credor.';

update auditorias
set detalhes = N'Contrato formalizado automaticamente e disponível para consulta.'
where detalhes = N'Contrato formalizado automaticamente e disponivel para consulta.';

update propostas
set
    finalidade = N'Tratamento odontológico com implante e exames',
    descricao_detalhada = N'Solicita crédito para cobrir implante dentário, radiografias e retorno clínico, mantendo parcelas dentro de uma faixa compatível com a renda declarada.'
where finalidade = N'Tratamento odontologico com implante e exames'
   or descricao_detalhada = N'Solicita credito para cobrir implante dentario, radiografias e retorno clinico, mantendo parcelas dentro de uma faixa compativel com a renda declarada.';

update propostas
set
    finalidade = N'Quitação de cartão e cheque especial em parcela única',
    descricao_detalhada = N'Busca consolidar saldo rotativo de cartão e cheque especial em uma única parcela mensal mais previsível.'
where finalidade = N'Quitacao de cartao e cheque especial em parcela unica'
   or descricao_detalhada = N'Busca consolidar saldo rotativo de cartao e cheque especial em uma unica parcela mensal mais previsivel.';

update propostas
set
    finalidade = N'Curso de pós-graduação e licenças de software',
    descricao_detalhada = N'Vai usar o valor para matrícula de pós-graduação em análise de dados, compra de livros e assinatura anual de ferramentas de estudo.'
where finalidade = N'Curso de pos-graduacao e licencas de software'
   or descricao_detalhada = N'Vai usar o valor para matricula de pos-graduacao em analise de dados, compra de livros e assinatura anual de ferramentas de estudo.';

update propostas
set finalidade = N'Quitação de parcelas atrasadas e reorganização do orçamento'
where finalidade = N'Quitacao de parcelas atrasadas e reorganizacao do orcamento';
