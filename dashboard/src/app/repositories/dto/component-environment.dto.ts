/** The deployed version of one package in one environment. */
export class ComponentEnvironmentPackageDto {

  public id!: string;
  public packageName!: string;
  public currentVersion?: string;

  constructor(init?: Partial<ComponentEnvironmentPackageDto>) {
    Object.assign(this, init);
  }
}

/** The version of one component in one environment. */
export class ComponentEnvironmentDto {

  public id!: string;
  public componentId!: string;
  public environmentId!: string;
  /** False means "deliberately not present here". */
  public active!: boolean;
  public currentVersion?: string;
  public currentDigest?: string;
  public lastChangedAt?: string;
  /** Active, but no version recorded — a gap, which an inactive cell is not. */
  public versionUnknown!: boolean;
  /** latest, stable, 17-alpine: allowed, but worth a warning. */
  public movingTag!: boolean;
  public packages: ComponentEnvironmentPackageDto[] = [];

  constructor(init?: Partial<ComponentEnvironmentDto>) {
    Object.assign(this, init);
  }
}

/**
 * Sets up a component in an environment or switches it off. The deployed version
 * is not changed here — that is the version dialog, which writes the history
 * entry in the same step.
 */
export class ComponentEnvironmentRequest {

  public componentId!: string;
  public environmentId!: string;
  public active?: boolean;
  public currentVersion?: string;

  constructor(init?: Partial<ComponentEnvironmentRequest>) {
    Object.assign(this, init);
  }
}
