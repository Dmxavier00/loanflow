export function hasBankAccount(user) {
  return Boolean(user?.contaBancariaId);
}

export function requiresBankAccount(user) {
  return user?.papel === 'SOLICITANTE' || user?.papel === 'CREDOR';
}

export function isBankAccountPending(user) {
  return requiresBankAccount(user) && !hasBankAccount(user);
}

export function getAuthenticatedSecondaryAction(user) {
  if (isBankAccountPending(user)) {
    return { to: '/minha-conta', label: 'Cadastrar conta bancária' };
  }

  if (user?.papel === 'CREDOR') {
    return { to: '/solicitacoes', label: 'Ativos do credor' };
  }

  if (user?.papel === 'ADMIN') {
    return { to: '/alertas', label: 'Ver alertas' };
  }

  return { to: '/solicitacoes', label: 'Nova proposta' };
}

export function getBankAccountStatusLabel(user) {
  if (!requiresBankAccount(user)) {
    return 'OPCIONAL';
  }
  return hasBankAccount(user) ? 'CADASTRADA' : 'PENDENTE';
}
