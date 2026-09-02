import {MaxStep, SourceType, VersionStrategy} from './enums.dto';

/** One package of a component that consists of several. */
export class ComponentPackageDto {

  public id!: string;
  public name!: string;
  public sortOrder!: number;
  public leadingPackage!: boolean;

  constructor(init?: Partial<ComponentPackageDto>) {
    Object.assign(this, init);
  }
}

/** A piece of deployed infrastructure — a database, a proxy, a monitoring stack. */
export class ComponentDto {

  public id!: string;
  public projectId!: string;
  public name!: string;
  public description?: string;
  public sourceType!: SourceType;
  public sourceRef?: string;
  public aptSuite?: string;
  public aptComponent?: string;
  public aptArchitecture?: string;
  public tagPattern?: string;
  public versionStrategy!: VersionStrategy;
  public majorConstraint?: string;
  public allowPrerelease!: boolean;
  public maxStep!: MaxStep;
  public upgradePathNotes?: string;
  public upgradeDocUrl?: string;
  public needsMaintenanceWindow!: boolean;
  public eolProduct?: string;
  public eolCycle?: string;
  public checkEnabled!: boolean;
  public packages: ComponentPackageDto[] = [];

  constructor(init?: Partial<ComponentDto>) {
    Object.assign(this, init);
  }
}

export class ComponentPackageRequest {

  public name!: string;
  public sortOrder?: number;
  public leadingPackage?: boolean;

  constructor(init?: Partial<ComponentPackageRequest>) {
    Object.assign(this, init);
  }
}

export class ComponentRequest {

  public projectId!: string;
  public name!: string;
  public description?: string;
  public sourceType!: SourceType;
  public sourceRef?: string;
  public aptSuite?: string;
  public aptComponent?: string;
  public aptArchitecture?: string;
  public tagPattern?: string;
  public versionStrategy?: VersionStrategy;
  public majorConstraint?: string;
  public allowPrerelease?: boolean;
  public maxStep?: MaxStep;
  public upgradePathNotes?: string;
  public upgradeDocUrl?: string;
  public needsMaintenanceWindow?: boolean;
  public eolProduct?: string;
  public eolCycle?: string;
  public checkEnabled?: boolean;
  /** Omitted leaves the stored packages alone; an empty list clears them. */
  public packages?: ComponentPackageRequest[];

  constructor(init?: Partial<ComponentRequest>) {
    Object.assign(this, init);
  }
}
