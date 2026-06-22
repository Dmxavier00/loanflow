package com.api.loanflow.usuario.infraestrutura.persistencia;

import com.api.loanflow.usuario.dominio.SolicitanteCredito;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SolicitanteCreditoRepository extends JpaRepository<SolicitanteCredito, Long> {
	Optional<SolicitanteCredito> findByUsuarioId(Long usuarioId);
}
