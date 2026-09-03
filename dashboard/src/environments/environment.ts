export const environment = {
  production: true,
  /**
   * Relative on purpose: the dashboard container serves the static files and
   * proxies /api to the backend, so one build works in every environment.
   */
  apiBaseUrl: '/api',
  i18n: {
    langs: ['en', 'de'],
    /** English is what a reader falls back to when their language has no value. */
    defaultLang: 'en',
    storageKey: 'vc:language',
    storageUse: true,
    browserUse: true
  }
};
