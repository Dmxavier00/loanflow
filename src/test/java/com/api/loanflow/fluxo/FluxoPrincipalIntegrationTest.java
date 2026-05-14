package com.api.loanflow.fluxo;

import com.api.loanflow.contrato.dominio.ContratoStatus;
import com.api.loanflow.contrato.infraestrutura.persistencia.ContratoRepository;
import com.api.loanflow.parcela.infraestrutura.persistencia.ParcelaRepository;
import com.api.loanflow.proposta.dominio.PropostaStatus;
import com.api.loanflow.proposta.infraestrutura.persistencia.PropostaRepository;
import com.api.loanflow.usuario.infraestrutura.persistencia.CredorRepository;
import com.api.loanflow.usuario.infraestrutura.persistencia.UsuarioRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class FluxoPrincipalIntegrationTest {

	private static final AtomicInteger SEQUENCE = new AtomicInteger(1);

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private PropostaRepository propostaRepository;

	@Autowired
	private ContratoRepository contratoRepository;

	@Autowired
	private ParcelaRepository parcelaRepository;

	@Autowired
	private UsuarioRepository usuarioRepository;

	@Autowired
	private CredorRepository credorRepository;

	@Test
	void devePercorrerFluxoCompletoDePropostaAContratoFormalizado() throws Exception {
		var solicitante = registrarSolicitante("fluxo-feliz", new BigDecimal("6000.00"));
		var credor = registrarCredor("fluxo-feliz", new BigDecimal("25000.00"));

		var propostaId = criarProposta(solicitante.token(), new BigDecimal("1500.00"));

		postSemCorpo("/propostas/%d/aceitar".formatted(propostaId), credor.token())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("ACEITA"));

		postSemCorpo("/propostas/%d/iniciar-analise".formatted(propostaId), credor.token())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("EM_ANALISE"));

		postSemCorpo("/propostas/%d/aprovar".formatted(propostaId), credor.token())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("APROVADA"));

		var contratoResponse = postSemCorpo("/contratos/proposta/%d/gerar".formatted(propostaId), credor.token())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("AGUARDANDO_ASSINATURAS"))
			.andReturn()
			.getResponse()
			.getContentAsString();

		var contratoId = readJson(contratoResponse).get("id").asLong();
		var desafioSolicitanteId = iniciarDesafioAssinatura(contratoId, solicitante.token());

		postJson("/contratos/%d/assinar".formatted(contratoId), solicitante.token(), """
			{
			  "aceite": true,
			  "desafioId": "%s",
			  "codigo": "Senha123!"
			}
			""".formatted(desafioSolicitanteId))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.papelSignatario").value("SOLICITANTE"));

		mockMvc.perform(get("/contratos/%d".formatted(contratoId))
				.header("Authorization", bearer(credor.token())))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("ASSINADO_PARCIALMENTE"));

		var desafioCredorId = iniciarDesafioAssinatura(contratoId, credor.token());

		postJson("/contratos/%d/assinar".formatted(contratoId), credor.token(), """
			{
			  "aceite": true,
			  "desafioId": "%s",
			  "codigo": "Senha123!"
			}
			""".formatted(desafioCredorId))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.papelSignatario").value("CREDOR"));

		mockMvc.perform(get("/contratos/%d".formatted(contratoId))
				.header("Authorization", bearer(credor.token())))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("FORMALIZADO"));

		mockMvc.perform(get("/propostas/%d".formatted(propostaId))
				.header("Authorization", bearer(solicitante.token())))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("CONTRATADA"));

		var proposta = propostaRepository.findById(propostaId).orElseThrow();
		var contrato = contratoRepository.findById(contratoId).orElseThrow();

		assertEquals(PropostaStatus.CONTRATADA, proposta.getStatus());
		assertEquals(ContratoStatus.FORMALIZADO, contrato.getStatus());
		assertTrue(parcelaRepository.existsByContratoId(contratoId));
		assertEquals(6, parcelaRepository.findByContratoIdOrderByNumeroAsc(contratoId).size());
		assertTrue(Files.exists(Path.of(contrato.getPdfPath())));
	}

	@Test
	void devePermitirFluxoDeRejeicaoAposAnalise() throws Exception {
		var solicitante = registrarSolicitante("fluxo-rejeicao", new BigDecimal("5500.00"));
		var credor = registrarCredor("fluxo-rejeicao", new BigDecimal("18000.00"));

		var propostaId = criarProposta(solicitante.token(), new BigDecimal("1200.00"));

		postSemCorpo("/propostas/%d/aceitar".formatted(propostaId), credor.token())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("ACEITA"));

		postSemCorpo("/propostas/%d/iniciar-analise".formatted(propostaId), credor.token())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("EM_ANALISE"));

		postSemCorpo("/propostas/%d/rejeitar".formatted(propostaId), credor.token())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("REJEITADA"));

		var proposta = propostaRepository.findById(propostaId).orElseThrow();
		assertEquals(PropostaStatus.REJEITADA, proposta.getStatus());
		assertTrue(contratoRepository.findByPropostaId(propostaId).isEmpty());
	}

	@Test
	void deveBloquearAceiteQuandoCredorNaoTemSaldoSuficiente() throws Exception {
		var solicitante = registrarSolicitante("fluxo-saldo", new BigDecimal("5000.00"));
		var credor = registrarCredor("fluxo-saldo", new BigDecimal("700.00"));

		var credorUsuario = usuarioRepository.findByEmail(credor.email()).orElseThrow();
		var entidadeCredor = credorRepository.findByUsuarioId(credorUsuario.getId()).orElseThrow();
		entidadeCredor.setSaldoDisponivelSimulado(new BigDecimal("500.00"));
		credorRepository.save(entidadeCredor);

		var propostaId = criarProposta(solicitante.token(), new BigDecimal("1200.00"));

		postSemCorpo("/propostas/%d/aceitar".formatted(propostaId), credor.token())
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value("Credor não possui saldo disponível suficiente para esta proposta."));

		var proposta = propostaRepository.findById(propostaId).orElseThrow();
		assertEquals(PropostaStatus.AGUARDANDO_ACEITE, proposta.getStatus());
		assertTrue(contratoRepository.findByPropostaId(propostaId).isEmpty());
	}

	private UUID iniciarDesafioAssinatura(Long contratoId, String token) throws Exception {
		var response = postJson("/contratos/%d/assinatura/desafio".formatted(contratoId), token, """
			{
			  "metodo": "REAUTENTICACAO_SENHA"
			}
			""")
			.andExpect(status().isOk())
			.andReturn()
			.getResponse()
			.getContentAsString();

		return UUID.fromString(readJson(response).get("desafioId").asText());
	}

	private Session registrarSolicitante(String prefixo, BigDecimal rendaMensal) throws Exception {
		int seed = SEQUENCE.getAndIncrement();
		var cpf = gerarCpfValido(seed);
		var email = "%s-solicitante-%d@loanflow.test".formatted(prefixo, seed);
		var response = postPublico("/auth/register", """
			{
			  "nome": "Solicitante %1$d",
			  "cpf": "%2$s",
			  "email": "%3$s",
			  "estadoCivil": "SOLTEIRO",
			  "nacionalidade": "Brasileira",
			  "profissao": "Analista",
			  "dataNascimento": "1994-03-12",
			  "telefone": "(11) 99999-0000",
			  "tipoDocumentoIdentidade": "RG",
			  "documentoIdentidade": "1234567%1$d",
			  "orgaoEmissor": "SSP-SP",
			  "pessoaExpostaPoliticamente": false,
			  "endereco": {
			    "cep": "01001-000",
			    "logradouro": "Praca da Se",
			    "numero": "100",
			    "complemento": "Sala %1$d",
			    "bairro": "Centro",
			    "cidade": "Sao Paulo",
			    "uf": "SP"
			  },
			  "senha": "Senha123!",
			  "papel": "SOLICITANTE",
			  "rendaMensal": %4$s,
			  "tipoOcupacao": "CLT",
			  "saldoDisponivelSimulado": null,
			  "contaBancaria": {
			    "banco": "Banco do Brasil",
			    "agencia": "1234-5",
			    "numeroConta": "987654-%1$d",
			    "tipoConta": "CORRENTE",
			    "chavePix": "solicitante-%1$d@pix.test"
			  }
			}
			""".formatted(seed, cpf, email, rendaMensal.toPlainString()))
			.andExpect(status().isCreated())
			.andReturn()
			.getResponse()
			.getContentAsString();

		return new Session(email, readJson(response).get("accessToken").asText());
	}

	private Session registrarCredor(String prefixo, BigDecimal saldoDisponivel) throws Exception {
		int seed = SEQUENCE.getAndIncrement();
		var cpf = gerarCpfValido(seed);
		var email = "%s-credor-%d@loanflow.test".formatted(prefixo, seed);
		var response = postPublico("/auth/register", """
			{
			  "nome": "Credor %1$d",
			  "cpf": "%2$s",
			  "email": "%3$s",
			  "estadoCivil": "CASADO",
			  "nacionalidade": "Brasileira",
			  "profissao": "Investidor",
			  "dataNascimento": "1988-08-21",
			  "telefone": "(21) 98888-0000",
			  "tipoDocumentoIdentidade": "CNH",
			  "documentoIdentidade": "9988776%1$d",
			  "orgaoEmissor": "DETRAN-RJ",
			  "pessoaExpostaPoliticamente": false,
			  "endereco": {
			    "cep": "20040-020",
			    "logradouro": "Rua da Assembleia",
			    "numero": "50",
			    "complemento": "Conjunto %1$d",
			    "bairro": "Centro",
			    "cidade": "Rio de Janeiro",
			    "uf": "RJ"
			  },
			  "senha": "Senha123!",
			  "papel": "CREDOR",
			  "rendaMensal": null,
			  "tipoOcupacao": null,
			  "saldoDisponivelSimulado": %4$s,
			  "contaBancaria": {
			    "banco": "Banco do Brasil",
			    "agencia": "4321-0",
			    "numeroConta": "123456-%1$d",
			    "tipoConta": "CORRENTE",
			    "chavePix": "credor-%1$d@pix.test"
			  }
			}
			""".formatted(seed, cpf, email, saldoDisponivel.toPlainString()))
			.andExpect(status().isCreated())
			.andReturn()
			.getResponse()
			.getContentAsString();

		return new Session(email, readJson(response).get("accessToken").asText());
	}

	private long criarProposta(String token, BigDecimal valorSolicitado) throws Exception {
		var response = postJson("/propostas", token, """
			{
			  "valorSolicitado": %s,
			  "taxaJuros": 5.00,
			  "prazoMeses": 6,
			  "finalidade": "Capital de giro",
			  "categoriaFinalidade": "CAPITAL_DE_GIRO",
			  "descricaoDetalhada": "Reforco de caixa e compra de estoque."
			}
			""".formatted(valorSolicitado.toPlainString()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("AGUARDANDO_ACEITE"))
			.andReturn()
			.getResponse()
			.getContentAsString();

		return readJson(response).get("id").asLong();
	}

	private org.springframework.test.web.servlet.ResultActions postSemCorpo(String path, String token) throws Exception {
		return mockMvc.perform(post(path)
			.header("Authorization", bearer(token)));
	}

	private org.springframework.test.web.servlet.ResultActions postJson(String path, String token, String body) throws Exception {
		return mockMvc.perform(post(path)
			.header("Authorization", bearer(token))
			.contentType(MediaType.APPLICATION_JSON)
			.content(body));
	}

	private org.springframework.test.web.servlet.ResultActions postPublico(String path, String body) throws Exception {
		return mockMvc.perform(post(path)
			.contentType(MediaType.APPLICATION_JSON)
			.content(body));
	}

	private JsonNode readJson(String body) throws IOException {
		return objectMapper.readTree(body);
	}

	private String bearer(String token) {
		return "Bearer " + token;
	}

	private String gerarCpfValido(int seed) {
		var base = String.format("%09d", seed % 1_000_000_000);
		var primeiroDigito = calcularDigitoCpf(base, 10);
		var segundoDigito = calcularDigitoCpf(base + primeiroDigito, 11);
		return base + primeiroDigito + segundoDigito;
	}

	private int calcularDigitoCpf(String base, int pesoInicial) {
		int soma = 0;
		for (int index = 0; index < base.length(); index++) {
			soma += Character.getNumericValue(base.charAt(index)) * (pesoInicial - index);
		}
		int resto = soma % 11;
		return resto < 2 ? 0 : 11 - resto;
	}

	private record Session(String email, String token) {
	}
}
