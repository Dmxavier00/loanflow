package com.api.loanflow.parcela.infrastructure.persistence;

import com.api.loanflow.contrato.domain.ContratoStatus;
import com.api.loanflow.parcela.domain.Parcela;
import com.api.loanflow.parcela.domain.ParcelaStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface ParcelaRepository extends JpaRepository<Parcela, Long>, JpaSpecificationExecutor<Parcela> {
	List<Parcela> findByContratoIdOrderByNumeroAsc(Long contratoId);

	boolean existsByContratoId(Long contratoId);

	long countByStatus(ParcelaStatus status);

	List<Parcela> findByStatusInAndDataVencimentoBefore(List<ParcelaStatus> status, LocalDate dataVencimento);

	long countDistinctContratoIdByContratoStatusAndContratoPropostaSolicitanteUsuarioIdAndStatusNot(
		ContratoStatus contratoStatus,
		Long usuarioId,
		ParcelaStatus status
	);

	boolean existsByContratoStatusAndContratoPropostaSolicitanteUsuarioIdAndStatusIn(
		ContratoStatus contratoStatus,
		Long usuarioId,
		Collection<ParcelaStatus> status
	);

	boolean existsByContratoStatusAndContratoPropostaSolicitanteUsuarioIdAndStatusNotAndDataVencimentoBefore(
		ContratoStatus contratoStatus,
		Long usuarioId,
		ParcelaStatus status,
		LocalDate dataVencimento
	);

	List<Parcela> findByContratoStatusAndContratoPropostaSolicitanteUsuarioIdAndStatusNotOrderByContratoIdAscNumeroAsc(
		ContratoStatus contratoStatus,
		Long usuarioId,
		ParcelaStatus status
	);
}
