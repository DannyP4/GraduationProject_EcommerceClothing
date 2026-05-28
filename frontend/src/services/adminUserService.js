import apiClient from '../lib/apiClient';

function unwrap(response) {
  const body = response?.data;
  if (body && body.success === false) {
    throw new Error(body.message || 'Request failed');
  }
  return body?.data;
}

function cleanParams(input) {
  const out = {};
  Object.entries(input).forEach(([k, v]) => {
    if (v !== undefined && v !== null && v !== '') out[k] = v;
  });
  return out;
}

export async function listUsers({ search, status, page = 0, size = 20, sort = 'createdAt,desc' } = {}) {
  const resp = await apiClient.get('/admin/users', {
    params: cleanParams({ search, status, page, size, sort }),
  });
  return unwrap(resp);
}

export async function getUser(id) {
  const resp = await apiClient.get(`/admin/users/${id}`);
  return unwrap(resp);
}

export async function suspendUser(id) {
  const resp = await apiClient.post(`/admin/users/${id}/suspend`);
  return unwrap(resp);
}

export async function activateUser(id) {
  const resp = await apiClient.post(`/admin/users/${id}/activate`);
  return unwrap(resp);
}

export async function softDeleteUser(id) {
  const resp = await apiClient.delete(`/admin/users/${id}`);
  return unwrap(resp);
}
