import client from '../api/client';

export const authApi = {
  login: (payload) => client.post('/auth/login', payload).then((r) => r.data),
  me: () => client.get('/auth/me').then((r) => r.data),
  logout: () => client.post('/auth/logout').then((r) => r.data),
  currentSession: () => client.get('/session/current').then((r) => r.data),
};
