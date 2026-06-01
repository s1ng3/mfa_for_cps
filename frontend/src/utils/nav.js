// Decides where to send a user once fully authenticated, based on their roles.
export function landingPath(profile) {
  if (!profile) return '/login';
  const roles = profile.roles || [];
  if (roles.includes('ADMIN') || roles.includes('SECURITY_OFFICER')) return '/admin';
  return '/hmi';
}
