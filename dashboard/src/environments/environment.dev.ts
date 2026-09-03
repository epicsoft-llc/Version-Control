export const environment = {
  production: false,
  /**
   * Relative, exactly like the production build: the dev server proxies /api to
   * the backend (proxy.conf.json). An absolute address here would make the
   * browser refuse every call as cross-origin, and the only way to allow it
   * would be a CORS rule in the backend that has no business existing.
   */
  apiBaseUrl: '/api',
  i18n: {
    langs: ['en', 'de'],
    defaultLang: 'en',
    storageKey: 'vc:language',
    storageUse: true,
    browserUse: true
  }
};
