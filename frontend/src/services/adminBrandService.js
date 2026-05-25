import apiClient from '../lib/apiClient';

function unwrap(response) {
  const body = response?.data;
  if (body && body.success === false) {
    throw new Error(body.message || 'Request failed');
  }
  return body?.data;
}

export async function listBrands() {
  const resp = await apiClient.get('/admin/brands');
  return unwrap(resp);
}

export async function getBrand(id) {
  const resp = await apiClient.get(`/admin/brands/${id}`);
  return unwrap(resp);
}

export async function createBrand(payload) {
  const resp = await apiClient.post('/admin/brands', payload);
  return unwrap(resp);
}

export async function updateBrand(id, payload) {
  const resp = await apiClient.put(`/admin/brands/${id}`, payload);
  return unwrap(resp);
}

export async function deleteBrand(id) {
  const resp = await apiClient.delete(`/admin/brands/${id}`);
  return unwrap(resp);
}
