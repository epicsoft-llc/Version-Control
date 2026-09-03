/**
 * Whether a dependency holds — with a third answer that has to stay visible.
 * A version nothing can read is UNKNOWN and never quietly SATISFIED.
 */
export type ConstraintState = 'SATISFIED' | 'VIOLATED' | 'UNKNOWN';

/** One dependency, evaluated in one environment. */
export class DependencyCheckDto {

  public dependencyId!: string;
  /** The dependent component. */
  public componentId!: string;
  public componentName!: string;
  /** What it needs. */
  public dependsOnId!: string;
  public dependsOnName!: string;
  public environmentId!: string;
  public versionRange!: string;
  public actualVersion?: string;
  public state!: ConstraintState;
  /** Where the requirement was read. */
  public note?: string;

  constructor(init?: Partial<DependencyCheckDto>) {
    Object.assign(this, init);
  }
}

/** A dependency as it is configured, without any evaluation. */
export class ComponentDependencyDto {

  public id!: string;
  public dependsOnId!: string;
  public versionRange!: string;
  public note?: string;

  constructor(init?: Partial<ComponentDependencyDto>) {
    Object.assign(this, init);
  }
}

export class ComponentDependencyRequest {

  public dependsOnId!: string;
  /** A version range in the npm style, for example ">=14 <18". */
  public versionRange!: string;
  public note?: string;

  constructor(init?: Partial<ComponentDependencyRequest>) {
    Object.assign(this, init);
  }
}
