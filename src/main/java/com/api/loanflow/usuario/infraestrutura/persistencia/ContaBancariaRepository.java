package com.api.loanflow.usuario.infraestrutura.persistencia;

import com.api.loanflow.usuario.dominio.ContaBancaria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ContaBancariaRepository extends JpaRepository<ContaBancaria, Long> {
	Optional<ContaBancaria> findByUsuarioId(Long usuarioId);

	boolean existsByUsuarioId(Long usuarioId);
}
