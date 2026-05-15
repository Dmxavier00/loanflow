import { formatCep } from './validation';

const currencyFormatter = new Intl.NumberFormat('pt-BR', {
  style: 'currency',
  currency: 'BRL'
});

const dateFormatter = new Intl.DateTimeFormat('pt-BR', {
  dateStyle: 'short'
});

const dateTimeFormatter = new Intl.DateTimeFormat('pt-BR', {
  dateStyle: 'short',
  timeStyle: 'short'
});

const labelWordReplacements = {
  Analise: 'Análise',
  Autenticacao: 'Autenticação',
  Codigo: 'Código',
  Credito: 'Crédito',
  Descricao: 'Descrição',
  Dividas: 'Dívidas',
  Eletronica: 'Eletrônica',
  Eletronico: 'Eletrônico',
  Emissao: 'Emissão',
  Endereco: 'Endereço',
  Estavel: 'Estável',
  Expiracao: 'Expiração',
  Historico: 'Histórico',
  Nao: 'Não',
  Numero: 'Número',
  Operacao: 'Operação',
  Operacoes: 'Operações',
  Orgao: 'Órgão',
  Periodo: 'Período',
  Poupanca: 'Poupança',
  Proxima: 'Próxima',
  Proximo: 'Próximo',
  Quitacao: 'Quitação',
  Reautenticacao: 'Reautenticação',
  Saude: 'Saúde',
  Ultimo: 'Último',
  Ultimos: 'Últimos',
  Uniao: 'União',
  Usuario: 'Usuário',
  Usuarios: 'Usuários',
  Valido: 'Válido',
  Visualizacao: 'Visualização',
  Viuvo: 'Viúvo'
};

export function formatCurrency(value) {
  if (value === null || value === undefined || Number.isNaN(Number(value))) {
    return '-';
  }
  return currencyFormatter.format(Number(value));
}

export function formatDate(value) {
  if (!value) {
    return '-';
  }
  return dateFormatter.format(new Date(value));
}

export function formatDateTime(value) {
  if (!value) {
    return '-';
  }
  return dateTimeFormatter.format(new Date(value));
}

export function formatLabel(value) {
  if (!value) {
    return '-';
  }
  return value
    .toString()
    .toLowerCase()
    .replaceAll('_', ' ')
    .replace(/\b\w/g, (letter) => letter.toUpperCase())
    .split(' ')
    .map((word) => labelWordReplacements[word] ?? word)
    .join(' ');
}

export function formatProposalHeadline(proposal) {
  if (!proposal) {
    return 'Proposta';
  }

  const applicantName = proposal.solicitanteNome?.trim();
  if (applicantName) {
    return applicantName;
  }

  return proposal.id ? `Proposta #${proposal.id}` : 'Proposta';
}

export function roleSummary(role) {
  if (!role) {
    return 'Sem perfil';
  }
  return formatLabel(role);
}

export function formatAddress(address) {
  if (!address) {
    return '-';
  }

  const firstLine = [address.logradouro, address.numero]
    .filter(Boolean)
    .join(', ');

  const secondLine = [address.bairro, address.cidade, address.uf]
    .filter(Boolean)
    .join(' - ');

  return [firstLine, address.complemento, secondLine, formatCep(address.cep)]
    .filter(Boolean)
    .join(' | ');
}

export function formatBooleanLabel(value) {
  if (value === null || value === undefined) {
    return '-';
  }

  return value ? 'Sim' : 'Não';
}
