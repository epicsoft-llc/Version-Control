# Changelog

All notable changes to this project are documented here, grouped as in
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/). There is no release tag yet, so every entry is headed
by its date.

## 2026-10-16

### Added
- Mail as a channel for the daily notification, through any SMTP server — `VC_MAIL_HOST`, `_PORT`, `_SECURITY`
  (`starttls`, required rather than attempted, `tls` or `none`), `_USERNAME`, `_PASSWORD`, `_FROM` and `_TO` (comma
  separated). Off until host, sender and recipient are set
- `NotificationChannel` as the interface of all channels; a new channel is one implementation and a constant in
  `ChannelType`
- `deploy/compose.yml` and `.env.example` pass the `VC_MAIL_*` settings to the backend
- Dependency `spring-boot-starter-mail` for the SMTP channel
- This changelog; `CONTRIBUTING.md` asks for a line per change a user notices

### Changed
- The daily notification goes to every configured channel and counts as reported only once all of them delivered
  it: a failing channel repeats the news on the others the next day instead of losing it for its own readers
- Gradle wrapper 9.8.1

## 2026-09-03

### Added
- Inventory of the versions each component runs in each environment, with a version matrix that marks environments
  which disagree and tells a deliberately empty cell from one nobody has filled in
- Task list as the start page: out of support, update available, blocked, failing checks — in a fixed order
- Update checks against Docker Hub, GitHub releases and APT repositories, spread across the day; manual components
  without a source are a regular case, never an error
- Version rules per component, the largest allowed step (`maxStep`), upgrade stops and an upgrade path through
  intermediate versions
- End-of-life tracking from endoflife.date with warning and escalation thresholds
- Dependencies between components, evaluated per environment, with blocked updates naming the constraint
- History of every version change with note, backup reference, rollback path and downtime, and decisions that
  postpone an update until a follow-up date
- Daily Telegram summary of what is new; nothing is sent on a day without news
- Interface in English and German
- Images `vc-core` and `vc-dashboard` in the GitLab Container Registry, PostgreSQL schema through Flyway, and
  `deploy/compose.yml` to run all three
