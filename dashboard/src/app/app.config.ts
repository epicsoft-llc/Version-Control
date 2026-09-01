import {ApplicationConfig, inject, provideAppInitializer} from '@angular/core';
import {provideRouter, withComponentInputBinding} from '@angular/router';
import {provideHttpClient, withInterceptorsFromDi} from '@angular/common/http';
import {provideTranslateService} from '@ngx-translate/core';
import {provideTranslateHttpLoader} from '@ngx-translate/http-loader';
import {routes} from './app.routes';
import {environment} from '../environments/environment';
import {I18nService} from './services/i18n/i18n.service';

export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes, withComponentInputBinding()),
    provideHttpClient(withInterceptorsFromDi()),
    provideTranslateService({
      fallbackLang: environment.i18n.defaultLang,
      loader: provideTranslateHttpLoader({prefix: './assets/i18n/', suffix: '.json'})
    }),
    provideAppInitializer(() => inject(I18nService).init())
  ]
};
