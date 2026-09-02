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
