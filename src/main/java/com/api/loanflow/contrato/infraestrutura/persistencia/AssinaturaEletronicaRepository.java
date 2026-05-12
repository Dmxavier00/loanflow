package com.api.loanflow.contrato.infraestrutura.persistencia;

import com.api.loanflow.contrato.dominio.AssinaturaEletronica;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AssinaturaEletronicaRepository extends JpaRepository<AssinaturaEletronica, Long> {
	boolean existsByContratoIdAndUsuarioId(Long contratoId, Long usuarioId);

	long countByContratoIdAndValidaTrue(Long contratoId);

	List<AssinaturaEletronica> findByContratoIdOrderByRegistroTemporalAsc(Long contratoId);
}
