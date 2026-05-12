package com.api.loanflow.auditoria.infrastructure.persistence;

import com.api.loanflow.auditoria.domain.Auditoria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditoriaRepository extends JpaRepository<Auditoria, Long> {
	List<Auditoria> findByEntidadeTipoAndEntidadeIdOrderByDataHoraAsc(String entidadeTipo, Long entidadeId);
}
