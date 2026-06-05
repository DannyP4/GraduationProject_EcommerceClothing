import apiClient from '../lib/apiClient';

function unwrap(response) {
  const body = response?.data;
  if (body && body.success === false) {
    throw new Error(body.message || 'Request failed');
  }
  return body?.data;
}

export async function placeOrder({ addressId, paymentMethod, notes, couponCode }) {
  const resp = await apiClient.post('/orders', { addressId, paymentMethod, notes, couponCode });
  return unwrap(resp);
}

export async function listOrders({ page = 0, size = 10 } = {}) {
  const resp = await apiClient.get('/orders', { params: { page, size } });
  return unwrap(resp);
}

export async function getOrder(orderNumber) {
  const resp = await apiClient.get(`/orders/${orderNumber}`);
  return unwrap(resp);
}

export async function cancelOrder(orderNumber) {
  const resp = await apiClient.post(`/orders/${orderNumber}/cancel`, {});
  return unwrap(resp);
}
