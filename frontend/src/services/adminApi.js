import client from '../api/client';

export const adminApi = {
  dashboard: () => client.get('/admin/dashboard').then((r) => r.data),
  users: () => client.get('/admin/users').then((r) => r.data),
  createUser: (payload) => client.post('/admin/users', payload).then((r) => r.data),
  lockUser: (id) => client.put(`/admin/users/${id}/lock`).then((r) => r.data),
  unlockUser: (id) => client.put(`/admin/users/${id}/unlock`).then((r) => r.data),
  sessions: () => client.get('/admin/sessions').then((r) => r.data),
  terminateSession: (id) => client.post(`/admin/sessions/${id}/terminate`).then((r) => r.data),
  auditLogs: (page = 0, size = 150) =>
    client.get(`/audit/logs?page=${page}&size=${size}`).then((r) => r.data),
};
