import { Navigate, Route, Routes } from 'react-router-dom';
import AppShell from './componentes/AppShell';
import ProtectedRoute from './componentes/ProtectedRoute';
import AccountPage from './paginas/AccountPage';
import ContractPage from './paginas/ContractPage';
import DashboardPage from './paginas/DashboardPage';
import HomePage from './paginas/HomePage';
import LoginPage from './paginas/LoginPage';
import NotificationsPage from './paginas/NotificationsPage';
import ParcelasPage from './paginas/ParcelasPage';
import PropostasPage from './paginas/PropostasPage';
import RegisterPage from './paginas/RegisterPage';

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
