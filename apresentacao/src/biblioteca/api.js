const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080';

function buildQuery(params = {}) {
  const search = new URLSearchParams();

  Object.entries(params).forEach(([key, value]) => {
    if (value === undefined || value === null || value === '') {
      return;
    }
    search.set(key, value);
  });

  const query = search.toString();
  return query ? `?${query}` : '';
}

async function readError(response) {
  const contentType = response.headers.get('content-type') ?? '';

  if (contentType.includes('application/json')) {
    const data = await response.json();
    return data.message ?? data.error ?? 'Ocorreu um erro inesperado.';
  }

  const text = await response.text();
  return text || 'Ocorreu um erro inesperado.';
}

async function request(path, options = {}) {
  const {
    method = 'GET',
    token,
    body,
    headers = {},
    responseType = 'json'
  } = options;

  const finalHeaders = { ...headers };
  const requestInit = { method, headers: finalHeaders };

  if (token) {
    finalHeaders.Authorization = `Bearer ${token}`;
  }

  if (body !== undefined && body !== null) {
    finalHeaders['Content-Type'] = 'application/json';
    requestInit.body = JSON.stringify(body);
  }

  const response = await fetch(`${API_BASE_URL}${path}`, requestInit);

  if (!response.ok) {
    const error = new Error(await readError(response));
    error.status = response.status;
    throw error;
  }

  if (responseType === 'blob') {
    return response.blob();
  }

  if (response.status === 204) {
    return null;
  }

  const contentType = response.headers.get('content-type') ?? '';
  if (contentType.includes('application/json')) {
    return response.json();
  }

  return response.text();
}

export const api = {
  login: (payload) => request('/auth/login', { method: 'POST', body: payload }),
  register: (payload) => request('/auth/register', { method: 'POST', body: payload }),
  lookupCep: (cep) => request(`/enderecos/cep/${cep}`),
  getMe: (token) => request('/usuarios/me', { token }),
  updateProfile: (token, payload) => request('/usuarios/me', { method: 'PUT', token, body: payload }),
  changePassword: (token, payload) => request('/usuarios/me/senha', { method: 'PUT', token, body: payload }),
  updateFinancialData: (token, payload) =>
    request('/usuarios/me/dados-financeiros', { method: 'PUT', token, body: payload }),
  getMyBankAccount: (token) => request('/usuarios/me/conta-bancaria', { token }),
  upsertMyBankAccount: (token, payload) =>
    request('/usuarios/me/conta-bancaria', { method: 'PUT', token, body: payload }),
  getUsers: (token) => request('/usuarios', { token }),
  getCreditors: (token, params) => request(`/usuarios/credores${buildQuery(params)}`, { token }),
  blockUser: (token, userId) => request(`/usuarios/${userId}/bloquear`, { method: 'POST', token }),
  reactivateUser: (token, userId) => request(`/usuarios/${userId}/reativar`, { method: 'POST', token }),

  searchProposals: (token, params) => request(`/propostas${buildQuery(params)}`, { token }),
  getMyProposals: (token) => request('/propostas/minhas', { token }),
  getAnalysisProposals: (token) => request('/propostas/analise', { token }),
  getPendingAcceptanceProposals: (token) => request('/propostas/aguardando-aceite', { token }),
  getAcceptedProposals: (token) => request('/propostas/aceitas', { token }),
  getProposalsByStatus: (token, status) => request(`/propostas/status/${status}`, { token }),
  getProposal: (token, proposalId) => request(`/propostas/${proposalId}`, { token }),
  createProposal: (token, payload) => request('/propostas', { method: 'POST', token, body: payload }),
  updateProposal: (token, proposalId, payload) =>
    request(`/propostas/${proposalId}`, { method: 'PUT', token, body: payload }),
  // Legacy support for historical proposals that may still be in draft.
  submitProposal: (token, proposalId) => request(`/propostas/${proposalId}/submeter`, { method: 'POST', token }),
  startProposalAnalysis: (token, proposalId) =>
    request(`/propostas/${proposalId}/iniciar-analise`, { method: 'POST', token }),
  acceptProposal: (token, proposalId) => request(`/propostas/${proposalId}/aceitar`, { method: 'POST', token }),
  approveProposal: (token, proposalId) => request(`/propostas/${proposalId}/aprovar`, { method: 'POST', token }),
  rejectProposal: (token, proposalId) => request(`/propostas/${proposalId}/rejeitar`, { method: 'POST', token }),
  cancelProposal: (token, proposalId) => request(`/propostas/${proposalId}/cancelar`, { method: 'POST', token }),

  generateContract: (token, proposalId) =>
    request(`/contratos/proposta/${proposalId}/gerar`, { method: 'POST', token }),
  searchContracts: (token, params) => request(`/contratos${buildQuery(params)}`, { token }),
  getContract: (token, contractId) => request(`/contratos/${contractId}`, { token }),
  cancelContract: (token, contractId) => request(`/contratos/${contractId}/cancelar`, { method: 'POST', token }),
  downloadContract: (token, contractId) =>
    request(`/contratos/${contractId}/download`, { token, responseType: 'blob' }),

  searchParcelas: (token, params) => request(`/parcelas${buildQuery(params)}`, { token }),
  getParcelasByContrato: (token, contractId) => request(`/contratos/${contractId}/parcelas`, { token }),
  getParcela: (token, parcelaId) => request(`/parcelas/${parcelaId}`, { token }),
  getPagamentosByParcela: (token, parcelaId) => request(`/parcelas/${parcelaId}/pagamentos`, { token }),
  registerPayment: (token, parcelaId, payload) =>
    request(`/parcelas/${parcelaId}/pagamentos`, { method: 'POST', token, body: payload }),
  cancelPayment: (token, parcelaId, paymentId) =>
    request(`/parcelas/${parcelaId}/pagamentos/${paymentId}/cancelar`, { method: 'POST', token }),

  getNotifications: (token, params) => request(`/notificacoes${buildQuery(params)}`, { token }),
  markNotificationAsRead: (token, notificationId) =>
    request(`/notificacoes/${notificationId}/lida`, { method: 'POST', token }),

  getAuditTrail: (token, params) => request(`/admin/auditorias${buildQuery(params)}`, { token }),
  getAdminDashboard: (token) => request('/admin/dashboard', { token }),
  markOverdueInstallments: (token) => request('/admin/parcelas/marcar-atrasadas', { method: 'POST', token })
};
