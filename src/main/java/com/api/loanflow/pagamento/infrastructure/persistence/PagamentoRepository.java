package com.api.loanflow.pagamento.infrastructure.persistence;

import com.api.loanflow.pagamento.domain.Pagamento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PagamentoRepository extends JpaRepository<Pagamento, Long> {
	List<Pagamento> findByParcelaIdOrderByDataHoraPagamentoAsc(Long parcelaId);
}
