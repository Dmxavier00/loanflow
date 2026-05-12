package com.api.loanflow.auditoria.infraestrutura.persistencia;

import com.api.loanflow.auditoria.dominio.Auditoria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditoriaRepository extends JpaRepository<Auditoria, Long> {
	List<Auditoria> findByEntidadeTipoAndEntidadeIdOrderByDataHoraAsc(String entidadeTipo, Long entidadeId);
}
