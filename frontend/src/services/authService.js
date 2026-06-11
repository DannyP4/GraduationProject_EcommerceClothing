import apiClient from '../lib/apiClient';

function unwrap(response) {
  const body = response?.data;
  if (body && body.success === false) {
    throw new Error(body.message || 'Request failed');
  }
  return body?.data;
}

export async function register({ email, password, fullName, phone, preferredLocale, captchaToken }) {
  const payload = { email, password, fullName };
  if (phone) payload.phone = phone;
  if (preferredLocale) payload.preferredLocale = preferredLocale;
  if (captchaToken) payload.captchaToken = captchaToken;
  const resp = await apiClient.post('/auth/register', payload, { _skipAuth: true });
  return unwrap(resp);
}

export async function login({ email, password, captchaToken }) {
  const resp = await apiClient.post('/auth/login', { email, password, captchaToken }, { _skipAuth: true });
  return unwrap(resp);
}

export async function me() {
  const resp = await apiClient.get('/auth/me');
  return unwrap(resp);
}

export async function logout() {
  // FE clears tokens regardless of server response.
  try {
    await apiClient.post('/auth/logout', {});
  } catch {
  }
}

export async function forgotPassword(email, captchaToken) {
  const resp = await apiClient.post('/auth/forgot-password', { email, captchaToken }, { _skipAuth: true });
  return unwrap(resp);
}

export async function resetPassword({ token, newPassword }) {
  const resp = await apiClient.post('/auth/reset-password', { token, newPassword }, { _skipAuth: true });
  return unwrap(resp);
}

export async function verifyEmail(token) {
  const resp = await apiClient.post('/auth/verify-email', { token }, { _skipAuth: true });
  return unwrap(resp);
}

export async function resendVerification() {
  const resp = await apiClient.post('/auth/resend-verification', {});
  return unwrap(resp);
}

export async function previewInvite(token) {
  const resp = await apiClient.get('/auth/invite', { params: { token }, _skipAuth: true });
  return unwrap(resp);
}

export async function acceptInvite({ token, fullName, password }) {
  const resp = await apiClient.post('/auth/accept-invite', { token, fullName, password }, { _skipAuth: true });
  return unwrap(resp);
}
