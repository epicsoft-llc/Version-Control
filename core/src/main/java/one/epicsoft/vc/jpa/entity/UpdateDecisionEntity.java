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
import one.epicsoft.vc.enums.DecisionState;
import java.time.Instant;
import java.util.UUID;

/**
 * What somebody decided about an available update, and why.
 *
 * <p>A note on its own would not do. "Why did we not move" needs a state and a
 * date to come back on — otherwise there is a sentence sitting in the inventory
 * three months later that nobody checks again.
 *
 * <p>{@link #decidedForVersion} is what keeps a decision honest: it records the
 * version the decision was about, so a newer one can reopen it. Without it a
 * postponement of 17.1 quietly swallows 17.5 as well.
 */
@Getter
@Setter
@Entity
@Table(name = "update_decision")
public class UpdateDecisionEntity extends AbstractEntity {

  @Nonnull
  @Enumerated(EnumType.STRING)
  @Column(name = "state", nullable = false, length = 32)
  private DecisionState state;

  /** The version this was decided about; null when nothing was available yet. */
  @Nullable
  @Column(name = "decided_for_version", length = 200)
  private String decidedForVersion;

  /** When a postponed update comes back onto the task list. */
  @Nullable
  @Column(name = "snooze_until")
  private Instant snoozeUntil;

  @Nullable
  @Column(name = "note", columnDefinition = "TEXT")
  private String note;

  @Nullable
  @Column(name = "release_url", length = 500)
  private String releaseUrl;

  @Nonnull
  @Column(name = "decided_at", nullable = false)
  private Instant decidedAt;

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

  // #### Helper
  /** True when the postponement has run out and the update is due again. */
  public boolean isSnoozeExpired() {
    return DecisionState.POSTPONED.equals(this.state)
           && (this.snoozeUntil == null || this.snoozeUntil.isBefore(Instant.now()));
  }

  // #### Custom Getter and Setter
  public void setComponent(@Nonnull final ComponentEntity component) {
    this.component = component;
    this.componentId = component.getId();
  }
}
