import client from '../api/client';

export const incidentApi = {
  list: () => client.get('/incidents').then((r) => r.data),
  get: (id) => client.get(`/incidents/${id}`).then((r) => r.data),
  create: (payload) => client.post('/incidents', payload).then((r) => r.data),
  assign: (id, assignedTo) => client.put(`/incidents/${id}/assign`, { assignedTo }).then((r) => r.data),
  setStatus: (id, status, note) => client.put(`/incidents/${id}/status`, { status, note }).then((r) => r.data),
};
