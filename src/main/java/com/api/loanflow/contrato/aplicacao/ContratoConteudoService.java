package com.api.loanflow.contrato.aplicacao;

import com.api.loanflow.compartilhado.financeiro.SimulacaoFinanceira;
import com.api.loanflow.proposta.dominio.Proposta;
import com.api.loanflow.usuario.dominio.ContaBancaria;
import com.api.loanflow.usuario.dominio.Usuario;
import com.api.loanflow.usuario.infraestrutura.persistencia.ContaBancariaRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
public class ContratoConteudoService {
	private final ContaBancariaRepository contaBancariaRepository;

	public ContratoConteudoService(ContaBancariaRepository contaBancariaRepository) {
		this.contaBancariaRepository = contaBancariaRepository;
	}

	public String montarConteudoContrato(
		String numero,
		Proposta proposta,
		LocalDateTime dataGeracao,
		LocalDateTime dataFormalizacao
	) {
		var solicitante = proposta.getSolicitante();
		var solicitanteUsuario = solicitante.getUsuario();
		var credor = proposta.getCredor();
		var credorUsuario = credor.getUsuario();
		var credorContaBancaria = buscarContaBancaria(credorUsuario);
		var totalEstimado = SimulacaoFinanceira.calcularTotalComJuros(proposta.getValorSolicitado(), proposta.getTaxaJuros());
		var jurosEstimados = totalEstimado.subtract(proposta.getValorSolicitado()).setScale(2, RoundingMode.HALF_UP);
		var parcelaEstimada = SimulacaoFinanceira.calcularParcelaMedia(totalEstimado, proposta.getPrazoMeses());

		return """
			Contrato LoanFlow
			<!-- contract-template:2026-05-26-neutral-terms -->

			Número do contrato: %s
			Proposta: %s
			Emissão: %s
			Formalização: %s
			Status: Formalizado
			Natureza: documento eletrônico gerado automaticamente pela plataforma LoanFlow

			# 1. Resumo da operação
			Finalidade: %s
			Categoria: %s
			Descrição: %s
			Valor solicitado: %s
			Juros simulados: %s
			Total com juros: %s
			Encargos estimados: %s
			Prazo: %s meses
			Parcela média: %s

			# 2. Partes
			## Solicitante
			Nome: %s
			CPF: %s
			E-mail: %s
			Telefone: %s
			Renda mensal: %s
			Score: %s
			Risco: %s

			## Credor
			Nome: %s
			CPF: %s
			E-mail: %s
			Telefone: %s
			Banco: %s
			Chave Pix: %s
			Total emprestado simulado: %s

			<!-- page-break -->
			# 3. Condições registradas
			- O credor aceitou a proposta na plataforma e o contrato foi formalizado automaticamente.
			- As parcelas são geradas pela plataforma após a formalização do contrato.
			- O pagamento e o acompanhamento das parcelas seguem o fluxo registrado no sistema.
			- Banco e Chave Pix indicam os dados bancários cadastrados pelo credor no momento da emissão.

			# 4. Integridade e observação
			- O documento possui hash SHA-256 armazenado pela plataforma.
			- Este contrato resume os dados do fluxo de negócio implementado no projeto LoanFlow.
			- A operação é simulada e registrada pela plataforma.

			# 5. Ciência das partes
			Solicitante: ______________________________________________
			Credor: ___________________________________________________
			Data da formalização: _____________________________________
			""".formatted(
			numero,
			textoOuNaoInformado(proposta.getNumeroProposta()),
			formatarDataHora(dataGeracao),
			formatarDataHora(dataFormalizacao),
			textoOuNaoInformado(proposta.getFinalidade()),
			humanizarEnum(proposta.getCategoriaFinalidade()),
			textoOuNaoInformado(proposta.getDescricaoDetalhada()),
			formatarMoeda(proposta.getValorSolicitado()),
			formatarPercentual(proposta.getTaxaJuros()),
			formatarMoeda(totalEstimado),
			formatarMoeda(jurosEstimados),
			proposta.getPrazoMeses(),
			formatarMoeda(parcelaEstimada),
			textoOuNaoInformado(solicitanteUsuario.getNome()),
			formatarCpf(solicitanteUsuario.getCpf()),
			textoOuNaoInformado(solicitanteUsuario.getEmail()),
			textoOuNaoInformado(solicitanteUsuario.getTelefone()),
			formatarMoedaOuNaoInformado(solicitante.getRendaMensal()),
			solicitante.getScoreCreditoSimulado() == null ? "Não informado" : solicitante.getScoreCreditoSimulado().toString(),
			textoOuNaoInformado(humanizarEnum(solicitante.getNivelRisco())),
			textoOuNaoInformado(credorUsuario.getNome()),
			formatarCpf(credorUsuario.getCpf()),
			textoOuNaoInformado(credorUsuario.getEmail()),
			textoOuNaoInformado(credorUsuario.getTelefone()),
			textoOuNaoInformado(credorContaBancaria == null ? null : credorContaBancaria.getBanco()),
			textoOuNaoInformado(credorContaBancaria == null ? null : credorContaBancaria.getChavePix()),
			formatarMoedaOuNaoInformado(credor.getTotalEmprestadoSimulado())
		);
	}

	private ContaBancaria buscarContaBancaria(Usuario usuario) {
		if (usuario == null || usuario.getId() == null) {
			return null;
		}
		var conta = contaBancariaRepository.findByUsuarioId(usuario.getId());
		return conta == null ? null : conta.orElse(null);
	}

	private String formatarMoeda(BigDecimal valor) {
		return NumberFormat.getCurrencyInstance(new Locale("pt", "BR")).format(valor);
	}

	private String formatarMoedaOuNaoInformado(BigDecimal valor) {
		return valor == null ? "Não informado" : formatarMoeda(valor);
	}

	private String formatarPercentual(BigDecimal valor) {
		return valor == null ? "Não informado" : valor.setScale(2, RoundingMode.HALF_UP).toPlainString() + "%";
	}

	private String formatarDataHora(LocalDateTime dataHora) {
		return dataHora == null
			? "Não informado"
			: dataHora.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", new Locale("pt", "BR")));
	}

	private String formatarCpf(String cpf) {
		if (cpf == null) {
			return "Não informado";
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

	private String humanizarEnum(Enum<?> valor) {
		if (valor == null) {
			return null;
		}
		var nome = valor.name();
		var humanizado = switch (nome) {
			case "CAPITAL_DE_GIRO" -> "Capital de giro";
			case "QUITACAO_DE_DIVIDAS" -> "Quitação de dívidas";
			case "EMERGENCIA" -> "Emergência";
			case "SAUDE" -> "Saúde";
			case "MEDIO" -> "Médio";
			default -> null;
		};
		if (humanizado != null) {
			return humanizado;
		}
		var partes = valor.name().toLowerCase(Locale.ROOT).split("_");
		var resultado = new StringBuilder();
		for (String parte : partes) {
			if (resultado.length() > 0) {
				resultado.append(' ');
			}
			if ("de".equals(parte) || "da".equals(parte) || "do".equals(parte) || "e".equals(parte)) {
				resultado.append(parte);
			} else {
				resultado.append(Character.toUpperCase(parte.charAt(0)));
				if (parte.length() > 1) {
					resultado.append(parte.substring(1));
				}
			}
		}
		return resultado.toString();
	}

	private String textoOuNaoInformado(String valor) {
		return valor == null || valor.isBlank() ? "Não informado" : valor.trim();
	}
}
