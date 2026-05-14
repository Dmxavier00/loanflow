package com.api.loanflow.contrato.infraestrutura.persistencia;

import com.api.loanflow.contrato.dominio.EventoAssinatura;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventoAssinaturaRepository extends JpaRepository<EventoAssinatura, Long> {
	List<EventoAssinatura> findByContratoIdOrderByDataHoraAsc(Long contratoId);
}
