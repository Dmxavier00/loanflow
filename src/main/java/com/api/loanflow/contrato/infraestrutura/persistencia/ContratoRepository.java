package com.api.loanflow.contrato.infraestrutura.persistencia;

import com.api.loanflow.contrato.dominio.Contrato;
import com.api.loanflow.contrato.dominio.ContratoStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface ContratoRepository extends JpaRepository<Contrato, Long>, JpaSpecificationExecutor<Contrato> {
	Optional<Contrato> findByPropostaId(Long propostaId);

	long countByStatus(ContratoStatus status);
}
