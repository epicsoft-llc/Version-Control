package one.epicsoft.vc.jpa.entity;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import java.util.UUID;

import static java.util.Objects.isNull;

/** Identifier and timestamps, shared by every entity. */
@Getter
@Setter
@MappedSuperclass
public abstract class AbstractEntity {

  @Nonnull
  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Nonnull
  @Column(name = "created", nullable = false, updatable = false)
  private Instant created;

  @Nullable
  @Column(name = "updated")
  private Instant updated;

  @PrePersist
  public void prePersist() {
    if (isNull(this.id)) {
      this.id = UUID.randomUUID();
    }
    this.created = Instant.now();
  }

  @PreUpdate
  public void preUpdate() {
    this.updated = Instant.now();
  }
}
