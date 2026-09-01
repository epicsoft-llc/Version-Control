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
./gradlew :core:build
cd dashboard && npm ci && npm run build
```

You need no credentials for any of this. Tests that require a database are tagged `integration` and
start a container through Testcontainers; without a Docker daemon they are skipped rather than failing.

## Branches and commits

- Work on `develop`; `main` carries the released state, and every release tag sits on `main`.
- Tags are plain versions — `1.2.3`, no leading `v`.
- Commit messages follow [Conventional Commits](https://www.conventionalcommits.org/): `feat:`, `fix:`,
  `ci:`, `docs:`, `refactor:`, `test:`, `revert:`. Write them in English, in the imperative.

## The rules that are not style

- **English everywhere in the repository** — code, identifiers, comments, commit messages, migrations,
  API paths, translation keys.
- **No secrets, no internal addresses.** Not in code, not in build files, not as an example value. Every
  credential comes from an environment variable; only `*.example` files are committed.
- **No user-visible literal.** Every string a person can read goes through a translation key. See
  [`docs/i18n.md`](docs/i18n.md).
- **Warnings are silenced by people, not by tools.** Do not add `@SuppressWarnings`, `// noinspection`,
  `eslint-disable`, `@ts-ignore` or `NOSONAR` to make a build quiet. Fix the cause, or leave the warning
  standing and say so in the pull request. Test code is the exception.
- **Never modify an applied migration.** Add a new one.
- **Pin every version you declare.** Exact versions in `package.json` and `libs.versions.toml`, no
  ranges; commit the resulting `package-lock.json`.

## CI

The pipeline needs **no configuration at all** to run: every job either works with GitLab's own
predefined variables or switches itself off. Three groups of variables unlock optional behaviour, and
each group is all-or-nothing — set every variable in it or none.

| Group | Variables | Without them |
|---|---|---|
| Translations | `VC_TOLGEE_API_KEY`, `VC_TOLGEE_URL`, `VC_TOLGEE_PROJECT_ID` | the committed translation files are shipped as they are |
| Vulnerability scan | `VC_DEPCHECK_URL`, `VC_DEPCHECK_USERNAME`, `VC_DEPCHECK_PASSWORD`, `VC_DEPCHECK_RUNNER_TAG`, optionally `VC_DEPCHECK_DRIVER` | the job does not run |
| Package mirror | `VC_MAVEN_MIRROR_URL`, `VC_GRADLE_PLUGIN_MIRROR_URL`, `VC_MAVEN_MIRROR_USERNAME`, `VC_MAVEN_MIRROR_PASSWORD`, `VC_NPM_REGISTRY`, `VC_NPM_USERNAME`, `VC_NPM_PASSWORD` | Maven Central, the Gradle Plugin Portal and npm are used directly |

Mask every credential. Do **not** mark them *protected* unless the branch is protected too — a protected
variable is absent on an unprotected branch, and the translation pull would then quietly ship the
committed files instead of failing.

**Job logs are readable by anyone** while *Public pipelines* is enabled on a public project. No step may
print a configuration, a registry address or an environment dump: masking protects only the values of
masked variables, and file variables are not masked at all. Diagnostics say which path was taken, never
with which values.

The image jobs need the project's container registry enabled and a runner tagged `image`; the test job
needs a runner that can run `docker:dind`, because the integration tests start a database container.

## Pull requests

Keep them one topic wide. Describe what changes for a user of the tool, not only what changes in the
code. If you touched a rule, a threshold or a check interval, say what the old and the new behaviour
are — those numbers are the ones people will have to live with.
