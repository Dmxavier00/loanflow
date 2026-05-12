import { Navigate, Route, Routes } from 'react-router-dom';
import AppShell from './components/AppShell';
import ProtectedRoute from './components/ProtectedRoute';
import AccountPage from './pages/AccountPage';
import ContractPage from './pages/ContractPage';
import DashboardPage from './pages/DashboardPage';
import HomePage from './pages/HomePage';
import LoginPage from './pages/LoginPage';
import NotificationsPage from './pages/NotificationsPage';
import ParcelasPage from './pages/ParcelasPage';
import PropostasPage from './pages/PropostasPage';
import RegisterPage from './pages/RegisterPage';

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<HomePage />} />
      <Route path="/login" element={<LoginPage />} />
      <Route path="/cadastro" element={<RegisterPage />} />
      <Route element={<ProtectedRoute />}>
        <Route element={<AppShell />}>
          <Route path="/dashboard" element={<DashboardPage />} />
          <Route path="/painel-de-controle" element={<Navigate to="/dashboard" replace />} />
          <Route path="/minha-conta" element={<AccountPage />} />
          <Route path="/solicitacoes" element={<PropostasPage />} />
          <Route path="/propostas" element={<Navigate to="/solicitacoes" replace />} />
          <Route path="/propostas-credor" element={<Navigate to="/solicitacoes" replace />} />
          <Route path="/contratos" element={<ContractPage />} />
          <Route path="/parcelas" element={<ParcelasPage />} />
          <Route path="/alertas" element={<NotificationsPage />} />
          <Route path="/central-notificacoes" element={<Navigate to="/alertas" replace />} />
        </Route>
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
