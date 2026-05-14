package com.api.loanflow.contrato.aplicacao;

import com.api.loanflow.auditoria.aplicacao.AuditoriaService;
import com.api.loanflow.auditoria.dominio.AuditoriaAcao;
import com.api.loanflow.compartilhado.criptografia.HashService;
import com.api.loanflow.compartilhado.excecao.RecursoNaoEncontradoException;
import com.api.loanflow.compartilhado.excecao.RegraNegocioException;
import com.api.loanflow.contrato.api.dto.AssinaturaResponse;
import com.api.loanflow.contrato.api.dto.ContratoResponse;
import com.api.loanflow.contrato.api.dto.IniciarDesafioAssinaturaResponse;
import com.api.loanflow.contrato.dominio.AssinaturaEletronica;
import com.api.loanflow.contrato.dominio.Contrato;
import com.api.loanflow.contrato.dominio.ContratoStatus;
import com.api.loanflow.contrato.dominio.EventoAssinaturaTipo;
import com.api.loanflow.contrato.dominio.MetodoAutenticacaoAssinatura;
import com.api.loanflow.contrato.infraestrutura.persistencia.AssinaturaEletronicaRepository;
import com.api.loanflow.contrato.infraestrutura.persistencia.ContratoRepository;
import com.api.loanflow.notificacao.aplicacao.NotificacaoService;
import com.api.loanflow.notificacao.dominio.TipoNotificacao;
import com.api.loanflow.parcela.aplicacao.ParcelaService;
import com.api.loanflow.proposta.aplicacao.PoliticaCreditoService;
import com.api.loanflow.proposta.aplicacao.PropostaService;
import com.api.loanflow.proposta.dominio.Proposta;
import com.api.loanflow.proposta.dominio.PropostaStatus;
import com.api.loanflow.usuario.aplicacao.UsuarioService;
import com.api.loanflow.usuario.dominio.Role;
import com.api.loanflow.usuario.dominio.Usuario;
import jakarta.persistence.criteria.JoinType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class ContratoService {
	private final ContratoRepository contratoRepository;
	private final AssinaturaEletronicaRepository assinaturaRepository;
	private final PropostaService propostaService;
	private final UsuarioService usuarioService;
	private final HashService hashService;
	private final PdfContratoService pdfContratoService;
	private final ParcelaService parcelaService;
	private final AuditoriaService auditoriaService;
	private final NotificacaoService notificacaoService;
	private final PoliticaCreditoService politicaCreditoService;
	private final AssinaturaDesafioService assinaturaDesafioService;
	private final EventoAssinaturaService eventoAssinaturaService;
	private final ContratoExpiracaoService contratoExpiracaoService;
	private final String signatureTermVersion;

	public ContratoService(
		ContratoRepository contratoRepository,
		AssinaturaEletronicaRepository assinaturaRepository,
		PropostaService propostaService,
		UsuarioService usuarioService,
		HashService hashService,
		PdfContratoService pdfContratoService,
		ParcelaService parcelaService,
		AuditoriaService auditoriaService,
		NotificacaoService notificacaoService,
		PoliticaCreditoService politicaCreditoService,
		AssinaturaDesafioService assinaturaDesafioService,
		EventoAssinaturaService eventoAssinaturaService,
		ContratoExpiracaoService contratoExpiracaoService,
		@Value("${loanflow.signature.term-version}") String signatureTermVersion
	) {
		this.contratoRepository = contratoRepository;
		this.assinaturaRepository = assinaturaRepository;
		this.propostaService = propostaService;
		this.usuarioService = usuarioService;
		this.hashService = hashService;
		this.pdfContratoService = pdfContratoService;
		this.parcelaService = parcelaService;
		this.auditoriaService = auditoriaService;
		this.notificacaoService = notificacaoService;
		this.politicaCreditoService = politicaCreditoService;
		this.assinaturaDesafioService = assinaturaDesafioService;
		this.eventoAssinaturaService = eventoAssinaturaService;
		this.contratoExpiracaoService = contratoExpiracaoService;
		this.signatureTermVersion = signatureTermVersion;
	}

	@Transactional
	public ContratoResponse gerar(Long propostaId, String ipOrigem) {
		var usuario = usuarioService.usuarioAtual();
		var proposta = propostaService.buscarComPermissao(propostaId);
		if (proposta.getStatus() != PropostaStatus.APROVADA) {
			throw new RegraNegocioException("Contrato so pode ser gerado para proposta aprovada.");
		}
		if (contratoRepository.findByPropostaId(propostaId).isPresent()) {
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

		var numero = "LF-" + proposta.getId() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
		var dataGeracao = LocalDateTime.now();
		var dataExpiracaoAssinatura = dataGeracao.plusDays(7);
		var conteudo = montarConteudoContrato(numero, proposta, dataGeracao, dataExpiracaoAssinatura);
		var hashDocumento = hashService.sha256(conteudo);
		var pdfPath = pdfContratoService.gerarContratoPdf(numero, conteudo);
		var hashPdfEmitido = hashService.sha256(pdfContratoService.lerPdf(pdfPath));

		var contrato = new Contrato();
		contrato.setProposta(proposta);
		contrato.setNumeroContrato(numero);
		contrato.setStatus(ContratoStatus.AGUARDANDO_ASSINATURAS);
		contrato.setConteudoSnapshot(conteudo);
		contrato.setHashDocumento(hashDocumento);
		contrato.setPdfPath(pdfPath);
		contrato.setHashPdfEmitido(hashPdfEmitido);
		contrato.setDataExpiracaoAssinatura(dataExpiracaoAssinatura);
		contrato.registrarVerificacaoAssinatura(dataGeracao);
		contrato = contratoRepository.save(contrato);

		auditoriaService.registrar(usuario, AuditoriaAcao.GERAR_CONTRATO, "Contrato", contrato.getId(), "Contrato gerado com hash SHA-256.", ipOrigem);
		notificacaoService.criar(proposta.getSolicitante().getUsuario(), TipoNotificacao.CONTRATO, "Contrato disponivel para assinatura.", "Contrato", contrato.getId());
		notificacaoService.criar(proposta.getCredor().getUsuario(), TipoNotificacao.CONTRATO, "Contrato disponivel para assinatura.", "Contrato", contrato.getId());
		return ContratoResponse.from(contrato);
	}

	@Transactional
	public ContratoResponse detalhar(Long id) {
		contratoExpiracaoService.expirarPendentes();
		return ContratoResponse.from(buscarComPermissao(id));
	}

	@Transactional
	public List<ContratoResponse> listarComFiltros(
		ContratoStatus status,
		String numeroContrato,
		String finalidade
	) {
		contratoExpiracaoService.expirarPendentes();
		var usuario = usuarioService.usuarioAtual();
		var spec = Specification.where(specAcessivelAoUsuario(usuario))
			.and(specStatus(status))
			.and(specNumeroContrato(numeroContrato))
			.and(specFinalidade(finalidade));
		return contratoRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "dataGeracao")).stream()
			.map(ContratoResponse::from)
			.toList();
	}

	@Transactional
	public byte[] baixarPdf(Long id) {
		contratoExpiracaoService.expirarPendentes();
		var contrato = buscarComPermissao(id);
		if (contrato.getPdfPath() == null) {
			throw new RecursoNaoEncontradoException("PDF do contrato nao encontrado.");
		}
		return pdfContratoService.lerPdf(contrato.getPdfPath());
	}

	@Transactional
	public IniciarDesafioAssinaturaResponse iniciarDesafioAssinatura(
		Long id,
		MetodoAutenticacaoAssinatura metodo,
		String ipOrigem,
		String userAgent
	) {
		contratoExpiracaoService.expirarPendentes();
		var usuario = usuarioService.usuarioAtual();
		var contrato = buscarComPermissao(id, usuario);
		exigirContasBancariasDaProposta(contrato.getProposta());
		validarContratoDisponivelParaAssinatura(contrato);
		validarUsuarioAindaNaoAssinou(contrato, usuario);
		identificarPapelSignatario(usuario, contrato.getProposta());
		var desafio = assinaturaDesafioService.iniciarDesafio(contrato, usuario, metodo, ipOrigem, userAgent);
		return IniciarDesafioAssinaturaResponse.from(
			desafio,
			assinaturaDesafioService.resolverMascaraDestino(usuario, desafio.getMetodoAutenticacao())
		);
	}

	@Transactional
	public AssinaturaResponse assinar(Long id, UUID desafioId, String codigo, String ipOrigem, String userAgent) {
		contratoExpiracaoService.expirarPendentes();
		var usuario = usuarioService.usuarioAtual();
		var contrato = buscarComPermissao(id, usuario);
		exigirContasBancariasDaProposta(contrato.getProposta());
		validarContratoDisponivelParaAssinatura(contrato);
		validarUsuarioAindaNaoAssinou(contrato, usuario);

		var papel = identificarPapelSignatario(usuario, contrato.getProposta());
		var desafio = assinaturaDesafioService.validarDesafio(desafioId, codigo, contrato, usuario, ipOrigem, userAgent);
		var documentoIntegro = validarIntegridadeDocumento(contrato);
		var instanteAssinatura = LocalDateTime.now();
		contrato.registrarVerificacaoAssinatura(instanteAssinatura);
		if (!documentoIntegro) {
			throw new RegraNegocioException("A integridade do contrato nao pode ser confirmada para assinatura.");
		}

		var assinatura = new AssinaturaEletronica();
		assinatura.setContrato(contrato);
		assinatura.setUsuario(usuario);
		assinatura.setPapelSignatario(papel);
		assinatura.setIpOrigem(ipOrigem);
		assinatura.setUserAgent(userAgent);
		assinatura.preencherMetadadosAceite(contrato, desafio, signatureTermVersion, documentoIntegro);
		assinatura.setHashAssinatura(
			hashService.sha256(
				contrato.getHashDocumento()
					+ ":"
					+ usuario.getId()
					+ ":"
					+ papel
					+ ":"
					+ desafio.getId()
					+ ":"
					+ signatureTermVersion
					+ ":"
					+ instanteAssinatura
			)
		);
		assinatura = assinaturaRepository.save(assinatura);
		assinaturaDesafioService.consumirDesafio(desafio, ipOrigem, userAgent);
		eventoAssinaturaService.registrar(
			contrato,
			usuario,
			EventoAssinaturaTipo.ASSINATURA_CONFIRMADA,
			"Aceite eletronico confirmado apos reautenticacao.",
			ipOrigem,
			userAgent
		);

		var assinaturasValidas = assinaturaRepository.countByContratoIdAndValidaTrue(contrato.getId());
		if (assinaturasValidas >= 2) {
			contrato.setStatus(ContratoStatus.FORMALIZADO);
			contrato.setDataFormalizacao(instanteAssinatura);
			contrato.getProposta().setStatus(PropostaStatus.CONTRATADA);
			eventoAssinaturaService.registrar(
				contrato,
				usuario,
				EventoAssinaturaTipo.CONTRATO_FORMALIZADO,
				"Contrato formalizado apos a confirmacao das duas assinaturas obrigatorias.",
				ipOrigem,
				userAgent
			);
			auditoriaService.registrar(usuario, AuditoriaAcao.FORMALIZAR, "Contrato", contrato.getId(), "Contrato formalizado apos assinaturas obrigatorias.", ipOrigem);
			parcelaService.gerarParcelas(contrato, usuario, ipOrigem);
		} else {
			contrato.setStatus(ContratoStatus.ASSINADO_PARCIALMENTE);
		}
		auditoriaService.registrar(usuario, AuditoriaAcao.ASSINAR, "Contrato", contrato.getId(), "Aceite eletronico registrado.", ipOrigem);
		return AssinaturaResponse.from(assinatura);
	}

	@Transactional
	public ContratoResponse cancelar(Long id, String ipOrigem) {
		contratoExpiracaoService.expirarPendentes();
		var usuario = usuarioService.usuarioAtual();
		var contrato = buscarComPermissao(id, usuario);
		exigirCredorOuAdmin(usuario, contrato.getProposta());
		if (contrato.getStatus() == ContratoStatus.FORMALIZADO) {
			throw new RegraNegocioException("Contrato formalizado nao pode ser cancelado por este fluxo.");
		}
		contrato.setStatus(ContratoStatus.CANCELADO);
		if (contrato.getProposta().getStatus() != PropostaStatus.CONTRATADA) {
			contrato.getProposta().setStatus(PropostaStatus.CANCELADA);
		}
		auditoriaService.registrar(usuario, AuditoriaAcao.CANCELAR, "Contrato", contrato.getId(), "Contrato cancelado antes da formalizacao.", ipOrigem);
		notificacaoService.criar(contrato.getProposta().getSolicitante().getUsuario(), TipoNotificacao.CONTRATO, "Contrato cancelado.", "Contrato", contrato.getId());
		return ContratoResponse.from(contrato);
	}

	private Contrato buscarComPermissao(Long id) {
		return buscarComPermissao(id, usuarioService.usuarioAtual());
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

	private Role identificarPapelSignatario(Usuario usuario, Proposta proposta) {
		if (proposta.getSolicitante().getUsuario().getId().equals(usuario.getId())) {
			return Role.SOLICITANTE;
		}
		if (proposta.getCredor() != null && proposta.getCredor().getUsuario().getId().equals(usuario.getId())) {
			return Role.CREDOR;
		}
		throw new RegraNegocioException("Usuario nao e signatario deste contrato.");
	}

	private void validarContratoDisponivelParaAssinatura(Contrato contrato) {
		contratoExpiracaoService.expirarSeNecessario(contrato);
		if (contrato.getStatus() == ContratoStatus.EXPIRADO) {
			throw new RegraNegocioException(
				contrato.getMotivoExpiracao() == null || contrato.getMotivoExpiracao().isBlank()
					? "Prazo de assinatura encerrado."
					: contrato.getMotivoExpiracao()
			);
		}
		if (!contrato.podeReceberAssinaturas()) {
			throw new RegraNegocioException("Contrato nao esta disponivel para assinatura.");
		}
	}

	private void validarUsuarioAindaNaoAssinou(Contrato contrato, Usuario usuario) {
		if (assinaturaRepository.existsByContratoIdAndUsuarioId(contrato.getId(), usuario.getId())) {
			throw new RegraNegocioException("Usuario ja assinou este contrato.");
		}
	}

	private boolean validarIntegridadeDocumento(Contrato contrato) {
		if (contrato.getConteudoSnapshot() == null || contrato.getHashDocumento() == null) {
			return false;
		}

		var hashConteudoAtual = hashService.sha256(contrato.getConteudoSnapshot());
		if (!hashConteudoAtual.equals(contrato.getHashDocumento())) {
			return false;
		}

		if (contrato.getPdfPath() == null || contrato.getPdfPath().isBlank()) {
			return false;
		}

		var hashPdfAtual = hashService.sha256(pdfContratoService.lerPdf(contrato.getPdfPath()));
		if (contrato.getHashPdfEmitido() == null || contrato.getHashPdfEmitido().isBlank()) {
			contrato.setHashPdfEmitido(hashPdfAtual);
		}
		return hashPdfAtual.equals(contrato.getHashPdfEmitido());
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

	private String montarConteudoContrato(
		String numero,
		Proposta proposta,
		LocalDateTime dataGeracao,
		LocalDateTime dataExpiracaoAssinatura
	) {
		var solicitante = proposta.getSolicitante();
		var solicitanteUsuario = solicitante.getUsuario();
		var credorUsuario = proposta.getCredor().getUsuario();
		var totalEstimado = calcularTotalEstimado(proposta);
		var jurosEstimados = totalEstimado.subtract(proposta.getValorSolicitado()).setScale(2, RoundingMode.HALF_UP);
		var parcelaEstimada = calcularParcelaEstimada(proposta, totalEstimado);

		return """
			Contrato de Microcredito P2P - Instrumento Particular

			Numero do contrato: %s
			Data de emissao: %s
			Prazo limite para assinaturas: %s
			Plataforma emissora: Loanflow
			Natureza do documento: formalizacao eletronica simulada para fins academicos

			# 1. Resumo executivo
			Valor principal solicitado: %s
			Taxa de juros simulada informada: %s
			Custo total estimado do contrato: %s
			Encargos simulados estimados: %s
			Parcela mensal estimada: %s
			Prazo total: %s meses
			Categoria da finalidade: %s

			# 2. Identificacao das partes
			## Solicitante
			Nome completo: %s
			CPF: %s
			Documento de identidade: %s
			Data de nascimento: %s
			Estado civil: %s
			Nacionalidade: %s
			Profissao: %s
			Tipo de ocupacao informada: %s
			Renda mensal declarada: %s
			Score de credito simulado: %s
			E-mail: %s
			Telefone: %s
			Endereco: %s

			## Credor
			Nome completo: %s
			CPF: %s
			Documento de identidade: %s
			Data de nascimento: %s
			Estado civil: %s
			Nacionalidade: %s
			Profissao: %s
			E-mail: %s
			Telefone: %s
			Endereco: %s

			# 3. Objeto e finalidade da operacao
			Finalidade principal declarada: %s
			Categoria da finalidade: %s
			Detalhamento fornecido pelo solicitante: %s
			- O credor concorda em disponibilizar, em carater simulado, o valor principal descrito neste instrumento.
			- O solicitante declara ciencia de que a proposta foi previamente aceita ou aprovada na plataforma.
			- A finalidade informada integra o historico do contrato e fundamenta a analise registrada no sistema.

			# 4. Condicoes financeiras simuladas
			- Principal concedido: %s
			- Taxa de juros simulada aplicada: %s
			- Montante total estimado ao fim do prazo: %s
			- Quantidade de parcelas previstas: %s
			- Valor medio estimado por parcela: %s
			- As parcelas sao geradas automaticamente pela plataforma apos a formalizacao.

			# 5. Fluxo de formalizacao e aceite eletronico
			- Este contrato nasce no status inicial de aguardando assinaturas.
			- O aceite do solicitante e do credor deve ocorrer ate %s.
			- Cada aceite eletronico registra usuario, papel, IP de origem, agente do navegador e carimbo temporal.
			- A formalizacao definitiva depende das duas assinaturas obrigatorias.
			- A confirmacao do aceite exige reautenticacao curta do usuario antes do registro final.
			- Apos a formalizacao, a plataforma gera o cronograma de parcelas e habilita o registro manual de pagamentos.

			# 6. Declaracoes e responsabilidades das partes
			- As partes declaram que os dados cadastrais utilizados neste documento foram informados por elas na plataforma.
			- O solicitante compromete-se a utilizar o valor conforme a finalidade declarada, ciente do carater academico e simulado da operacao.
			- O credor reconhece que esta contratacao integra um prototipo de microcredito P2P sem liquidacao bancaria automatica.
			- Divergencias cadastrais, cancelamentos, auditorias e registros posteriores permanecem vinculados ao numero deste contrato.

			# 7. Integridade, auditoria e limitacoes do prototipo
			- A integridade logica deste contrato e controlada por hash SHA-256 armazenado pela plataforma.
			- O historico de geracao, assinatura, formalizacao, cancelamento e pagamentos fica sujeito a trilha de auditoria da aplicacao.
			- Este instrumento nao representa integracao bancaria real, liquidacao automatica via PIX nem assinatura ICP-Brasil.
			- O documento serve como evidencia funcional do fluxo de negocio implementado no projeto.

			> Documento emitido eletronicamente pela plataforma Loanflow. Recomenda-se que solicitante e credor revisem os dados cadastrais, valores simulados e finalidade antes do aceite.

			# 8. Espaco para anuencia das partes
			Solicitante: ______________________________________________
			Credor: ___________________________________________________
			Data do aceite eletronico: _________________________________
			""".formatted(
			numero,
			formatarDataHora(dataGeracao),
			formatarDataHora(dataExpiracaoAssinatura),
			formatarMoeda(proposta.getValorSolicitado()),
			formatarPercentual(proposta.getTaxaJuros()),
			formatarMoeda(totalEstimado),
			formatarMoeda(jurosEstimados),
			formatarMoeda(parcelaEstimada),
			proposta.getPrazoMeses(),
			humanizarEnum(proposta.getCategoriaFinalidade()),
			solicitanteUsuario.getNome(),
			formatarCpf(solicitanteUsuario.getCpf()),
			formatarDocumentoIdentidade(solicitanteUsuario),
			formatarData(solicitanteUsuario.getDataNascimento()),
			textoOuNaoInformado(humanizarEnum(solicitanteUsuario.getEstadoCivil())),
			textoOuNaoInformado(solicitanteUsuario.getNacionalidade()),
			textoOuNaoInformado(solicitanteUsuario.getProfissao()),
			textoOuNaoInformado(solicitante.getTipoOcupacao()),
			formatarMoedaOuNaoInformado(solicitante.getRendaMensal()),
			solicitante.getScoreCreditoSimulado() == null ? "Nao informado" : solicitante.getScoreCreditoSimulado().toString(),
			textoOuNaoInformado(solicitanteUsuario.getEmail()),
			textoOuNaoInformado(solicitanteUsuario.getTelefone()),
			formatarEndereco(solicitanteUsuario),
			credorUsuario.getNome(),
			formatarCpf(credorUsuario.getCpf()),
			formatarDocumentoIdentidade(credorUsuario),
			formatarData(credorUsuario.getDataNascimento()),
			textoOuNaoInformado(humanizarEnum(credorUsuario.getEstadoCivil())),
			textoOuNaoInformado(credorUsuario.getNacionalidade()),
			textoOuNaoInformado(credorUsuario.getProfissao()),
			textoOuNaoInformado(credorUsuario.getEmail()),
			textoOuNaoInformado(credorUsuario.getTelefone()),
			formatarEndereco(credorUsuario),
			textoOuNaoInformado(proposta.getFinalidade()),
			humanizarEnum(proposta.getCategoriaFinalidade()),
			textoOuNaoInformado(proposta.getDescricaoDetalhada()),
			formatarMoeda(proposta.getValorSolicitado()),
			formatarPercentual(proposta.getTaxaJuros()),
			formatarMoeda(totalEstimado),
			proposta.getPrazoMeses(),
			formatarMoeda(parcelaEstimada),
			formatarDataHora(dataExpiracaoAssinatura)
		);
	}

	private BigDecimal calcularTotalEstimado(Proposta proposta) {
		var fatorJuros = BigDecimal.ONE.add(proposta.getTaxaJuros().divide(BigDecimal.valueOf(100), 8, RoundingMode.HALF_UP));
		return proposta.getValorSolicitado().multiply(fatorJuros).setScale(2, RoundingMode.HALF_UP);
	}

	private BigDecimal calcularParcelaEstimada(Proposta proposta, BigDecimal totalEstimado) {
		return totalEstimado.divide(BigDecimal.valueOf(proposta.getPrazoMeses()), 2, RoundingMode.HALF_UP);
	}

	private String formatarMoeda(BigDecimal valor) {
		return NumberFormat.getCurrencyInstance(new Locale("pt", "BR")).format(valor);
	}

	private String formatarMoedaOuNaoInformado(BigDecimal valor) {
		return valor == null ? "Nao informado" : formatarMoeda(valor);
	}

	private String formatarPercentual(BigDecimal valor) {
		return valor == null ? "Nao informado" : valor.setScale(2, RoundingMode.HALF_UP).toPlainString() + "%";
	}

	private String formatarData(LocalDate data) {
		return data == null
			? "Nao informado"
			: data.format(DateTimeFormatter.ofPattern("dd/MM/yyyy", new Locale("pt", "BR")));
	}

	private String formatarDataHora(LocalDateTime dataHora) {
		return dataHora.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", new Locale("pt", "BR")));
	}

	private String formatarCpf(String cpf) {
		if (cpf == null) {
			return "Nao informado";
		}
		var digitos = cpf.replaceAll("\\D", "");
		if (digitos.length() != 11) {
			return cpf;
		}
		return "%s.%s.%s-%s".formatted(
			digitos.substring(0, 3),
			digitos.substring(3, 6),
			digitos.substring(6, 9),
			digitos.substring(9)
		);
	}

	private String formatarDocumentoIdentidade(Usuario usuario) {
		if (usuario.getTipoDocumentoIdentidade() == null && (usuario.getDocumentoIdentidade() == null || usuario.getDocumentoIdentidade().isBlank())) {
			return "Nao informado";
		}
		var tipo = textoOuNaoInformado(humanizarEnum(usuario.getTipoDocumentoIdentidade()));
		var numero = textoOuNaoInformado(usuario.getDocumentoIdentidade());
		var orgaoEmissor = usuario.getOrgaoEmissor() == null || usuario.getOrgaoEmissor().isBlank()
			? ""
			: " - " + usuario.getOrgaoEmissor().trim();
		return tipo + ": " + numero + orgaoEmissor;
	}

	private String formatarEndereco(Usuario usuario) {
		if (usuario.getEndereco() == null || !usuario.getEndereco().isInformado()) {
			return "Nao informado";
		}
		var endereco = usuario.getEndereco();
		var base = "%s, %s".formatted(
			textoOuNaoInformado(endereco.getLogradouro()),
			textoOuNaoInformado(endereco.getNumero())
		);
		var complemento = endereco.getComplemento() == null || endereco.getComplemento().isBlank()
			? ""
			: ", " + endereco.getComplemento().trim();
		var localidade = "%s - %s/%s".formatted(
			textoOuNaoInformado(endereco.getBairro()),
			textoOuNaoInformado(endereco.getCidade()),
			textoOuNaoInformado(endereco.getUf())
		);
		var cep = endereco.getCep() == null || endereco.getCep().isBlank()
			? ""
			: " - CEP " + endereco.getCep().trim();
		return base + complemento + ", " + localidade + cep;
	}

	private String humanizarEnum(Enum<?> valor) {
		if (valor == null) {
			return null;
		}
		var partes = valor.name().toLowerCase(Locale.ROOT).split("_");
		var resultado = new StringBuilder();
		for (String parte : partes) {
			if (resultado.length() > 0) {
				resultado.append(' ');
			}
			resultado.append(Character.toUpperCase(parte.charAt(0)));
			if (parte.length() > 1) {
				resultado.append(parte.substring(1));
			}
		}
		return resultado.toString();
	}

	private String textoOuNaoInformado(String valor) {
		return valor == null || valor.isBlank() ? "Nao informado" : valor.trim();
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

	private Specification<Contrato> specFinalidade(String finalidade) {
		return (root, query, cb) -> {
			if (finalidade == null || finalidade.isBlank()) {
				return cb.conjunction();
			}
			var termo = "%" + finalidade.trim().toLowerCase(Locale.ROOT) + "%";
			var proposta = root.join("proposta");
			return cb.or(
				cb.like(cb.lower(proposta.get("finalidade")), termo),
				cb.like(cb.lower(proposta.get("descricaoDetalhada")), termo)
			);
		};
	}
}
