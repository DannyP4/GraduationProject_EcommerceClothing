import apiClient from '../lib/apiClient';

function unwrap(response) {
  const body = response?.data;
  if (body && body.success === false) {
    throw new Error(body.message || 'Request failed');
  }
  return body?.data;
}

export async function listCoupons({ status, search, page = 0, size = 100 } = {}) {
  const params = { page, size };
  if (status) params.status = status;
  if (search) params.search = search;
  const resp = await apiClient.get('/admin/coupons', { params });
  return unwrap(resp);
}

export async function getCoupon(id) {
  const resp = await apiClient.get(`/admin/coupons/${id}`);
  return unwrap(resp);
}

export async function createCoupon(payload) {
  const resp = await apiClient.post('/admin/coupons', payload);
  return unwrap(resp);
}

export async function updateCoupon(id, payload) {
  const resp = await apiClient.put(`/admin/coupons/${id}`, payload);
  return unwrap(resp);
}

export async function deleteCoupon(id) {
  const resp = await apiClient.delete(`/admin/coupons/${id}`);
  return unwrap(resp);
}
