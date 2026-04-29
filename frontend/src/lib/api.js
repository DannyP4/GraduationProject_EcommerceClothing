export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api';

if (import.meta.env.DEV || import.meta.env.MODE !== 'production') {
  // eslint-disable-next-line no-console
  console.info('[boot] API_BASE_URL =', API_BASE_URL);
}
