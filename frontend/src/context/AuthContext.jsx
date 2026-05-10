import { createContext, useContext, useEffect, useState, useCallback } from 'react';
import * as authService from '../services/authService';
import {
  getStoredTokens,
  getStoredUser,
  setStoredTokens,
  setStoredUser,
  clearStoredAuth,
} from '../lib/apiClient';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => getStoredUser());
  const [status, setStatus] = useState(() => {
    const { accessToken } = getStoredTokens();
    return accessToken ? 'loading' : 'unauthenticated';
  });

  useEffect(() => {
    const { accessToken } = getStoredTokens();
    if (!accessToken) return;

    let cancelled = false;
    authService
      .me()
      .then((freshUser) => {
        if (cancelled) return;
        setUser(freshUser);
        setStoredUser(freshUser);
        setStatus('authenticated');
      })
      .catch(() => {
        if (cancelled) return;
        clearStoredAuth();
        setUser(null);
        setStatus('unauthenticated');
      });

    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => {
    const onLogout = () => {
      setUser(null);
      setStatus('unauthenticated');
    };
    window.addEventListener('auth:logout', onLogout);
    return () => window.removeEventListener('auth:logout', onLogout);
  }, []);

  const login = useCallback(async (email, password) => {
    const data = await authService.login({ email, password });
    setStoredTokens(data.accessToken, data.refreshToken);
    setStoredUser(data.user);
    setUser(data.user);
    setStatus('authenticated');
    return data.user;
  }, []);

  const register = useCallback(async ({ email, password, fullName }) => {
    const data = await authService.register({ email, password, fullName });
    setStoredTokens(data.accessToken, data.refreshToken);
    setStoredUser(data.user);
    setUser(data.user);
    setStatus('authenticated');
    return data.user;
  }, []);

  const logout = useCallback(async () => {
    await authService.logout();
    clearStoredAuth();
    setUser(null);
    setStatus('unauthenticated');
  }, []);

  // Re-fetch /auth/me
  const refreshUser = useCallback(async () => {
    const fresh = await authService.me();
    setUser(fresh);
    setStoredUser(fresh);
    return fresh;
  }, []);

  return (
    <AuthContext.Provider value={{ user, status, login, register, logout, refreshUser }}>
      {children}
    </AuthContext.Provider>
  );
}

export const useAuth = () => {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
};
