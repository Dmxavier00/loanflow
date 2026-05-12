package com.api.loanflow.autenticacao.aplicacao;

import com.api.loanflow.autenticacao.api.dto.AuthResponse;
import com.api.loanflow.autenticacao.api.dto.LoginRequest;
import com.api.loanflow.autenticacao.api.dto.RegisterRequest;
import com.api.loanflow.seguranca.JwtService;
import com.api.loanflow.compartilhado.excecao.RegraNegocioException;
import com.api.loanflow.compartilhado.validacao.DocumentoValidator;
import com.api.loanflow.usuario.api.dto.AtualizarContaBancariaRequest;
import com.api.loanflow.usuario.api.dto.UsuarioResponse;
import com.api.loanflow.usuario.dominio.Administrador;
import com.api.loanflow.usuario.dominio.BancoCatalogo;
import com.api.loanflow.usuario.dominio.ContaBancaria;
import com.api.loanflow.usuario.dominio.Credor;
import com.api.loanflow.usuario.dominio.Endereco;
import com.api.loanflow.usuario.dominio.Role;
import com.api.loanflow.usuario.dominio.SolicitanteCredito;
import com.api.loanflow.usuario.dominio.Usuario;
import com.api.loanflow.usuario.infraestrutura.persistencia.AdministradorRepository;
import com.api.loanflow.usuario.infraestrutura.persistencia.ContaBancariaRepository;
import com.api.loanflow.usuario.infraestrutura.persistencia.CredorRepository;
import com.api.loanflow.usuario.infraestrutura.persistencia.SolicitanteCreditoRepository;
import com.api.loanflow.usuario.infraestrutura.persistencia.UsuarioRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
public class AuthService {
	private final UsuarioRepository usuarioRepository;
	private final SolicitanteCreditoRepository solicitanteRepository;
	private final CredorRepository credorRepository;
	private final AdministradorRepository administradorRepository;
	private final ContaBancariaRepository contaBancariaRepository;
	private final PasswordEncoder passwordEncoder;
	private final AuthenticationManager authenticationManager;
	private final JwtService jwtService;

	public AuthService(
		UsuarioRepository usuarioRepository,
		SolicitanteCreditoRepository solicitanteRepository,
		CredorRepository credorRepository,
		AdministradorRepository administradorRepository,
		ContaBancariaRepository contaBancariaRepository,
		PasswordEncoder passwordEncoder,
		AuthenticationManager authenticationManager,
		JwtService jwtService
	) {
		this.usuarioRepository = usuarioRepository;
		this.solicitanteRepository = solicitanteRepository;
		this.credorRepository = credorRepository;
		this.administradorRepository = administradorRepository;
		this.contaBancariaRepository = contaBancariaRepository;
		this.passwordEncoder = passwordEncoder;
		this.authenticationManager = authenticationManager;
		this.jwtService = jwtService;
	}

	@Transactional
	public AuthResponse registrar(RegisterRequest request) {
		var email = request.email().trim().toLowerCase();
		var cpf = DocumentoValidator.normalizarCpf(request.cpf());
		if (usuarioRepository.existsByEmail(email)) {
			throw new RegraNegocioException("E-mail já cadastrado.");
		}
		if (usuarioRepository.existsByCpf(cpf)) {
			throw new RegraNegocioException("CPF já cadastrado.");
		}

		var papel = request.papel();
		if (papel == null) {
			throw new RegraNegocioException("Informe um papel de usuário.");
		}
		exigirContaBancariaInicial(request, papel);
		exigirDadosFinanceirosIniciais(request, papel);

		var usuario = new Usuario();
		usuario.setNome(normalizarTexto(request.nome()));
		usuario.setCpf(cpf);
		usuario.setEmail(email);
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
		usuario.setSenhaHash(passwordEncoder.encode(request.senha()));
		usuario.setPapel(papel);
		usuario = usuarioRepository.save(usuario);

		if (papel == Role.SOLICITANTE) {
			var solicitante = new SolicitanteCredito();
			solicitante.setUsuario(usuario);
			solicitante.setRendaMensal(request.rendaMensal());
			solicitante.setTipoOcupacao(normalizarTexto(request.tipoOcupacao()));
			solicitanteRepository.save(solicitante);
		}
		if (papel == Role.CREDOR) {
			var credor = new Credor();
			credor.setUsuario(usuario);
			credor.setSaldoDisponivelSimulado(request.saldoDisponivelSimulado() == null ? BigDecimal.ZERO : request.saldoDisponivelSimulado());
			credorRepository.save(credor);
		}
		if (papel == Role.ADMIN) {
			var administrador = new Administrador();
			administrador.setUsuario(usuario);
			administrador.setNivelAcesso("ADMIN");
			administrador.setSetorResponsavel("TCC");
			administrador.setDataDesignacao(LocalDate.now());
			administrador.setStatusAdministrativo("ATIVO");
			administradorRepository.save(administrador);
		}
		salvarContaBancariaInicial(usuario, request.contaBancaria());

		return tokenResponse(usuario);
	}

