package one.epicsoft.vc.jpa.entity;

import jakarta.annotation.Nonnull;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import java.time.Instant;
import java.util.UUID;

/**
 * One thing the daily notification has already reported.
 *
 * <p>This is what makes the message a message about what is new. Without it the
 * same twelve entries would arrive every morning, and a notification that
 * repeats itself is one nobody reads by the end of the week.
 *
 * <p>A row disappears again as soon as the thing it describes is gone from the
 * current state. Something that comes back is therefore reported once more —
 * silence for a problem that returned would be the worse mistake.
 */
@Getter
@Setter
@Entity
@Table(name = "notification_entry")
public class NotificationEntryEntity extends AbstractEntity {

  /** Identifies the thing reported: section, component, environment, version. */
  @Nonnull
  @Column(name = "entry_key", nullable = false, length = 500)
  private String entryKey;

  @Nonnull
  @Column(name = "reported_at", nullable = false)
  private Instant reportedAt;

  @Nonnull
  @ToString.Exclude
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "project_id", nullable = false)
  private ProjectEntity project;

  // #### Relation IDs
  @Nonnull
  @Setter(AccessLevel.NONE)
  @Column(name = "project_id", insertable = false, updatable = false)
  private UUID projectId;

  // #### Custom Getter and Setter
  public void setProject(@Nonnull final ProjectEntity project) {
    this.project = project;
    this.projectId = project.getId();
  }
}
