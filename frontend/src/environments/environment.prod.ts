// Production environment configuration
// Uses relative URLs - nginx proxies to backend
export const environment = {
  production: true,
  // Relative URLs - nginx handles proxying to backend
  apiUrl: '/api',
  apiBaseUrl: '',
  oauth2AuthorizationUrl: '/oauth2/authorization/azure',
};