	@Transactional(readOnly = true)
	public AuthResponse login(LoginRequest request) {
		var email = request.email().trim().toLowerCase();
		authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, request.senha()));
		var usuario = usuarioRepository.findByEmail(email)
			.orElseThrow(() -> new RegraNegocioException("Credenciais inválidas."));
		return tokenResponse(usuario);
	}

	private AuthResponse tokenResponse(Usuario usuario) {
		var token = jwtService.gerarToken(usuario.getEmail(), usuario.getPapel());
		var solicitante = solicitanteRepository.findByUsuarioId(usuario.getId()).orElse(null);
		var credor = credorRepository.findByUsuarioId(usuario.getId()).orElse(null);
		var administradorId = administradorRepository.findByUsuarioId(usuario.getId()).map(administrador -> administrador.getId()).orElse(null);
		var contaBancariaId = contaBancariaRepository.findByUsuarioId(usuario.getId()).map(contaBancaria -> contaBancaria.getId()).orElse(null);
		return new AuthResponse(
			"Bearer",
			token,
			jwtService.getExpirationSeconds(),
			UsuarioResponse.from(
				usuario,
				solicitante == null ? null : solicitante.getId(),
				credor == null ? null : credor.getId(),
				administradorId,
				contaBancariaId,
				solicitante == null ? null : solicitante.getRendaMensal(),
				credor == null ? null : credor.getSaldoDisponivelSimulado(),
				credor == null ? null : credor.getLimiteOperacoes()
			)
		);
	}

	private Endereco toEndereco(RegisterRequest request) {
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

	private void exigirContaBancariaInicial(RegisterRequest request, Role papel) {
		if ((papel == Role.SOLICITANTE || papel == Role.CREDOR) && request.contaBancaria() == null) {
			throw new RegraNegocioException("Informe uma conta bancária no cadastro inicial.");
		}
	}

	private void exigirDadosFinanceirosIniciais(RegisterRequest request, Role papel) {
		if (papel == Role.SOLICITANTE && (request.rendaMensal() == null || request.rendaMensal().signum() <= 0)) {
			throw new RegraNegocioException("Solicitante precisa informar renda mensal maior que zero no cadastro.");
		}
		if (papel == Role.CREDOR && (request.saldoDisponivelSimulado() == null || request.saldoDisponivelSimulado().signum() <= 0)) {
			throw new RegraNegocioException("Credor precisa informar saldo disponível maior que zero no cadastro.");
		}
	}

	private void salvarContaBancariaInicial(Usuario usuario, AtualizarContaBancariaRequest request) {
		if (request == null) {
			return;
		}
		var contaBancaria = new ContaBancaria();
		contaBancaria.setUsuario(usuario);
		var banco = normalizarTextoObrigatorio(request.banco(), "banco");
		validarBancoSuportado(banco);
		contaBancaria.setBanco(banco);
		contaBancaria.setAgencia(normalizarTextoObrigatorio(request.agencia(), "agência"));
		contaBancaria.setNumeroConta(normalizarTextoObrigatorio(request.numeroConta(), "número da conta"));
		contaBancaria.setTipoConta(request.tipoConta());
		contaBancaria.setChavePix(normalizarTexto(request.chavePix()));
		contaBancariaRepository.save(contaBancaria);
	}

	private String normalizarTexto(String valor) {
		return valor == null ? null : valor.trim();
	}

	private String normalizarTextoObrigatorio(String valor, String campo) {
		var normalizado = normalizarTexto(valor);
		if (normalizado == null || normalizado.isBlank()) {
			throw new RegraNegocioException("Campo obrigatório ausente: " + campo + ".");
		}
		return normalizado;
	}

	private void validarBancoSuportado(String banco) {
		if (!BancoCatalogo.contem(banco)) {
			throw new RegraNegocioException("Banco inválido. Selecione uma opção da lista.");
		}
	}

	private String normalizarUf(String uf) {
		return normalizarTexto(uf) == null ? null : normalizarTexto(uf).toUpperCase();
	}
}
