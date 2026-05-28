import apiClient from '../lib/apiClient';

function unwrap(response) {
  const body = response?.data;
  if (body && body.success === false) {
    throw new Error(body.message || 'Request failed');
  }
  return body?.data;
}

export async function listImages(productId) {
  const resp = await apiClient.get(`/admin/products/${productId}/images`);
  return unwrap(resp);
}

export async function createImage(productId, payload) {
  const resp = await apiClient.post(`/admin/products/${productId}/images`, payload);
  return unwrap(resp);
}

export async function updateImage(imageId, payload) {
  const resp = await apiClient.put(`/admin/images/${imageId}`, payload);
  return unwrap(resp);
}

export async function deleteImage(imageId) {
  const resp = await apiClient.delete(`/admin/images/${imageId}`);
  return unwrap(resp);
}
