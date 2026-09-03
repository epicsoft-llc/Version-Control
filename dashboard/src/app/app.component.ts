import {ChangeDetectionStrategy, Component, inject} from '@angular/core';
import {UpperCasePipe} from '@angular/common';
import {RouterLink, RouterLinkActive, RouterOutlet} from '@angular/router';
import {TranslatePipe} from '@ngx-translate/core';
import {I18nService} from './services/i18n/i18n.service';

@Component({
  // Default, explicitly: this Angular version compiles a component as OnPush
  // unless told otherwise, and these components hold their state in plain
  // fields written from subscribe callbacks. Under OnPush such a view is
  // never marked dirty, so every page renders its heading and then stays
  // empty for good - with no error anywhere. Until the state is held in
  // signals, the strategy has to say what the code assumes.
  changeDetection: ChangeDetectionStrategy.Default,
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
