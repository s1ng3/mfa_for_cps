import client from '../api/client';

export const hmiApi = {
  status: () => client.get('/hmi/status').then((r) => r.data),
  startPump: () => client.post('/hmi/start-pump').then((r) => r.data),
  stopPump: () => client.post('/hmi/stop-pump').then((r) => r.data),
  acknowledgeAlarm: () => client.post('/hmi/acknowledge-alarm').then((r) => r.data),
  changeMotorSpeed: (value) => client.post('/hmi/change-motor-speed', { value }).then((r) => r.data),
  changeTemperature: (value) => client.post('/hmi/change-temperature-setpoint', { value }).then((r) => r.data),
  changePressure: (value) => client.post('/hmi/change-pressure-setpoint', { value }).then((r) => r.data),
  resetEmergencyStop: () => client.post('/hmi/reset-emergency-stop').then((r) => r.data),
};

export const stepUpApi = {
  request: (actionType) => client.post('/step-up/request', { actionType }).then((r) => r.data),
  verify: (credentialId) => client.post('/step-up/verify', { credentialId }).then((r) => r.data),
  executeAction: (actionId) => client.post('/step-up/execute-action', { actionId }).then((r) => r.data),
};
