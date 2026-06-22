package com.api.loanflow.autenticacao.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthRegisterBankAccountWebTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void registerDeveCriarContaBancariaNoCadastroInicial() throws Exception {
		mockMvc.perform(post("/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "nome": "Maria de Souza",
					  "cpf": "529.982.247-25",
					  "email": "maria.cadastro@loanflow.test",
					  "estadoCivil": "SOLTEIRO",
					  "nacionalidade": "Brasileira",
					  "profissao": "Analista",
					  "dataNascimento": "1994-03-12",
					  "telefone": "(11) 99999-0000",
					  "tipoDocumentoIdentidade": "RG",
					  "documentoIdentidade": "12345678",
					  "orgaoEmissor": "SSP-SP",
					  "pessoaExpostaPoliticamente": false,
					  "endereco": {
					    "cep": "01001-000",
					    "logradouro": "Praca da Se",
					    "numero": "100",
					    "complemento": "Sala 4",
					    "bairro": "Centro",
					    "cidade": "Sao Paulo",
					    "uf": "SP"
					  },
					  "senha": "Senha123!",
					  "papel": "SOLICITANTE",
					  "rendaMensal": 3500.00,
					  "tipoOcupacao": "CLT",
					  "saldoDisponivelSimulado": null,
					  "contaBancaria": {
					    "banco": "Banco do Brasil",
					    "agencia": "1234-5",
					    "numeroConta": "987654-3",
					    "tipoConta": "CORRENTE",
					    "chavePix": "maria@pix.test"
					  }
					}
					"""))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.tokenType").value("Bearer"))
			.andExpect(jsonPath("$.accessToken").isString())
			.andExpect(jsonPath("$.usuario.email").value("maria.cadastro@loanflow.test"))
			.andExpect(jsonPath("$.usuario.contaBancariaId").isNumber())
			.andExpect(jsonPath("$.usuario.scoreCredito").value(75))
			.andExpect(jsonPath("$.usuario.nivelRisco").value("BAIXO"))
			.andExpect(jsonPath("$.usuario.solicitanteId").isNumber());
	}

	@Test
	void registerDeveExigirContaBancariaNoCadastroInicial() throws Exception {
		mockMvc.perform(post("/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "nome": "Joao Pereira",
					  "cpf": "111.444.777-35",
					  "email": "joao.semconta@loanflow.test",
					  "estadoCivil": "CASADO",
					  "nacionalidade": "Brasileira",
					  "profissao": "Consultor",
					  "dataNascimento": "1990-08-21",
					  "telefone": "(21) 98888-1111",
					  "tipoDocumentoIdentidade": "CNH",
					  "documentoIdentidade": "99887766",
					  "orgaoEmissor": "DETRAN-RJ",
					  "pessoaExpostaPoliticamente": false,
					  "endereco": {
					    "cep": "20040-020",
					    "logradouro": "Rua da Assembleia",
					    "numero": "50",
					    "complemento": "",
					    "bairro": "Centro",
					    "cidade": "Rio de Janeiro",
					    "uf": "RJ"
					  },
					  "senha": "Senha123!",
					  "papel": "CREDOR",
					  "rendaMensal": null,
					  "tipoOcupacao": null,
					  "saldoDisponivelSimulado": 10000.00
					}
					"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.status").value(400))
			.andExpect(jsonPath("$.error").value("Bad Request"))
			.andExpect(jsonPath("$.message").value("Informe uma conta bancária no cadastro inicial."));
	}

	@Test
	void registerDeveExigirRendaMensalPositivaParaSolicitante() throws Exception {
		mockMvc.perform(post("/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "nome": "Bruna Sem Renda",
					  "cpf": "529.982.247-25",
					  "email": "bruna.sem.renda@loanflow.test",
					  "estadoCivil": "SOLTEIRO",
					  "nacionalidade": "Brasileira",
					  "profissao": "Analista",
					  "dataNascimento": "1995-06-18",
					  "telefone": "(11) 97777-2020",
					  "tipoDocumentoIdentidade": "RG",
					  "documentoIdentidade": "22334455",
					  "orgaoEmissor": "SSP-SP",
					  "pessoaExpostaPoliticamente": false,
					  "endereco": {
					    "cep": "01001-000",
					    "logradouro": "Praca da Se",
					    "numero": "120",
					    "complemento": "",
					    "bairro": "Centro",
					    "cidade": "Sao Paulo",
					    "uf": "SP"
					  },
					  "senha": "Senha123!",
					  "papel": "SOLICITANTE",
					  "rendaMensal": 0,
					  "tipoOcupacao": "CLT",
					  "saldoDisponivelSimulado": null,
					  "contaBancaria": {
					    "banco": "Banco do Brasil",
					    "agencia": "1234-5",
					    "numeroConta": "987654-3",
					    "tipoConta": "CORRENTE",
					    "chavePix": "bruna@pix.test"
					  }
					}
					"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.status").value(400))
			.andExpect(jsonPath("$.error").value("Bad Request"))
			.andExpect(jsonPath("$.message").value("Solicitante precisa informar renda mensal maior que zero no cadastro."));
	}

	@Test
	void registerDeveExigirSaldoDisponivelPositivoParaCredor() throws Exception {
		mockMvc.perform(post("/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "nome": "Rafael Sem Saldo",
					  "cpf": "111.444.777-35",
					  "email": "rafael.sem.saldo@loanflow.test",
					  "estadoCivil": "CASADO",
					  "nacionalidade": "Brasileira",
					  "profissao": "Consultor",
					  "dataNascimento": "1989-09-09",
					  "telefone": "(21) 98888-2222",
					  "tipoDocumentoIdentidade": "CNH",
					  "documentoIdentidade": "88997766",
					  "orgaoEmissor": "DETRAN-RJ",
					  "pessoaExpostaPoliticamente": false,
					  "endereco": {
					    "cep": "20040-020",
					    "logradouro": "Rua da Assembleia",
					    "numero": "50",
					    "complemento": "",
					    "bairro": "Centro",
					    "cidade": "Rio de Janeiro",
					    "uf": "RJ"
					  },
					  "senha": "Senha123!",
					  "papel": "CREDOR",
					  "rendaMensal": null,
					  "tipoOcupacao": null,
					  "saldoDisponivelSimulado": 0,
					  "contaBancaria": {
					    "banco": "Banco do Brasil",
					    "agencia": "9999-9",
					    "numeroConta": "123456-7",
					    "tipoConta": "CORRENTE",
					    "chavePix": "rafael@pix.test"
					  }
					}
					"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.status").value(400))
			.andExpect(jsonPath("$.error").value("Bad Request"))
			.andExpect(jsonPath("$.message").value("Credor precisa informar saldo disponível maior que zero no cadastro."));
	}

	@Test
	void registerDeveRejeitarBancoForaDaLista() throws Exception {
		mockMvc.perform(post("/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "nome": "Carla Oliveira",
					  "cpf": "390.533.447-05",
					  "email": "carla.banco@loanflow.test",
					  "estadoCivil": "SOLTEIRO",
					  "nacionalidade": "Brasileira",
					  "profissao": "Designer",
					  "dataNascimento": "1992-04-15",
					  "telefone": "(31) 97777-1111",
					  "tipoDocumentoIdentidade": "RG",
					  "documentoIdentidade": "55443322",
					  "orgaoEmissor": "SSP-MG",
					  "pessoaExpostaPoliticamente": false,
					  "endereco": {
					    "cep": "30130-110",
					    "logradouro": "Avenida Afonso Pena",
					    "numero": "900",
					    "complemento": "",
					    "bairro": "Centro",
					    "cidade": "Belo Horizonte",
					    "uf": "MG"
					  },
					  "senha": "Senha123!",
					  "papel": "SOLICITANTE",
					  "rendaMensal": 4200.00,
					  "tipoOcupacao": "CLT",
					  "saldoDisponivelSimulado": null,
					  "contaBancaria": {
					    "banco": "Banco Inventado",
					    "agencia": "1234-5",
					    "numeroConta": "987654-3",
					    "tipoConta": "CORRENTE",
					    "chavePix": "carla@pix.test"
					  }
					}
					"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.status").value(400))
			.andExpect(jsonPath("$.error").value("Bad Request"))
			.andExpect(jsonPath("$.message").value("Banco inválido. Selecione uma opção da lista."));
	}

	@Test
	void loginDeveRetornarMensagemClaraQuandoCredenciaisForemInvalidas() throws Exception {
		mockMvc.perform(post("/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "nome": "Paula Login",
					  "cpf": "529.982.247-25",
					  "email": "paula.login@loanflow.test",
					  "estadoCivil": "SOLTEIRO",
					  "nacionalidade": "Brasileira",
					  "profissao": "Analista",
					  "dataNascimento": "1993-02-10",
					  "telefone": "(11) 95555-0000",
					  "tipoDocumentoIdentidade": "RG",
					  "documentoIdentidade": "44556677",
					  "orgaoEmissor": "SSP-SP",
					  "pessoaExpostaPoliticamente": false,
					  "endereco": {
					    "cep": "01001-000",
					    "logradouro": "Praca da Se",
					    "numero": "100",
					    "complemento": "Sala 7",
					    "bairro": "Centro",
					    "cidade": "Sao Paulo",
					    "uf": "SP"
					  },
					  "senha": "Senha123!",
					  "papel": "SOLICITANTE",
					  "rendaMensal": 3500.00,
					  "tipoOcupacao": "CLT",
					  "saldoDisponivelSimulado": null,
					  "contaBancaria": {
					    "banco": "Banco do Brasil",
					    "agencia": "1234-5",
					    "numeroConta": "987654-3",
					    "tipoConta": "CORRENTE",
					    "chavePix": "paula@pix.test"
					  }
					}
					"""))
			.andExpect(status().isCreated());

		mockMvc.perform(post("/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "email": "paula.login@loanflow.test",
					  "senha": "SenhaErrada123!"
					}
					"""))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.status").value(401))
			.andExpect(jsonPath("$.error").value("Unauthorized"))
			.andExpect(jsonPath("$.message").value("Credenciais inválidas."));
	}
}
