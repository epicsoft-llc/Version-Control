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
 * The deployed version of one package in one environment. Needed because the
 * packages of a component do not move in lockstep: docker-ce and containerd.io
 * carry separate version streams.
 */
@Getter
@Setter
@Entity
@Table(name = "component_environment_package")
public class ComponentEnvironmentPackageEntity extends AbstractEntity {

  @Nonnull
  @Column(name = "package_name", nullable = false, length = 200)
  private String packageName;

  @Nullable
  @Column(name = "current_version", length = 200)
  private String currentVersion;

  @Nonnull
  @ToString.Exclude
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "component_environment_id", nullable = false)
  private ComponentEnvironmentEntity componentEnvironment;

  // #### Relation IDs
  @Nonnull
  @Setter(AccessLevel.NONE)
  @Column(name = "component_environment_id", insertable = false, updatable = false)
  private UUID componentEnvironmentId;

  // #### Custom Getter and Setter
  public void setComponentEnvironment(@Nonnull final ComponentEnvironmentEntity componentEnvironment) {
    this.componentEnvironment = componentEnvironment;
    this.componentEnvironmentId = componentEnvironment.getId();
  }
}
