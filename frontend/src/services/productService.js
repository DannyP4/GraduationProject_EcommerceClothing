import apiClient from '../lib/apiClient';

function unwrap(response) {
  const body = response?.data;
  if (body && body.success === false) {
    throw new Error(body.message || 'Request failed');
  }
  return body?.data;
}

export async function getProducts(params = {}) {
  const cleaned = Object.fromEntries(
    Object.entries(params).filter(([, v]) => v !== undefined && v !== null && v !== '')
  );
  const resp = await apiClient.get('/products', { params: cleaned, _skipAuth: true });
  return unwrap(resp);
}

export async function getProductByIdOrSlug(idOrSlug) {
  const resp = await apiClient.get(`/products/${encodeURIComponent(idOrSlug)}`, { _skipAuth: true });
  return unwrap(resp);
}

export async function getCategories() {
  const resp = await apiClient.get('/categories', { _skipAuth: true });
  return unwrap(resp);
}

export async function getBrands() {
  const resp = await apiClient.get('/brands', { _skipAuth: true });
  return unwrap(resp);
}
