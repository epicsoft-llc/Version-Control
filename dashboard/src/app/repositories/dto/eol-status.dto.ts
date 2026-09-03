import {CheckState} from './update-check.dto';

/**
 * When support for the running release ends. Kept apart from the update check:
 * this has a deadline, and no decision postpones it.
 */
export class EolStatusDto {

  public id!: string;
  public componentId!: string;
  public eolDate?: string;
  public supportedUntil?: string;
  public eol!: boolean;
  public checkedAt!: string;
  public checkState!: CheckState;
  public errorMessage?: string;

  constructor(init?: Partial<EolStatusDto>) {
    Object.assign(this, init);
  }
}
