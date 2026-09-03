package one.epicsoft.vc.jpa.repository;

import jakarta.annotation.Nonnull;
import one.epicsoft.vc.jpa.entity.ComponentEnvironmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ComponentEnvironmentRepository extends JpaRepository<ComponentEnvironmentEntity, UUID> {

  Optional<ComponentEnvironmentEntity> findByIdAndDeletedIsNull(@Nonnull UUID id);

  Optional<ComponentEnvironmentEntity> findByComponentIdAndEnvironmentIdAndDeletedIsNull(@Nonnull UUID componentId,
                                                                                         @Nonnull UUID environmentId);

  /** Excludes deleted environments too, or the details page shows a column the environment list no longer has. */
  @Query("""
      SELECT ce FROM ComponentEnvironmentEntity ce
      WHERE ce.componentId = :componentId
        AND ce.deleted IS NULL
        AND ce.environment.deleted IS NULL
      """)
  List<ComponentEnvironmentEntity> findAllByComponent(@Nonnull @Param("componentId") final UUID componentId);

  @Query("""
      SELECT ce FROM ComponentEnvironmentEntity ce
      WHERE ce.environmentId = :environmentId
        AND ce.deleted IS NULL
        AND ce.component.deleted IS NULL
      """)
  List<ComponentEnvironmentEntity> findAllByEnvironment(@Nonnull @Param("environmentId") final UUID environmentId);

  /** Everything the matrix of one project needs, in one query. */
  @Query("""
      SELECT ce FROM ComponentEnvironmentEntity ce
      WHERE ce.component.projectId = :projectId
        AND ce.deleted IS NULL
        AND ce.component.deleted IS NULL
        AND ce.environment.deleted IS NULL
      """)
  List<ComponentEnvironmentEntity> findAllByProject(@Nonnull @Param("projectId") final UUID projectId);
}
