import apiClient from '../lib/apiClient';

function unwrap(response) {
  const body = response?.data;
  if (body && body.success === false) {
    throw new Error(body.message || 'Request failed');
  }
  return body?.data;
}

export async function getWishlist() {
  return unwrap(await apiClient.get('/wishlist'));
}

export async function getWishlistIds() {
  return unwrap(await apiClient.get('/wishlist/ids'));
}

export async function toggleWishlist(productId) {
  return unwrap(await apiClient.post(`/wishlist/${productId}/toggle`));
}

export async function removeFromWishlist(productId) {
  return unwrap(await apiClient.delete(`/wishlist/${productId}`));
}

export async function clearWishlist() {
  return unwrap(await apiClient.delete('/wishlist'));
}
