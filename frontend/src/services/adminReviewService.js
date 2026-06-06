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

export async function listReviews({ status, search, page = 0, size = 20 } = {}) {
  const resp = await apiClient.get('/admin/reviews', {
    params: cleanParams({ status, search, page, size }),
  });
  return unwrap(resp);
}

export async function getReview(id) {
  return unwrap(await apiClient.get(`/admin/reviews/${id}`));
}

export async function approveReview(id) {
  return unwrap(await apiClient.post(`/admin/reviews/${id}/approve`));
}

export async function rejectReview(id) {
  return unwrap(await apiClient.post(`/admin/reviews/${id}/reject`));
}

export async function deleteReview(id) {
  return unwrap(await apiClient.delete(`/admin/reviews/${id}`));
}
