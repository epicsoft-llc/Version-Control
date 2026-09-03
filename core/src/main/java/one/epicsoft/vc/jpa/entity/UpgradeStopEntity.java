package one.epicsoft.vc.jpa.entity;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
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
import java.util.UUID;

/**
 * A version that cannot be skipped on the way up.
 *
 * <p>Not a rule engine and not derived from anything: somebody read the release
 * notes once and wrote down that this version has to be passed through. From
 * then on it appears with every proposal that would jump over it.
 */
@Getter
@Setter
@Entity
@Table(name = "upgrade_stop")
public class UpgradeStopEntity extends AbstractEntity {

  @Nonnull
  @Column(name = "version", nullable = false, length = 200)
  private String version;

  /** Why it cannot be skipped — the sentence that saves the next reader an hour. */
  @Nullable
  @Column(name = "note", columnDefinition = "TEXT")
  private String note;

  @Nonnull
  @ToString.Exclude
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
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
