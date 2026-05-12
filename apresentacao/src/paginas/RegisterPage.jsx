import { useEffect, useMemo, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import MessageBanner from '../componentes/MessageBanner';
import { useAuth } from '../contexto/AuthContext';
import { api } from '../biblioteca/api';
import { bankOptions } from '../biblioteca/bankOptions';
import { formatCep, formatCpf, isValidCep, isValidCpf, normalizeCep, normalizeCpf } from '../biblioteca/validation';
import brandFull from '../recursos/loanflow-full.png';

const roleOptions = [
  {
    value: 'SOLICITANTE',
    label: 'Solicitante',
    description: 'Vai criar propostas de empréstimo e acompanhar contratos e parcelas.'
  },
  {
    value: 'CREDOR',
    label: 'Credor',
    description: 'Vai analisar propostas, aprovar operações e formalizar contratos.'
  }
];

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
  { value: 'POUPANCA', label: 'Poupança' },
  { value: 'PAGAMENTO', label: 'Conta de pagamento' }
];

const initialForm = {
  nome: '',
  cpf: '',
  email: '',
  estadoCivil: 'SOLTEIRO',
  nacionalidade: 'Brasileira',
  profissao: '',
  dataNascimento: '',
  telefone: '',
  tipoDocumentoIdentidade: 'RG',
  documentoIdentidade: '',
  orgaoEmissor: '',
  pessoaExpostaPoliticamente: false,
  cep: '',
  logradouro: '',
  numero: '',
  complemento: '',
  bairro: '',
  cidade: '',
  uf: '',
  banco: '',
  agencia: '',
  numeroConta: '',
  tipoConta: 'CORRENTE',
  chavePix: '',
  senha: '',
  confirmarSenha: '',
  perfil: 'SOLICITANTE',
  rendaMensal: '',
  tipoOcupacao: '',
  saldoDisponivelSimulado: ''
};

