package com.api.loanflow.usuario.infraestrutura.persistencia;

import com.api.loanflow.usuario.dominio.Administrador;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AdministradorRepository extends JpaRepository<Administrador, Long> {
	Optional<Administrador> findByUsuarioId(Long usuarioId);
}
