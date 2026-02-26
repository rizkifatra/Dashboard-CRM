// Production environment configuration
// These values will be set during Azure deployment
export const environment = {
  production: true,
  // Replace with your Azure Container App backend URL
  apiUrl: 'https://YOUR_BACKEND_APP_NAME.azurecontainerapps.io/api',
  apiBaseUrl: 'https://YOUR_BACKEND_APP_NAME.azurecontainerapps.io',
  oauth2AuthorizationUrl:
    'https://YOUR_BACKEND_APP_NAME.azurecontainerapps.io/oauth2/authorization/azure',
};
