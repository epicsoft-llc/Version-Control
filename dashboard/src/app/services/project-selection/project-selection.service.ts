import {Injectable} from '@angular/core';
import {ProjectDto} from '../../repositories/dto/project.dto';
import {isDefined} from '../../libs/utils/vc.utils';

/**
 * The project the reader is working in, shared by every page that shows one.
 * Each page used to start on the first project again — after a reload and on the
 * way back from a component page alike.
 */
@Injectable({providedIn: 'root'})
export class ProjectSelectionService {

  private readonly storageKey: string = 'vc:project';

  private selectedId?: string;

  /** The project chosen last, while it still exists; otherwise the first one. */
  public pick(projects: ProjectDto[]): string | undefined {
    const remembered = this.selectedId ?? this.read();
    return isDefined(remembered) && projects.some((project) => project.id === remembered) ? remembered : projects[0]?.id;
  }

  public select(projectId: string): void {
    this.selectedId = projectId;
    this.write(projectId);
  }

  // The storage only carries the choice across a reload. A browser that refuses it
  // (private window, blocked site data) keeps the choice for the session instead.
  private read(): string | undefined {
    try {
      return localStorage.getItem(this.storageKey) ?? undefined;
    } catch {
      return undefined;
    }
  }

  private write(projectId: string): void {
    try {
      localStorage.setItem(this.storageKey, projectId);
    } catch {
      // ok: the session keeps the choice, see read()
    }
  }
}
