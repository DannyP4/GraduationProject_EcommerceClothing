import apiClient from '../lib/apiClient';

function unwrap(response) {
  const body = response?.data;
  if (body && body.success === false) {
    throw new Error(body.message || 'Request failed');
  }
  return body?.data;
}

export async function getSignature({ folder, filenameHint } = {}) {
  const resp = await apiClient.post('/admin/upload-signature', { folder, filenameHint });
  return unwrap(resp);
}

export async function uploadDirectToCloudinary(file, signature) {
  const form = new FormData();
  form.append('file', file);
  form.append('api_key', signature.apiKey);
  form.append('timestamp', String(signature.timestamp));
  form.append('signature', signature.signature);
  form.append('folder', signature.folder);
  form.append('public_id', signature.publicId);

  const url = `https://api.cloudinary.com/v1_1/${signature.cloudName}/image/upload`;
  const resp = await fetch(url, { method: 'POST', body: form });
  if (!resp.ok) {
    let detail = '';
    try {
      const err = await resp.json();
      detail = err?.error?.message || JSON.stringify(err);
    } catch {
      detail = await resp.text();
    }
    throw new Error(`Cloudinary upload failed (${resp.status}): ${detail}`);
  }
  return resp.json();
}

export async function uploadImageEndToEnd(file, { folder, filenameHint } = {}) {
  const signature = await getSignature({ folder, filenameHint });
  const cloudResult = await uploadDirectToCloudinary(file, signature);
  return {
    url: cloudResult.secure_url,
    publicId: cloudResult.public_id,
    width: cloudResult.width,
    height: cloudResult.height,
    format: cloudResult.format,
    bytes: cloudResult.bytes,
  };
}
