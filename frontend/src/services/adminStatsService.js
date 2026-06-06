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

export async function getSummary({ from, to } = {}) {
  const resp = await apiClient.get('/admin/stats/summary', {
    params: cleanParams({ from, to }),
  });
  return unwrap(resp);
}

export async function getRevenue({ from, to, granularity = 'DAY' } = {}) {
  const resp = await apiClient.get('/admin/stats/revenue', {
    params: cleanParams({ from, to, granularity }),
  });
  return unwrap(resp);
}

export async function getPaymentBreakdown({ from, to } = {}) {
  const resp = await apiClient.get('/admin/stats/payment-breakdown', {
    params: cleanParams({ from, to }),
  });
  return unwrap(resp);
}

export async function getOrdersByStatus({ from, to } = {}) {
  const resp = await apiClient.get('/admin/stats/orders-by-status', {
    params: cleanParams({ from, to }),
  });
  return unwrap(resp);
}

export async function getTopProducts({ from, to, limit = 10 } = {}) {
  const resp = await apiClient.get('/admin/stats/top-products', {
    params: cleanParams({ from, to, limit }),
  });
  return unwrap(resp);
}

export async function getTopCustomers({ from, to, limit = 5 } = {}) {
  const resp = await apiClient.get('/admin/stats/top-customers', {
    params: cleanParams({ from, to, limit }),
  });
  return unwrap(resp);
}

export async function getOps() {
  const resp = await apiClient.get('/admin/stats/ops');
  return unwrap(resp);
}
