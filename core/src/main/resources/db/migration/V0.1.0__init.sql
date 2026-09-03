-- Initial schema.
--
-- Conventions: uuid primary keys, TIMESTAMP(6) in UTC, soft delete only where the
-- change history must keep pointing at something. Uniqueness on those tables is a
-- partial index, because a plain constraint would let a deleted row block a
-- re-insert of the same name.

CREATE TABLE project
(
    id          UUID         NOT NULL,
    name        VARCHAR(100) NOT NULL,
    description TEXT,
    sort_order  INTEGER      NOT NULL,
    created     TIMESTAMP(6) NOT NULL,
    updated     TIMESTAMP(6),
    deleted     TIMESTAMP(6),
    CONSTRAINT PK_project PRIMARY KEY (id)
);
CREATE UNIQUE INDEX UQ_project_name ON project (name) WHERE deleted IS NULL;

CREATE TABLE environment
(
    id         UUID         NOT NULL,
    project_id UUID         NOT NULL,
    name       VARCHAR(100) NOT NULL,
    sort_order INTEGER      NOT NULL,
    production BOOLEAN      NOT NULL,
    created    TIMESTAMP(6) NOT NULL,
    updated    TIMESTAMP(6),
    deleted    TIMESTAMP(6),
    CONSTRAINT PK_environment PRIMARY KEY (id),
    CONSTRAINT FK_environment_project FOREIGN KEY (project_id) REFERENCES project (id)
);
CREATE UNIQUE INDEX UQ_environment_project_name ON environment (project_id, name) WHERE deleted IS NULL;

CREATE TABLE component
(
    id                       UUID         NOT NULL,
    project_id               UUID         NOT NULL,
    name                     VARCHAR(100) NOT NULL,
    description              TEXT,

    -- Where to look for newer versions
    source_type              VARCHAR(32)  NOT NULL,
    source_ref               VARCHAR(500),
    apt_suite                VARCHAR(50),
    apt_component            VARCHAR(50),
    apt_architecture         VARCHAR(50),

    -- Which tags count as a candidate
    tag_pattern              VARCHAR(200),
    version_strategy         VARCHAR(32)  NOT NULL,
    major_constraint         VARCHAR(50),
    allow_prerelease         BOOLEAN      NOT NULL,

    -- How far a single step may go
    max_step                 VARCHAR(16)  NOT NULL,
    upgrade_path_notes       TEXT,
    upgrade_doc_url          VARCHAR(500),
    needs_maintenance_window BOOLEAN      NOT NULL,

    -- End of life
    eol_product              VARCHAR(100),
    eol_cycle                VARCHAR(50),

    -- Check scheduling
    check_enabled            BOOLEAN      NOT NULL,
    next_check_due           TIMESTAMP(6),
    check_failure_count      INTEGER      NOT NULL,

    created                  TIMESTAMP(6) NOT NULL,
    updated                  TIMESTAMP(6),
    deleted                  TIMESTAMP(6),
    CONSTRAINT PK_component PRIMARY KEY (id),
    CONSTRAINT FK_component_project FOREIGN KEY (project_id) REFERENCES project (id)
);
CREATE UNIQUE INDEX UQ_component_project_name ON component (project_id, name) WHERE deleted IS NULL;
CREATE INDEX IX_component_next_check_due ON component (next_check_due) WHERE deleted IS NULL;

-- One component, several packages: Docker on Ubuntu is docker-ce, docker-ce-cli,
-- docker-ce-rootless-extras and containerd.io, updated in a single action. Only
-- APT components use this; for everything else the table stays empty.
CREATE TABLE component_package
(
    id              UUID         NOT NULL,
    component_id    UUID         NOT NULL,
    name            VARCHAR(200) NOT NULL,
    sort_order      INTEGER      NOT NULL,
    -- "leading" is a reserved word in SQL
    leading_package BOOLEAN      NOT NULL,
    created         TIMESTAMP(6) NOT NULL,
    updated         TIMESTAMP(6),
    CONSTRAINT PK_component_package PRIMARY KEY (id),
    CONSTRAINT FK_component_package_component FOREIGN KEY (component_id) REFERENCES component (id) ON DELETE CASCADE
);
CREATE UNIQUE INDEX UQ_component_package_component_name ON component_package (component_id, name);

