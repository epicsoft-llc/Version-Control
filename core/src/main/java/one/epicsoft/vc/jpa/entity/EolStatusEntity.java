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
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import one.epicsoft.vc.enums.CheckState;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static java.util.Objects.isNull;
import static one.epicsoft.vc.util.VcUtil.isTrue;

/**
 * When support for the running release ends.
 *
 * <p>Deliberately not part of the update check: its own cycle, its own error
 * state, its own thresholds. An end of support is categorically different from
 * an available patch — it has a deadline, and it does not go away by postponing
 * it. That is also why no decision can silence it.
 */
@Getter
@Setter
@Entity
@Table(name = "eol_status")
public class EolStatusEntity extends AbstractEntity {

  /** The day support ends, as the upstream database states it. */
  @Nullable
  @Column(name = "eol_date")
  private LocalDate eolDate;

  /** Where a source distinguishes active support from security fixes. */
  @Nullable
  @Column(name = "supported_until")
  private LocalDate supportedUntil;

  @Nonnull
  @Getter(AccessLevel.NONE)
  @Column(name = "eol", nullable = false)
  private Boolean eol;

  @Nonnull
  @Column(name = "checked_at", nullable = false)
  private Instant checkedAt;

  /** Its own due date: the end-of-life cycle is weekly, the update check is not. */
  @Nullable
  @Column(name = "next_check_due")
  private Instant nextCheckDue;

  @Nonnull
  @Enumerated(EnumType.STRING)
  @Column(name = "check_state", nullable = false, length = 32)
  private CheckState checkState;

  @Nullable
  @Column(name = "error_message", columnDefinition = "TEXT")
  private String errorMessage;

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

  @PrePersist
  public void prePersistEolStatus() {
    if (isNull(this.eol)) {
      this.eol = Boolean.FALSE;
    }
  }

  // #### Custom Getter and Setter
  public boolean isEol() {
    return isTrue(this.eol);
  }

  public void setComponent(@Nonnull final ComponentEntity component) {
    this.component = component;
    this.componentId = component.getId();
  }
}
