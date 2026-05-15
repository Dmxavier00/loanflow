package com.api.loanflow.contrato.aplicacao;

import com.api.loanflow.compartilhado.criptografia.HashService;
import com.api.loanflow.compartilhado.excecao.RegraNegocioException;
import com.api.loanflow.contrato.dominio.Contrato;
import com.api.loanflow.contrato.dominio.DesafioAssinatura;
import com.api.loanflow.contrato.dominio.DesafioAssinaturaStatus;
import com.api.loanflow.contrato.dominio.EventoAssinaturaTipo;
import com.api.loanflow.contrato.dominio.MetodoAutenticacaoAssinatura;
import com.api.loanflow.contrato.infraestrutura.persistencia.DesafioAssinaturaRepository;
import com.api.loanflow.notificacao.aplicacao.NotificacaoService;
import com.api.loanflow.notificacao.dominio.TipoNotificacao;
import com.api.loanflow.usuario.aplicacao.UsuarioService;
import com.api.loanflow.usuario.dominio.Usuario;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Locale;
import java.util.UUID;

@Service
public class AssinaturaDesafioService {
	private static final EnumSet<DesafioAssinaturaStatus> STATUSES_ABERTOS =
		EnumSet.of(DesafioAssinaturaStatus.PENDENTE, DesafioAssinaturaStatus.VALIDADO);
	private static final SecureRandom RANDOM = new SecureRandom();

	private final DesafioAssinaturaRepository desafioAssinaturaRepository;
	private final EventoAssinaturaService eventoAssinaturaService;
	private final UsuarioService usuarioService;
	private final NotificacaoService notificacaoService;
	private final HashService hashService;
	private final long challengeTtlMinutes;
	private final int challengeLength;

	public AssinaturaDesafioService(
		DesafioAssinaturaRepository desafioAssinaturaRepository,
		EventoAssinaturaService eventoAssinaturaService,
		UsuarioService usuarioService,
		NotificacaoService notificacaoService,
		HashService hashService,
		@Value("${loanflow.signature.challenge-ttl-minutes}") long challengeTtlMinutes,
		@Value("${loanflow.signature.challenge-length}") int challengeLength
	) {
		this.desafioAssinaturaRepository = desafioAssinaturaRepository;
		this.eventoAssinaturaService = eventoAssinaturaService;
		this.usuarioService = usuarioService;
		this.notificacaoService = notificacaoService;
		this.hashService = hashService;
		this.challengeTtlMinutes = challengeTtlMinutes;
		this.challengeLength = challengeLength;
	}

	@Transactional
	public DesafioAssinatura iniciarDesafio(
		Contrato contrato,
		Usuario usuario,
		MetodoAutenticacaoAssinatura metodo,
		String ipOrigem,
		String userAgent
	) {
		validarMetodoDisponivel(metodo);
		expirarDesafiosAbertos(contrato, usuario, ipOrigem, userAgent);

		var desafiosAbertos = desafioAssinaturaRepository.findByContratoIdAndUsuarioIdAndStatusIn(
			contrato.getId(),
			usuario.getId(),
			STATUSES_ABERTOS
		);
		var agora = LocalDateTime.now();
		for (DesafioAssinatura desafioAberto : desafiosAbertos) {
			desafioAberto.cancelar(agora);
		}

		var desafio = new DesafioAssinatura();
		desafio.setContrato(contrato);
		desafio.setUsuario(usuario);
		desafio.setMetodoAutenticacao(metodo);
		desafio.setStatus(DesafioAssinaturaStatus.PENDENTE);
		desafio.setIpOrigem(ipOrigem);
		desafio.setUserAgent(userAgent);
		desafio.setExpiraEm(agora.plusMinutes(challengeTtlMinutes));
		var mascaraDestino = resolverMascaraDestino(usuario, metodo);
		if (metodo == MetodoAutenticacaoAssinatura.CODIGO_ONE_TIME) {
			var codigoTemporario = gerarCodigoTemporario();
			desafio.setCodigoHash(hashService.sha256(codigoTemporario));
			notificacaoService.criar(
				usuario,
				TipoNotificacao.SISTEMA,
				"Código temporário para assinatura do contrato %s: %s. Válido até %s."
					.formatted(
						contrato.getNumeroContrato(),
						codigoTemporario,
						desafio.getExpiraEm().format(DateTimeFormatter.ofPattern("dd/MM HH:mm", new Locale("pt", "BR")))
					),
				"Contrato",
				contrato.getId()
			);
		}
		desafio = desafioAssinaturaRepository.save(desafio);

		eventoAssinaturaService.registrar(
			contrato,
			usuario,
			EventoAssinaturaTipo.DESAFIO_INICIADO,
			montarDetalheInicio(metodo, mascaraDestino),
			ipOrigem,
			userAgent
		);
		return desafio;
	}

