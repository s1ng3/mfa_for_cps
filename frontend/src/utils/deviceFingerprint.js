// Computes a stable-ish device fingerprint for the risk engine. For the demo this combines a
// persisted random id with coarse browser characteristics. A real deployment would use a
// hardened library (e.g. FingerprintJS) — the backend only cares that the value is stable.

const STORAGE_KEY = 'cps_device_id';

function persistentId() {
  let id = localStorage.getItem(STORAGE_KEY);
  if (!id) {
    id = crypto.randomUUID();
    localStorage.setItem(STORAGE_KEY, id);
  }
  return id;
}

export function getDeviceFingerprint() {
  const parts = [
    persistentId(),
    navigator.platform,
    navigator.language,
    `${screen.width}x${screen.height}`,
    new Date().getTimezoneOffset(),
  ];
  return btoa(parts.join('|')).slice(0, 48);
}

// Used by the "simulate new device" demo toggle: forget the persisted id so the next login
// presents an unrecognised fingerprint to the risk engine.
export function resetDeviceFingerprint() {
  localStorage.removeItem(STORAGE_KEY);
}
