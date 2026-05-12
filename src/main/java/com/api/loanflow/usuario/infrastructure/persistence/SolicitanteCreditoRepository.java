package com.api.loanflow.usuario.infrastructure.persistence;

import com.api.loanflow.usuario.domain.SolicitanteCredito;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SolicitanteCreditoRepository extends JpaRepository<SolicitanteCredito, Long> {
	Optional<SolicitanteCredito> findByUsuarioId(Long usuarioId);
}
