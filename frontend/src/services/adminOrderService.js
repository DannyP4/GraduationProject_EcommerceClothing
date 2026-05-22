import apiClient from '../lib/apiClient';

function unwrap(response) {
  const body = response?.data;
  if (body && body.success === false) {
    throw new Error(body.message || 'Request failed');
  }
  return body?.data;
}

function cleanParams(input) {
  const out = {};
  Object.entries(input).forEach(([k, v]) => {
    if (v !== undefined && v !== null && v !== '') out[k] = v;
  });
  return out;
}

export async function listOrders({ status, search, from, to, page = 0, size = 20 } = {}) {
  const resp = await apiClient.get('/admin/orders', {
    params: cleanParams({ status, search, from, to, page, size }),
  });
  return unwrap(resp);
}

export async function getOrder(orderNumber) {
  const resp = await apiClient.get(`/admin/orders/${orderNumber}`);
  return unwrap(resp);
}

export async function transitionOrder(orderNumber, { targetStatus, note } = {}) {
  const resp = await apiClient.patch(`/admin/orders/${orderNumber}/transition`, { targetStatus, note });
  return unwrap(resp);
}

export async function cancelOrder(orderNumber, { reason } = {}) {
  const resp = await apiClient.post(`/admin/orders/${orderNumber}/cancel`, { reason });
  return unwrap(resp);
}
