import {Component, inject, OnDestroy, OnInit} from '@angular/core';
import {DatePipe} from '@angular/common';
import {HttpErrorResponse} from '@angular/common/http';
import {FormsModule} from '@angular/forms';
import {RouterLink} from '@angular/router';
import {NgbModal} from '@ng-bootstrap/ng-bootstrap';
import {TranslatePipe} from '@ngx-translate/core';
import {catchError, forkJoin, of, Subject, Subscription, switchMap, tap} from 'rxjs';
import {ProjectDto} from '../../repositories/dto/project.dto';
import {MatrixCellDto, MatrixDto} from '../../repositories/dto/matrix.dto';
import {ProjectRepo} from '../../repositories/api/project.repo';
import {ComponentRepo} from '../../repositories/api/component.repo';
import {ComponentEnvironmentRepo} from '../../repositories/api/component-environment.repo';
import {VersionChangeDialogComponent} from '../../components/version-change-dialog/version-change-dialog.component';
import {isDefined, unsubscribe} from '../../libs/utils/vc.utils';

/**
 * The overview: components as rows, environments as columns.
 *
 * <p>The four cell states are drawn differently on purpose. A cell that is
 * switched off says "not here, and that is intended"; an empty one says "runs
 * here and nobody knows which version" — reading one as the other is the mistake
 * this table exists to prevent.
 */
@Component({
  selector: 'vc-matrix',
  imports: [DatePipe, FormsModule, RouterLink, TranslatePipe],
  templateUrl: './matrix.component.html'
})
export class MatrixComponent implements OnInit, OnDestroy {

  private readonly projectRepo: ProjectRepo = inject(ProjectRepo);
  private readonly componentRepo: ComponentRepo = inject(ComponentRepo);
  private readonly componentEnvironmentRepo: ComponentEnvironmentRepo = inject(ComponentEnvironmentRepo);
  private readonly modal: NgbModal = inject(NgbModal);
  private readonly subs: Subscription[] = [];

  /**
   * Every selection goes through here so that a slow answer for the project the
   * reader has already left cannot overwrite the one they are looking at.
   */
  private readonly selection = new Subject<string>();

  protected projects: ProjectDto[] = [];
  protected selectedProjectId?: string;
  protected matrix?: MatrixDto;
  protected loading = true;
  protected errorMessage?: string;

  public ngOnInit(): void {
    this.subs.push(this.selection.pipe(
      tap((projectId) => {
        this.selectedProjectId = projectId;
        // Dropped before the request, not after it: the previous project's rows
        // under the new project's name would be read as this project's state.
        this.matrix = undefined;
        this.loading = true;
        this.errorMessage = undefined;
      }),
      switchMap((projectId) => this.projectRepo.getMatrix(projectId).pipe(catchError((error) => {
        this.showError(error);
        return of(undefined);
      })))
    ).subscribe((matrix) => {
      this.matrix = matrix;
      this.loading = false;
    }));

    this.subs.push(this.projectRepo.findAll().subscribe({
      next: (projects) => {
        this.projects = projects;
        this.loading = false;
        if (projects.length > 0) {
          this.selectProject(projects[0].id);
        }
      },
      error: (error: HttpErrorResponse) => {
        this.loading = false;
        this.showError(error);
      }
    }));
  }

  public ngOnDestroy(): void {
    unsubscribe(this.subs);
  }

  protected selectProject(projectId: string): void {
    this.selection.next(projectId);
  }

  protected environmentName(environmentId: string): string {
    return this.matrix?.environments.find((environment) => environment.id === environmentId)?.name ?? '';
  }

  /** Only a cell that exists and is switched on can take a new version. */
  protected canChange(cell: MatrixCellDto): boolean {
    return isDefined(cell.componentEnvironmentId) && cell.state !== 'INACTIVE';
  }

  protected openVersionChange(componentId: string, cell: MatrixCellDto): void {
    if (!this.canChange(cell) || !isDefined(cell.componentEnvironmentId)) {
      return;
    }
    const cellId = cell.componentEnvironmentId;
    this.subs.push(forkJoin({
      component: this.componentRepo.getById(componentId),
      cell: this.componentEnvironmentRepo.getById(cellId)
    }).subscribe({
      next: (loaded) => {
        const reference = this.modal.open(VersionChangeDialogComponent, {size: 'lg'});
        reference.componentInstance.component = loaded.component;
        reference.componentInstance.cell = loaded.cell;
        reference.componentInstance.environmentName = this.environmentName(cell.environmentId);
        reference.result.then(() => this.reload(), () => undefined);
      },
      // Without this the click does nothing at all and nothing says why.
      error: (error: HttpErrorResponse) => this.showError(error)
    }));
  }

  private reload(): void {
    if (isDefined(this.selectedProjectId)) {
      this.selectProject(this.selectedProjectId);
    }
  }

  private showError(error: HttpErrorResponse): void {
    // The server sends a translated sentence; the status code alone would leave
    // the reader looking at an empty page with no idea what happened.
    this.errorMessage = error.error?.message ?? error.message;
  }
}
