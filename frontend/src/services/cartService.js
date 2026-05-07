import apiClient from '../lib/apiClient';

function unwrap(response) {
  const body = response?.data;
  if (body && body.success === false) {
    throw new Error(body.message || 'Request failed');
  }
  return body?.data;
}

export async function getCart() {
  const resp = await apiClient.get('/cart');
  return unwrap(resp);
}

export async function addItem({ variantId, quantity }) {
  const resp = await apiClient.post('/cart/items', { variantId, quantity });
  return unwrap(resp);
}

export async function updateItem(itemId, quantity) {
  const resp = await apiClient.patch(`/cart/items/${itemId}`, { quantity });
  return unwrap(resp);
}

export async function removeItem(itemId) {
  const resp = await apiClient.delete(`/cart/items/${itemId}`);
  return unwrap(resp);
}

export async function clearCart() {
  const resp = await apiClient.delete('/cart');
  return unwrap(resp);
}

export async function mergeCart(items) {
  const resp = await apiClient.post('/cart/merge', { items });
  return unwrap(resp);
}
