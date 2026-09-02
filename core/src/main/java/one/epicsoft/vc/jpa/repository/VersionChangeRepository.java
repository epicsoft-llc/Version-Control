package one.epicsoft.vc.jpa.repository;

import jakarta.annotation.Nonnull;
import one.epicsoft.vc.jpa.entity.VersionChangeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface VersionChangeRepository extends JpaRepository<VersionChangeEntity, UUID> {

  /**
   * The history of one cell, newest first. The join fetch is what keeps the list
   * endpoint at one query — every entry reports which environment it belongs to.
   */
  @Query("""
      SELECT vc FROM VersionChangeEntity vc
      JOIN FETCH vc.componentEnvironment ce
      WHERE vc.componentEnvironmentId = :componentEnvironmentId
      ORDER BY vc.changedAt DESC
      """)
  List<VersionChangeEntity> findAllByComponentEnvironment(@Nonnull @Param("componentEnvironmentId") UUID componentEnvironmentId);

  /**
   * The full history of a component across every environment, newest first.
   *
   * <p>Deleted cells and deleted environments stay out: their entries name an
   * environment the list no longer has, so the history table could not label the
   * row. The entry itself survives in the database — that is what the soft delete
   * is for — and is reachable through the cell it belongs to.
   */
  @Query("""
      SELECT vc FROM VersionChangeEntity vc
      JOIN FETCH vc.componentEnvironment ce
      WHERE ce.componentId = :componentId
        AND ce.deleted IS NULL
        AND ce.environment.deleted IS NULL
      ORDER BY vc.changedAt DESC
      """)
  List<VersionChangeEntity> findAllByComponent(@Nonnull @Param("componentId") UUID componentId);

  /** Recent changes across a whole project — the entry point for the overview. */
  @Query("""
      SELECT vc FROM VersionChangeEntity vc
      JOIN FETCH vc.componentEnvironment ce
      WHERE ce.component.projectId = :projectId
        AND ce.deleted IS NULL
        AND ce.component.deleted IS NULL
        AND ce.environment.deleted IS NULL
      ORDER BY vc.changedAt DESC
      """)
  List<VersionChangeEntity> findAllByProject(@Nonnull @Param("projectId") UUID projectId);
}
