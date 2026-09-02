/** A deployment environment of a project — test, staging, production. */
export class EnvironmentDto {

  public id!: string;
  public projectId!: string;
  public name!: string;
  public sortOrder!: number;
  public production!: boolean;

  constructor(init?: Partial<EnvironmentDto>) {
    Object.assign(this, init);
  }
}

export class EnvironmentRequest {

  public projectId!: string;
  public name!: string;
  public sortOrder?: number;
  public production?: boolean;

  constructor(init?: Partial<EnvironmentRequest>) {
    Object.assign(this, init);
  }
}
