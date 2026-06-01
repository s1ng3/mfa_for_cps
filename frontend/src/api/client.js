import axios from 'axios';
import { getDeviceFingerprint } from '../utils/deviceFingerprint';

// Same-origin /api (Vite proxies to the Spring Boot gateway on :8080).
const client = axios.create({ baseURL: '/api' });

const TOKEN_KEY = 'cps_session_token';

export const tokenStore = {
  get: () => localStorage.getItem(TOKEN_KEY),
  set: (t) => localStorage.setItem(TOKEN_KEY, t),
  clear: () => localStorage.removeItem(TOKEN_KEY),
};

// Attach the opaque session token + device fingerprint to every request.
client.interceptors.request.use((config) => {
  const token = tokenStore.get();
  if (token) config.headers.Authorization = `Bearer ${token}`;
  config.headers['X-Device-Fingerprint'] = getDeviceFingerprint();
  return config;
});

// Callback registered by AuthContext so a 401 anywhere forces a clean logout.
let onUnauthorized = null;
export const setUnauthorizedHandler = (fn) => { onUnauthorized = fn; };

client.interceptors.response.use(
  (res) => res,
  (error) => {
    const status = error?.response?.status;
    const code = error?.response?.data?.code;
    if (status === 401 && onUnauthorized) {
      onUnauthorized(code || 'UNAUTHORIZED');
    }
    return Promise.reject(error);
  }
);

// Normalises backend error envelopes into a readable message.
export function apiError(error, fallback = 'Request failed') {
  return error?.response?.data?.message || error?.message || fallback;
}

export default client;
