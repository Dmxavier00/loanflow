package com.api.loanflow.auditoria.application;

import com.api.loanflow.auditoria.domain.Auditoria;
import com.api.loanflow.auditoria.domain.AuditoriaAcao;
import com.api.loanflow.auditoria.infrastructure.persistence.AuditoriaRepository;
import com.api.loanflow.usuario.domain.Usuario;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditoriaService {
	private final AuditoriaRepository auditoriaRepository;

	public AuditoriaService(AuditoriaRepository auditoriaRepository) {
		this.auditoriaRepository = auditoriaRepository;
	}

	@Transactional
	public void registrar(Usuario usuario, AuditoriaAcao acao, String entidadeTipo, Long entidadeId, String detalhes, String ipOrigem) {
		var auditoria = new Auditoria();
		auditoria.setUsuario(usuario);
		auditoria.setAcao(acao);
		auditoria.setEntidadeTipo(entidadeTipo);
		auditoria.setEntidadeId(entidadeId);
		auditoria.setDetalhes(detalhes);
		auditoria.setIpOrigem(ipOrigem);
		auditoriaRepository.save(auditoria);
	}
}
