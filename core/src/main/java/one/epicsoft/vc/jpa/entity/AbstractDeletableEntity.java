package one.epicsoft.vc.jpa.entity;

import jakarta.annotation.Nullable;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

import static java.util.Objects.nonNull;

/**
 * Soft delete, for the entities a change history points at. Everything else is
 * deleted outright — a snapshot has no value once it is gone.
 */
@Getter
@Setter
@MappedSuperclass
public abstract class AbstractDeletableEntity extends AbstractEntity {

  @Nullable
  @Column(name = "deleted")
  private Instant deleted;

  public void delete() {
    this.deleted = Instant.now();
  }

  public boolean isDeleted() {
    return nonNull(this.deleted);
  }
}
