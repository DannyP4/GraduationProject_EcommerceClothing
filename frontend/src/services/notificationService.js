import apiClient from '../lib/apiClient';

function unwrap(response) {
  const body = response?.data;
  if (body && body.success === false) {
    throw new Error(body.message || 'Request failed');
  }
  return body?.data;
}

export async function getNotifications({ page = 0, size = 10 } = {}) {
  const resp = await apiClient.get('/notifications', { params: { page, size } });
  return unwrap(resp);
}

export async function getUnreadCount() {
  const resp = await apiClient.get('/notifications/unread-count');
  return unwrap(resp);
}

export async function markNotificationRead(id) {
  const resp = await apiClient.post(`/notifications/${id}/read`);
  return unwrap(resp);
}

export async function markAllNotificationsRead() {
  const resp = await apiClient.post('/notifications/read-all');
  return unwrap(resp);
}
