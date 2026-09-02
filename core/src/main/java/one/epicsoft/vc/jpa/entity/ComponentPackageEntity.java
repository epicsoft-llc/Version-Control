package one.epicsoft.vc.jpa.entity;

import jakarta.annotation.Nonnull;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import java.util.UUID;

import static java.util.Objects.isNull;
import static one.epicsoft.vc.util.VcUtil.isTrue;

/**
 * One package of a component that consists of several. Docker on Ubuntu is one
 * component with four packages, updated in a single action — four rows in the
 * matrix would misrepresent the effort.
 */
@Getter
@Setter
@Entity
@Table(name = "component_package")
public class ComponentPackageEntity extends AbstractEntity {

  @Nonnull
  @Column(name = "name", nullable = false, length = 200)
  private String name;

  @Nonnull
  @Column(name = "sort_order", nullable = false)
  private Integer sortOrder;

  /** Its version is the one shown in the matrix cell. */
  @Nonnull
  @Getter(AccessLevel.NONE)
  @Column(name = "leading_package", nullable = false)
  private Boolean leadingPackage;

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

  @PrePersist
  public void prePersistComponentPackage() {
    if (isNull(this.leadingPackage)) {
      this.leadingPackage = Boolean.FALSE;
    }
  }

  // #### Custom Getter and Setter
  public boolean isLeadingPackage() {
    return isTrue(this.leadingPackage);
  }

  public void setComponent(@Nonnull final ComponentEntity component) {
    this.component = component;
    this.componentId = component.getId();
  }
}
