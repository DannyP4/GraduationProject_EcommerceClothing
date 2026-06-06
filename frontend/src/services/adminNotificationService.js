import apiClient from '../lib/apiClient';

function unwrap(response) {
  const body = response?.data;
  if (body && body.success === false) {
    throw new Error(body.message || 'Request failed');
  }
  return body?.data;
}

export async function getNotifications() {
  const resp = await apiClient.get('/admin/notifications');
  return unwrap(resp);
}
