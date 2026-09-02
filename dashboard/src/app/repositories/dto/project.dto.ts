/** A grouping of components and environments — not a customer project. */
export class ProjectDto {

  public id!: string;
  public name!: string;
  public description?: string;
  public sortOrder!: number;

  constructor(init?: Partial<ProjectDto>) {
    Object.assign(this, init);
  }
}

export class ProjectRequest {

  public name!: string;
  public description?: string;
  public sortOrder?: number;

  constructor(init?: Partial<ProjectRequest>) {
    Object.assign(this, init);
  }
}
