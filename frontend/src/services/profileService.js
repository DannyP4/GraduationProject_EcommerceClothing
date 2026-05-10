import apiClient from '../lib/apiClient';

function unwrap(response) {
  const body = response?.data;
  if (body && body.success === false) {
    throw new Error(body.message || 'Request failed');
  }
  return body?.data;
}

export async function updateProfile({ fullName, phone, preferredLocale }) {
  const payload = {};
  if (fullName !== undefined) payload.fullName = fullName;
  if (phone !== undefined) payload.phone = phone;
  if (preferredLocale !== undefined) payload.preferredLocale = preferredLocale;
  const resp = await apiClient.patch('/me/profile', payload);
  return unwrap(resp);
}

export async function changePassword({ currentPassword, newPassword }) {
  const resp = await apiClient.post('/me/password', { currentPassword, newPassword });
  return unwrap(resp);
}
