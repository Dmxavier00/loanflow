package com.api.loanflow.parcela.aplicacao;

import com.api.loanflow.auditoria.aplicacao.AuditoriaService;
import com.api.loanflow.auditoria.dominio.AuditoriaAcao;
import com.api.loanflow.contrato.dominio.Contrato;
import com.api.loanflow.parcela.api.dto.ParcelaResponse;
import com.api.loanflow.parcela.dominio.Parcela;
import com.api.loanflow.parcela.dominio.ParcelaStatus;
import com.api.loanflow.parcela.infraestrutura.persistencia.ParcelaRepository;
import com.api.loanflow.compartilhado.excecao.RecursoNaoEncontradoException;
import com.api.loanflow.usuario.aplicacao.UsuarioService;
import com.api.loanflow.usuario.dominio.Role;
import com.api.loanflow.usuario.dominio.Usuario;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class ParcelaService {
	private final ParcelaRepository parcelaRepository;
	private final AuditoriaService auditoriaService;
	private final UsuarioService usuarioService;

	public ParcelaService(
		ParcelaRepository parcelaRepository,
		AuditoriaService auditoriaService,
		UsuarioService usuarioService
	) {
		this.parcelaRepository = parcelaRepository;
		this.auditoriaService = auditoriaService;
		this.usuarioService = usuarioService;
	}

	@Transactional
	public boolean gerarParcelas(Contrato contrato, Usuario usuario, String ipOrigem) {
		if (parcelaRepository.existsByContratoId(contrato.getId())) {
			return false;
		}
		var proposta = contrato.getProposta();
		var principal = proposta.getValorSolicitado();
		var fatorJuros = BigDecimal.ONE.add(proposta.getTaxaJuros().divide(BigDecimal.valueOf(100), 8, RoundingMode.HALF_UP));
		var total = principal.multiply(fatorJuros).setScale(2, RoundingMode.HALF_UP);
		var prazo = proposta.getPrazoMeses();
		var valorBase = total.divide(BigDecimal.valueOf(prazo), 2, RoundingMode.DOWN);
		var acumulado = BigDecimal.ZERO;
		var parcelas = new ArrayList<Parcela>();

		for (int numero = 1; numero <= prazo; numero++) {
			var parcela = new Parcela();
			parcela.setContrato(contrato);
			parcela.setNumero(numero);
			var valor = numero == prazo ? total.subtract(acumulado) : valorBase;
			parcela.setValorPrevisto(valor);
			parcela.setDataVencimento(LocalDate.now().plusMonths(numero));
			parcelas.add(parcela);
			acumulado = acumulado.add(valor);
		}

		parcelaRepository.saveAll(parcelas);
		auditoriaService.registrar(usuario, AuditoriaAcao.GERAR_PARCELAS, "Contrato", contrato.getId(), "Parcelas geradas após formalização do contrato.", ipOrigem);
		return true;
	}

	@Transactional(readOnly = true)
	public List<ParcelaResponse> listarPorContrato(Long contratoId) {
		var usuario = usuarioService.usuarioAtual();
		return parcelaRepository.findAll(
			Specification.where(specAcessivelAoUsuario(usuario))
				.and(specContratoId(contratoId)),
			Sort.by(Sort.Direction.ASC, "numero")
		).stream()
			.map(ParcelaResponse::from)
			.toList();
	}

	@Transactional(readOnly = true)
	public List<ParcelaResponse> listarComFiltros(
		String numeroContrato,
		ParcelaStatus status,
		LocalDate dataVencimentoInicio,
		LocalDate dataVencimentoFim
	) {
		var usuario = usuarioService.usuarioAtual();
		var spec = Specification.where(specAcessivelAoUsuario(usuario))
			.and(specNumeroContrato(numeroContrato))
			.and(specStatus(status))
			.and(specDataVencimentoInicio(dataVencimentoInicio))
			.and(specDataVencimentoFim(dataVencimentoFim));
		return parcelaRepository.findAll(
			spec,
			Sort.by(
				Sort.Order.asc("dataVencimento"),
				Sort.Order.asc("numero")
			)
		).stream()
			.map(ParcelaResponse::from)
			.toList();
	}

	@Transactional(readOnly = true)
	public Parcela buscar(Long id) {
		return parcelaRepository.findById(id)
			.orElseThrow(() -> new RecursoNaoEncontradoException("Parcela não encontrada."));
	}

	@Transactional(readOnly = true)
	public Parcela buscarComPermissao(Long id) {
		var usuario = usuarioService.usuarioAtual();
		var parcela = buscar(id);
		if (podeAcessar(usuario, parcela)) {
			return parcela;
		}
		throw new RecursoNaoEncontradoException("Parcela não encontrada.");
	}

	@Transactional(readOnly = true)
	public ParcelaResponse detalhar(Long id) {
		return ParcelaResponse.from(buscarComPermissao(id));
	}

	@Transactional
	public List<ParcelaResponse> marcarAtrasadas(String ipOrigem) {
		var admin = usuarioService.usuarioAtual();
		var parcelas = parcelaRepository.findByStatusInAndDataVencimentoBefore(
			List.of(ParcelaStatus.ABERTA, ParcelaStatus.PARCIALMENTE_PAGA),
			LocalDate.now()
		);
		parcelas.forEach(parcela -> parcela.setStatus(ParcelaStatus.EM_ATRASO));
		auditoriaService.registrar(
			admin,
			AuditoriaAcao.ATUALIZAR,
			"Parcela",
			null,
			"Parcelas vencidas marcadas como em atraso: " + parcelas.size() + ".",
			ipOrigem
		);
		return parcelas.stream()
			.map(ParcelaResponse::from)
			.toList();
	}

	private boolean podeAcessar(Usuario usuario, Parcela parcela) {
		if (usuario.possuiPapel(Role.ADMIN)) {
			return true;
		}
		var proposta = parcela.getContrato().getProposta();
		if (proposta.getSolicitante().getUsuario().getId().equals(usuario.getId())) {
			return true;
		}
		return proposta.getCredor() != null && proposta.getCredor().getUsuario().getId().equals(usuario.getId());
	}

	private Specification<Parcela> specAcessivelAoUsuario(Usuario usuario) {
		return (root, query, cb) -> {
			query.distinct(true);
			if (usuario.possuiPapel(Role.ADMIN)) {
				return cb.conjunction();
			}

			var proposta = root.join("contrato").join("proposta");
			var solicitanteUsuario = proposta.join("solicitante").join("usuario");
			var credor = proposta.join("credor", JoinType.LEFT);
			var credorUsuario = credor.join("usuario", JoinType.LEFT);

			return cb.or(
				cb.equal(solicitanteUsuario.get("id"), usuario.getId()),
				cb.equal(credorUsuario.get("id"), usuario.getId())
			);
		};
	}

	private Specification<Parcela> specContratoId(Long contratoId) {
		return (root, query, cb) -> contratoId == null ? cb.conjunction() : cb.equal(root.join("contrato").get("id"), contratoId);
	}

	private Specification<Parcela> specNumeroContrato(String numeroContrato) {
		return (root, query, cb) -> {
			if (numeroContrato == null || numeroContrato.isBlank()) {
				return cb.conjunction();
			}
			return cb.like(
				cb.lower(root.join("contrato").get("numeroContrato")),
				"%" + numeroContrato.trim().toLowerCase(Locale.ROOT) + "%"
			);
		};
	}

	private Specification<Parcela> specStatus(ParcelaStatus status) {
		return (root, query, cb) -> status == null ? cb.conjunction() : cb.equal(root.get("status"), status);
	}

	private Specification<Parcela> specDataVencimentoInicio(LocalDate dataVencimentoInicio) {
		return (root, query, cb) -> dataVencimentoInicio == null
			? cb.conjunction()
			: cb.greaterThanOrEqualTo(root.get("dataVencimento"), dataVencimentoInicio);
	}

	private Specification<Parcela> specDataVencimentoFim(LocalDate dataVencimentoFim) {
		return (root, query, cb) -> dataVencimentoFim == null
			? cb.conjunction()
			: cb.lessThanOrEqualTo(root.get("dataVencimento"), dataVencimentoFim);
	}
}
