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
 * One component needs another at a certain version.
 *
 * <p>The pair is directed: {@code component} is the dependent one, Zitadel in
 * the example, and {@code dependsOn} is what it needs, PostgreSQL. The range is
 * written in the npm style, and the note says where that requirement was read —
 * a year later that sentence is what saves somebody looking it up again.
 */
@Getter
@Setter
@Entity
@Table(name = "component_dependency")
public class ComponentDependencyEntity extends AbstractEntity {

  @Nonnull
  @Column(name = "version_range", nullable = false, length = 200)
  private String versionRange;

  @Nullable
  @Column(name = "note", columnDefinition = "TEXT")
  private String note;

  @Nonnull
  @ToString.Exclude
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "component_id", nullable = false)
  private ComponentEntity component;

  @Nonnull
  @ToString.Exclude
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "depends_on_id", nullable = false)
  private ComponentEntity dependsOn;

  // #### Relation IDs
  @Nonnull
  @Setter(AccessLevel.NONE)
  @Column(name = "component_id", insertable = false, updatable = false)
  private UUID componentId;

  @Nonnull
  @Setter(AccessLevel.NONE)
  @Column(name = "depends_on_id", insertable = false, updatable = false)
  private UUID dependsOnId;

  // #### Custom Getter and Setter
  public void setComponent(@Nonnull final ComponentEntity component) {
    this.component = component;
    this.componentId = component.getId();
  }

  public void setDependsOn(@Nonnull final ComponentEntity dependsOn) {
    this.dependsOn = dependsOn;
    this.dependsOnId = dependsOn.getId();
  }
}
