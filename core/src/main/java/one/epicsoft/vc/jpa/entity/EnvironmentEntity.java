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
 * A deployment environment of a project — test, staging, production.
 *
 * <p>An entity, not a set of columns on the version record: a fourth environment
 * must not require a schema migration.
 */
@Getter
@Setter
@Entity
@Table(name = "environment")
public class EnvironmentEntity extends AbstractDeletableEntity {

  @Nonnull
  @Column(name = "name", nullable = false, length = 100)
  private String name;

  /** Column order of the matrix. */
  @Nonnull
  @Column(name = "sort_order", nullable = false)
  private Integer sortOrder;

  /** Drives warnings; more than one environment may carry it. */
  @Nonnull
  @Getter(AccessLevel.NONE)
  @Column(name = "production", nullable = false)
  private Boolean production;

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

  @PrePersist
  public void prePersistEnvironment() {
    if (isNull(this.production)) {
      this.production = Boolean.FALSE;
    }
  }

  // #### Custom Getter and Setter
  public boolean isProduction() {
    return isTrue(this.production);
  }

  public void setProject(@Nonnull final ProjectEntity project) {
    this.project = project;
    this.projectId = project.getId();
  }
}
