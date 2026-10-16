import {TestBed} from '@angular/core/testing';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';
import {ProjectDto} from '../../repositories/dto/project.dto';
import {ProjectSelectionService} from './project-selection.service';

describe('ProjectSelectionService', () => {

  const projects = [new ProjectDto({id: 'acc', name: 'Acc'}), new ProjectDto({id: 'infra', name: 'Infra'})];

  beforeEach(() => {
    localStorage.clear();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('starts on the first project', () => {
    expect(TestBed.inject(ProjectSelectionService).pick(projects)).toBe('acc');
  });

  it('keeps the chosen project across a reload', () => {
    TestBed.inject(ProjectSelectionService).select('infra');

    // A fresh injector is what a reload leaves behind: only the storage survives.
    TestBed.resetTestingModule();
    expect(TestBed.inject(ProjectSelectionService).pick(projects)).toBe('infra');
  });

  it('falls back to the first project once the chosen one is gone', () => {
    TestBed.inject(ProjectSelectionService).select('deleted');

    expect(TestBed.inject(ProjectSelectionService).pick(projects)).toBe('acc');
  });

  it('keeps the choice for the session when the storage refuses it', () => {
    vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => {
      throw new DOMException('blocked', 'SecurityError');
    });
    vi.spyOn(Storage.prototype, 'getItem').mockImplementation(() => {
      throw new DOMException('blocked', 'SecurityError');
    });
    const service = TestBed.inject(ProjectSelectionService);

    service.select('infra');

    expect(service.pick(projects)).toBe('infra');
  });

  it('has nothing to pick without projects', () => {
    expect(TestBed.inject(ProjectSelectionService).pick([])).toBeUndefined();
  });
});
