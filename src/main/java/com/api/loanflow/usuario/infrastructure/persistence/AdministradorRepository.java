package com.api.loanflow.usuario.infrastructure.persistence;

import com.api.loanflow.usuario.domain.Administrador;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AdministradorRepository extends JpaRepository<Administrador, Long> {
	Optional<Administrador> findByUsuarioId(Long usuarioId);
}
