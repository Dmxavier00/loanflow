package com.api.loanflow.contrato.infraestrutura.persistencia;

import com.api.loanflow.contrato.dominio.DesafioAssinatura;
import com.api.loanflow.contrato.dominio.DesafioAssinaturaStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DesafioAssinaturaRepository extends JpaRepository<DesafioAssinatura, UUID> {
	Optional<DesafioAssinatura> findByIdAndUsuarioId(UUID id, Long usuarioId);

	Optional<DesafioAssinatura> findByContratoIdAndUsuarioIdAndStatus(Long contratoId, Long usuarioId, DesafioAssinaturaStatus status);

	List<DesafioAssinatura> findByContratoIdAndUsuarioIdAndStatusIn(
		Long contratoId,
		Long usuarioId,
		Collection<DesafioAssinaturaStatus> statuses
	);
}
