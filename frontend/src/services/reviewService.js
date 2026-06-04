import apiClient from '../lib/apiClient';
import { uploadDirectToCloudinary } from './adminUploadService';

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

export async function getProductReviews(idOrSlug, { page = 0, size = 5, sort = 'newest' } = {}) {
  const resp = await apiClient.get(`/products/${encodeURIComponent(idOrSlug)}/reviews`, {
    params: cleanParams({ page, size, sort }),
  });
  return unwrap(resp);
}

export async function getReviewEligibility(productId) {
  const resp = await apiClient.get('/reviews/eligibility', { params: { productId } });
  return unwrap(resp);
}

export async function createReview(payload) {
  const resp = await apiClient.post('/reviews', payload);
  return unwrap(resp);
}

export async function updateReview(id, payload) {
  const resp = await apiClient.put(`/reviews/${id}`, payload);
  return unwrap(resp);
}

export async function deleteReview(id) {
  const resp = await apiClient.delete(`/reviews/${id}`);
  return unwrap(resp);
}

export async function setReviewHelpful(id, helpful) {
  const resp = helpful
    ? await apiClient.post(`/reviews/${id}/helpful`)
    : await apiClient.delete(`/reviews/${id}/helpful`);
  return unwrap(resp);
}

export async function uploadReviewImage(file) {
  const signature = unwrap(await apiClient.post('/reviews/upload-signature', { filenameHint: file.name }));
  const cloud = await uploadDirectToCloudinary(file, signature);
  return { url: cloud.secure_url, publicId: cloud.public_id };
}
