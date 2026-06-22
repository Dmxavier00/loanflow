package com.api.loanflow.contrato.aplicacao;

import com.api.loanflow.auditoria.aplicacao.AuditoriaService;
import com.api.loanflow.auditoria.dominio.AuditoriaAcao;
import com.api.loanflow.compartilhado.aplicacao.NumeroNegocioService;
import com.api.loanflow.compartilhado.criptografia.HashService;
import com.api.loanflow.compartilhado.excecao.RecursoNaoEncontradoException;
import com.api.loanflow.compartilhado.excecao.RegraNegocioException;
import com.api.loanflow.contrato.api.dto.ContratoResponse;
import com.api.loanflow.contrato.dominio.Contrato;
import com.api.loanflow.contrato.dominio.ContratoStatus;
import com.api.loanflow.contrato.infraestrutura.persistencia.ContratoRepository;
import com.api.loanflow.notificacao.aplicacao.NotificacaoService;
import com.api.loanflow.notificacao.dominio.TipoNotificacao;
import com.api.loanflow.parcela.aplicacao.ParcelaService;
import com.api.loanflow.proposta.aplicacao.PoliticaCreditoService;
import com.api.loanflow.proposta.dominio.CategoriaFinalidade;
import com.api.loanflow.proposta.dominio.Proposta;
import com.api.loanflow.proposta.dominio.PropostaStatus;
import com.api.loanflow.proposta.infraestrutura.persistencia.PropostaRepository;
import com.api.loanflow.usuario.aplicacao.UsuarioService;
import com.api.loanflow.usuario.dominio.Role;
import com.api.loanflow.usuario.dominio.Usuario;
import com.api.loanflow.usuario.infraestrutura.persistencia.ContaBancariaRepository;
import jakarta.persistence.criteria.JoinType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContratoService {
	private final ContratoRepository contratoRepository;
	private final PropostaRepository propostaRepository;
	private final UsuarioService usuarioService;
	private final HashService hashService;
	private final PdfContratoService pdfContratoService;
	private final ParcelaService parcelaService;
	private final AuditoriaService auditoriaService;
	private final NotificacaoService notificacaoService;
	private final PoliticaCreditoService politicaCreditoService;
	private final ContratoConteudoService contratoConteudoService;
	private final NumeroNegocioService numeroNegocioService;
	private final ContaBancariaRepository contaBancariaRepository;

	public ContratoService(
		ContratoRepository contratoRepository,
		PropostaRepository propostaRepository,
		UsuarioService usuarioService,
		HashService hashService,
		PdfContratoService pdfContratoService,
		ParcelaService parcelaService,
		AuditoriaService auditoriaService,
		NotificacaoService notificacaoService,
		PoliticaCreditoService politicaCreditoService,
		ContratoConteudoService contratoConteudoService,
		NumeroNegocioService numeroNegocioService,
		ContaBancariaRepository contaBancariaRepository
	) {
		this.contratoRepository = contratoRepository;
		this.propostaRepository = propostaRepository;
		this.usuarioService = usuarioService;
		this.hashService = hashService;
		this.pdfContratoService = pdfContratoService;
		this.parcelaService = parcelaService;
		this.auditoriaService = auditoriaService;
		this.notificacaoService = notificacaoService;
		this.politicaCreditoService = politicaCreditoService;
		this.contratoConteudoService = contratoConteudoService;
		this.numeroNegocioService = numeroNegocioService;
		this.contaBancariaRepository = contaBancariaRepository;
	}

	@Transactional
	public ContratoResponse gerar(Long propostaId, String ipOrigem) {
		var usuario = usuarioService.usuarioAtual();
		var proposta = buscarPropostaComPermissao(propostaId, usuario);
		var contrato = gerarEFormalizarAutomaticamente(proposta, usuario, ipOrigem);
		return toResponse(contrato);
	}

	@Transactional
	public Contrato gerarEFormalizarAutomaticamente(Proposta proposta, Usuario usuario, String ipOrigem) {
		validarPropostaParaGeracao(proposta);
		if (contratoRepository.findByPropostaId(proposta.getId()).isPresent()) {
			throw new RegraNegocioException("Contrato ja gerado para esta proposta.");
		}
		exigirCredorOuAdmin(usuario, proposta);
		exigirContasBancariasDaProposta(proposta);
		politicaCreditoService.validarNovaContratacao(
			proposta.getSolicitante(),
			proposta.getValorSolicitado(),
			proposta.getTaxaJuros(),
			proposta.getPrazoMeses()
		);

		var contrato = new Contrato();
		contrato.setProposta(proposta);
		contrato.setNumeroContrato(numeroNegocioService.gerarNumeroTemporario());
		contrato.setStatus(ContratoStatus.GERADO);
		contrato.setConteudoSnapshot("EMISSAO_PENDENTE");
		contrato.setHashDocumento("EMISSAO_PENDENTE");
		contrato = contratoRepository.saveAndFlush(contrato);

		var dataGeracao = contrato.getDataGeracao();
		var instanteFormalizacao = LocalDateTime.now();
		var numero = numeroNegocioService.gerarNumeroContrato(contrato);
		var conteudo = contratoConteudoService.montarConteudoContrato(
			numero,
			proposta,
			dataGeracao,
			instanteFormalizacao
		);
		var hashDocumento = hashService.sha256(conteudo);
		var pdfPath = pdfContratoService.gerarContratoPdf(numero, conteudo);
		var hashPdfEmitido = hashService.sha256(pdfContratoService.lerPdf(pdfPath));

		contrato.setNumeroContrato(numero);
		contrato.setConteudoSnapshot(conteudo);
		contrato.setHashDocumento(hashDocumento);
		contrato.setPdfPath(pdfPath);
		contrato.setHashPdfEmitido(hashPdfEmitido);

		contrato = formalizarContrato(contrato, usuario, ipOrigem, instanteFormalizacao);
		auditoriaService.registrar(
			usuario,
			AuditoriaAcao.GERAR_CONTRATO,
			"Contrato",
			contrato.getId(),
			"Contrato gerado com hash SHA-256.",
			ipOrigem
		);
		notificacaoService.criar(
			proposta.getSolicitante().getUsuario(),
			TipoNotificacao.CONTRATO,
			"Contrato formalizado automaticamente e disponivel para consulta.",
			"Contrato",
			contrato.getId()
		);
		notificacaoService.criar(
			proposta.getCredor().getUsuario(),
			TipoNotificacao.CONTRATO,
			"Contrato formalizado automaticamente e disponivel para consulta.",
			"Contrato",
			contrato.getId()
		);
		return contrato;
	}

	@Transactional
	public ContratoResponse detalhar(Long id) {
		return toResponse(buscarComPermissao(id));
	}

	@Transactional
	public List<ContratoResponse> listarComFiltros(
		ContratoStatus status,
		String numeroContrato,
		CategoriaFinalidade categoriaFinalidade,
		String nomeContraparte
	) {
		var usuario = usuarioService.usuarioAtual();
		var statusEfetivo = status == null ? ContratoStatus.FORMALIZADO : status;
		var spec = Specification.where(specAcessivelAoUsuario(usuario))
			.and(specStatus(statusEfetivo))
			.and(specNumeroContrato(numeroContrato))
			.and(specCategoriaFinalidade(categoriaFinalidade))
			.and(specNomeContraparte(usuario, nomeContraparte));
		return contratoRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "dataGeracao")).stream()
			.map(this::toResponse)
			.toList();
	}

	@Transactional
	public byte[] baixarPdf(Long id) {
		var contrato = buscarComPermissao(id);
		if (contrato.getPdfPath() == null) {
			throw new RecursoNaoEncontradoException("PDF do contrato nao encontrado.");
		}
		return pdfContratoService.lerPdf(contrato.getPdfPath());
	}

	@Transactional
	public ContratoResponse cancelar(Long id, String ipOrigem) {
		var usuario = usuarioService.usuarioAtual();
		var contrato = buscarComPermissao(id, usuario);
		exigirCredorOuAdmin(usuario, contrato.getProposta());
		if (contrato.getStatus() == ContratoStatus.FORMALIZADO || contrato.getStatus() == ContratoStatus.QUITADO) {
			throw new RegraNegocioException("Contrato formalizado ou quitado nao pode ser cancelado por este fluxo.");
		}
		contrato.setStatus(ContratoStatus.CANCELADO);
		if (contrato.getProposta().getStatus() != PropostaStatus.CONTRATADA) {
			contrato.getProposta().setStatus(PropostaStatus.CANCELADA);
		}
		auditoriaService.registrar(
			usuario,
			AuditoriaAcao.CANCELAR,
			"Contrato",
			contrato.getId(),
			"Contrato cancelado antes da formalizacao.",
			ipOrigem
		);
		notificacaoService.criar(
			contrato.getProposta().getSolicitante().getUsuario(),
			TipoNotificacao.CONTRATO,
			"Contrato cancelado.",
			"Contrato",
			contrato.getId()
		);
		return toResponse(contrato);
	}

	private ContratoResponse toResponse(Contrato contrato) {
		var credor = contrato.getProposta().getCredor();
		if (credor == null || credor.getUsuario() == null || credor.getUsuario().getId() == null) {
			return ContratoResponse.from(contrato);
		}

		var credorContaBancaria = contaBancariaRepository.findByUsuarioId(credor.getUsuario().getId()).orElse(null);
		return ContratoResponse.from(contrato, credorContaBancaria);
	}

	private Contrato buscarComPermissao(Long id) {
		return buscarComPermissao(id, usuarioService.usuarioAtual());
	}

	private Proposta buscarPropostaComPermissao(Long id, Usuario usuario) {
		var proposta = propostaRepository.findById(id)
			.orElseThrow(() -> new RecursoNaoEncontradoException("Proposta nao encontrada."));
		if (ehAdmin(usuario) || proposta.getSolicitante().getUsuario().getId().equals(usuario.getId())) {
			return proposta;
		}
		if (proposta.getCredor() != null && proposta.getCredor().getUsuario().getId().equals(usuario.getId())) {
			return proposta;
		}
		if (usuario.possuiPapel(Role.CREDOR) && proposta.getCredor() == null && proposta.getStatus() == PropostaStatus.AGUARDANDO_ACEITE) {
			return proposta;
		}
		throw new RecursoNaoEncontradoException("Proposta nao encontrada.");
	}

	private Contrato buscarComPermissao(Long id, Usuario usuario) {
		var contrato = contratoRepository.findById(id)
			.orElseThrow(() -> new RecursoNaoEncontradoException("Contrato nao encontrado."));
		var proposta = contrato.getProposta();
		if (ehAdmin(usuario) || proposta.getSolicitante().getUsuario().getId().equals(usuario.getId())) {
			return contrato;
		}
		if (proposta.getCredor() != null && proposta.getCredor().getUsuario().getId().equals(usuario.getId())) {
			return contrato;
		}
		throw new RecursoNaoEncontradoException("Contrato nao encontrado.");
	}

	private void validarPropostaParaGeracao(Proposta proposta) {
		if (proposta.getStatus() != PropostaStatus.ACEITA && proposta.getStatus() != PropostaStatus.APROVADA) {
			throw new RegraNegocioException("Contrato so pode ser gerado para proposta aceita ou aprovada.");
		}
	}

	private Contrato formalizarContrato(
		Contrato contrato,
		Usuario usuario,
		String ipOrigem,
		LocalDateTime instanteFormalizacao
	) {
		contrato.setStatus(ContratoStatus.FORMALIZADO);
		contrato.setDataFormalizacao(instanteFormalizacao);
		contrato.getProposta().setStatus(PropostaStatus.CONTRATADA);
		contrato = contratoRepository.save(contrato);
		auditoriaService.registrar(
			usuario,
			AuditoriaAcao.FORMALIZAR,
			"Contrato",
			contrato.getId(),
			"Contrato formalizado automaticamente apos o aceite do credor.",
			ipOrigem
		);
		parcelaService.gerarParcelas(contrato, usuario, ipOrigem);
		return contrato;
	}

	private void exigirCredorOuAdmin(Usuario usuario, Proposta proposta) {
		if (ehAdmin(usuario)) {
			return;
		}
		if (proposta.getCredor() == null || !proposta.getCredor().getUsuario().getId().equals(usuario.getId())) {
			throw new RegraNegocioException("Apenas o credor responsavel ou admin pode gerar o contrato.");
		}
	}

	private void exigirContasBancariasDaProposta(Proposta proposta) {
		usuarioService.exigirContaBancaria(
			proposta.getSolicitante().getUsuario(),
			"Solicitante precisa cadastrar conta bancaria antes de prosseguir com o contrato."
		);
		if (proposta.getCredor() == null) {
			throw new RegraNegocioException("Proposta ainda nao possui credor vinculado.");
		}
		usuarioService.exigirContaBancaria(
			proposta.getCredor().getUsuario(),
			"Credor precisa cadastrar conta bancaria antes de prosseguir com o contrato."
		);
	}

	private boolean ehAdmin(Usuario usuario) {
		return usuario.possuiPapel(Role.ADMIN);
	}

	private Specification<Contrato> specAcessivelAoUsuario(Usuario usuario) {
		return (root, query, cb) -> {
			query.distinct(true);
			if (ehAdmin(usuario)) {
				return cb.conjunction();
			}

			var proposta = root.join("proposta");
			var solicitanteUsuario = proposta.join("solicitante").join("usuario");
			var credor = proposta.join("credor", JoinType.LEFT);
			var credorUsuario = credor.join("usuario", JoinType.LEFT);

			return cb.or(
				cb.equal(solicitanteUsuario.get("id"), usuario.getId()),
				cb.equal(credorUsuario.get("id"), usuario.getId())
			);
		};
	}

	private Specification<Contrato> specStatus(ContratoStatus status) {
		return (root, query, cb) -> status == null ? cb.conjunction() : cb.equal(root.get("status"), status);
	}

	private Specification<Contrato> specNumeroContrato(String numeroContrato) {
		return (root, query, cb) -> {
			if (numeroContrato == null || numeroContrato.isBlank()) {
				return cb.conjunction();
			}
			return cb.like(cb.lower(root.get("numeroContrato")), "%" + numeroContrato.trim().toLowerCase(Locale.ROOT) + "%");
		};
	}

	private Specification<Contrato> specCategoriaFinalidade(CategoriaFinalidade categoriaFinalidade) {
		return (root, query, cb) -> {
			if (categoriaFinalidade == null) {
				return cb.conjunction();
			}
			var proposta = root.join("proposta");
			return cb.equal(proposta.get("categoriaFinalidade"), categoriaFinalidade);
		};
	}

	private Specification<Contrato> specNomeContraparte(Usuario usuario, String nomeContraparte) {
		return (root, query, cb) -> {
			if (nomeContraparte == null || nomeContraparte.isBlank()) {
				return cb.conjunction();
			}

			var termo = "%" + nomeContraparte.trim().toLowerCase(Locale.ROOT) + "%";
			var proposta = root.join("proposta");
			var solicitanteUsuario = proposta.join("solicitante").join("usuario");
			var credor = proposta.join("credor", JoinType.LEFT);
			var credorUsuario = credor.join("usuario", JoinType.LEFT);

			if (usuario.possuiPapel(Role.CREDOR)) {
				return cb.like(cb.lower(solicitanteUsuario.get("nome")), termo);
			}

			if (usuario.possuiPapel(Role.SOLICITANTE)) {
				return cb.like(cb.lower(credorUsuario.get("nome")), termo);
			}

			return cb.or(
				cb.like(cb.lower(solicitanteUsuario.get("nome")), termo),
				cb.like(cb.lower(credorUsuario.get("nome")), termo)
			);
		};
	}
}
