package com.api.loanflow.usuario.infraestrutura.persistencia;

import com.api.loanflow.usuario.dominio.Credor;
import com.api.loanflow.usuario.dominio.UsuarioStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CredorRepository extends JpaRepository<Credor, Long> {
	Optional<Credor> findByUsuarioId(Long usuarioId);

	List<Credor> findByUsuarioStatusOrderByUsuarioNomeAsc(UsuarioStatus status);

	List<Credor> findByUsuarioStatusAndUsuarioNomeContainingIgnoreCaseOrderByUsuarioNomeAsc(UsuarioStatus status, String nome);
}
