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

/** The newest version found for one package of a component that has several. */
@Getter
@Setter
@Entity
@Table(name = "update_check_package")
public class UpdateCheckPackageEntity extends AbstractEntity {

  @Nonnull
  @Column(name = "package_name", nullable = false, length = 200)
  private String packageName;

  @Nullable
  @Column(name = "latest_version", length = 200)
  private String latestVersion;

  @Nonnull
  @ToString.Exclude
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "update_check_id", nullable = false)
  private UpdateCheckEntity updateCheck;

  // #### Relation IDs
  @Nonnull
  @Setter(AccessLevel.NONE)
  @Column(name = "update_check_id", insertable = false, updatable = false)
  private UUID updateCheckId;

  // #### Custom Getter and Setter
  public void setUpdateCheck(@Nonnull final UpdateCheckEntity updateCheck) {
    this.updateCheck = updateCheck;
    this.updateCheckId = updateCheck.getId();
  }
}