CREATE TABLE component_environment
(
    id              UUID         NOT NULL,
    component_id    UUID         NOT NULL,
    environment_id  UUID         NOT NULL,
    -- false means "deliberately not present here", which is not the same as an
    -- unknown version: current_version NULL with active TRUE is the unknown case.
    active          BOOLEAN      NOT NULL,
    current_version VARCHAR(200),
    current_digest  VARCHAR(100),
    last_changed_at TIMESTAMP(6),
    created         TIMESTAMP(6) NOT NULL,
    updated         TIMESTAMP(6),
    deleted         TIMESTAMP(6),
    CONSTRAINT PK_component_environment PRIMARY KEY (id),
    CONSTRAINT FK_component_environment_component FOREIGN KEY (component_id) REFERENCES component (id),
    CONSTRAINT FK_component_environment_environment FOREIGN KEY (environment_id) REFERENCES environment (id)
);
CREATE UNIQUE INDEX UQ_component_environment_component_environment
    ON component_environment (component_id, environment_id) WHERE deleted IS NULL;

CREATE TABLE component_environment_package
(
    id                       UUID         NOT NULL,
    component_environment_id UUID         NOT NULL,
    package_name             VARCHAR(200) NOT NULL,
    current_version          VARCHAR(200),
    created                  TIMESTAMP(6) NOT NULL,
    updated                  TIMESTAMP(6),
    CONSTRAINT PK_component_environment_package PRIMARY KEY (id),
    CONSTRAINT FK_component_environment_package_component_environment
        FOREIGN KEY (component_environment_id) REFERENCES component_environment (id) ON DELETE CASCADE
);
CREATE UNIQUE INDEX UQ_component_environment_package_ce_name
    ON component_environment_package (component_environment_id, package_name);

-- The history entry. Written only together with a version change, in the same
-- transaction, and never deleted with the record it belongs to — which is why
-- component_environment is soft deleted rather than removed.
CREATE TABLE version_change
(
    id                       UUID         NOT NULL,
    component_environment_id UUID         NOT NULL,
    -- Not a single tag but the summary of an action: five packages with Debian
    -- version strings run past any length one would pick, and a change that
    -- cannot be saved because of a column width is the worst way to find out.
    from_version             TEXT,
    to_version               TEXT         NOT NULL,
    changed_at               TIMESTAMP(6) NOT NULL,
    note                     TEXT,
    -- Deliberately a checkbox that starts at "no": an uncomfortable checkbox
    -- gets more attention than a free text field that stays empty.
    backup_taken             BOOLEAN      NOT NULL,
    backup_ref               VARCHAR(500),
    rollback_path            TEXT,
    downtime_minutes         INTEGER,
    issues                   TEXT,
    created                  TIMESTAMP(6) NOT NULL,
    updated                  TIMESTAMP(6),
    CONSTRAINT PK_version_change PRIMARY KEY (id),
    CONSTRAINT FK_version_change_component_environment
        FOREIGN KEY (component_environment_id) REFERENCES component_environment (id)
);
CREATE INDEX IX_version_change_component_environment
    ON version_change (component_environment_id, changed_at DESC);

-- The result of the last check of a component. One row per component, overwritten
-- every time; a history of checks would be noise, the history that matters hangs
-- on the version change.
CREATE TABLE update_check
(
    id                       UUID         NOT NULL,
    component_id             UUID         NOT NULL,
    latest_version           VARCHAR(200),
    latest_released_at       TIMESTAMP(6),
    latest_digest            VARCHAR(100),
    checked_at               TIMESTAMP(6) NOT NULL,
    -- OK, ERROR or NOT_APPLICABLE. A failed check is ERROR and never silently
    -- "up to date" - that is the one failure mode that stays unnoticed for months.
    check_state              VARCHAR(32)  NOT NULL,
    error_message            TEXT,
    last_successful_check_at TIMESTAMP(6),
    -- The candidates of the last successful check, so the rule preview can run
    -- without asking the source again inside the minimum interval.
    candidate_tags           TEXT,
    created                  TIMESTAMP(6) NOT NULL,
    updated                  TIMESTAMP(6),
    CONSTRAINT PK_update_check PRIMARY KEY (id),
    CONSTRAINT FK_update_check_component FOREIGN KEY (component_id) REFERENCES component (id) ON DELETE CASCADE
);
CREATE UNIQUE INDEX UQ_update_check_component ON update_check (component_id);

-- One row per package of an APT component; latest_version on update_check itself
-- carries the leading package.
CREATE TABLE update_check_package
(
    id              UUID         NOT NULL,
    update_check_id UUID         NOT NULL,
    package_name    VARCHAR(200) NOT NULL,
    latest_version  VARCHAR(200),
    created         TIMESTAMP(6) NOT NULL,
    updated         TIMESTAMP(6),
    CONSTRAINT PK_update_check_package PRIMARY KEY (id),
    CONSTRAINT FK_update_check_package_update_check
        FOREIGN KEY (update_check_id) REFERENCES update_check (id) ON DELETE CASCADE
);
CREATE UNIQUE INDEX UQ_update_check_package_check_name ON update_check_package (update_check_id, package_name);

