import { useEffect, useMemo, useState } from 'react';
import BankAccountNotice from '../componentes/BankAccountNotice';
import MessageBanner from '../componentes/MessageBanner';
import StatusBadge from '../componentes/StatusBadge';
import UiIcon from '../componentes/UiIcon';
import { useAuth } from '../contexto/AuthContext';
import { api } from '../biblioteca/api';
import { getBankSelectOptions } from '../biblioteca/bankOptions';
import { formatCurrency, formatDateTime, formatLabel, roleSummary } from '../biblioteca/format';
import { formatCep, formatCpf, isValidCep } from '../biblioteca/validation';

const maritalStatusOptions = [
  { value: 'SOLTEIRO', label: 'Solteiro(a)' },
  { value: 'CASADO', label: 'Casado(a)' },
  { value: 'UNIAO_ESTAVEL', label: 'União estável' },
  { value: 'DIVORCIADO', label: 'Divorciado(a)' },
  { value: 'SEPARADO', label: 'Separado(a)' },
  { value: 'VIUVO', label: 'Viúvo(a)' }
];

const documentTypeOptions = [
  { value: 'RG', label: 'RG' },
  { value: 'CNH', label: 'CNH' },
  { value: 'RNE', label: 'RNE' },
  { value: 'PASSAPORTE', label: 'Passaporte' },
  { value: 'OUTRO', label: 'Outro' }
];

const bankAccountTypeOptions = [
  { value: 'CORRENTE', label: 'Conta corrente' },
  { value: 'POUPANCA', label: 'Conta de poupança' },
  { value: 'PAGAMENTO', label: 'Conta de pagamento' }
];

const MASKED_PASSWORD_VALUE = '••••••••';

function buildProfileForm(user) {
  return {
    nome: user?.nome ?? '',
    estadoCivil: user?.estadoCivil ?? 'SOLTEIRO',
    nacionalidade: user?.nacionalidade ?? 'Brasileira',
    profissao: user?.profissao ?? '',
    dataNascimento: user?.dataNascimento ?? '',
    telefone: user?.telefone ?? '',
    tipoDocumentoIdentidade: user?.tipoDocumentoIdentidade ?? 'RG',
    documentoIdentidade: user?.documentoIdentidade ?? '',
    orgaoEmissor: user?.orgaoEmissor ?? '',
    pessoaExpostaPoliticamente: Boolean(user?.pessoaExpostaPoliticamente),
    cep: formatCep(user?.endereco?.cep ?? ''),
    logradouro: user?.endereco?.logradouro ?? '',
    numero: user?.endereco?.numero ?? '',
    complemento: user?.endereco?.complemento ?? '',
    bairro: user?.endereco?.bairro ?? '',
    cidade: user?.endereco?.cidade ?? '',
    uf: user?.endereco?.uf ?? ''
  };
}

function buildBankAccountForm(account) {
  return {
    banco: account?.banco ?? '',
    agencia: account?.agencia ?? '',
    numeroConta: account?.numeroConta ?? '',
    tipoConta: account?.tipoConta ?? 'CORRENTE',
    chavePix: account?.chavePix ?? ''
  };
}

function buildFinancialForm(user) {
  return {
    rendaMensal: user?.rendaMensal == null ? '' : String(user.rendaMensal),
    saldoDisponivelSimulado:
      user?.saldoDisponivelSimulado == null ? '' : String(user.saldoDisponivelSimulado),
    limiteOperacoes: user?.limiteOperacoes == null ? '' : String(user.limiteOperacoes)
  };
}

function normalizeCurrencyInput(value) {
  if (!value) {
    return '';
  }

  const sanitized = value.replace(/[^\d.,]/g, '');
  const decimalSeparatorIndex = Math.max(sanitized.lastIndexOf(','), sanitized.lastIndexOf('.'));

  if (decimalSeparatorIndex === -1) {
    return sanitized.replace(/\D/g, '');
  }

  const integerPart = sanitized.slice(0, decimalSeparatorIndex).replace(/\D/g, '');
  const decimalPart = sanitized
    .slice(decimalSeparatorIndex + 1)
    .replace(/\D/g, '')
    .slice(0, 2);

  if (!integerPart && !decimalPart) {
    return '';
  }

  if (!decimalPart) {
    return `${integerPart}.`;
  }

  return `${integerPart || '0'}.${decimalPart}`;
}

