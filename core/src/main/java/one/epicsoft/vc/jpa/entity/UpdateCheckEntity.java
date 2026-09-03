package one.epicsoft.vc.jpa.entity;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import one.epicsoft.vc.enums.CheckState;
import java.time.Instant;
import java.util.UUID;

/**
 * What the last check of a component found. One row per component, overwritten
 * on every check — a history of checks would be noise, and the history that
 * matters hangs on the version change instead.
 */
@Getter
@Setter
@Entity
@Table(name = "update_check")
public class UpdateCheckEntity extends AbstractEntity {

  /** The leading package for a component that consists of several. */
  @Nullable
  @Column(name = "latest_version", length = 200)
  private String latestVersion;

  @Nullable
  @Column(name = "latest_released_at")
  private Instant latestReleasedAt;

  @Nullable
  @Column(name = "latest_digest", length = 100)
  private String latestDigest;

  @Nonnull
  @Column(name = "checked_at", nullable = false)
  private Instant checkedAt;

  /**
   * A failed check is {@link CheckState#ERROR} and never quietly "up to date".
   * That is the one failure mode which stays unnoticed for months.
   */
  @Nonnull
  @Enumerated(EnumType.STRING)
  @Column(name = "check_state", nullable = false, length = 32)
  private CheckState checkState;

  @Nullable
  @Column(name = "error_message", columnDefinition = "TEXT")
  private String errorMessage;

  /** Stays put through a failure, so the age of the last good answer is visible. */
  @Nullable
  @Column(name = "last_successful_check_at")
  private Instant lastSuccessfulCheckAt;

  /**
   * The candidates of the last successful check, newline separated. Kept so the
   * rule preview can show which tags match without asking the source again
   * inside the minimum interval.
   */
  @Nullable
  @Column(name = "candidate_tags", columnDefinition = "TEXT")
  private String candidateTags;

  @Nonnull
  @ToString.Exclude
  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "component_id", nullable = false)
  private ComponentEntity component;

  // #### Relation IDs
  @Nonnull
  @Setter(AccessLevel.NONE)
  @Column(name = "component_id", insertable = false, updatable = false)
  private UUID componentId;

  // #### Custom Getter and Setter
  public void setComponent(@Nonnull final ComponentEntity component) {
    this.component = component;
    this.componentId = component.getId();
  }
}
