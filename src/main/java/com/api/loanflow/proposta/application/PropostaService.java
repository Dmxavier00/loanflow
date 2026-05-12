package com.api.loanflow.proposta.application;

import com.api.loanflow.auditoria.application.AuditoriaService;
import com.api.loanflow.auditoria.domain.AuditoriaAcao;
import com.api.loanflow.notificacao.application.NotificacaoService;
import com.api.loanflow.notificacao.domain.TipoNotificacao;
import com.api.loanflow.proposta.api.dto.AtualizarPropostaRequest;
import com.api.loanflow.proposta.api.dto.CriarPropostaRequest;
import com.api.loanflow.proposta.api.dto.PropostaResponse;
import com.api.loanflow.proposta.domain.CategoriaFinalidade;
import com.api.loanflow.proposta.domain.Proposta;
import com.api.loanflow.proposta.domain.PropostaStatus;
import com.api.loanflow.proposta.infrastructure.persistence.PropostaRepository;
import com.api.loanflow.shared.exception.RecursoNaoEncontradoException;
import com.api.loanflow.shared.exception.RegraNegocioException;
import com.api.loanflow.usuario.application.UsuarioService;
import com.api.loanflow.usuario.domain.Role;
import com.api.loanflow.usuario.domain.Usuario;
import com.api.loanflow.usuario.infrastructure.persistence.CredorRepository;
import com.api.loanflow.usuario.infrastructure.persistence.SolicitanteCreditoRepository;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class PropostaService {
	private static final long PRAZO_EXPIRACAO_PADRAO_DIAS = 7L;

	private final PropostaRepository propostaRepository;
	private final SolicitanteCreditoRepository solicitanteRepository;
	private final CredorRepository credorRepository;
	private final UsuarioService usuarioService;
	private final AuditoriaService auditoriaService;
	private final NotificacaoService notificacaoService;
	private final PoliticaCreditoService politicaCreditoService;
	private final PoliticaCredorService politicaCredorService;

	public PropostaService(
		PropostaRepository propostaRepository,
		SolicitanteCreditoRepository solicitanteRepository,
		CredorRepository credorRepository,
		UsuarioService usuarioService,
		AuditoriaService auditoriaService,
		NotificacaoService notificacaoService,
		PoliticaCreditoService politicaCreditoService,
		PoliticaCredorService politicaCredorService
	) {
		this.propostaRepository = propostaRepository;
		this.solicitanteRepository = solicitanteRepository;
		this.credorRepository = credorRepository;
		this.usuarioService = usuarioService;
		this.auditoriaService = auditoriaService;
		this.notificacaoService = notificacaoService;
		this.politicaCreditoService = politicaCreditoService;
		this.politicaCredorService = politicaCredorService;
	}

	@Transactional
	public PropostaResponse criar(CriarPropostaRequest request, String ipOrigem) {
		var usuario = usuarioService.usuarioAtual();
		var solicitante = solicitanteRepository.findByUsuarioId(usuario.getId())
			.orElseThrow(() -> new RegraNegocioException("Usuário autenticado não possui perfil de solicitante."));
		usuarioService.exigirContaBancaria(usuario, "Cadastre uma conta bancária antes de criar uma proposta.");
		politicaCreditoService.validarNovaContratacao(
			solicitante,
			request.valorSolicitado(),
			request.taxaJuros(),
			request.prazoMeses()
		);

		var proposta = new Proposta();
		proposta.setSolicitante(solicitante);
		proposta.setValorSolicitado(request.valorSolicitado());
		proposta.setTaxaJuros(request.taxaJuros());
		proposta.setPrazoMeses(request.prazoMeses());
		proposta.setFinalidade(request.finalidade());
		proposta.setCategoriaFinalidade(request.categoriaFinalidade());
		proposta.setDescricaoDetalhada(request.descricaoDetalhada());
		proposta.setDataExpiracao(calcularDataExpiracaoPadrao());
		proposta.setStatus(PropostaStatus.AGUARDANDO_ACEITE);
		proposta = propostaRepository.save(proposta);

		auditoriaService.registrar(usuario, AuditoriaAcao.CRIAR, "Proposta", proposta.getId(), "Proposta criada aguardando aceite de credor.", ipOrigem);
		return PropostaResponse.from(proposta);
	}

	@Transactional(readOnly = true)
	public List<PropostaResponse> minhas() {
		var usuario = usuarioService.usuarioAtual();
		return propostaRepository.findBySolicitanteUsuarioIdOrderByDataCriacaoDesc(usuario.getId()).stream()
			.map(PropostaResponse::from)
			.toList();
	}

	@Transactional(readOnly = true)
	public List<PropostaResponse> paraAnalise() {
		var usuario = usuarioService.usuarioAtual();
		return propostaRepository.findByCredorUsuarioIdOrderByDataCriacaoDesc(usuario.getId()).stream()
			.map(PropostaResponse::from)
			.toList();
	}

	@Transactional(readOnly = true)
	public List<PropostaResponse> aguardandoAceite() {
		credorRepository.findByUsuarioId(usuarioService.usuarioAtual().getId())
			.orElseThrow(() -> new RegraNegocioException("Usuário autenticado não possui perfil de credor."));
		return propostaRepository.findByStatusAndCredorIsNullOrderByDataCriacaoDesc(PropostaStatus.AGUARDANDO_ACEITE).stream()
			.map(PropostaResponse::from)
			.toList();
	}

	@Transactional(readOnly = true)
	public List<PropostaResponse> aceitasPeloCredor() {
		var usuario = usuarioService.usuarioAtual();
		return propostaRepository.findByCredorUsuarioIdAndStatusInOrderByDataCriacaoDesc(
			usuario.getId(),
			List.of(
				PropostaStatus.ACEITA,
				PropostaStatus.EM_ANALISE,
				PropostaStatus.APROVADA,
				PropostaStatus.REJEITADA,
				PropostaStatus.CONTRATADA
			)
		).stream()
			.map(PropostaResponse::from)
			.toList();
	}

	@Transactional(readOnly = true)
	public PropostaResponse detalhar(Long id) {
		return PropostaResponse.from(buscarComPermissao(id));
	}

	@Transactional(readOnly = true)
	public List<PropostaResponse> listarComFiltros(
		PropostaStatus status,
		CategoriaFinalidade categoriaFinalidade,
		BigDecimal valorMinimo,
		BigDecimal valorMaximo,
		String finalidade
	) {
		var usuario = usuarioService.usuarioAtual();
		var spec = Specification.where(specAcessivelAoUsuario(usuario))
			.and(specStatus(status))
			.and(specCategoria(categoriaFinalidade))
			.and(specValorMinimo(valorMinimo))
			.and(specValorMaximo(valorMaximo))
			.and(specFinalidade(finalidade));
		return propostaRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "dataCriacao")).stream()
			.map(PropostaResponse::from)
			.toList();
	}

	@Transactional(readOnly = true)
	public List<PropostaResponse> listarPorStatus(PropostaStatus status) {
		var usuario = usuarioService.usuarioAtual();
		return propostaRepository.findByStatusOrderByDataCriacaoDesc(status).stream()
			.filter(proposta -> podeVisualizar(usuario, proposta))
			.map(PropostaResponse::from)
			.toList();
	}

	@Transactional
	public PropostaResponse atualizar(Long id, AtualizarPropostaRequest request, String ipOrigem) {
		var usuario = usuarioService.usuarioAtual();
		var proposta = buscarComPermissao(id);
		exigirSolicitanteDono(usuario, proposta);
		exigirStatusEditavel(proposta);
		usuarioService.exigirContaBancaria(usuario, "Cadastre uma conta bancária antes de atualizar a proposta.");
		politicaCreditoService.validarNovaContratacao(
			proposta.getSolicitante(),
			request.valorSolicitado(),
			request.taxaJuros(),
			request.prazoMeses()
		);
		proposta.setValorSolicitado(request.valorSolicitado());
		proposta.setTaxaJuros(request.taxaJuros());
		proposta.setPrazoMeses(request.prazoMeses());
		proposta.setFinalidade(request.finalidade());
		proposta.setCategoriaFinalidade(request.categoriaFinalidade());
		proposta.setDescricaoDetalhada(request.descricaoDetalhada());
		proposta.setDataExpiracao(calcularDataExpiracaoPadrao());
		auditoriaService.registrar(usuario, AuditoriaAcao.ATUALIZAR, "Proposta", proposta.getId(), "Proposta aguardando aceite atualizada.", ipOrigem);
		return PropostaResponse.from(proposta);
	}

	@Transactional
	public PropostaResponse submeter(Long id, String ipOrigem) {
		var usuario = usuarioService.usuarioAtual();
		var proposta = buscarComPermissao(id);
		exigirSolicitanteDono(usuario, proposta);
		exigirStatus(proposta, PropostaStatus.RASCUNHO, "Somente proposta em rascunho pode ser submetida.");
		usuarioService.exigirContaBancaria(usuario, "Cadastre uma conta bancária antes de submeter a proposta.");
		politicaCreditoService.validarNovaContratacao(
			proposta.getSolicitante(),
			proposta.getValorSolicitado(),
			proposta.getTaxaJuros(),
			proposta.getPrazoMeses()
		);
		proposta.setStatus(PropostaStatus.AGUARDANDO_ACEITE);
		auditoriaService.registrar(usuario, AuditoriaAcao.SUBMETER, "Proposta", proposta.getId(), "Proposta submetida para a fila de aceite.", ipOrigem);
		if (proposta.getCredor() != null) {
			notificacaoService.criar(proposta.getCredor().getUsuario(), TipoNotificacao.PROPOSTA, "Nova proposta disponível para aceite.", "Proposta", proposta.getId());
		}
		return PropostaResponse.from(proposta);
	}

	@Transactional
	public PropostaResponse iniciarAnalise(Long id, String ipOrigem) {
		var usuario = usuarioService.usuarioAtual();
		var proposta = buscarComPermissao(id);
		credorRepository.findByUsuarioId(usuario.getId())
			.orElseThrow(() -> new RegraNegocioException("Usuário autenticado não possui perfil de credor."));
		usuarioService.exigirContaBancaria(usuario, "Cadastre uma conta bancária antes de iniciar a análise de propostas.");
		exigirStatus(proposta, PropostaStatus.ACEITA, "Somente proposta aceita pode entrar em análise.");
		exigirCredorResponsavel(usuario, proposta);
		if (proposta.getDataExpiracao().isBefore(LocalDate.now())) {
			proposta.setStatus(PropostaStatus.EXPIRADA);
			throw new RegraNegocioException("Proposta expirada.");
		}
		proposta.setStatus(PropostaStatus.EM_ANALISE);
		auditoriaService.registrar(usuario, AuditoriaAcao.INICIAR_ANALISE, "Proposta", proposta.getId(), "Credor iniciou análise da proposta.", ipOrigem);
		return PropostaResponse.from(proposta);
	}

	@Transactional
	public PropostaResponse aceitar(Long id, String ipOrigem) {
		var usuario = usuarioService.usuarioAtual();
		usuarioService.exigirContaBancaria(usuario, "Cadastre uma conta bancária antes de aceitar uma proposta.");
		var credor = credorRepository.findByUsuarioId(usuario.getId())
			.orElseThrow(() -> new RegraNegocioException("Usuário autenticado não possui perfil de credor."));
		var proposta = propostaRepository.findById(id)
			.orElseThrow(() -> new RecursoNaoEncontradoException("Proposta não encontrada."));
		exigirStatus(proposta, PropostaStatus.AGUARDANDO_ACEITE, "Somente proposta aguardando aceite pode ser aceita.");
		if (proposta.getCredor() != null) {
			throw new RegraNegocioException("Proposta já foi aceita por outro credor.");
		}
		if (proposta.getDataExpiracao().isBefore(LocalDate.now())) {
			proposta.setStatus(PropostaStatus.EXPIRADA);
			throw new RegraNegocioException("Proposta expirada.");
		}
		politicaCreditoService.validarNovaContratacao(
			proposta.getSolicitante(),
			proposta.getValorSolicitado(),
			proposta.getTaxaJuros(),
			proposta.getPrazoMeses()
		);
		politicaCredorService.validarDisponibilidade(credor, proposta);
		proposta.setCredor(credor);
		proposta.setStatus(PropostaStatus.ACEITA);
		auditoriaService.registrar(usuario, AuditoriaAcao.ACEITAR, "Proposta", proposta.getId(), "Credor aceitou a proposta.", ipOrigem);
		notificacaoService.criar(proposta.getSolicitante().getUsuario(), TipoNotificacao.PROPOSTA, "Sua proposta foi aceita por um credor.", "Proposta", proposta.getId());
		return PropostaResponse.from(proposta);
	}

	@Transactional
	public PropostaResponse aprovar(Long id, String ipOrigem) {
		var usuario = usuarioService.usuarioAtual();
		var proposta = buscarComPermissao(id);
		exigirCredorResponsavel(usuario, proposta);
		exigirStatus(proposta, PropostaStatus.EM_ANALISE, "Somente proposta em análise pode ser aprovada.");
		usuarioService.exigirContaBancaria(usuario, "Cadastre uma conta bancária antes de aprovar a proposta.");
		politicaCreditoService.validarNovaContratacao(
			proposta.getSolicitante(),
			proposta.getValorSolicitado(),
			proposta.getTaxaJuros(),
			proposta.getPrazoMeses()
		);
		politicaCredorService.validarDisponibilidade(proposta.getCredor(), proposta);
		proposta.setStatus(PropostaStatus.APROVADA);
		auditoriaService.registrar(usuario, AuditoriaAcao.APROVAR, "Proposta", proposta.getId(), "Proposta aprovada pelo credor.", ipOrigem);
		notificacaoService.criar(proposta.getSolicitante().getUsuario(), TipoNotificacao.PROPOSTA, "Sua proposta foi aprovada.", "Proposta", proposta.getId());
		return PropostaResponse.from(proposta);
	}

	@Transactional
	public PropostaResponse rejeitar(Long id, String ipOrigem) {
		var usuario = usuarioService.usuarioAtual();
		var proposta = buscarComPermissao(id);
		exigirCredorResponsavel(usuario, proposta);
		exigirStatus(proposta, PropostaStatus.EM_ANALISE, "Somente proposta em análise pode ser rejeitada.");
		proposta.setStatus(PropostaStatus.REJEITADA);
		auditoriaService.registrar(usuario, AuditoriaAcao.REJEITAR, "Proposta", proposta.getId(), "Proposta rejeitada pelo credor.", ipOrigem);
		notificacaoService.criar(proposta.getSolicitante().getUsuario(), TipoNotificacao.PROPOSTA, "Sua proposta foi rejeitada.", "Proposta", proposta.getId());
		return PropostaResponse.from(proposta);
	}

	@Transactional
	public PropostaResponse cancelar(Long id, String ipOrigem) {
		var usuario = usuarioService.usuarioAtual();
		var proposta = buscarComPermissao(id);
		if (!ehAdmin(usuario)) {
			exigirSolicitanteDono(usuario, proposta);
		}
		if (proposta.getStatus() == PropostaStatus.ACEITA || proposta.getStatus() == PropostaStatus.APROVADA || proposta.getStatus() == PropostaStatus.CONTRATADA) {
			throw new RegraNegocioException("Proposta aceita, aprovada ou contratada não pode ser cancelada por este fluxo.");
		}
		proposta.setStatus(PropostaStatus.CANCELADA);
		auditoriaService.registrar(usuario, AuditoriaAcao.CANCELAR, "Proposta", proposta.getId(), "Proposta cancelada.", ipOrigem);
		return PropostaResponse.from(proposta);
	}

	@Transactional(readOnly = true)
	public Proposta buscarComPermissao(Long id) {
		var usuario = usuarioService.usuarioAtual();
		var proposta = propostaRepository.findById(id)
			.orElseThrow(() -> new RecursoNaoEncontradoException("Proposta não encontrada."));
		if (ehAdmin(usuario) || proposta.getSolicitante().getUsuario().getId().equals(usuario.getId())) {
			return proposta;
		}
		if (proposta.getCredor() != null && proposta.getCredor().getUsuario().getId().equals(usuario.getId())) {
			return proposta;
		}
		if (usuario.possuiPapel(Role.CREDOR) && proposta.getCredor() == null && proposta.getStatus() == PropostaStatus.AGUARDANDO_ACEITE) {
			return proposta;
		}
		throw new RecursoNaoEncontradoException("Proposta não encontrada.");
	}

	@Transactional(readOnly = true)
	public Proposta buscarParaFluxoInterno(Long id) {
		return propostaRepository.findById(id)
			.orElseThrow(() -> new RecursoNaoEncontradoException("Proposta não encontrada."));
	}

	private void exigirStatus(Proposta proposta, PropostaStatus esperado, String mensagem) {
		if (proposta.getStatus() != esperado) {
			throw new RegraNegocioException(mensagem);
		}
	}

	private void exigirStatusEditavel(Proposta proposta) {
		if (proposta.getStatus() != PropostaStatus.RASCUNHO && proposta.getStatus() != PropostaStatus.AGUARDANDO_ACEITE) {
			throw new RegraNegocioException("Somente proposta aguardando aceite pode ser atualizada.");
		}
	}

	private void exigirSolicitanteDono(Usuario usuario, Proposta proposta) {
		if (!proposta.getSolicitante().getUsuario().getId().equals(usuario.getId())) {
			throw new RegraNegocioException("Apenas o solicitante responsável pode executar esta ação.");
		}
	}

	private void exigirCredorResponsavel(Usuario usuario, Proposta proposta) {
		if (proposta.getCredor() == null || !proposta.getCredor().getUsuario().getId().equals(usuario.getId())) {
			throw new RegraNegocioException("Apenas o credor responsável pode executar esta ação.");
		}
	}

	private boolean ehAdmin(Usuario usuario) {
		return usuario.possuiPapel(Role.ADMIN);
	}

	private boolean podeVisualizar(Usuario usuario, Proposta proposta) {
		if (ehAdmin(usuario) || proposta.getSolicitante().getUsuario().getId().equals(usuario.getId())) {
			return true;
		}
		if (proposta.getCredor() != null && proposta.getCredor().getUsuario().getId().equals(usuario.getId())) {
			return true;
		}
		return usuario.possuiPapel(Role.CREDOR) && proposta.getCredor() == null && proposta.getStatus() == PropostaStatus.AGUARDANDO_ACEITE;
	}

	private LocalDate calcularDataExpiracaoPadrao() {
		return LocalDate.now().plusDays(PRAZO_EXPIRACAO_PADRAO_DIAS);
	}

	private Specification<Proposta> specAcessivelAoUsuario(Usuario usuario) {
		return (root, query, cb) -> {
			query.distinct(true);
			if (ehAdmin(usuario)) {
				return cb.conjunction();
			}

			var solicitanteUsuario = root.join("solicitante").join("usuario");
			var credor = root.join("credor", JoinType.LEFT);
			var credorUsuario = credor.join("usuario", JoinType.LEFT);

			var predicados = new java.util.ArrayList<Predicate>();
			predicados.add(cb.equal(solicitanteUsuario.get("id"), usuario.getId()));
			predicados.add(cb.equal(credorUsuario.get("id"), usuario.getId()));

			if (usuario.possuiPapel(Role.CREDOR)) {
				predicados.add(cb.and(
					cb.isNull(root.get("credor")),
					cb.equal(root.get("status"), PropostaStatus.AGUARDANDO_ACEITE)
				));
			}

			return cb.or(predicados.toArray(Predicate[]::new));
		};
	}

	private Specification<Proposta> specStatus(PropostaStatus status) {
		return (root, query, cb) -> status == null ? cb.conjunction() : cb.equal(root.get("status"), status);
	}

	private Specification<Proposta> specCategoria(CategoriaFinalidade categoriaFinalidade) {
		return (root, query, cb) -> categoriaFinalidade == null
			? cb.conjunction()
			: cb.equal(root.get("categoriaFinalidade"), categoriaFinalidade);
	}

	private Specification<Proposta> specValorMinimo(BigDecimal valorMinimo) {
		return (root, query, cb) -> valorMinimo == null
			? cb.conjunction()
			: cb.greaterThanOrEqualTo(root.get("valorSolicitado"), valorMinimo);
	}

	private Specification<Proposta> specValorMaximo(BigDecimal valorMaximo) {
		return (root, query, cb) -> valorMaximo == null
			? cb.conjunction()
			: cb.lessThanOrEqualTo(root.get("valorSolicitado"), valorMaximo);
	}

	private Specification<Proposta> specFinalidade(String finalidade) {
		return (root, query, cb) -> {
			if (finalidade == null || finalidade.isBlank()) {
				return cb.conjunction();
			}
			var termo = "%" + finalidade.trim().toLowerCase() + "%";
			return cb.or(
				cb.like(cb.lower(root.get("finalidade")), termo),
				cb.like(cb.lower(root.get("descricaoDetalhada")), termo)
			);
		};
	}
}
