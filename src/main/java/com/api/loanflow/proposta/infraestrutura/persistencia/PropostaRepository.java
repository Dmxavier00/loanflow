package com.api.loanflow.proposta.infraestrutura.persistencia;

import com.api.loanflow.proposta.dominio.Proposta;
import com.api.loanflow.proposta.dominio.PropostaStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.List;

public interface PropostaRepository extends JpaRepository<Proposta, Long>, JpaSpecificationExecutor<Proposta> {
	List<Proposta> findBySolicitanteUsuarioIdOrderByDataCriacaoDesc(Long usuarioId);

	List<Proposta> findByCredorUsuarioIdOrderByDataCriacaoDesc(Long usuarioId);

	List<Proposta> findByCredorUsuarioIdAndStatusInOrderByDataCriacaoDesc(Long usuarioId, Collection<PropostaStatus> status);

	long countByCredorUsuarioIdAndStatusIn(Long usuarioId, Collection<PropostaStatus> status);

	List<Proposta> findByStatusAndCredorIsNullOrderByDataCriacaoDesc(PropostaStatus status);

	List<Proposta> findByStatusOrderByDataCriacaoDesc(PropostaStatus status);

	long countByStatus(PropostaStatus status);

	long countByStatusIn(Collection<PropostaStatus> status);
}
