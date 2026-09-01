import {Component, inject} from '@angular/core';
import {UpperCasePipe} from '@angular/common';
import {RouterLink, RouterLinkActive, RouterOutlet} from '@angular/router';
import {TranslatePipe} from '@ngx-translate/core';
import {I18nService} from './services/i18n/i18n.service';

@Component({
  selector: 'vc-root',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, TranslatePipe, UpperCasePipe],
  templateUrl: './app.component.html'
})
export class AppComponent {

  private readonly i18n: I18nService = inject(I18nService);

  protected readonly langs: string[] = this.i18n.getLangs();

  protected switchLanguage(lang: string): void {
    this.i18n.use(lang);
  }

  protected isCurrent(lang: string): boolean {
    return this.i18n.getCurrent() === lang;
  }
}
