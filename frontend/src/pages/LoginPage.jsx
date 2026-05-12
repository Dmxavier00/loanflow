import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import MessageBanner from '../components/MessageBanner';
import { useAuth } from '../context/AuthContext';
import brandFull from '../assets/loanflow-full.png';

export default function LoginPage() {
  const { isAuthenticated, login } = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState({ email: '', senha: '' });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    if (isAuthenticated) {
      navigate('/dashboard', { replace: true });
    }
  }, [isAuthenticated, navigate]);

  const handleChange = (event) => {
    const { name, value } = event.target;
    setForm((current) => ({ ...current, [name]: value }));
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    setLoading(true);
    setError('');

    try {
      await login(form);
      navigate('/dashboard', { replace: true });
    } catch (submitError) {
      setError(submitError.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="login-page auth-gradient-page">
      <div className="login-panel">
        <div className="login-copy">
          <img className="auth-brand-image" src={brandFull} alt="LoanFlow" />
          <span className="eyebrow">LoanFlow</span>
          <h1>Entre para acompanhar propostas, contratos e pagamentos.</h1>
          <p>
            A LoanFlow simplifica toda a jornada de crédito, conectando solicitantes e
            credores com mais agilidade, segurança e controle.
          </p>
        </div>

        <form className="login-form" onSubmit={handleSubmit}>
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
            Senha
            <input
              type="password"
              name="senha"
              placeholder="Sua senha"
              value={form.senha}
              onChange={handleChange}
              required
            />
          </label>

          <MessageBanner type="error">{error}</MessageBanner>

          <button type="submit" className="primary-button" disabled={loading}>
            {loading ? 'Entrando...' : 'Entrar'}
          </button>

          <div className="auth-actions">
            <Link to="/cadastro" className="secondary-button auth-link-button">
              Criar conta
            </Link>
            <Link to="/" className="inline-action auth-link-button">
              Conhecer a LoanFlow
            </Link>
          </div>

          <div className="login-hint">
            <strong>Use um usuário já cadastrado ou crie uma nova conta.</strong>
            <span>Você pode se registrar como solicitante ou credor direto pela interface.</span>
          </div>
        </form>
      </div>
    </div>
  );
}
