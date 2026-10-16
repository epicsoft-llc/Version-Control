import {provideZoneChangeDetection} from '@angular/core';
import {ComponentFixture, TestBed} from '@angular/core/testing';
import {provideTranslateService, TranslateService} from '@ngx-translate/core';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';
import {CopyValueComponent} from './copy-value.component';

describe('CopyValueComponent', () => {

  let fixture: ComponentFixture<CopyValueComponent>;
  let writeText: ReturnType<typeof vi.fn>;

  const button = (): HTMLButtonElement => fixture.nativeElement.querySelector('button') as HTMLButtonElement;

  async function click(): Promise<void> {
    button().click();
    // One macrotask lets the clipboard promise settle; whenStable would also wait
    // out the confirmation and find the button back at its plain icon.
    await new Promise((resolve) => setTimeout(resolve));
    fixture.detectChanges();
  }

  beforeEach(async () => {
    writeText = vi.fn().mockResolvedValue(undefined);
    Object.defineProperty(navigator, 'clipboard', {value: {writeText}, configurable: true});

    await TestBed.configureTestingModule({
      imports: [CopyValueComponent],
      providers: [provideZoneChangeDetection({eventCoalescing: true}), provideTranslateService({fallbackLang: 'en'})]
    }).compileComponents();
    TestBed.inject(TranslateService).setTranslation('en', {
      'copy.title': 'Copy to clipboard',
      'copy.done': 'Copied',
      'copy.failed': 'Could not copy'
    });

    fixture = TestBed.createComponent(CopyValueComponent);
    // Padded the way a template hands it over when the value sits on a line of its own.
    fixture.componentRef.setInput('value', '  17.11-alpine\n');
    fixture.detectChanges();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('copies the version without the whitespace around it', async () => {
    await click();

    expect(writeText).toHaveBeenCalledWith('17.11-alpine');
    expect(button().getAttribute('title')).toBe('Copied');
  });

  it('says so when the clipboard refuses', async () => {
    writeText.mockRejectedValue(new DOMException('denied', 'NotAllowedError'));

    await click();

    expect(button().getAttribute('title')).toBe('Could not copy');
  });

  it('does not pass the click on to the cell it sits in', async () => {
    const cell = vi.fn();
    fixture.nativeElement.addEventListener('click', cell);

    await click();

    expect(cell).not.toHaveBeenCalled();
  });
});
