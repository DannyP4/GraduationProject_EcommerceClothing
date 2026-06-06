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

export async function getQuote({ region, subtotal } = {}) {
  const resp = await apiClient.get('/shipping/quote', { params: cleanParams({ region, subtotal }) });
  return unwrap(resp);
}
