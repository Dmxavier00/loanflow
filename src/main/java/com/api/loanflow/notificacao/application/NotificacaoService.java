package com.api.loanflow.notificacao.application;

import com.api.loanflow.shared.exception.RecursoNaoEncontradoException;
import com.api.loanflow.notificacao.api.dto.NotificacaoResponse;
import com.api.loanflow.notificacao.domain.Notificacao;
import com.api.loanflow.notificacao.domain.TipoNotificacao;
import com.api.loanflow.notificacao.infrastructure.persistence.NotificacaoRepository;
import com.api.loanflow.usuario.domain.Usuario;
import com.api.loanflow.usuario.application.UsuarioService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NotificacaoService {
	private final NotificacaoRepository notificacaoRepository;
	private final UsuarioService usuarioService;

	public NotificacaoService(NotificacaoRepository notificacaoRepository, UsuarioService usuarioService) {
		this.notificacaoRepository = notificacaoRepository;
		this.usuarioService = usuarioService;
	}

	@Transactional
	public void criar(Usuario usuario, TipoNotificacao tipo, String mensagem, String referenciaTipo, Long referenciaId) {
		var notificacao = new Notificacao();
		notificacao.setUsuario(usuario);
		notificacao.setTipo(tipo);
		notificacao.setMensagem(mensagem);
		notificacao.setReferenciaTipo(referenciaTipo);
		notificacao.setReferenciaId(referenciaId);
		notificacaoRepository.save(notificacao);
	}

	@Transactional(readOnly = true)
	public List<NotificacaoResponse> listarMinhas(Boolean lida, TipoNotificacao tipo) {
		var usuario = usuarioService.usuarioAtual();
		return notificacaoRepository.findByUsuarioIdOrderByDataEnvioDesc(usuario.getId()).stream()
			.filter(notificacao -> lida == null || notificacao.isLida() == lida)
			.filter(notificacao -> tipo == null || notificacao.getTipo() == tipo)
			.map(NotificacaoResponse::from)
			.toList();
	}

	@Transactional
	public NotificacaoResponse marcarComoLida(Long id) {
		var usuario = usuarioService.usuarioAtual();
		var notificacao = notificacaoRepository.findById(id)
			.orElseThrow(() -> new RecursoNaoEncontradoException("Notificação não encontrada."));
		if (!notificacao.getUsuario().getId().equals(usuario.getId())) {
			throw new RecursoNaoEncontradoException("Notificação não encontrada.");
		}
		notificacao.setLida(true);
		return NotificacaoResponse.from(notificacao);
	}
}
