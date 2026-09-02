package one.epicsoft.vc.jpa.entity;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * A bracket around components and environments that belong together. Several
 * projects are possible, but this is not multi-tenancy: every project is visible
 * to the same single user.
 */
@Getter
@Setter
@Entity
@Table(name = "project")
public class ProjectEntity extends AbstractDeletableEntity {

  @Nonnull
  @Column(name = "name", nullable = false, length = 100)
  private String name;

  @Nullable
  @Column(name = "description")
  private String description;

  @Nonnull
  @Column(name = "sort_order", nullable = false)
  private Integer sortOrder;
}
