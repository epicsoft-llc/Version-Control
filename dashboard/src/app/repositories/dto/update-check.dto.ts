import {VersionStrategy} from './enums.dto';

/** OK, ERROR or NOT_APPLICABLE — a failed check is never a quiet "up to date". */
export type CheckState = 'OK' | 'ERROR' | 'NOT_APPLICABLE';

/** The newest version found for one package of a component that has several. */
export class UpdateCheckPackageDto {

  public packageName!: string;
  public latestVersion?: string;

  constructor(init?: Partial<UpdateCheckPackageDto>) {
    Object.assign(this, init);
  }
}

/**
 * What the last check found. A failed check keeps the version of the last good
 * one and says how old that answer is, so the screen can show both.
 */
export class UpdateCheckDto {

  public id!: string;
  public componentId!: string;
  public latestVersion?: string;
  public latestReleasedAt?: string;
  public latestDigest?: string;
  public checkedAt!: string;
  public checkState!: CheckState;
  /** Technical, for whoever has to fix the rule or the address. */
  public errorMessage?: string;
  public lastSuccessfulCheckAt?: string;
  public packages: UpdateCheckPackageDto[] = [];

  constructor(init?: Partial<UpdateCheckDto>) {
    Object.assign(this, init);
  }
}

/** One tag a rule accepts. */
export class PreviewCandidateDto {

  public tag!: string;
  public releasedAt?: string;
  public packageName?: string;

  constructor(init?: Partial<PreviewCandidateDto>) {
    Object.assign(this, init);
  }
}

/**
 * What a rule would select. A pattern that matches nothing comes back as an
 * empty list with a message — writing a rule means half finished patterns, and
 * an error for each of them would make the screen unusable.
 */
export class VersionPreviewDto {

  public winner?: string;
  public message?: string;
  public matching: PreviewCandidateDto[] = [];

  constructor(init?: Partial<VersionPreviewDto>) {
    Object.assign(this, init);
  }
}

export class VersionPreviewRequest {

  public componentId!: string;
  /** Each field falls back to the stored one when it is not sent. */
  public tagPattern?: string;
  public majorConstraint?: string;
  public allowPrerelease?: boolean;
  public versionStrategy?: VersionStrategy;

  constructor(init?: Partial<VersionPreviewRequest>) {
    Object.assign(this, init);
  }
}
