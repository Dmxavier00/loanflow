package com.api.loanflow.pagamento.infraestrutura.persistencia;

import com.api.loanflow.pagamento.dominio.Pagamento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PagamentoRepository extends JpaRepository<Pagamento, Long> {
	List<Pagamento> findByParcelaIdOrderByDataHoraPagamentoAsc(Long parcelaId);
}
