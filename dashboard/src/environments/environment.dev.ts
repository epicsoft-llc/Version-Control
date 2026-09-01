export const environment = {
  production: false,
  /** The dev server has no proxy in front of it, so the backend is addressed directly. */
  apiBaseUrl: 'http://localhost:8080/api',
  i18n: {
    langs: ['en', 'de'],
    defaultLang: 'en',
    storageKey: 'vc:language',
    storageUse: true,
    browserUse: true
  }
};