-- What somebody decided about an available update. One row per component,
-- overwritten; the reasoning that has to survive belongs to the version change.
CREATE TABLE update_decision
(
    id                  UUID         NOT NULL,
    component_id        UUID         NOT NULL,
    state               VARCHAR(32)  NOT NULL,
    -- The version the decision was made about. A newer one resets the decision
    -- to OPEN: a snooze on 17.1 must not silently swallow 17.5 as well.
    decided_for_version VARCHAR(200),
    snooze_until        TIMESTAMP(6),
    note                TEXT,
    release_url         VARCHAR(500),
    decided_at          TIMESTAMP(6) NOT NULL,
    created             TIMESTAMP(6) NOT NULL,
    updated             TIMESTAMP(6),
    CONSTRAINT PK_update_decision PRIMARY KEY (id),
    CONSTRAINT FK_update_decision_component FOREIGN KEY (component_id) REFERENCES component (id) ON DELETE CASCADE
);
CREATE UNIQUE INDEX UQ_update_decision_component ON update_decision (component_id);

-- Mandatory intermediate versions on the upgrade path. Not a rule engine: facts
-- somebody looked up once, shown with every proposal after that.
CREATE TABLE upgrade_stop
(
    id           UUID         NOT NULL,
    component_id UUID         NOT NULL,
    version      VARCHAR(200) NOT NULL,
    note         TEXT,
    created      TIMESTAMP(6) NOT NULL,
    updated      TIMESTAMP(6),
    CONSTRAINT PK_upgrade_stop PRIMARY KEY (id),
    CONSTRAINT FK_upgrade_stop_component FOREIGN KEY (component_id) REFERENCES component (id) ON DELETE CASCADE
);
CREATE UNIQUE INDEX UQ_upgrade_stop_component_version ON upgrade_stop (component_id, version);

-- The result of the end-of-life lookup, deliberately separate from update_check:
-- its own cycle, its own error state, its own thresholds. An end of support has a
-- deadline and does not go away by postponing it.
CREATE TABLE eol_status
(
    id              UUID         NOT NULL,
    component_id    UUID         NOT NULL,
    eol_date        DATE,
    supported_until DATE,
    eol             BOOLEAN      NOT NULL,
    checked_at      TIMESTAMP(6) NOT NULL,
    next_check_due  TIMESTAMP(6),
    check_state     VARCHAR(32)  NOT NULL,
    error_message   TEXT,
    created         TIMESTAMP(6) NOT NULL,
    updated         TIMESTAMP(6),
    CONSTRAINT PK_eol_status PRIMARY KEY (id),
    CONSTRAINT FK_eol_status_component FOREIGN KEY (component_id) REFERENCES component (id) ON DELETE CASCADE
);
CREATE UNIQUE INDEX UQ_eol_status_component ON eol_status (component_id);
CREATE INDEX IX_eol_status_next_check_due ON eol_status (next_check_due);

-- One component needs another at a certain version. Evaluated per environment,
-- because production and staging can hold two different answers to the same
-- question.
CREATE TABLE component_dependency
(
    id             UUID         NOT NULL,
    component_id   UUID         NOT NULL,
    depends_on_id  UUID         NOT NULL,
    -- A version range in the npm style, for example ">=14 <18".
    version_range  VARCHAR(200) NOT NULL,
    note           TEXT,
    created        TIMESTAMP(6) NOT NULL,
    updated        TIMESTAMP(6),
    CONSTRAINT PK_component_dependency PRIMARY KEY (id),
    CONSTRAINT FK_component_dependency_component
        FOREIGN KEY (component_id) REFERENCES component (id) ON DELETE CASCADE,
    CONSTRAINT FK_component_dependency_depends_on
        FOREIGN KEY (depends_on_id) REFERENCES component (id) ON DELETE CASCADE
);
CREATE UNIQUE INDEX UQ_component_dependency_pair ON component_dependency (component_id, depends_on_id);
CREATE INDEX IX_component_dependency_depends_on ON component_dependency (depends_on_id);

-- What the daily notification has already reported. One row per thing that was
-- sent; a row that is gone from the current state is removed again, so a problem
-- that comes back is reported once more rather than staying silent for good.
CREATE TABLE notification_entry
(
    id           UUID         NOT NULL,
    entry_key    VARCHAR(500) NOT NULL,
    project_id   UUID         NOT NULL,
    reported_at  TIMESTAMP(6) NOT NULL,
    created      TIMESTAMP(6) NOT NULL,
    updated      TIMESTAMP(6),
    CONSTRAINT PK_notification_entry PRIMARY KEY (id),
    CONSTRAINT FK_notification_entry_project FOREIGN KEY (project_id) REFERENCES project (id) ON DELETE CASCADE
);
CREATE UNIQUE INDEX UQ_notification_entry_key ON notification_entry (entry_key);
