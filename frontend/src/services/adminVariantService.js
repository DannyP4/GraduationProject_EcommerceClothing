import apiClient from '../lib/apiClient';

function unwrap(response) {
  const body = response?.data;
  if (body && body.success === false) {
    throw new Error(body.message || 'Request failed');
  }
  return body?.data;
}

export async function listVariants(productId) {
  const resp = await apiClient.get(`/admin/products/${productId}/variants`);
  return unwrap(resp);
}

export async function createVariant(productId, payload) {
  const resp = await apiClient.post(`/admin/products/${productId}/variants`, payload);
  return unwrap(resp);
}

export async function updateVariant(variantId, payload) {
  const resp = await apiClient.put(`/admin/variants/${variantId}`, payload);
  return unwrap(resp);
}

export async function deleteVariant(variantId) {
  const resp = await apiClient.delete(`/admin/variants/${variantId}`);
  return unwrap(resp);
}