function parseCurrencyNumber(value) {
  if (value === null || value === undefined || value === '') {
    return null;
  }

  const normalized = normalizeCurrencyInput(String(value));
  if (!normalized) {
    return null;
  }

  const parsedValue = Number(normalized);
  return Number.isFinite(parsedValue) ? parsedValue : null;
}

function formatEditableCurrencyValue(value) {
  if (!value) {
    return '';
  }

  return value.replace('.', ',');
}

function buildPasswordForm() {
  return {
    senha: ''
  };
}

function buildProfileSyncKey(user) {
  return JSON.stringify({
    nome: user?.nome ?? '',
    estadoCivil: user?.estadoCivil ?? '',
    nacionalidade: user?.nacionalidade ?? '',
    profissao: user?.profissao ?? '',
    dataNascimento: user?.dataNascimento ?? '',
    telefone: user?.telefone ?? '',
    tipoDocumentoIdentidade: user?.tipoDocumentoIdentidade ?? '',
    documentoIdentidade: user?.documentoIdentidade ?? '',
    orgaoEmissor: user?.orgaoEmissor ?? '',
    pessoaExpostaPoliticamente: Boolean(user?.pessoaExpostaPoliticamente),
    cep: user?.endereco?.cep ?? '',
    logradouro: user?.endereco?.logradouro ?? '',
    numero: user?.endereco?.numero ?? '',
    complemento: user?.endereco?.complemento ?? '',
    bairro: user?.endereco?.bairro ?? '',
    cidade: user?.endereco?.cidade ?? '',
    uf: user?.endereco?.uf ?? ''
  });
}

function buildFinancialSyncKey(user) {
  return JSON.stringify({
    papel: user?.papel ?? '',
    rendaMensal: user?.rendaMensal ?? '',
    saldoDisponivelSimulado: user?.saldoDisponivelSimulado ?? '',
    limiteOperacoes: user?.limiteOperacoes ?? ''
  });
}

function getBankAccountStatusValue(account, requiresBankAccount) {
  if (account) {
    return 'CADASTRADA';
  }

  return requiresBankAccount ? 'PENDENTE' : 'OPCIONAL';
}

function getDisplayId(userId) {
  if (!userId) {
    return '-';
  }

  return `LF-${String(userId).padStart(6, '0')}`;
}

function AccountMetricCard({ icon, label, value, tone = 'blue' }) {
  return (
    <article className={`account-metric-card tone-${tone}`}>
      <span className={`account-metric-icon tone-${tone}`} aria-hidden="true">
        <UiIcon name={icon} size={21} />
      </span>

      <div className="account-metric-copy">
        <span>{label}</span>
        <strong>{value}</strong>
      </div>
    </article>
  );
}

function AccountPanel({ icon, title, subtitle, className = '', children }) {
  return (
    <section className={`account-panel ${className}`.trim()}>
      <header className="account-panel-head">
        <div className="account-panel-title">
          <UiIcon name={icon} size={19} />
          <div>
            <h2>{title}</h2>
            {subtitle ? <p>{subtitle}</p> : null}
          </div>
        </div>
      </header>

      <div className="account-panel-divider" />
      {children}
    </section>
  );
}

function AccountFormSection({ title, subtitle, children }) {
  return (
    <section className="account-form-section">
      <div className="account-form-section-head">
        <h3>{title}</h3>
        {subtitle ? <p>{subtitle}</p> : null}
      </div>

      <div className="account-form-grid">{children}</div>
    </section>
  );
}