	@Transactional
	public DesafioAssinatura validarDesafio(
		UUID desafioId,
		String codigo,
		Contrato contrato,
		Usuario usuario,
		String ipOrigem,
		String userAgent
	) {
		var desafio = desafioAssinaturaRepository.findByIdAndUsuarioId(desafioId, usuario.getId())
			.orElseThrow(() -> new RegraNegocioException("Desafio de assinatura inválido."));
		if (!desafio.getContrato().getId().equals(contrato.getId())) {
			throw new RegraNegocioException("Desafio de assinatura inválido.");
		}

		var agora = LocalDateTime.now();
		if (desafio.expiradoEm(agora)) {
			if (desafio.estaAberto()) {
				desafio.expirar(agora);
				eventoAssinaturaService.registrar(
					contrato,
					usuario,
					EventoAssinaturaTipo.DESAFIO_EXPIRADO,
					"Desafio de assinatura expirou antes da confirmação final.",
					ipOrigem,
					userAgent
				);
			}
			throw new RegraNegocioException("Desafio de assinatura expirado. Gere uma nova validação para continuar.");
		}

		if (desafio.getStatus() == DesafioAssinaturaStatus.CONSUMIDO || desafio.getStatus() == DesafioAssinaturaStatus.CANCELADO) {
			throw new RegraNegocioException("Desafio de assinatura já foi utilizado ou não está mais válido.");
		}

		if (desafio.getStatus() == DesafioAssinaturaStatus.VALIDADO) {
			return desafio;
		}

		desafio.incrementarTentativas();
		if (!codigoConfere(desafio, usuario, codigo)) {
			eventoAssinaturaService.registrar(
				contrato,
				usuario,
				EventoAssinaturaTipo.ASSINATURA_RECUSADA,
				"Código ou senha informados não conferem com o desafio de assinatura.",
				ipOrigem,
				userAgent
			);
			throw new RegraNegocioException("Código ou senha inválidos para confirmar a assinatura.");
		}

		desafio.marcarValidado(agora);
		eventoAssinaturaService.registrar(
			contrato,
			usuario,
			EventoAssinaturaTipo.DESAFIO_VALIDADO,
			"Desafio de assinatura validado com sucesso.",
			ipOrigem,
			userAgent
		);
		return desafio;
	}

	@Transactional
	public void consumirDesafio(DesafioAssinatura desafio, String ipOrigem, String userAgent) {
		desafio.consumir(LocalDateTime.now());
		eventoAssinaturaService.registrar(
			desafio.getContrato(),
			desafio.getUsuario(),
			EventoAssinaturaTipo.DESAFIO_CONSUMIDO,
			"Desafio de assinatura consumido com sucesso.",
			ipOrigem,
			userAgent
		);
	}

	@Transactional
	public void expirarDesafiosAbertos(Contrato contrato, Usuario usuario, String ipOrigem, String userAgent) {
		var agora = LocalDateTime.now();
		var desafios = desafioAssinaturaRepository.findByContratoIdAndUsuarioIdAndStatusIn(
			contrato.getId(),
			usuario.getId(),
			STATUSES_ABERTOS
		);
		for (DesafioAssinatura desafio : desafios) {
			if (!desafio.expiradoEm(agora)) {
				continue;
			}
			desafio.expirar(agora);
			eventoAssinaturaService.registrar(
				contrato,
				usuario,
				EventoAssinaturaTipo.DESAFIO_EXPIRADO,
				"Desafio de assinatura expirou antes da confirmação final.",
				ipOrigem,
				userAgent
			);
		}
	}

	public String resolverMascaraDestino(Usuario usuario, MetodoAutenticacaoAssinatura metodo) {
		if (metodo != MetodoAutenticacaoAssinatura.CODIGO_ONE_TIME) {
			return null;
		}
		if (usuario.getEmail() == null || usuario.getEmail().isBlank()) {
			return "destino cadastrado";
		}

		var email = usuario.getEmail().trim();
		var separador = email.indexOf('@');
		if (separador <= 1) {
			return "***" + email.substring(Math.max(0, separador));
		}
		return email.substring(0, 2) + "***" + email.substring(separador);
	}

	private void validarMetodoDisponivel(MetodoAutenticacaoAssinatura metodo) {
		if (metodo == null) {
			throw new RegraNegocioException("Método de autenticação obrigatório para iniciar a assinatura.");
		}
		if (metodo == MetodoAutenticacaoAssinatura.ACEITE_WEB_AUTENTICADO) {
			throw new RegraNegocioException("O método selecionado não está disponível para o desafio de assinatura.");
		}
	}

	private boolean codigoConfere(DesafioAssinatura desafio, Usuario usuario, String codigo) {
		if (codigo == null || codigo.isBlank()) {
			return false;
		}

		if (desafio.getMetodoAutenticacao() == MetodoAutenticacaoAssinatura.REAUTENTICACAO_SENHA) {
			try {
				usuarioService.validarSenhaAtual(usuario, codigo);
				return true;
			} catch (RegraNegocioException exception) {
				return false;
			}
		}

		if (desafio.getMetodoAutenticacao() == MetodoAutenticacaoAssinatura.CODIGO_ONE_TIME) {
			return desafio.getCodigoHash() != null
				&& hashService.sha256(codigo.trim()).equals(desafio.getCodigoHash());
		}

		return false;
	}

	private String gerarCodigoTemporario() {
		var tamanho = Math.max(challengeLength, 4);
		var builder = new StringBuilder(tamanho);
		for (int index = 0; index < tamanho; index++) {
			builder.append(RANDOM.nextInt(10));
		}
		return builder.toString();
	}

	private String montarDetalheInicio(MetodoAutenticacaoAssinatura metodo, String mascaraDestino) {
		if (metodo == MetodoAutenticacaoAssinatura.CODIGO_ONE_TIME && mascaraDestino != null && !mascaraDestino.isBlank()) {
			return "Desafio de assinatura iniciado com código temporário enviado ao destino mascarado " + mascaraDestino + ".";
		}
		if (metodo == MetodoAutenticacaoAssinatura.CODIGO_ONE_TIME) {
			return "Desafio de assinatura iniciado com código temporário.";
		}
		return "Desafio de assinatura iniciado com reautenticação por senha.";
	}
}
