import apiClient from '../lib/apiClient';

function unwrap(response) {
  const body = response?.data;
  if (body && body.success === false) {
    throw new Error(body.message || 'Request failed');
  }
  return body?.data;
}

export async function verifyVnpayReturn(queryParams) {
  const resp = await apiClient.get('/payments/vnpay/verify', {
    params: queryParams,
    _skipAuth: true,
  });
  return unwrap(resp);
}

export async function retryPayment(orderNumber) {
  const resp = await apiClient.post(`/payments/${encodeURIComponent(orderNumber)}/retry`, {});
  return unwrap(resp);
}
