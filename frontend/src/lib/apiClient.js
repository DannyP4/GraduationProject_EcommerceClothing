import axios from 'axios';
import { API_BASE_URL } from './api';

const STORAGE_KEYS = {
  accessToken: 'auth.accessToken',
  refreshToken: 'auth.refreshToken',
  user: 'auth.user',
};

export function getStoredTokens() {
  return {
    accessToken: localStorage.getItem(STORAGE_KEYS.accessToken),
    refreshToken: localStorage.getItem(STORAGE_KEYS.refreshToken),
  };
}

export function getStoredUser() {
  const raw = localStorage.getItem(STORAGE_KEYS.user);
  if (!raw) return null;
  try {
    return JSON.parse(raw);
  } catch {
    return null;
  }
}

export function setStoredTokens(accessToken, refreshToken) {
  if (accessToken) localStorage.setItem(STORAGE_KEYS.accessToken, accessToken);
  if (refreshToken) localStorage.setItem(STORAGE_KEYS.refreshToken, refreshToken);
}

export function setStoredUser(user) {
  if (user) localStorage.setItem(STORAGE_KEYS.user, JSON.stringify(user));
}

export function clearStoredAuth() {
  localStorage.removeItem(STORAGE_KEYS.accessToken);
  localStorage.removeItem(STORAGE_KEYS.refreshToken);
  localStorage.removeItem(STORAGE_KEYS.user);
}

const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: { 'Content-Type': 'application/json' },
});

apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem(STORAGE_KEYS.accessToken);
  if (token && !config._skipAuth) {
    config.headers = config.headers || {};
    config.headers.Authorization = `Bearer ${token}`;
  }
  // Forward UI locale to BE — RequestLocaleResolver picks the right ProductTranslation row.
  const locale = localStorage.getItem('app.locale');
  if (locale) {
    config.headers = config.headers || {};
    config.headers['Accept-Language'] = locale;
  }
  return config;
});

let refreshPromise = null;

async function performRefresh() {
  const { refreshToken } = getStoredTokens();
  if (!refreshToken) throw new Error('No refresh token');

  const resp = await axios.post(
    `${API_BASE_URL}/auth/refresh`,
    { refreshToken },
    { headers: { 'Content-Type': 'application/json' } }
  );
  const data = resp.data?.data;
  if (!data?.accessToken || !data?.refreshToken) {
    throw new Error('Malformed refresh response');
  }
  setStoredTokens(data.accessToken, data.refreshToken);
  if (data.user) setStoredUser(data.user);
  return data.accessToken;
}

apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;
    const status = error.response?.status;
    const url = originalRequest?.url || '';
    const isAuthEndpoint = url.includes('/auth/login') || url.includes('/auth/refresh') || url.includes('/auth/register');

    if (status === 401 && !originalRequest._retry && !isAuthEndpoint) {
      originalRequest._retry = true;
      try {
        if (!refreshPromise) refreshPromise = performRefresh().finally(() => { refreshPromise = null; });
        const newToken = await refreshPromise;
        originalRequest.headers = originalRequest.headers || {};
        originalRequest.headers.Authorization = `Bearer ${newToken}`;
        return apiClient(originalRequest);
      } catch (refreshErr) {
        clearStoredAuth();
        window.dispatchEvent(new CustomEvent('auth:logout'));
        return Promise.reject(unwrapError(error));
      }
    }

    return Promise.reject(unwrapError(error));
  }
);

function unwrapError(error) {
  const apiMessage = error.response?.data?.message;
  if (apiMessage) {
    const wrapped = new Error(apiMessage);
    wrapped.status = error.response?.status;
    wrapped.original = error;
    return wrapped;
  }
  return error;
}

export default apiClient;
