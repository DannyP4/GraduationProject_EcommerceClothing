import apiClient from '../lib/apiClient';

function unwrap(response) {
  const body = response?.data;
  if (body && body.success === false) {
    throw new Error(body.message || 'Request failed');
  }
  return body?.data;
}

export async function listAddresses() {
  const resp = await apiClient.get('/addresses');
  return unwrap(resp);
}

export async function createAddress(payload) {
  const resp = await apiClient.post('/addresses', payload);
  return unwrap(resp);
}

export async function updateAddress(id, payload) {
  const resp = await apiClient.patch(`/addresses/${id}`, payload);
  return unwrap(resp);
}

export async function deleteAddress(id) {
  const resp = await apiClient.delete(`/addresses/${id}`);
  return unwrap(resp);
}

export async function setDefaultAddress(id) {
  const resp = await apiClient.post(`/addresses/${id}/default`, {});
  return unwrap(resp);
}
