import apiClient from '../lib/apiClient';

function unwrap(response) {
  const body = response?.data;
  if (body && body.success === false) {
    throw new Error(body.message || 'Request failed');
  }
  return body?.data;
}

export async function sendChatMessage(message, history = []) {
  const resp = await apiClient.post('/chat', { message, history }, { _skipAuth: true });
  return unwrap(resp);
}
