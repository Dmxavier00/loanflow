import { createContext, useContext, useMemo, useState } from 'react';
import { api } from '../lib/api';
import { hasBankAccount, isBankAccountPending, requiresBankAccount } from '../lib/bankAccountPolicy';

const STORAGE_KEY = 'loanflow.front.auth';
const AuthContext = createContext(null);

function readStoredAuth() {
  const raw = window.localStorage.getItem(STORAGE_KEY);
  if (!raw) {
    return null;
  }

  try {
    return JSON.parse(raw);
  } catch (error) {
    window.localStorage.removeItem(STORAGE_KEY);
    return null;
  }
}

export function AuthProvider({ children }) {
  const [auth, setAuth] = useState(() => readStoredAuth());

  const persist = (nextAuth) => {
    setAuth(nextAuth);
    if (nextAuth) {
      window.localStorage.setItem(STORAGE_KEY, JSON.stringify(nextAuth));
      return;
    }
    window.localStorage.removeItem(STORAGE_KEY);
  };

  const login = async (credentials) => {
    const response = await api.login(credentials);
    const nextAuth = {
      tokenType: response.tokenType,
      accessToken: response.accessToken,
      expiresInSeconds: response.expiresInSeconds,
      user: response.usuario
    };
    persist(nextAuth);
    return nextAuth;
  };

  const register = async (payload) => {
    const response = await api.register(payload);
    const nextAuth = {
      tokenType: response.tokenType,
      accessToken: response.accessToken,
      expiresInSeconds: response.expiresInSeconds,
      user: response.usuario
    };
    persist(nextAuth);
    return nextAuth;
  };

  const logout = () => {
    persist(null);
  };

  const updateUser = (user) => {
    if (!auth) {
      return;
    }

    persist({
      ...auth,
      user
    });
  };

  const value = useMemo(
    () => ({
      auth,
      token: auth?.accessToken ?? null,
      user: auth?.user ?? null,
      isAuthenticated: Boolean(auth?.accessToken),
      roles: auth?.user?.papel ? [auth.user.papel] : [],
      hasBankAccount: hasBankAccount(auth?.user),
      requiresBankAccount: requiresBankAccount(auth?.user),
      isBankAccountPending: isBankAccountPending(auth?.user),
      hasRole: (role) => auth?.user?.papel === role,
      login,
      register,
      logout,
      updateUser
    }),
    [auth]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth precisa ser usado dentro de AuthProvider.');
  }
  return context;
}
