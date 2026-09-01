import {inject, Injectable} from '@angular/core';
import {TranslateService} from '@ngx-translate/core';
import {environment} from '../../../environments/environment';
import {isDefined} from '../../libs/utils/vc.utils';

/**
 * Language selection: stored choice, then browser language, then the default.
 * The default is also the fallback ngx-translate uses for a missing key.
 */
@Injectable({providedIn: 'root'})
export class I18nService {

  private readonly translate: TranslateService = inject(TranslateService);

  private readonly langs: string[] = environment.i18n.langs;
  private readonly defaultLang: string = environment.i18n.defaultLang;
  private readonly storageKey: string = environment.i18n.storageKey;

  public init(): void {
    this.translate.addLangs(this.langs);
    this.translate.setFallbackLang(this.defaultLang);
    this.use(this.resolveInitialLanguage());
  }

  public use(lang: string): void {
    if (!this.langs.includes(lang)) {
      return;
    }
    this.translate.use(lang);
    if (environment.i18n.storageUse) {
      localStorage.setItem(this.storageKey, lang);
    }
  }

  public getCurrent(): string {
    return this.translate.getCurrentLang() ?? this.defaultLang;
  }

  public getLangs(): string[] {
    return this.langs;
  }

  private resolveInitialLanguage(): string {
    const stored: string | null = environment.i18n.storageUse ? localStorage.getItem(this.storageKey) : null;
    if (isDefined(stored) && this.langs.includes(stored)) {
      return stored;
    }
    const browser: string | undefined = environment.i18n.browserUse ? this.translate.getBrowserLang() : undefined;
    if (isDefined(browser) && this.langs.includes(browser)) {
      return browser;
    }
    return this.defaultLang;
  }
}
