# Contributing

Thanks for looking. This is a small, opinionated tool; the fastest way to get a change merged is to
match what is already there.

## Before you start

- Read [`CODE_CONVENTION.md`](CODE_CONVENTION.md). It is short and it is enforced in review.
- For anything larger than a fix, open an issue first. The tool has a deliberately narrow scope — it is
  an inventory, not a deployment system — and a change that widens it needs agreement before code.

## Building

Java 25 and Node 24; `.vfox.toml` pins both.

```bash
./gradlew :core:check                       # compile, tests, and the offline verifications
cd dashboard && npm ci && npm run build
cd dashboard && npm test                    # unit tests, vitest in jsdom
```

You need no credentials for any of this. Tests that require a database are tagged `integration`. They
use the database named by `VC_DATABASE_HOST` when that variable is set, and otherwise start one through
Testcontainers — so a Docker daemon is enough, and nothing has to be installed. With neither at hand the
tag is skipped rather than failing, except in CI, where the build stops instead: a pipeline must not go
green for a test set that never ran.

The dashboard tests need no browser and no display: they run in jsdom through the Angular `unit-test`
builder. Write them against the rendered component — a test that only calls methods misses everything
the template does, and the template is where this interface keeps its rules.

## Running it while you work

```bash
./gradlew :core:bootRun                     # backend on :8080, actuator on :8090
cd dashboard && npm start                   # dev server on :4200
```

The dev server forwards `/api` to `localhost:8080` (`proxy.conf.json`), exactly as the dashboard
container does in a deployment, so the interface always calls a relative URL and the backend needs no
CORS rule. A database is the only thing you have to supply — the compose file in `deploy/` starts one.

## Branches and commits

- Work on `develop`; `main` carries the released state, and every release tag sits on `main`.
- Tags are plain versions — `1.2.3`, no leading `v`.
- Commit messages follow [Conventional Commits](https://www.conventionalcommits.org/): `feat:`, `fix:`,
  `ci:`, `docs:`, `refactor:`, `test:`, `revert:`. Write them in English, in the imperative.
- Every change a user notices gets a line in [`CHANGELOG.md`](CHANGELOG.md), in the same commit — under
  the version of its release tag, and under the date of the change as long as there is none.

## The rules that are not style

- **English everywhere in the repository** — code, identifiers, comments, commit messages, migrations,
  API paths, translation keys.
- **No secrets, no internal addresses.** Not in code, not in build files, not as an example value. Every
  credential comes from an environment variable; only `*.example` files are committed.
- **No user-visible literal.** Every string a person can read goes through a translation key. See
  [`docs/i18n.md`](docs/i18n.md). Two checks enforce it from the other side —
  `./gradlew :core:verifyMessageKeys` and `npm run i18n:verify` fail when the code asks for a key the
  bundles do not have. Both run in the pipeline; a missing key is otherwise invisible, because what
  reaches the reader is the key itself.
- **Warnings are silenced by people, not by tools.** Do not add `@SuppressWarnings`, `// noinspection`,
  `eslint-disable`, `@ts-ignore` or `NOSONAR` to make a build quiet. Fix the cause, or leave the warning
  standing and say so in the pull request. Test code is the exception.
- **Never modify an applied migration.** Add a new one. `V0.1.0__init.sql` is closed: it shipped in a
  published image, so a database somewhere has applied it, and editing it now stops Flyway on exactly
  that database.
- **Pin every version you declare.** Exact versions in `package.json` and `libs.versions.toml`, no
  ranges; commit the resulting `package-lock.json`.

## CI

The pipeline needs **no configuration at all** to run: every job either works with GitLab's own
predefined variables or switches itself off. Three groups of variables unlock optional behaviour, and
each group is all-or-nothing — set every variable in it or none.

| Group | Variables | Without them |
|---|---|---|
| Translations | `VC_TOLGEE_API_KEY`, `VC_TOLGEE_URL`, `VC_TOLGEE_PROJECT_ID` | the committed translation files are shipped as they are |
| Vulnerability scan | `VC_DEPCHECK_URL`, `VC_DEPCHECK_USERNAME`, `VC_DEPCHECK_PASSWORD`, `VC_DEPCHECK_RUNNER_TAG`, `VC_DEPCHECK_DRIVER`, `VC_DEPCHECK_DRIVER_JAR` | the job does not run |
| Package mirror | `VC_MAVEN_MIRROR_URL`, `VC_GRADLE_PLUGIN_MIRROR_URL`, `VC_MAVEN_MIRROR_USERNAME`, `VC_MAVEN_MIRROR_PASSWORD`, `VC_NPM_REGISTRY`, `VC_NPM_USERNAME`, `VC_NPM_PASSWORD` | Maven Central, the Gradle Plugin Portal and npm are used directly |

A freshly added key is translated asynchronously and can take minutes. Pull only once every key has a
value in the fallback language — otherwise the committed files miss it and the next pipeline fails on a
check that is doing its job.

Mask every credential. Do **not** mark them *protected* unless the branch is protected too — a protected
variable is absent on an unprotected branch, and the translation pull would then quietly ship the
committed files instead of failing.

**Job logs are readable by anyone** while *Public pipelines* is enabled on a public project. No step may
print a configuration, a registry address or an environment dump: masking protects only the values of
masked variables, and file variables are not masked at all. Diagnostics say which path was taken, never
with which values.

The image jobs need the project's container registry enabled and a runner tagged `image`. The test job
attaches PostgreSQL as a service and points the tests at it through `VC_DATABASE_*`, so it needs no
Docker of its own.

## Pull requests

Keep them one topic wide. Describe what changes for a user of the tool, not only what changes in the
code. If you touched a rule, a threshold or a check interval, say what the old and the new behaviour
are — those numbers are the ones people will have to live with.
