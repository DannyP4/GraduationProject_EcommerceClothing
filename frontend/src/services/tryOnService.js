import apiClient from '../lib/apiClient';
import { uploadDirectToCloudinary } from './adminUploadService';

function unwrap(response) {
  const body = response?.data;
  if (body && body.success === false) {
    throw new Error(body.message || 'Request failed');
  }
  return body?.data;
}

export async function uploadTryOnPhoto(file) {
  const signature = unwrap(await apiClient.post('/try-on/upload-signature', { filenameHint: file.name }));
  const cloud = await uploadDirectToCloudinary(file, signature);
  return { url: cloud.secure_url, publicId: cloud.public_id };
}

export async function createTryOn({ productId, userImageUrl }) {
  const resp = await apiClient.post('/try-on', { productId, userImageUrl });
  return unwrap(resp);
}

export async function getTryOn(id) {
  const resp = await apiClient.get(`/try-on/${encodeURIComponent(id)}`);
  return unwrap(resp);
}
