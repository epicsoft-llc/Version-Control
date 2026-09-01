// Pulls the dashboard translations from Tolgee into src/assets/i18n/<lang>.json.
//
// Run with: npm run i18n:pull
//
// Instance, project and token come from the environment, so nothing about the
// instance is stored in this repository, and CI can redirect the output into the
// build directory instead of the source tree:
//
//   VC_TOLGEE_API_KEY     required — without it the committed files are kept
//   VC_TOLGEE_URL         base URL of the instance
//   VC_TOLGEE_PROJECT_ID  numeric project id
//   VC_TOLGEE_LANGS       comma separated, default "de,en"
//   VC_TOLGEE_OUT_DIR     output directory, relative to the project
//   VC_TOLGEE_TAG         override for the tag below; normally not set
//
// Which keys belong to this artifact is a property of the artifact, not of the
// environment — so the tag lives here, exactly as the backend keeps its own in
// core/build.gradle. Instance, project and token are deployment facts and come
// from the environment.
//
// The behaviour mirrors the backend's Gradle plugin on purpose: all languages
// are fetched before the first file is written, an empty or nested response is
// an error, and without a token nothing is touched at all.

import {mkdir, writeFile} from 'node:fs/promises';
import {dirname, resolve} from 'node:path';
import {fileURLToPath} from 'node:url';

const projectDir = resolve(dirname(fileURLToPath(import.meta.url)), '..');

/** Tag of the keys this artifact consumes. */
const TAG = 'dashboard';

const token = process.env.VC_TOLGEE_API_KEY;
const url = (process.env.VC_TOLGEE_URL ?? '').replace(/\/+$/, '');
const projectId = process.env.VC_TOLGEE_PROJECT_ID;
const tag = process.env.VC_TOLGEE_TAG ?? TAG;
const langs = (process.env.VC_TOLGEE_LANGS ?? 'de,en').split(',').map((l) => l.trim()).filter(Boolean);
const outDir = resolve(projectDir, process.env.VC_TOLGEE_OUT_DIR ?? 'src/assets/i18n');

if (!token) {
  console.log('Tolgee: VC_TOLGEE_API_KEY is not set — keeping the committed translation files.');
  process.exit(0);
}
for (const [name, value] of Object.entries({VC_TOLGEE_URL: url, VC_TOLGEE_PROJECT_ID: projectId})) {
  if (!value) {
    console.error(`Tolgee: ${name} is not set.`);
    process.exit(1);
  }
}

/**
 * `structureDelimiter` is sent empty on purpose: it keeps the JSON flat. With a
 * delimiter Tolgee nests on every "." in a key and ngx-translate resolves
 * nothing — the build stays green and only the interface is empty.
 */
async function fetchLanguage(lang) {
  const exportUrl = `${url}/v2/projects/${projectId}/export`
    + `?languages=${encodeURIComponent(lang)}&format=JSON&zip=false&structureDelimiter=&filterTag=${encodeURIComponent(tag)}`;
  const response = await fetch(exportUrl, {headers: {Accept: 'application/json', 'X-API-Key': token}});
  if (!response.ok) {
    throw new Error(`export failed for '${lang}': HTTP ${response.status} ${response.statusText}`);
  }
  const json = await response.json();
  const keys = Object.keys(json);
  if (keys.length === 0) {
    throw new Error(`export for '${lang}' returned no keys — writing it would empty the interface`);
  }
  if (keys.some((key) => typeof json[key] === 'object' && json[key] !== null)) {
    throw new Error(`export for '${lang}' is nested — the request must send an empty 'structureDelimiter'`);
  }
  // Sorted: the server's order can change between calls, and an unsorted file
  // turns one changed string into a diff over the whole file.
  return Object.fromEntries(keys.sort().map((key) => [key, json[key]]));
}

const translations = new Map();
for (const lang of langs) {
  translations.set(lang, await fetchLanguage(lang));
}

await mkdir(outDir, {recursive: true});
for (const [lang, content] of translations) {
  await writeFile(resolve(outDir, `${lang}.json`), `${JSON.stringify(content, null, 2)}\n`, 'utf8');
  console.log(`Tolgee: ${lang} (${Object.keys(content).length} keys)`);
}
