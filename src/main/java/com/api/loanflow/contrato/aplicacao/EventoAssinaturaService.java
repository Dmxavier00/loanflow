package com.api.loanflow.contrato.aplicacao;

import com.api.loanflow.contrato.dominio.Contrato;
import com.api.loanflow.contrato.dominio.EventoAssinatura;
import com.api.loanflow.contrato.dominio.EventoAssinaturaTipo;
import com.api.loanflow.contrato.infraestrutura.persistencia.EventoAssinaturaRepository;
import com.api.loanflow.usuario.dominio.Usuario;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EventoAssinaturaService {
	private final EventoAssinaturaRepository eventoAssinaturaRepository;

	public EventoAssinaturaService(EventoAssinaturaRepository eventoAssinaturaRepository) {
		this.eventoAssinaturaRepository = eventoAssinaturaRepository;
	}

	@Transactional
	public void registrar(
		Contrato contrato,
		Usuario usuario,
		EventoAssinaturaTipo tipo,
		String detalhes,
		String ipOrigem,
		String userAgent
	) {
		var evento = new EventoAssinatura();
		evento.setContrato(contrato);
		evento.setUsuario(usuario);
		evento.setTipo(tipo);
		evento.setDetalhes(detalhes);
		evento.setIpOrigem(ipOrigem);
		evento.setUserAgent(userAgent);
		eventoAssinaturaRepository.save(evento);
	}
}
