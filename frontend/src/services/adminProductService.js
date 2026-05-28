import apiClient from '../lib/apiClient';

function unwrap(response) {
  const body = response?.data;
  if (body && body.success === false) {
    throw new Error(body.message || 'Request failed');
  }
  return body?.data;
}

export async function listProducts(params = {}) {
  const resp = await apiClient.get('/admin/products', { params });
  return unwrap(resp);
}

export async function getProduct(id) {
  const resp = await apiClient.get(`/admin/products/${id}`);
  return unwrap(resp);
}

export async function createProduct(payload) {
  const resp = await apiClient.post('/admin/products', payload);
  return unwrap(resp);
}

export async function updateProduct(id, payload) {
  const resp = await apiClient.put(`/admin/products/${id}`, payload);
  return unwrap(resp);
}

export async function softDeleteProduct(id) {
  const resp = await apiClient.delete(`/admin/products/${id}`);
  return unwrap(resp);
}

export async function restoreProduct(id) {
  const resp = await apiClient.post(`/admin/products/${id}/restore`);
  return unwrap(resp);
}

export async function hardDeleteProduct(id) {
  const resp = await apiClient.delete(`/admin/products/${id}/permanent`);
  return unwrap(resp);
}
