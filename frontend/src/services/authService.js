import apiClient from '../lib/apiClient';

function unwrap(response) {
  const body = response?.data;
  if (body && body.success === false) {
    throw new Error(body.message || 'Request failed');
  }
  return body?.data;
}

export async function register({ email, password, fullName, phone, preferredLocale }) {
  const payload = { email, password, fullName };
  if (phone) payload.phone = phone;
  if (preferredLocale) payload.preferredLocale = preferredLocale;
  const resp = await apiClient.post('/auth/register', payload, { _skipAuth: true });
  return unwrap(resp);
}

export async function login({ email, password }) {
  const resp = await apiClient.post('/auth/login', { email, password }, { _skipAuth: true });
  return unwrap(resp);
}

export async function me() {
  const resp = await apiClient.get('/auth/me');
  return unwrap(resp);
}
