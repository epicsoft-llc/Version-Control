# Version Control

An inventory of the versions your infrastructure actually runs.

It answers four questions:

1. Which version of a component runs in which environment?
2. Where do the environments differ?
3. Where is something newer available — and may you even jump there?
4. When was what changed, and why was something *not* changed?

It is an inventory, not a deployment system. It runs no actions on target hosts, it does not manage your
application dependencies (Renovate and Dependabot do that), and it does not read the deployed versions
by itself: you enter them, in the same work step in which you upgrade the component.

> **Status: early.** Everything described here is built and tested; the interface is not yet
> translated in full, and the screenshots are still missing.

## What it watches

Infrastructure components you maintain by hand — a database, an identity provider, a reverse proxy, a
monitoring stack, the Docker packages on a host. Sources it can check:

| Source | Example |
|---|---|
| Docker Hub | `library/postgres` |
| GitHub releases | `owner/repository` |
| APT repository | the Docker packages on Ubuntu — one component, several packages |
| Manual | anything without a machine readable source; kept by hand, never an error state |

On top of that it tracks end-of-life dates, records every change with a note, backup reference,
rollback path and downtime, and remembers why an available update was deliberately postponed.

## Running it

```bash
cp .env.example .env      # fill in at least the database credentials
docker compose -f deploy/compose.yml up
```

The dashboard is then on <http://localhost:8081>. It serves the interface and forwards `/api` to the
backend, so there is a single entry point and the frontend build is environment independent.

**Access control is not part of this application.** It is meant to sit behind a reverse proxy or an
access gateway that handles authentication. Do not expose it directly.

## Developing

Requires Java 25 and Node 24 — `.vfox.toml` pins the versions.

```bash
./gradlew :core:build          # backend: compile and test
./gradlew :core:bootRun        # backend on :8080, actuator on :8090
./gradlew :core:test --tests "one.epicsoft.vc.util.VcUtilTest"

cd dashboard
npm ci
npm start                      # dev server on :4200, API on localhost:8080
npm run build
```

Unit tests run anywhere. Tests tagged `integration` start a PostgreSQL container through
Testcontainers and are skipped automatically when no Docker daemon answers, so `./gradlew test` stays
green on a machine without one.

### Images

Two images are built from this repository and published to the GitLab Container Registry of the
project, tagged by branch (`vc-core-main`, `vc-dashboard-develop`, …):

| Image | Base | Contains |
|---|---|---|
| `vc-core` | `eclipse-temurin:25-jre-alpine` | the backend as a single jar, non-root, health check on 8090 |
| `vc-dashboard` | `nginx:alpine` | the built interface, SPA fallback, `/api` forwarded to the backend |

PostgreSQL comes from the official image; `deploy/compose.yml` starts all three. Swap the `image` line
for the commented `build` block in that file to run local changes.

### Layout

```
core/          Java 25 / Spring Boot 4 backend, REST under /api/v1
dashboard/     Angular frontend, served by nginx in its own image
deploy/        compose file for the local stack
docs/          documentation
```

### Conventions

[`CODE_CONVENTION.md`](CODE_CONVENTION.md) — read it before changing code. [`CONTRIBUTING.md`](CONTRIBUTING.md)
covers the workflow. Translations are explained in [`docs/i18n.md`](docs/i18n.md).

### Configuration

Everything is configured through environment variables; see [`.env.example`](.env.example). Values that
only come from the environment carry the literal `USE_ENV` in `application.yml`, and every consumer
treats that exactly like a missing value — an unconfigured feature switches itself off instead of
calling an upstream API with the word `USE_ENV` as its token.

**Environment variables** — everything the containers need:

| Variable | Required | Effect |
|---|---|---|
| `VC_DATABASE_HOST` / `_PORT` / `_NAME` | yes | where PostgreSQL is; the compose file sets the first two |
| `VC_DATABASE_USERNAME` / `_PASSWORD` | yes | database credentials |
| `VC_HTTP_PORT` | no (8081) | the port the dashboard is published on |
| `VC_IMAGE_TAG` | no (main) | which branch's images the compose file pulls |
| `VC_GITHUB_TOKEN` | no | raises the GitHub rate limit from 60 to 5.000 requests/hour |
| `VC_TELEGRAM_BOT_TOKEN` / `VC_TELEGRAM_CHAT_ID` | no | without them the daily summary is not sent |
| `VC_TOLGEE_URL` / `_PROJECT_ID` / `_API_KEY` | no | build time only, to re-export the translations |

**Application settings** — `application.yml`, overridable per environment:

| Setting | Default | Effect |
|---|---|---|
| `vc.i18n.default-lang` | en | the language of the interface and of the daily message |
| `vc.scheduler.enabled` | true | switches every background job off in one place |
| `vc.scheduler.tick-interval` | 15m | how often the scheduler picks up **one** due component |
| `vc.check.default-interval` | 48h | how often a component is checked |
| `vc.check.min-interval` | 48h | hard lower bound, manual checks included |
| `vc.check.jitter` | 2h | spreads the checks instead of running them in a nightly batch |
| `vc.check.backoff` | 1h, 4h, 12h | how long a failing source is left alone, per consecutive failure |
| `vc.check.error-grace-days` | 3 | how long a failing check is tolerated before it becomes a task |
| `vc.eol.check-interval` | 7d | how often end-of-life dates are refreshed |
| `vc.eol.warn-months` / `escalate-months` | 6 / 3 | end-of-life thresholds |
| `vc.notification.daily-time` | 08:00 | when the daily summary is sent |

The two ports are fixed inside the images: 8080 serves the API, 8090 serves the actuator and stays
inside the compose network, which is what the container health check uses.

### Using a package mirror

The build resolves from Maven Central, the Gradle Plugin Portal and npm. To route it through a mirror,
set `VC_MAVEN_MIRROR_URL`, `VC_GRADLE_PLUGIN_MIRROR_URL` (optionally `VC_MAVEN_MIRROR_USERNAME` and
`VC_MAVEN_MIRROR_PASSWORD`) for Gradle, and `VC_NPM_REGISTRY`, `VC_NPM_USERNAME`, `VC_NPM_PASSWORD`
for npm in CI. Locally, npm reads an `.npmrc` — in your home directory, or in `dashboard/`, where it is
ignored by git. A mirror that requires authentication needs the credentials in it: an address alone
carries none, and the mirror answers `401 Unauthorized`.

```
registry=https://mirror.example.com/repository/npm-group/
//mirror.example.com/repository/npm-group/:username=<user>
//mirror.example.com/repository/npm-group/:_password=<base64 of the password>
```

Nothing about a mirror is stored in this repository.

## License

[MIT](LICENSE) — Copyright (c) 2026 epicsoft LLC
