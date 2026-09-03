// Checks that every translation key the interface uses exists in the committed
// bundles. Run with: npm run i18n:verify
//
// Offline and dependency free, so it can run in every build. What it catches is
// the one failure ngx-translate hides: a missing key is rendered as the key
// itself, so the screen shows "masterData.dependencies.incomplete" where a
// sentence belongs, and nothing anywhere says that something is wrong.
//
// Dynamic keys are resolved rather than skipped. A template writes
// `'constraintState.' + check.state`, and the values that prefix can take are
// the members of the TypeScript union of the same name, so the union is read
// out of the DTO files and every member checked.

import {readdir, readFile} from 'node:fs/promises';
import {dirname, join, resolve} from 'node:path';
import {fileURLToPath} from 'node:url';

const projectDir = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const sourceDir = join(projectDir, 'src', 'app');
const bundleDir = join(projectDir, 'src', 'assets', 'i18n');

/** The language the interface falls back to; a key missing here is visible. */
const FALLBACK = 'en';

/** `{{ 'key' | translate }}`, `[title]="'key' | translate"`. */
const PIPED = /'([a-zA-Z][a-zA-Z0-9]*(?:\.[a-zA-Z0-9_]+)+)'\s*\|\s*translate/g;

/**
 * `this.translate.instant('key')` and its siblings. The receiver has to carry
 * the word in its name: a bare `…('key')` would take in every dotted string
 * that happens to close a call — `form.get('component.name')`,
 * `transform(d, 'YYYY.MM.DD')` — and report keys nobody ever used. The name
 * itself may be the whole word, so nothing is required in front of it.
 */
const CALLED = /[\w$]*[Tt]ranslate[\w$]*\s*\.\s*(?:instant|get|stream)\(\s*'([^']+)'/g;

/**
 * `'constraintState.' + check.state` — a prefix joined with a value at runtime.
 * More than one segment is allowed on purpose: `'check.state.' + check.state`
 * is the obvious next refactor, and a pattern that only accepts one segment
 * would collect nothing from it and report neither the keys nor a problem.
 */
const DYNAMIC = /'([a-zA-Z][a-zA-Z0-9]*(?:\.[a-zA-Z0-9_]+)*\.)'\s*\+/g;

async function sourceFiles(directory) {
  const entries = await readdir(directory, {withFileTypes: true});
  const files = await Promise.all(entries.map((entry) => {
    const path = join(directory, entry.name);
    if (entry.isDirectory()) {
      return sourceFiles(path);
    }
    return /\.(html|ts)$/.test(entry.name) ? [path] : [];
  }));
  return files.flat();
}

/** The members of `export type Name = 'A' | 'B';`, wherever it is declared. */
async function unionMembers(typeName) {
  const files = await sourceFiles(join(projectDir, 'src', 'app', 'repositories'));
  for (const file of files) {
    const text = await readFile(file, 'utf8');
    const declaration = new RegExp(`export type ${typeName}\\s*=([^;]+);`).exec(text);
    if (declaration) {
      return [...declaration[1].matchAll(/'([^']+)'/g)].map((match) => match[1]);
    }
  }
  return null;
}

const used = new Set();
const dynamicPrefixes = new Set();
for (const file of await sourceFiles(sourceDir)) {
  const text = await readFile(file, 'utf8');
  for (const pattern of [PIPED, CALLED]) {
    for (const match of text.matchAll(pattern)) {
      used.add(match[1]);
    }
  }
  for (const match of text.matchAll(DYNAMIC)) {
    dynamicPrefixes.add(match[1]);
  }
}

const unresolved = [];
for (const prefix of dynamicPrefixes) {
  // constraintState. -> ConstraintState, check.state. -> CheckState: every
  // segment capitalised and joined. A prefix with no such type behind it is
  // reported rather than passed over.
  const typeName = prefix.slice(0, -1).split('.')
                         .map((segment) => segment[0].toUpperCase() + segment.slice(1))
                         .join('');
  const members = await unionMembers(typeName);
  if (members === null) {
    unresolved.push(`${prefix} (no union type ${typeName} found)`);
    continue;
  }
  members.forEach((member) => used.add(prefix + member));
}

const bundle = JSON.parse(await readFile(join(bundleDir, `${FALLBACK}.json`), 'utf8'));
const missing = [...used].filter((key) => typeof bundle[key] !== 'string' || bundle[key] === '').sort();

if (unresolved.length > 0) {
  console.error(`i18n: ${unresolved.length} dynamic key prefix(es) could not be resolved:`);
  unresolved.forEach((prefix) => console.error(`  ${prefix}`));
}
if (missing.length > 0) {
  console.error(`i18n: ${missing.length} key(s) used by the interface are missing from ${FALLBACK}.json:`);
  missing.forEach((key) => console.error(`  ${key}`));
}
if (unresolved.length > 0 || missing.length > 0) {
  console.error('Run "npm run i18n:pull" after the keys have been created and translated in Tolgee.');
  process.exit(1);
}

console.log(`i18n: ${used.size} keys used, all present in ${FALLBACK}.json.`);
