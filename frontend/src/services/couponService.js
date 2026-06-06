import apiClient from '../lib/apiClient';

function unwrap(response) {
  const body = response?.data;
  if (body && body.success === false) {
    throw new Error(body.message || 'Request failed');
  }
  return body?.data;
}

export async function validateCoupon(code, { variantId, quantity } = {}) {
  const resp = await apiClient.post('/coupons/validate', { code, variantId, quantity });
  return unwrap(resp);
}
