import {Component, inject, Input, OnDestroy} from '@angular/core';
import {DatePipe} from '@angular/common';
import {HttpErrorResponse} from '@angular/common/http';
import {RouterLink} from '@angular/router';
import {NgbModal} from '@ng-bootstrap/ng-bootstrap';
import {TranslatePipe} from '@ngx-translate/core';
import {forkJoin, Subscription} from 'rxjs';
import {ComponentDto} from '../../repositories/dto/component.dto';
import {ComponentEnvironmentDto, ComponentEnvironmentRequest} from '../../repositories/dto/component-environment.dto';
import {EnvironmentDto} from '../../repositories/dto/environment.dto';
import {VersionChangeDto} from '../../repositories/dto/version-change.dto';
import {ComponentRepo} from '../../repositories/api/component.repo';
import {ComponentEnvironmentRepo} from '../../repositories/api/component-environment.repo';
import {EnvironmentRepo} from '../../repositories/api/environment.repo';
import {VersionChangeRepo} from '../../repositories/api/version-change.repo';
import {VersionChangeDialogComponent} from '../../components/version-change-dialog/version-change-dialog.component';
import {isDefined, nonBlank, unsubscribe} from '../../libs/utils/vc.utils';

/** One environment of this component, with whatever is known about it. */
export class EnvironmentRow {

  public environment!: EnvironmentDto;
  public cell?: ComponentEnvironmentDto;

  constructor(init?: Partial<EnvironmentRow>) {
    Object.assign(this, init);
  }
}

/**
 * Everything about one component: what it is, where it runs, and every change
 * that was ever recorded for it — newest first, because that is the end people
 * read.
 */
@Component({
  selector: 'vc-component-detail',
  imports: [DatePipe, RouterLink, TranslatePipe],
  templateUrl: './component-detail.component.html'
})
export class ComponentDetailComponent implements OnDestroy {

  private readonly componentRepo: ComponentRepo = inject(ComponentRepo);
  private readonly environmentRepo: EnvironmentRepo = inject(EnvironmentRepo);
  private readonly componentEnvironmentRepo: ComponentEnvironmentRepo = inject(ComponentEnvironmentRepo);
  private readonly versionChangeRepo: VersionChangeRepo = inject(VersionChangeRepo);
  private readonly modal: NgbModal = inject(NgbModal);
  private readonly subs: Subscription[] = [];

  private componentId?: string;

  /** Placeholder for an empty field; punctuation, so not a translation key. */
  protected readonly dash = '–';

  protected component?: ComponentDto;
  protected rows: EnvironmentRow[] = [];
  protected history: VersionChangeDto[] = [];
  protected loading = true;
  protected errorMessage?: string;

  /**
   * From the route, bound by `withComponentInputBinding`. A setter rather than
   * `ngOnInit`: the router reuses the instance when only the id changes, and the
   * page would otherwise keep showing the component that was open before.
   */
  @Input()
  public set id(value: string) {
    if (!nonBlank(value) || value === this.componentId) {
      return;
    }
    this.componentId = value;
    this.load();
  }

  public ngOnDestroy(): void {
    unsubscribe(this.subs);
  }

  protected environmentName(environmentId: string): string {
    return this.rows.find((row) => row.environment.id === environmentId)?.environment.name ?? '';
  }

  protected openVersionChange(row: EnvironmentRow): void {
    if (!isDefined(this.component) || !isDefined(row.cell) || !row.cell.active) {
      return;
    }
    const reference = this.modal.open(VersionChangeDialogComponent, {size: 'lg'});
    reference.componentInstance.component = this.component;
    reference.componentInstance.cell = row.cell;
    reference.componentInstance.environmentName = row.environment.name;
    reference.result.then(() => this.load(), () => undefined);
  }

  /**
   * Switching a cell on or off says whether the component belongs in that
   * environment at all. It never touches the version — that is the dialog.
   */
  protected setActive(row: EnvironmentRow, active: boolean): void {
    if (!isDefined(this.component)) {
      return;
    }
    const request = new ComponentEnvironmentRequest({
      componentId: this.component.id,
      environmentId: row.environment.id,
      active: active
    });
    this.subs.push(this.componentEnvironmentRepo.createOrUpdate(request).subscribe({
      next: () => this.load(),
      // The refusal is a translated sentence; swallowing it leaves a button that
      // looks broken instead of one that says why it did nothing.
      error: (error: HttpErrorResponse) => this.showError(error)
    }));
  }

  private load(): void {
    if (!isDefined(this.componentId)) {
      return;
    }
    this.loading = true;
    this.errorMessage = undefined;
    this.subs.push(this.componentRepo.getById(this.componentId).subscribe({
      next: (component) => this.loadDetails(component),
      error: (error: HttpErrorResponse) => this.failed(error)
    }));
  }

  private loadDetails(component: ComponentDto): void {
    this.component = component;
    this.subs.push(forkJoin({
      environments: this.environmentRepo.findAllByProject(component.projectId),
      cells: this.componentEnvironmentRepo.findAllByComponent(component.id),
      history: this.versionChangeRepo.findAllByComponent(component.id)
    }).subscribe({
      next: (loaded) => {
        this.rows = loaded.environments.map((environment) => new EnvironmentRow({
          environment: environment,
          cell: loaded.cells.find((cell) => cell.environmentId === environment.id)
        }));
        this.history = loaded.history;
        this.loading = false;
      },
      error: (error: HttpErrorResponse) => this.failed(error)
    }));
  }

  /**
   * A failure has to end the loading state as well. Left standing it hides both
   * the error and the "no such component" text behind a page that stays blank.
   */
  private failed(error: HttpErrorResponse): void {
    this.component = undefined;
    this.rows = [];
    this.history = [];
    this.loading = false;
    this.showError(error);
  }

  private showError(error: HttpErrorResponse): void {
    this.errorMessage = error.error?.message ?? error.message;
  }
}
