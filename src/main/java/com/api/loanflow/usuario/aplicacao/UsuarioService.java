package com.api.loanflow.usuario.aplicacao;

import com.api.loanflow.auditoria.aplicacao.AuditoriaService;
import com.api.loanflow.auditoria.dominio.AuditoriaAcao;
import com.api.loanflow.usuario.api.dto.AlterarSenhaRequest;
import com.api.loanflow.compartilhado.excecao.RegraNegocioException;
import com.api.loanflow.compartilhado.excecao.RecursoNaoEncontradoException;
import com.api.loanflow.seguranca.SecurityUtils;
import com.api.loanflow.compartilhado.validacao.DocumentoValidator;
import com.api.loanflow.usuario.api.dto.AtualizarContaBancariaRequest;
import com.api.loanflow.usuario.api.dto.AtualizarDadosFinanceirosRequest;
import com.api.loanflow.usuario.api.dto.AtualizarPerfilRequest;
import com.api.loanflow.usuario.api.dto.ContaBancariaResponse;
import com.api.loanflow.usuario.api.dto.CredorLookupResponse;
import com.api.loanflow.usuario.api.dto.UsuarioResponse;
import com.api.loanflow.usuario.dominio.BancoCatalogo;
import com.api.loanflow.usuario.dominio.ContaBancaria;
import com.api.loanflow.usuario.dominio.Credor;
import com.api.loanflow.usuario.dominio.Endereco;
import com.api.loanflow.usuario.dominio.Role;
import com.api.loanflow.usuario.dominio.SolicitanteCredito;
import com.api.loanflow.usuario.dominio.Usuario;
import com.api.loanflow.usuario.dominio.UsuarioStatus;
import com.api.loanflow.usuario.infraestrutura.persistencia.AdministradorRepository;
import com.api.loanflow.usuario.infraestrutura.persistencia.ContaBancariaRepository;
import com.api.loanflow.usuario.infraestrutura.persistencia.CredorRepository;
import com.api.loanflow.usuario.infraestrutura.persistencia.SolicitanteCreditoRepository;
import com.api.loanflow.usuario.infraestrutura.persistencia.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class UsuarioService {
	private final UsuarioRepository usuarioRepository;
	private final SolicitanteCreditoRepository solicitanteRepository;
	private final CredorRepository credorRepository;
	private final AdministradorRepository administradorRepository;
	private final ContaBancariaRepository contaBancariaRepository;
	private final AuditoriaService auditoriaService;
	private final PasswordEncoder passwordEncoder;

	public UsuarioService(
		UsuarioRepository usuarioRepository,
		SolicitanteCreditoRepository solicitanteRepository,
		CredorRepository credorRepository,
		AdministradorRepository administradorRepository,
		ContaBancariaRepository contaBancariaRepository,
		AuditoriaService auditoriaService,
		PasswordEncoder passwordEncoder
	) {
		this.usuarioRepository = usuarioRepository;
		this.solicitanteRepository = solicitanteRepository;
		this.credorRepository = credorRepository;
		this.administradorRepository = administradorRepository;
		this.contaBancariaRepository = contaBancariaRepository;
		this.auditoriaService = auditoriaService;
		this.passwordEncoder = passwordEncoder;
	}

	@Transactional(readOnly = true)
	public Usuario usuarioAtual() {
		var email = SecurityUtils.emailAutenticado();
		return usuarioRepository.findByEmail(email)
			.orElseThrow(() -> new RecursoNaoEncontradoException("UsuÃ¡rio autenticado nÃ£o encontrado."));
	}

	@Transactional(readOnly = true)
	public Usuario buscarPorId(Long id) {
		return usuarioRepository.findById(id)
			.orElseThrow(() -> new RecursoNaoEncontradoException("UsuÃ¡rio nÃ£o encontrado."));
	}

	@Transactional(readOnly = true)
	public UsuarioResponse meuPerfil() {
		return toResponse(usuarioAtual());
	}

	@Transactional(readOnly = true)
	public void exigirContaBancaria(Usuario usuario, String mensagemSeAusente) {
		exigirContaBancaria(usuario.getId(), mensagemSeAusente);
	}

	@Transactional(readOnly = true)
	public void exigirContaBancaria(Long usuarioId, String mensagemSeAusente) {
		if (!contaBancariaRepository.existsByUsuarioId(usuarioId)) {
			throw new RegraNegocioException(mensagemSeAusente);
		}
	}

	@Transactional(readOnly = true)
	public void validarSenhaAtual(Usuario usuario, String senha) {
		if (senha == null || senha.isBlank() || !passwordEncoder.matches(senha, usuario.getSenhaHash())) {
			throw new RegraNegocioException("Senha atual invalida para confirmar a assinatura.");
		}
	}

	@Transactional(readOnly = true)
	public ContaBancariaResponse minhaContaBancaria() {
		var usuario = usuarioAtual();
		var contaBancaria = contaBancariaRepository.findByUsuarioId(usuario.getId())
			.orElseThrow(() -> new RecursoNaoEncontradoException("Conta bancÃ¡ria nÃ£o cadastrada."));
		return ContaBancariaResponse.from(contaBancaria);
	}

	@Transactional
	public UsuarioResponse atualizarMeuPerfil(AtualizarPerfilRequest request) {
		var usuario = usuarioAtual();
		usuario.setNome(normalizarTexto(request.nome()));
		usuario.setEstadoCivil(request.estadoCivil());
		usuario.setNacionalidade(normalizarTexto(request.nacionalidade()));
		usuario.setProfissao(normalizarTexto(request.profissao()));
		usuario.setDataNascimento(request.dataNascimento());
		usuario.setTelefone(normalizarTexto(request.telefone()));
		usuario.setTipoDocumentoIdentidade(request.tipoDocumentoIdentidade());
		usuario.setDocumentoIdentidade(normalizarTexto(request.documentoIdentidade()));
		usuario.setOrgaoEmissor(normalizarTexto(request.orgaoEmissor()));
		usuario.setPessoaExpostaPoliticamente(Boolean.TRUE.equals(request.pessoaExpostaPoliticamente()));
		usuario.setEndereco(toEndereco(request));
		return toResponse(usuario);
	}

	@Transactional
	public ContaBancariaResponse salvarMinhaContaBancaria(AtualizarContaBancariaRequest request) {
		var usuario = usuarioAtual();
		var contaBancaria = contaBancariaRepository.findByUsuarioId(usuario.getId())
			.orElseGet(() -> novaContaBancaria(usuario));
		var banco = normalizarTextoObrigatorio(request.banco(), "banco");
		validarBancoSuportado(banco);
		contaBancaria.setBanco(banco);
		contaBancaria.setAgencia(normalizarTextoObrigatorio(request.agencia(), "agÃªncia"));
		contaBancaria.setNumeroConta(normalizarTextoObrigatorio(request.numeroConta(), "nÃºmero da conta"));
		contaBancaria.setTipoConta(request.tipoConta());
		contaBancaria.setChavePix(normalizarTexto(request.chavePix()));
		contaBancaria = contaBancariaRepository.save(contaBancaria);
		return ContaBancariaResponse.from(contaBancaria);
	}

	@Transactional
	public void alterarMinhaSenha(AlterarSenhaRequest request) {
		var usuario = usuarioAtual();
		if (passwordEncoder.matches(request.senha(), usuario.getSenhaHash())) {
			return;
		}
		usuario.setSenhaHash(passwordEncoder.encode(request.senha()));
	}

	@Transactional
	public UsuarioResponse atualizarMeusDadosFinanceiros(AtualizarDadosFinanceirosRequest request) {
		var usuario = usuarioAtual();

		if (usuario.possuiPapel(Role.SOLICITANTE)) {
			var solicitante = solicitanteRepository.findByUsuarioId(usuario.getId())
				.orElseThrow(() -> new RecursoNaoEncontradoException("Perfil financeiro do solicitante nÃƒÂ£o encontrado."));
			exigirRendaMensalValida(request.rendaMensal());
			if (request.saldoDisponivelSimulado() != null || request.limiteOperacoes() != null) {
				throw new RegraNegocioException("Solicitante pode atualizar apenas a renda mensal neste cadastro.");
			}
			solicitante.setRendaMensal(request.rendaMensal());
			return toResponse(usuario, solicitante, null);
		}

		if (usuario.possuiPapel(Role.CREDOR)) {
			var credor = credorRepository.findByUsuarioId(usuario.getId())
				.orElseThrow(() -> new RecursoNaoEncontradoException("Perfil financeiro do credor nÃƒÂ£o encontrado."));
			exigirSaldoDisponivelValido(request.saldoDisponivelSimulado());
			exigirLimiteOperacoesValido(request.limiteOperacoes());
			if (request.rendaMensal() != null) {
				throw new RegraNegocioException("Credor pode atualizar apenas saldo disponÃƒÂ­vel e limite de operaÃƒÂ§ÃƒÂµes neste cadastro.");
			}
			credor.setSaldoDisponivelSimulado(request.saldoDisponivelSimulado());
			credor.setLimiteOperacoes(request.limiteOperacoes());
			return toResponse(usuario, null, credor);
		}

		throw new RegraNegocioException("Perfil atual nÃƒÂ£o possui dados financeiros editÃƒÂ¡veis.");
	}

	@Transactional(readOnly = true)
	public List<UsuarioResponse> listar() {
		return usuarioRepository.findAll().stream()
			.map(this::toResponse)
			.toList();
	}

	@Transactional(readOnly = true)
	public List<CredorLookupResponse> listarCredores(String nome) {
		var termo = nome == null ? "" : nome.trim();
		var credores = termo.isBlank()
			? credorRepository.findByUsuarioStatusOrderByUsuarioNomeAsc(UsuarioStatus.ATIVO)
			: credorRepository.findByUsuarioStatusAndUsuarioNomeContainingIgnoreCaseOrderByUsuarioNomeAsc(UsuarioStatus.ATIVO, termo);
		return credores.stream()
			.map(CredorLookupResponse::from)
			.toList();
	}

	@Transactional
	public UsuarioResponse bloquear(Long id, String ipOrigem) {
		var admin = usuarioAtual();
		var usuario = buscarPorId(id);
		usuario.setStatus(UsuarioStatus.BLOQUEADO);
		auditoriaService.registrar(admin, AuditoriaAcao.ATUALIZAR, "UsuÃ¡rio", usuario.getId(), "UsuÃ¡rio bloqueado pelo administrador.", ipOrigem);
		return toResponse(usuario);
	}

	@Transactional
	public UsuarioResponse reativar(Long id, String ipOrigem) {
		var admin = usuarioAtual();
		var usuario = buscarPorId(id);
		usuario.setStatus(UsuarioStatus.ATIVO);
		auditoriaService.registrar(admin, AuditoriaAcao.ATUALIZAR, "UsuÃ¡rio", usuario.getId(), "UsuÃ¡rio reativado pelo administrador.", ipOrigem);
		return toResponse(usuario);
	}

	private UsuarioResponse toResponse(Usuario usuario) {
		var solicitante = solicitanteRepository.findByUsuarioId(usuario.getId()).orElse(null);
		var credor = credorRepository.findByUsuarioId(usuario.getId()).orElse(null);
		var administradorId = administradorRepository.findByUsuarioId(usuario.getId()).map(administrador -> administrador.getId()).orElse(null);
		var contaBancariaId = contaBancariaRepository.findByUsuarioId(usuario.getId()).map(contaBancaria -> contaBancaria.getId()).orElse(null);
		return UsuarioResponse.from(
			usuario,
			solicitante == null ? null : solicitante.getId(),
			credor == null ? null : credor.getId(),
			administradorId,
			contaBancariaId,
			solicitante == null ? null : solicitante.getRendaMensal(),
			credor == null ? null : credor.getSaldoDisponivelSimulado(),
			credor == null ? null : credor.getLimiteOperacoes()
		);
	}

	private UsuarioResponse toResponse(Usuario usuario, SolicitanteCredito solicitante, Credor credor) {
		var resolvedSolicitante = solicitante != null ? solicitante : solicitanteRepository.findByUsuarioId(usuario.getId()).orElse(null);
		var resolvedCredor = credor != null ? credor : credorRepository.findByUsuarioId(usuario.getId()).orElse(null);
		var administradorId = administradorRepository.findByUsuarioId(usuario.getId()).map(administrador -> administrador.getId()).orElse(null);
		var contaBancariaId = contaBancariaRepository.findByUsuarioId(usuario.getId()).map(contaBancaria -> contaBancaria.getId()).orElse(null);
		return UsuarioResponse.from(
			usuario,
			resolvedSolicitante == null ? null : resolvedSolicitante.getId(),
			resolvedCredor == null ? null : resolvedCredor.getId(),
			administradorId,
			contaBancariaId,
			resolvedSolicitante == null ? null : resolvedSolicitante.getRendaMensal(),
			resolvedCredor == null ? null : resolvedCredor.getSaldoDisponivelSimulado(),
			resolvedCredor == null ? null : resolvedCredor.getLimiteOperacoes()
		);
	}

	private Endereco toEndereco(AtualizarPerfilRequest request) {
		var endereco = new Endereco();
		endereco.setCep(DocumentoValidator.normalizarCep(request.endereco().cep()));
		endereco.setLogradouro(normalizarTexto(request.endereco().logradouro()));
		endereco.setNumero(normalizarTexto(request.endereco().numero()));
		endereco.setComplemento(normalizarTexto(request.endereco().complemento()));
		endereco.setBairro(normalizarTexto(request.endereco().bairro()));
		endereco.setCidade(normalizarTexto(request.endereco().cidade()));
		endereco.setUf(normalizarUf(request.endereco().uf()));
		return endereco;
	}

	private ContaBancaria novaContaBancaria(Usuario usuario) {
		var contaBancaria = new ContaBancaria();
		contaBancaria.setUsuario(usuario);
		return contaBancaria;
	}

	private String normalizarTexto(String valor) {
		return valor == null ? null : valor.trim();
	}

	private String normalizarTextoObrigatorio(String valor, String campo) {
		var normalizado = normalizarTexto(valor);
		if (normalizado == null || normalizado.isBlank()) {
			throw new RegraNegocioException("Campo obrigatÃ³rio ausente: " + campo + ".");
		}
		return normalizado;
	}

	private void exigirRendaMensalValida(BigDecimal rendaMensal) {
		if (rendaMensal == null || rendaMensal.signum() <= 0) {
			throw new RegraNegocioException("Informe uma renda mensal maior que zero.");
		}
	}

	private void exigirSaldoDisponivelValido(BigDecimal saldoDisponivelSimulado) {
		if (saldoDisponivelSimulado == null || saldoDisponivelSimulado.signum() <= 0) {
			throw new RegraNegocioException("Informe um saldo disponÃƒÂ­vel maior que zero.");
		}
	}

	private void exigirLimiteOperacoesValido(Integer limiteOperacoes) {
		if (limiteOperacoes == null || limiteOperacoes <= 0) {
			throw new RegraNegocioException("Informe um limite de operaÃƒÂ§ÃƒÂµes maior que zero.");
		}
	}

	private void validarBancoSuportado(String banco) {
		if (!BancoCatalogo.contem(banco)) {
			throw new RegraNegocioException("Banco invÃ¡lido. Selecione uma opÃ§Ã£o da lista.");
		}
	}

	private String normalizarUf(String uf) {
		return normalizarTexto(uf) == null ? null : normalizarTexto(uf).toUpperCase();
	}
}
