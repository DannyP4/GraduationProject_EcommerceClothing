import apiClient from '../lib/apiClient';

function unwrap(response) {
  const body = response?.data;
  if (body && body.success === false) {
    throw new Error(body.message || 'Request failed');
  }
  return body?.data;
}

export async function listCategories() {
  const resp = await apiClient.get('/admin/categories');
  return unwrap(resp);
}

export async function getCategory(id) {
  const resp = await apiClient.get(`/admin/categories/${id}`);
  return unwrap(resp);
}

export async function createCategory(payload) {
  const resp = await apiClient.post('/admin/categories', payload);
  return unwrap(resp);
}

export async function updateCategory(id, payload) {
  const resp = await apiClient.put(`/admin/categories/${id}`, payload);
  return unwrap(resp);
}

export async function deleteCategory(id) {
  const resp = await apiClient.delete(`/admin/categories/${id}`);
  return unwrap(resp);
}
