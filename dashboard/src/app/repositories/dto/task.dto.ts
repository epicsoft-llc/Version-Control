/**
 * The sections of the task list, in the order they are read. Two of them cannot
 * be filled yet: the dependency check and the end-of-life watch are later steps.
 */
export type TaskSection =
  'EOL_EXCEEDED'
  | 'DEPENDENCY_VIOLATED'
  | 'UPDATE_OPEN'
  | 'SNOOZE_EXPIRED'
  | 'BLOCKED'
  | 'CHECK_ERROR'
  | 'MOVING_TAG_IN_PRODUCTION'
  | 'EOL_WARNING';

/** One thing to do, with what it takes to judge it without opening anything. */
export class TaskEntryDto {

  public componentId!: string;
  public componentName!: string;
  public environmentName?: string;
  public currentVersion?: string;
  public latestVersion?: string;
  /** Free text the section decides: an error message, a blocker, a date. */
  public detail?: string;
  public since?: string;

  constructor(init?: Partial<TaskEntryDto>) {
    Object.assign(this, init);
  }
}

export class TaskSectionDto {

  public section!: TaskSection;
  public entries: TaskEntryDto[] = [];

  constructor(init?: Partial<TaskSectionDto>) {
    Object.assign(this, init);
  }
}

/** What has to be done in one project. Sections without entries are not sent. */
export class TaskListDto {

  public projectId!: string;
  public sections: TaskSectionDto[] = [];

  constructor(init?: Partial<TaskListDto>) {
    Object.assign(this, init);
  }
}
