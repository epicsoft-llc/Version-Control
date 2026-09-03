import {ChangeDetectionStrategy, Component, inject, OnDestroy, OnInit} from '@angular/core';
import {DatePipe} from '@angular/common';
import {HttpErrorResponse} from '@angular/common/http';
import {FormsModule} from '@angular/forms';
import {RouterLink} from '@angular/router';
import {NgbModal} from '@ng-bootstrap/ng-bootstrap';
import {TranslatePipe} from '@ngx-translate/core';
import {catchError, of, Subject, Subscription, switchMap, tap} from 'rxjs';
import {ProjectDto} from '../../repositories/dto/project.dto';
import {TaskEntryDto, TaskListDto, TaskSection} from '../../repositories/dto/task.dto';
import {ProjectRepo} from '../../repositories/api/project.repo';
import {TaskRepo} from '../../repositories/api/task.repo';
import {DecisionDialogComponent} from '../../components/decision-dialog/decision-dialog.component';
import {isDefined, unsubscribe} from '../../libs/utils/vc.utils';

/**
 * The start page, and deliberately not the matrix.
 *
 * <p>A table leaves it to the reader to work out what needs doing. This answers
 * the question directly, in a fixed order: what is out of support before what is
 * merely behind, and a check that has been failing for days before a cosmetic
 * warning. Sections without entries are not shown at all.
 */
@Component({
  // Default, explicitly: this Angular version compiles a component as OnPush
  // unless told otherwise, and these components hold their state in plain
  // fields written from subscribe callbacks. Under OnPush such a view is
  // never marked dirty, so every page renders its heading and then stays
  // empty for good - with no error anywhere. Until the state is held in
  // signals, the strategy has to say what the code assumes.
  changeDetection: ChangeDetectionStrategy.Default,
  selector: 'vc-tasks',
  imports: [DatePipe, FormsModule, RouterLink, TranslatePipe],
  templateUrl: './tasks.component.html'
})
export class TasksComponent implements OnInit, OnDestroy {

  private readonly projectRepo: ProjectRepo = inject(ProjectRepo);
  private readonly taskRepo: TaskRepo = inject(TaskRepo);
  private readonly modal: NgbModal = inject(NgbModal);
  private readonly subs: Subscription[] = [];

  private readonly selection = new Subject<string>();

  protected projects: ProjectDto[] = [];
  protected selectedProjectId?: string;
  protected list?: TaskListDto;
  protected loading = true;
  protected errorMessage?: string;

  public ngOnInit(): void {
    this.subs.push(this.selection.pipe(
      tap((projectId) => {
        this.selectedProjectId = projectId;
        this.list = undefined;
        this.loading = true;
        this.errorMessage = undefined;
      }),
      switchMap((projectId) => this.taskRepo.getByProject(projectId).pipe(catchError((error) => {
        this.showError(error);
        return of(undefined);
      })))
    ).subscribe((list) => {
      this.list = list;
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

  /**
   * Sections about an available update can be answered with a decision —
   * including a blocked one. Somebody may well decide to skip an update that is
   * blocked, and without the button a blocked entry could be neither done nor
   * answered, only silently lived with.
   */
  protected decidable(section: TaskSection): boolean {
    return section === 'UPDATE_OPEN' || section === 'SNOOZE_EXPIRED' || section === 'BLOCKED';
  }

  protected openDecision(entry: TaskEntryDto): void {
    const reference = this.modal.open(DecisionDialogComponent);
    reference.componentInstance.componentId = entry.componentId;
    reference.componentInstance.componentName = entry.componentName;
    reference.componentInstance.latestVersion = entry.latestVersion;
    reference.result.then(() => this.reload(), () => undefined);
  }

  private reload(): void {
    if (isDefined(this.selectedProjectId)) {
      this.selectProject(this.selectedProjectId);
    }
  }

  private showError(error: HttpErrorResponse): void {
    this.errorMessage = error.error?.message ?? error.message;
  }
}