export default function RegisterPage() {
  const { isAuthenticated, register } = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState(initialForm);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [addressFeedback, setAddressFeedback] = useState('');
  const [addressFeedbackType, setAddressFeedbackType] = useState('info');

  useEffect(() => {
    if (isAuthenticated) {
      navigate('/dashboard', { replace: true });
    }
  }, [isAuthenticated, navigate]);

  useEffect(() => {
    const cepNormalizado = normalizeCep(form.cep);

    if (cepNormalizado.length !== 8) {
      setAddressFeedback('');
      setAddressFeedbackType('info');
      return undefined;
    }

    let isCurrentRequest = true;

    setAddressFeedbackType('info');
    setAddressFeedback('Buscando endereço pelo CEP...');
    setForm((current) => {
      if (normalizeCep(current.cep) !== cepNormalizado) {
        return current;
      }

      return {
        ...current,
        logradouro: '',
        bairro: '',
        cidade: '',
        uf: ''
      };
    });

    api.lookupCep(cepNormalizado)
      .then((address) => {
        if (!isCurrentRequest) {
          return;
        }

        setForm((current) => {
          if (normalizeCep(current.cep) !== cepNormalizado) {
            return current;
          }

          return {
            ...current,
            cep: formatCep(address.cep ?? cepNormalizado),
            logradouro: address.logradouro ?? '',
            bairro: address.bairro ?? '',
            cidade: address.cidade ?? '',
            uf: (address.uf ?? '').toUpperCase()
          };
        });
        setAddressFeedbackType('success');
        setAddressFeedback('Endereço preenchido automaticamente. Informe apenas número e complemento.');
      })
      .catch((lookupError) => {
        if (!isCurrentRequest) {
          return;
        }

        setAddressFeedbackType('error');
        setAddressFeedback(lookupError.message);
      });

    return () => {
      isCurrentRequest = false;
    };
  }, [form.cep]);

  const selectedRole = useMemo(
    () => roleOptions.find((role) => role.value === form.perfil) ?? roleOptions[0],
    [form.perfil]
  );

  const handleChange = (event) => {
    const { name, value, type, checked } = event.target;

    if (type === 'checkbox') {
      setForm((current) => ({ ...current, [name]: checked }));
      return;
    }

    let nextValue = value;
    if (name === 'cpf') {
      nextValue = formatCpf(value);
    } else if (name === 'cep') {
      nextValue = formatCep(value);
    } else if (name === 'uf') {
      nextValue = value.replace(/[^a-zA-Z]/g, '').slice(0, 2).toUpperCase();
    }

    setForm((current) => ({ ...current, [name]: nextValue }));
  };

  const buildPayload = () => {
    const payload = {
      nome: form.nome.trim(),
      cpf: normalizeCpf(form.cpf),
      email: form.email.trim(),
      estadoCivil: form.estadoCivil,
      nacionalidade: form.nacionalidade.trim(),
      profissao: form.profissao.trim(),
      dataNascimento: form.dataNascimento,
      telefone: form.telefone.trim(),
      tipoDocumentoIdentidade: form.tipoDocumentoIdentidade,
      documentoIdentidade: form.documentoIdentidade.trim(),
      orgaoEmissor: form.orgaoEmissor.trim(),
      pessoaExpostaPoliticamente: form.pessoaExpostaPoliticamente,
      endereco: {
        cep: formatCep(form.cep),
        logradouro: form.logradouro.trim(),
        numero: form.numero.trim(),
        complemento: form.complemento.trim(),
        bairro: form.bairro.trim(),
        cidade: form.cidade.trim(),
        uf: form.uf.trim().toUpperCase()
      },
      contaBancaria: {
        banco: form.banco.trim(),
        agencia: form.agencia.trim(),
        numeroConta: form.numeroConta.trim(),
        tipoConta: form.tipoConta,
        chavePix: form.chavePix.trim() || null
      },
      senha: form.senha,
      papel: form.perfil,
      rendaMensal: null,
      tipoOcupacao: null,
      saldoDisponivelSimulado: null
    };

    if (form.perfil === 'SOLICITANTE') {
      payload.rendaMensal = form.rendaMensal ? Number(form.rendaMensal) : null;
      payload.tipoOcupacao = form.tipoOcupacao.trim();
    }

    if (form.perfil === 'CREDOR') {
      payload.saldoDisponivelSimulado = form.saldoDisponivelSimulado
        ? Number(form.saldoDisponivelSimulado)
        : null;
    }

    return payload;
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError('');

    if (!isValidCpf(form.cpf)) {
      setError('Informe um CPF válido.');
      return;
    }

    if (!isValidCep(form.cep)) {
      setError('Informe um CEP válido com 8 dígitos.');
      return;
    }

    if (!form.banco.trim() || !form.agencia.trim() || !form.numeroConta.trim()) {
      setError('Preencha banco, agência e número da conta no cadastro inicial.');
      return;
    }

    if (form.senha !== form.confirmarSenha) {
      setError('A confirmação da senha precisa ser igual à senha informada.');
      return;
    }

    if (form.perfil === 'SOLICITANTE' && !form.tipoOcupacao.trim()) {
      setError('Informe o tipo de ocupação para o perfil de solicitante.');
      return;
    }

    if (form.perfil === 'SOLICITANTE' && (!form.rendaMensal || Number(form.rendaMensal) <= 0)) {
      setError('Informe uma renda mensal maior que zero para o perfil de solicitante.');
      return;
    }

    if (form.perfil === 'CREDOR' && (!form.saldoDisponivelSimulado || Number(form.saldoDisponivelSimulado) <= 0)) {
      setError('Informe um saldo disponível maior que zero para o perfil de credor.');
      return;
    }

    setLoading(true);

    try {
      await register(buildPayload());
      navigate('/dashboard', { replace: true });
    } catch (submitError) {
      setError(submitError.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="login-page auth-gradient-page register-page">
      <div className="register-panel">
        <div className="login-copy">
          <img className="auth-brand-image" src={brandFull} alt="LoanFlow" />
          <span className="eyebrow">Cadastro LoanFlow</span>
          <h1>Crie sua conta e escolha como quer atuar na plataforma.</h1>
          <p>
            O cadastro registra um perfil mais completo, com dados pessoais e endereço, e faz o
            login automaticamente ao final do processo.
          </p>
        </div>

        <form className="login-form register-form" onSubmit={handleSubmit}>
          <div className="role-selector">
            {roleOptions.map((role) => (
              <button
                key={role.value}
                type="button"
                className={`role-option${form.perfil === role.value ? ' is-selected' : ''}`}
                onClick={() => setForm((current) => ({ ...current, perfil: role.value }))}
              >
                <strong>{role.label}</strong>
                <span>{role.description}</span>
              </button>
            ))}
          </div>

          <div className="login-hint role-hint">
            <strong>Perfil selecionado: {selectedRole.label}</strong>
            <span>{selectedRole.description}</span>
          </div>

          <div className="form-grid">
            <label>
              Nome completo
              <input
                type="text"
                name="nome"
                placeholder="Seu nome"
                value={form.nome}
                onChange={handleChange}
                required
              />
            </label>

            <label>
              CPF
              <input
                type="text"
                name="cpf"
                inputMode="numeric"
                maxLength={14}
                placeholder="000.000.000-00"
                value={form.cpf}
                onChange={handleChange}
                required
              />
            </label>

            <label>
              E-mail
              <input
                type="email"
                name="email"
                placeholder="voce@loanflow.com"
                value={form.email}
                onChange={handleChange}
                required
              />
            </label>

            <label>
              Estado civil
              <select name="estadoCivil" value={form.estadoCivil} onChange={handleChange} required>
                {maritalStatusOptions.map((option) => (
                  <option key={option.value} value={option.value}>
                    {option.label}
                  </option>
                ))}
              </select>
            </label>

            <label>
              Profissão
              <input
                type="text"
                name="profissao"
                placeholder="Ex.: Analista de sistemas"
                value={form.profissao}
                onChange={handleChange}
                required
              />
            </label>

            <label>
              Data de nascimento
              <input
                type="date"
                name="dataNascimento"
                value={form.dataNascimento}
                onChange={handleChange}
                required
              />
            </label>

            <label>
              Nacionalidade
              <input
                type="text"
                name="nacionalidade"
                placeholder="Ex.: Brasileira"
                value={form.nacionalidade}
                onChange={handleChange}
                required
              />
            </label>

            <label>
              Telefone
              <input
                type="text"
                name="telefone"
                placeholder="Ex.: (11) 99999-0000"
                value={form.telefone}
                onChange={handleChange}
                required
              />
            </label>

            <label>
              Senha
              <input
                type="password"
                name="senha"
                placeholder="Mínimo de 8 caracteres"
                value={form.senha}
                onChange={handleChange}
                required
              />
            </label>

            <label>
              Confirmar senha
              <input
                type="password"
                name="confirmarSenha"
                placeholder="Repita a senha"
                value={form.confirmarSenha}
                onChange={handleChange}
                required
              />
            </label>

            <label>
              Tipo de documento
              <select
                name="tipoDocumentoIdentidade"
                value={form.tipoDocumentoIdentidade}
                onChange={handleChange}
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
              Documento de identidade
              <input
                type="text"
                name="documentoIdentidade"
                placeholder="Número do documento"
                value={form.documentoIdentidade}
                onChange={handleChange}
                required
              />
            </label>

            <label>
              Órgão emissor
              <input
                type="text"
                name="orgaoEmissor"
                placeholder="Ex.: SSP-SP"
                value={form.orgaoEmissor}
                onChange={handleChange}
                required
              />
            </label>

            <label className="form-span-2 checkbox-field">
              <span>Pessoa exposta politicamente (PEP)</span>
              <input
                type="checkbox"
                name="pessoaExpostaPoliticamente"
                checked={form.pessoaExpostaPoliticamente}
                onChange={handleChange}
              />
            </label>

            <label>
              CEP
              <input
                type="text"
                name="cep"
                inputMode="numeric"
                maxLength={9}
                placeholder="00000-000"
                value={form.cep}
                onChange={handleChange}
                required
              />
            </label>

            <label>
              UF
              <input
                type="text"
                name="uf"
                placeholder="SP"
                maxLength={2}
                value={form.uf}
                onChange={handleChange}
                required
              />
            </label>

            <label>
              Cidade
              <input
                type="text"
                name="cidade"
                placeholder="Ex.: São Paulo"
                value={form.cidade}
                onChange={handleChange}
                required
              />
            </label>

            <label>
              Bairro
              <input
                type="text"
                name="bairro"
                placeholder="Ex.: Centro"
                value={form.bairro}
                onChange={handleChange}
                required
              />
            </label>

            <label className="form-span-2">
              Logradouro
              <input
                type="text"
                name="logradouro"
                placeholder="Ex.: Avenida Paulista"
                value={form.logradouro}
                onChange={handleChange}
                required
              />
            </label>

            <label>
              Número
              <input
                type="text"
                name="numero"
                placeholder="Ex.: 1000"
                value={form.numero}
                onChange={handleChange}
                required
              />
            </label>

            <label>
              Complemento
              <input
                type="text"
                name="complemento"
                placeholder="Apto, bloco, sala..."
                value={form.complemento}
                onChange={handleChange}
              />
            </label>

            <div className="form-span-2">
              <MessageBanner type={addressFeedbackType}>{addressFeedback}</MessageBanner>
            </div>

            <div className="form-span-2 login-hint">
              <strong>Conta bancária inicial</strong>
              <span>
                Esses dados entram logo no cadastro para liberar propostas, aceite e assinatura
                sem exigir uma etapa extra depois do primeiro login.
              </span>
            </div>

            <label>
              Banco
              <select name="banco" value={form.banco} onChange={handleChange} required>
                {bankOptions.map((option) => (
                  <option key={option.value || 'placeholder'} value={option.value}>
                    {option.label}
                  </option>
                ))}
              </select>
            </label>

            <label>
              Tipo de conta
              <select name="tipoConta" value={form.tipoConta} onChange={handleChange} required>
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
                type="text"
                name="agencia"
                placeholder="Ex.: 1234-5"
                value={form.agencia}
                onChange={handleChange}
                required
              />
            </label>

            <label>
              Número da conta
              <input
                type="text"
                name="numeroConta"
                placeholder="Ex.: 987654-3"
                value={form.numeroConta}
                onChange={handleChange}
                required
              />
            </label>

            <label className="form-span-2">
              Chave PIX
              <input
                type="text"
                name="chavePix"
                placeholder="Opcional"
                value={form.chavePix}
                onChange={handleChange}
              />
            </label>

            {form.perfil === 'SOLICITANTE' ? (
              <>
                <label>
                  Renda mensal
                  <input
                    type="number"
                    min="0.01"
                    step="0.01"
                    name="rendaMensal"
                    placeholder="Ex.: 3000.00"
                    value={form.rendaMensal}
                    onChange={handleChange}
                    required
                  />
                </label>

                <label className="form-span-2">
                  Tipo de ocupação
                  <input
                    type="text"
                    name="tipoOcupacao"
                    placeholder="Ex.: CLT, autônomo, MEI"
                    value={form.tipoOcupacao}
                    onChange={handleChange}
                    required
                  />
                </label>
              </>
            ) : null}

            {form.perfil === 'CREDOR' ? (
              <label className="form-span-2">
                Saldo disponível simulado
                <input
                  type="number"
                  min="0.01"
                  step="0.01"
                  name="saldoDisponivelSimulado"
                  placeholder="Ex.: 10000.00"
                  value={form.saldoDisponivelSimulado}
                  onChange={handleChange}
                  required
                />
              </label>
            ) : null}
          </div>

          <MessageBanner type="error">{error}</MessageBanner>

          <div className="auth-actions">
            <button type="submit" className="primary-button" disabled={loading}>
              {loading ? 'Cadastrando...' : 'Criar conta'}
            </button>
            <Link to="/login" className="secondary-button auth-link-button">
              Voltar para login
            </Link>
            <Link to="/" className="inline-action auth-link-button">
              Ver home institucional
            </Link>
          </div>
        </form>
      </div>
    </div>
  );
}