export default function AccountPage() {
  const {
    token,
    user,
    updateUser,
    requiresBankAccount,
    hasBankAccount: authHasBankAccount
  } = useAuth();
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [bankAccount, setBankAccount] = useState(null);
  const [profileForm, setProfileForm] = useState(() => buildProfileForm(user));
  const [financialForm, setFinancialForm] = useState(() => buildFinancialForm(user));
  const [passwordForm, setPasswordForm] = useState(() => buildPasswordForm());
  const [bankAccountForm, setBankAccountForm] = useState(() => buildBankAccountForm(null));
  const [profileFeedback, setProfileFeedback] = useState('');
  const [profileFeedbackType, setProfileFeedbackType] = useState('info');
  const [profileFeedbackScope, setProfileFeedbackScope] = useState(null);
  const [bankAccountFeedback, setBankAccountFeedback] = useState('');
  const [bankAccountFeedbackType, setBankAccountFeedbackType] = useState('info');
  const [savingProfile, setSavingProfile] = useState(false);
  const [savingProfileScope, setSavingProfileScope] = useState(null);
  const [savingBankAccount, setSavingBankAccount] = useState(false);
  const [isEditingPassword, setIsEditingPassword] = useState(false);
  const [isEditingRendaMensal, setIsEditingRendaMensal] = useState(false);
  const profileSyncKey = buildProfileSyncKey(user);
  const financialSyncKey = buildFinancialSyncKey(user);
  const hasFinancialSettings = user?.papel === 'SOLICITANTE' || user?.papel === 'CREDOR';
  const hasSavedRendaMensal = user?.papel === 'SOLICITANTE' && user?.rendaMensal != null;

  const bankSelectOptions = useMemo(
    () => getBankSelectOptions(bankAccountForm.banco),
    [bankAccountForm.banco]
  );

  useEffect(() => {
    setProfileForm(buildProfileForm(user));
  }, [profileSyncKey]);

  useEffect(() => {
    setFinancialForm(buildFinancialForm(user));
    setIsEditingRendaMensal(false);
  }, [financialSyncKey]);

  useEffect(() => {
    setBankAccountForm(buildBankAccountForm(bankAccount));
  }, [bankAccount]);

  useEffect(() => {
    async function loadData() {
      setLoading(true);
      setError('');

      try {
        const [me, account] = await Promise.all([
          api.getMe(token),
          api.getMyBankAccount(token).catch((requestError) => {
            if (requestError.status === 404) {
              return null;
            }

            throw requestError;
          })
        ]);

        updateUser(me);
        setBankAccount(account ?? null);
      } catch (loadError) {
        setError(loadError.message);
      } finally {
        setLoading(false);
      }
    }

    loadData();
  }, [token]);

  const handleProfileChange = (event) => {
    const { name, value, type, checked } = event.target;

    setProfileForm((current) => ({
      ...current,
      [name]:
        type === 'checkbox'
          ? checked
          : name === 'uf'
            ? value.replace(/[^a-zA-Z]/g, '').slice(0, 2).toUpperCase()
            : name === 'cep'
              ? formatCep(value)
              : value
    }));
  };

  const handleBankAccountChange = (event) => {
    const { name, value } = event.target;

    setBankAccountForm((current) => ({
      ...current,
      [name]: value
    }));
  };

  const handleFinancialChange = (event) => {
    const { name, value } = event.target;

    setFinancialForm((current) => ({
      ...current,
      [name]: name === 'rendaMensal' ? normalizeCurrencyInput(value) : value
    }));
  };

  const handlePasswordChange = (event) => {
    const { name, value } = event.target;

    setPasswordForm((current) => ({
      ...current,
      [name]: value
    }));
  };

  const handlePasswordFocus = () => {
    if (!isEditingPassword) {
      setIsEditingPassword(true);
    }
  };

  const handlePasswordBlur = () => {
    if (!passwordForm.senha) {
      setIsEditingPassword(false);
    }
  };

  const handleRendaMensalFocus = () => {
    setIsEditingRendaMensal(true);
  };

  const handleRendaMensalBlur = () => {
    setIsEditingRendaMensal(false);
  };

  const buildProfilePayload = () => ({
    nome: profileForm.nome.trim(),
    estadoCivil: profileForm.estadoCivil,
    nacionalidade: profileForm.nacionalidade.trim(),
    profissao: profileForm.profissao.trim(),
    dataNascimento: profileForm.dataNascimento,
    telefone: profileForm.telefone.trim(),
    tipoDocumentoIdentidade: profileForm.tipoDocumentoIdentidade,
    documentoIdentidade: profileForm.documentoIdentidade.trim(),
    orgaoEmissor: profileForm.orgaoEmissor.trim(),
    pessoaExpostaPoliticamente: profileForm.pessoaExpostaPoliticamente,
    endereco: {
      cep: formatCep(profileForm.cep),
      logradouro: profileForm.logradouro.trim(),
      numero: profileForm.numero.trim(),
      complemento: profileForm.complemento.trim(),
      bairro: profileForm.bairro.trim(),
      cidade: profileForm.cidade.trim(),
      uf: profileForm.uf.trim().toUpperCase()
    }
  });

  const buildBankAccountPayload = () => ({
    banco: bankAccountForm.banco.trim(),
    agencia: bankAccountForm.agencia.trim(),
    numeroConta: bankAccountForm.numeroConta.trim(),
    tipoConta: bankAccountForm.tipoConta,
    chavePix: bankAccountForm.chavePix.trim() || null
  });

  const buildFinancialPayload = () => {
    if (user?.papel === 'SOLICITANTE') {
      return {
        rendaMensal: parseCurrencyNumber(financialForm.rendaMensal),
        saldoDisponivelSimulado: null,
        limiteOperacoes: null
      };
    }

    if (user?.papel === 'CREDOR') {
      return {
        rendaMensal: null,
        saldoDisponivelSimulado: financialForm.saldoDisponivelSimulado
          ? Number(financialForm.saldoDisponivelSimulado)
          : null,
        limiteOperacoes: financialForm.limiteOperacoes ? Number(financialForm.limiteOperacoes) : null
      };
    }

    return {
      rendaMensal: null,
      saldoDisponivelSimulado: null,
      limiteOperacoes: null
    };
  };

  const handleProfileSubmit = (scope) => async (event) => {
    event.preventDefault();
    setSavingProfile(true);
    setSavingProfileScope(scope);
    setProfileFeedback('');
    setProfileFeedbackScope(scope);
    const hasPasswordChanges = isEditingPassword && Boolean(passwordForm.senha);

    if (!isValidCep(profileForm.cep)) {
      setProfileFeedbackType('error');
      setProfileFeedback('Informe um CEP válido com 8 dígitos.');
      setSavingProfile(false);
      setSavingProfileScope(null);
      return;
    }

    if (scope === 'profile-personal' && hasPasswordChanges) {
      if (passwordForm.senha.length < 8) {
        setProfileFeedbackType('error');
        setProfileFeedback('A senha precisa ter pelo menos 8 caracteres.');
        setSavingProfile(false);
        setSavingProfileScope(null);
        return;
      }
    }

    try {
      const updatedUser = await api.updateProfile(token, buildProfilePayload());
      updateUser(updatedUser);
      setProfileForm(buildProfileForm(updatedUser));

      if (scope === 'profile-personal' && hasPasswordChanges) {
        try {
          await api.changePassword(token, passwordForm);
          setPasswordForm(buildPasswordForm());
          setIsEditingPassword(false);
          setProfileFeedbackType('success');
          setProfileFeedback('Cadastro e senha atualizados com sucesso.');
        } catch (passwordError) {
          setProfileFeedbackType('error');
          setProfileFeedback(`Cadastro salvo, mas a senha não foi atualizada: ${passwordError.message}`);
        }
      } else {
        setProfileFeedbackType('success');
        setProfileFeedback('Cadastro atualizado com sucesso.');
      }
    } catch (submitError) {
      setProfileFeedbackType('error');
      setProfileFeedback(submitError.message);
    } finally {
      setSavingProfile(false);
      setSavingProfileScope(null);
    }
  };

  const handleBankAccountSubmit = async (event) => {
    event.preventDefault();
    setSavingBankAccount(true);
    setBankAccountFeedback('');
    const parsedRendaMensal = parseCurrencyNumber(financialForm.rendaMensal);

    if (user?.papel === 'SOLICITANTE') {
      if (!parsedRendaMensal || parsedRendaMensal <= 0) {
        setBankAccountFeedbackType('error');
        setBankAccountFeedback('Informe uma renda mensal maior que zero.');
        setSavingBankAccount(false);
        return;
      }
    }

    if (user?.papel === 'CREDOR') {
      if (
        !financialForm.saldoDisponivelSimulado ||
        Number(financialForm.saldoDisponivelSimulado) <= 0
      ) {
        setBankAccountFeedbackType('error');
        setBankAccountFeedback('Informe um saldo disponível maior que zero.');
        setSavingBankAccount(false);
        return;
      }

      if (!financialForm.limiteOperacoes || Number(financialForm.limiteOperacoes) <= 0) {
        setBankAccountFeedbackType('error');
        setBankAccountFeedback('Informe um limite de operações maior que zero.');
        setSavingBankAccount(false);
        return;
      }
    }

    try {
      const savedAccount = await api.upsertMyBankAccount(token, buildBankAccountPayload());
      setBankAccount(savedAccount);
      setBankAccountForm(buildBankAccountForm(savedAccount));
      const fallbackUser = {
        ...(user ?? {}),
        contaBancariaId: savedAccount.id
      };
      if (hasFinancialSettings) {
        try {
          const updatedUser = await api.updateFinancialData(token, buildFinancialPayload());
          const mergedUser = {
            ...updatedUser,
            contaBancariaId: updatedUser?.contaBancariaId ?? savedAccount.id
          };
          updateUser(mergedUser);
          setFinancialForm(buildFinancialForm(mergedUser));
          setBankAccountFeedbackType('success');
          setBankAccountFeedback(
            user?.papel === 'SOLICITANTE'
              ? 'Conta bancária e renda mensal salvas com sucesso.'
              : 'Conta bancária e dados financeiros salvos com sucesso.'
          );
        } catch (financialError) {
          updateUser(fallbackUser);
          setBankAccountFeedbackType('error');
          setBankAccountFeedback(
            `Conta bancária salva, mas os dados financeiros não foram atualizados: ${financialError.message}`
          );
        }
        return;
      }

      updateUser(fallbackUser);
      setBankAccountFeedbackType('success');
      setBankAccountFeedback('Conta bancária salva com sucesso.');
    } catch (submitError) {
      setBankAccountFeedbackType('error');
      setBankAccountFeedback(submitError.message);
    } finally {
      setSavingBankAccount(false);
    }
  };

  const bankAccountStatus = getBankAccountStatusValue(bankAccount, requiresBankAccount);
  const bankAccountUpdatedAt = bankAccount
    ? formatDateTime(bankAccount.dataAtualizacao ?? bankAccount.dataCadastro)
    : 'Sem registro';

  const renderProfilePanelActions = (scope, buttonLabel, leadingContent = null) => (
    <div className="account-panel-footer">
      <MessageBanner type={profileFeedbackType}>
        {profileFeedbackScope === scope ? profileFeedback : ''}
      </MessageBanner>

      <div className={leadingContent ? 'account-panel-footer-row' : undefined}>
        {leadingContent}

        <div className="account-form-actions">
          <button type="submit" className="primary-button" disabled={savingProfile}>
            {savingProfile && savingProfileScope === scope ? 'Salvando...' : buttonLabel}
          </button>
        </div>
      </div>
    </div>
  );

  const metrics = useMemo(
    () => [
      {
        icon: 'user',
        label: 'Perfil',
        value: roleSummary(user?.papel),
        tone: 'blue'
      },
      {
        icon: 'shield-check',
        label: 'Status',
        value: formatLabel(user?.status ?? 'ATIVO'),
        tone: 'green'
      },
      {
        icon: 'id-card',
        label: 'Identificador da conta',
        value: getDisplayId(user?.id),
        tone: 'blue'
      },
      {
        icon: 'clock',
        label: 'Última atualização',
        value: bankAccountUpdatedAt,
        tone: 'violet'
      }
    ],
    [bankAccountUpdatedAt, user]
  );
  const displayedPasswordValue = isEditingPassword ? passwordForm.senha : MASKED_PASSWORD_VALUE;
  const rendaMensalIndicatorText = 'Renda mensal cadastrada';
  const displayedRendaMensalValue = isEditingRendaMensal
    ? formatEditableCurrencyValue(financialForm.rendaMensal)
    : financialForm.rendaMensal
      ? formatCurrency(parseCurrencyNumber(financialForm.rendaMensal))
      : '';

  return (
    <div className="page-stack account-reference-page">
      <MessageBanner type="error">{error}</MessageBanner>
      <BankAccountNotice
        show={requiresBankAccount && !authHasBankAccount}
        message="Cadastre uma conta bancária nesta área para liberar os fluxos operacionais."
        showLink={false}
      />

      <header className="account-page-header">
        <h1>Minha conta</h1>
      </header>

      <section className="account-metrics-grid">
        {metrics.map((metric) => (
          <AccountMetricCard
            key={metric.label}
            icon={metric.icon}
            label={metric.label}
            value={loading ? 'Atualizando...' : metric.value}
            tone={metric.tone}
          />
        ))}
      </section>

      <div className="account-panels-grid account-profile-grid">
        <AccountPanel
          icon="user"
          title="Dados pessoais"
          className="account-panel-wide"
        >
          {loading ? (
            <p className="helper-text">Carregando dados da conta...</p>
          ) : (
            <form className="account-panel-form" onSubmit={handleProfileSubmit('profile-personal')}>
              <div className="account-form-grid">
                <label>
                  Nome completo
                  <input name="nome" value={profileForm.nome} onChange={handleProfileChange} required />
                </label>

                <label className="account-readonly-field">
                  E-mail de acesso
                  <input value={user?.email ?? ''} readOnly />
                </label>

                <label>
                  Telefone
                  <input
                    name="telefone"
                    value={profileForm.telefone}
                    onChange={handleProfileChange}
                    required
                  />
                </label>

                <label className="account-readonly-field">
                  CPF
                  <input value={formatCpf(user?.cpf) || ''} readOnly />
                </label>

                <label>
                  Data de nascimento
                  <input
                    type="date"
                    name="dataNascimento"
                    value={profileForm.dataNascimento}
                    onChange={handleProfileChange}
                    required
                  />
                </label>

                <label>
                  Estado civil
                  <select
                    name="estadoCivil"
                    value={profileForm.estadoCivil}
                    onChange={handleProfileChange}
                    required
                  >
                    {maritalStatusOptions.map((option) => (
                      <option key={option.value} value={option.value}>
                        {option.label}
                      </option>
                    ))}
                  </select>
                </label>

                <label>
                  Nacionalidade
                  <input
                    name="nacionalidade"
                    value={profileForm.nacionalidade}
                    onChange={handleProfileChange}
                    required
                  />
                </label>

                <label>
                  Profissão
                  <input
                    name="profissao"
                    value={profileForm.profissao}
                    onChange={handleProfileChange}
                    required
                  />
                </label>

                <label className="form-span-2">
                  Senha
                  <input
                    type="text"
                    name="senha"
                    autoComplete="new-password"
                    value={displayedPasswordValue}
                    onChange={handlePasswordChange}
                    onFocus={handlePasswordFocus}
                    onBlur={handlePasswordBlur}
                  />
                </label>
              </div>

              {renderProfilePanelActions(
                'profile-personal',
                'Salvar',
                <label className="account-inline-checkbox">
                  <input
                    type="checkbox"
                    name="pessoaExpostaPoliticamente"
                    checked={profileForm.pessoaExpostaPoliticamente}
                    onChange={handleProfileChange}
                  />
                  <span>Pessoa exposta politicamente (PEP)</span>
                </label>
              )}
            </form>
          )}
        </AccountPanel>

        <AccountPanel
          icon="id-card"
          title="Documentos e endereço"
          className="account-panel-wide"
        >
          {loading ? (
            <p className="helper-text">Carregando documentos e endereço...</p>
          ) : (
            <form className="account-panel-form" onSubmit={handleProfileSubmit('documents-address')}>
              <div className="account-form-stack">
                <AccountFormSection
                  title="Documentação"
                >
                  <label>
                    Tipo de documento
                    <select
                      name="tipoDocumentoIdentidade"
                      value={profileForm.tipoDocumentoIdentidade}
                      onChange={handleProfileChange}
                      required
                    >
                      {documentTypeOptions.map((option) => (
                        <option key={option.value} value={option.value}>
                          {option.label}
                        </option>
                      ))}
                    </select>
                  </label>

                  <label>
                    Nº do documento
                    <input
                      name="documentoIdentidade"
                      value={profileForm.documentoIdentidade}
                      onChange={handleProfileChange}
                      required
                    />
                  </label>

                  <label>
                    Órgão emissor
                    <input
                      name="orgaoEmissor"
                      value={profileForm.orgaoEmissor}
                      onChange={handleProfileChange}
                      required
                    />
                  </label>
                </AccountFormSection>

                <AccountFormSection
                  title="Endereço"
                >
                  <label>
                    CEP
                    <input
                      name="cep"
                      inputMode="numeric"
                      maxLength={9}
                      value={profileForm.cep}
                      onChange={handleProfileChange}
                      required
                    />
                  </label>

                  <label>
                    UF
                    <input
                      name="uf"
                      maxLength={2}
                      value={profileForm.uf}
                      onChange={handleProfileChange}
                      required
                    />
                  </label>

                  <label>
                    Cidade
                    <input
                      name="cidade"
                      value={profileForm.cidade}
                      onChange={handleProfileChange}
                      required
                    />
                  </label>

                  <label>
                    Bairro
                    <input
                      name="bairro"
                      value={profileForm.bairro}
                      onChange={handleProfileChange}
                      required
                    />
                  </label>

                  <label className="form-span-2">
                    Logradouro
                    <input
                      name="logradouro"
                      value={profileForm.logradouro}
                      onChange={handleProfileChange}
                      required
                    />
                  </label>

                  <label>
                    Número
                    <input
                      name="numero"
                      value={profileForm.numero}
                      onChange={handleProfileChange}
                      required
                    />
                  </label>

                  <label>
                    Complemento
                    <input
                      name="complemento"
                      value={profileForm.complemento}
                      onChange={handleProfileChange}
                    />
                  </label>
                </AccountFormSection>
              </div>

              {renderProfilePanelActions('documents-address', 'Salvar')}
            </form>
          )}
        </AccountPanel>
      </div>

      <AccountPanel
        icon="bank"
        title="Conta bancária"
        className="account-panel-bank"
      >
        {loading ? (
          <p className="helper-text">Carregando conta bancária...</p>
        ) : (
          <form className="account-panel-form" onSubmit={handleBankAccountSubmit}>
            <div className="account-panel-meta">
              <div className="account-panel-meta-item">
                <span>Status</span>
                <StatusBadge value={bankAccountStatus} />
              </div>

              <div className="account-panel-meta-item">
                <span>Última atualização</span>
                <strong>{bankAccountUpdatedAt}</strong>
              </div>
            </div>

            {!bankAccount ? (
              <p className="helper-text">
                Preencha os dados abaixo para cadastrar sua conta e liberar os fluxos operacionais.
              </p>
            ) : null}

            <div className="account-form-grid">
              <label>
                Banco
                <select
                  name="banco"
                  value={bankAccountForm.banco}
                  onChange={handleBankAccountChange}
                  required
                >
                  {bankSelectOptions.map((option) => (
                    <option key={option.value || 'placeholder'} value={option.value}>
                      {option.label}
                    </option>
                  ))}
                </select>
              </label>

              <label>
                Tipo de conta
                <select
                  name="tipoConta"
                  value={bankAccountForm.tipoConta}
                  onChange={handleBankAccountChange}
                  required
                >
                  {bankAccountTypeOptions.map((option) => (
                    <option key={option.value} value={option.value}>
                      {option.label}
                    </option>
                  ))}
                </select>
              </label>

              <label>
                Agência
                <input
                  name="agencia"
                  value={bankAccountForm.agencia}
                  onChange={handleBankAccountChange}
                  required
                />
              </label>

              <label>
                Número da conta
                <input
                  name="numeroConta"
                  value={bankAccountForm.numeroConta}
                  onChange={handleBankAccountChange}
                  required
                />
              </label>

              <label className="form-span-2">
                Chave PIX
                <input
                  name="chavePix"
                  value={bankAccountForm.chavePix}
                  onChange={handleBankAccountChange}
                  placeholder="Opcional"
                />
              </label>

              {user?.papel === 'SOLICITANTE' ? (
                <label className="form-span-2">
                  <span className="account-field-label-row">
                    <span>Renda mensal</span>
                    {hasSavedRendaMensal ? (
                      <span className="account-field-indicator">{rendaMensalIndicatorText}</span>
                    ) : null}
                  </span>
                  <input
                    type="text"
                    inputMode="decimal"
                    name="rendaMensal"
                    value={displayedRendaMensalValue}
                    onChange={handleFinancialChange}
                    onFocus={handleRendaMensalFocus}
                    onBlur={handleRendaMensalBlur}
                    placeholder="R$ 0,00"
                    required
                  />
                </label>
              ) : null}

              {user?.papel === 'CREDOR' ? (
                <>
                  <label>
                    Saldo disponível
                    <input
                      type="number"
                      min="0.01"
                      step="0.01"
                      name="saldoDisponivelSimulado"
                      value={financialForm.saldoDisponivelSimulado}
                      onChange={handleFinancialChange}
                      required
                    />
                  </label>

                  <label>
                    Limite de operações
                    <input
                      type="number"
                      min="1"
                      step="1"
                      name="limiteOperacoes"
                      value={financialForm.limiteOperacoes}
                      onChange={handleFinancialChange}
                      required
                    />
                  </label>
                </>
              ) : null}
            </div>

            <div className="account-panel-footer">
              <MessageBanner type={bankAccountFeedbackType}>{bankAccountFeedback}</MessageBanner>

              <div className="account-form-actions">
                <button type="submit" className="primary-button" disabled={savingBankAccount}>
                  {savingBankAccount ? 'Salvando...' : 'Salvar'}
                </button>
              </div>
            </div>
          </form>
        )}
      </AccountPanel>
    </div>
  );
}
