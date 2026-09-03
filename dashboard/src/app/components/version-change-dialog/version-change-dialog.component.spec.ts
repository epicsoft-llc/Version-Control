import {provideZoneChangeDetection} from '@angular/core';
import {ComponentFixture, TestBed} from '@angular/core/testing';
import {NgbActiveModal} from '@ng-bootstrap/ng-bootstrap';
import {provideTranslateService} from '@ngx-translate/core';
import {of} from 'rxjs';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {VersionChangeRepo} from '../../repositories/api/version-change.repo';
import {ComponentDto, ComponentPackageDto} from '../../repositories/dto/component.dto';
import {ComponentEnvironmentDto, ComponentEnvironmentPackageDto} from '../../repositories/dto/component-environment.dto';
import {VersionChangeDto, VersionChangeRequest} from '../../repositories/dto/version-change.dto';
import {VersionChangeDialogComponent} from './version-change-dialog.component';

/**
 * The only way a version changes, and therefore the only place history is
 * written. What the form asks for — note, backup, rollback path, downtime,
 * problems — is asked here because a second step would never be filled in, and
 * it is exactly the part that pays off the next time the same upgrade comes
 * round.
 */
describe('VersionChangeDialogComponent', () => {

  let fixture: ComponentFixture<VersionChangeDialogComponent>;
  let changeVersion: ReturnType<typeof vi.fn>;
  let close: ReturnType<typeof vi.fn>;

  const field = (id: string): HTMLInputElement =>
    fixture.nativeElement.querySelector('#' + id) as HTMLInputElement;

  const saveButton = (): HTMLButtonElement =>
    fixture.nativeElement.querySelector('.modal-footer .btn-primary') as HTMLButtonElement;

  const sent = (): VersionChangeRequest => changeVersion.mock.calls[0][0] as VersionChangeRequest;

  async function type(input: HTMLInputElement, value: string): Promise<void> {
    input.value = value;
    input.dispatchEvent(new Event('input'));
    await fixture.whenStable();
    fixture.detectChanges();
  }

  async function open(component: ComponentDto, cell: ComponentEnvironmentDto): Promise<void> {
    fixture = TestBed.createComponent(VersionChangeDialogComponent);
    fixture.componentInstance.component = component;
    fixture.componentInstance.cell = cell;
    fixture.componentInstance.environmentName = 'Production';
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
  }

  function plainComponent(): ComponentDto {
    return new ComponentDto({id: 'component-1', name: 'PostgreSQL', packages: []});
  }

  function cellWith(version?: string): ComponentEnvironmentDto {
    return new ComponentEnvironmentDto({id: 'cell-1', currentVersion: version, packages: []});
  }

  beforeEach(async () => {
    changeVersion = vi.fn().mockReturnValue(of(new VersionChangeDto({id: 'change-1'})));
    close = vi.fn();

    await TestBed.configureTestingModule({
      imports: [VersionChangeDialogComponent],
      providers: [
        provideZoneChangeDetection({eventCoalescing: true}),
        provideTranslateService({fallbackLang: 'en'}),
        {provide: NgbActiveModal, useValue: {close, dismiss: vi.fn()}},
        {provide: VersionChangeRepo, useValue: {changeVersion}}
      ]
    }).compileComponents();
  });

  it('refuses to save without a new version', async () => {
    await open(plainComponent(), cellWith('17.2'));

    // Nothing else is required, and nothing else can stand in for it: an entry
    // without a new version records no change.
    expect(saveButton().disabled).toBe(true);

    await type(field('toVersion'), '17.4');

    expect(saveButton().disabled).toBe(false);
  });

  it('starts with the backup checkbox unticked', async () => {
    await open(plainComponent(), cellWith('17.2'));

    // Preset to yes it would be ticked by habit and mean nothing. Unticked, the
    // answer costs one click and is therefore worth reading afterwards.
    expect((field('backupTaken') as HTMLInputElement).checked).toBe(false);

    await type(field('toVersion'), '17.4');
    saveButton().click();

    expect(sent().backupTaken).toBe(false);
  });

  it('carries the whole story of the change, not just the number', async () => {
    await open(plainComponent(), cellWith('17.2'));

    await type(field('toVersion'), ' 17.4 ');
    await type(field('note'), 'Minor upgrade in the Saturday window');
    field('backupTaken').click();
    await type(field('backupRef'), 'snapshot-2026-09-03');
    await type(field('rollbackPath'), 'Restore the snapshot, then start 17.2');
    await type(field('downtimeMinutes'), '25');
    await type(field('issues'), 'Two dashboards had to be re-saved');

    saveButton().click();

    const request = sent();
    expect(request.componentEnvironmentId).toBe('cell-1');
    // Trimmed: a version with a stray space would compare as a different one
    // for the rest of its life.
    expect(request.toVersion).toBe('17.4');
    expect(request.note).toBe('Minor upgrade in the Saturday window');
    expect(request.backupTaken).toBe(true);
    expect(request.backupRef).toBe('snapshot-2026-09-03');
    expect(request.rollbackPath).toBe('Restore the snapshot, then start 17.2');
    expect(request.downtimeMinutes).toBe(25);
    expect(request.issues).toBe('Two dashboards had to be re-saved');
    expect(close).toHaveBeenCalledTimes(1);
  });

  it('sends only the packages that were filled in', async () => {
    const component = new ComponentDto({
      id: 'component-1', name: 'Docker',
      packages: [new ComponentPackageDto({name: 'docker-ce', leadingPackage: true}),
                 new ComponentPackageDto({name: 'containerd.io', leadingPackage: false})]
    });
    const cell = new ComponentEnvironmentDto({
      id: 'cell-1', currentVersion: '5:29.7.2',
      packages: [new ComponentEnvironmentPackageDto({packageName: 'docker-ce', currentVersion: '5:29.7.2'}),
                 new ComponentEnvironmentPackageDto({packageName: 'containerd.io', currentVersion: '1.7.28'})]
    });

    await open(component, cell);
    await type(field('package-docker-ce'), '5:29.8.1');

    saveButton().click();

    // The package nobody touched keeps the version it has. Sending it along
    // would write a change that never happened into the history.
    expect(sent().packages).toEqual([{packageName: 'docker-ce', version: '5:29.8.1'}]);
    expect(sent().toVersion).toBeUndefined();
  });

  it('leaves out what nobody wrote', async () => {
    await open(plainComponent(), cellWith(undefined));

    await type(field('toVersion'), '17.4');
    await type(field('note'), '   ');
    saveButton().click();

    // A note of three spaces looks like an explanation in the history list and
    // says nothing.
    expect(sent().note).toBeUndefined();
    expect(sent().backupRef).toBeUndefined();
    expect(sent().downtimeMinutes).toBeUndefined();
  });
});
