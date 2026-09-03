package one.epicsoft.vc.jpa.repository;

import jakarta.annotation.Nonnull;
import one.epicsoft.vc.jpa.entity.ComponentDependencyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface ComponentDependencyRepository extends JpaRepository<ComponentDependencyEntity, UUID> {

  /**
   * What this component needs.
   *
   * <p>Both sides have to be alive. A component is deleted softly, and its
   * dependency rows outlive it — without this filter a deleted component keeps
   * blocking upgrades from a row nobody can reach any more.
   */
  @Query("""
      SELECT cd FROM ComponentDependencyEntity cd
      WHERE cd.componentId = :componentId
        AND cd.component.deleted IS NULL
        AND cd.dependsOn.deleted IS NULL
      """)
  List<ComponentDependencyEntity> findAllByComponent(@Nonnull @Param("componentId") UUID componentId);

  /** The same, for a whole list of components in one query. */
  @Query("""
      SELECT cd FROM ComponentDependencyEntity cd
      WHERE cd.componentId IN :componentIds
        AND cd.component.deleted IS NULL
        AND cd.dependsOn.deleted IS NULL
      """)
  List<ComponentDependencyEntity> findAllByComponents(@Nonnull @Param("componentIds") Collection<UUID> componentIds);

  /** What needs this component — the direction that decides whether an update breaks something. */
  @Query("""
      SELECT cd FROM ComponentDependencyEntity cd
      WHERE cd.dependsOnId = :dependsOnId
        AND cd.component.deleted IS NULL
        AND cd.dependsOn.deleted IS NULL
      """)
  List<ComponentDependencyEntity> findAllByDependsOn(@Nonnull @Param("dependsOnId") UUID dependsOnId);

  /** Everything a project needs to evaluate, in one query. */
  @Query("""
      SELECT cd FROM ComponentDependencyEntity cd
      WHERE cd.component.projectId = :projectId
        AND cd.component.deleted IS NULL
        AND cd.dependsOn.deleted IS NULL
      """)
  List<ComponentDependencyEntity> findAllByProject(@Nonnull @Param("projectId") UUID projectId);

  void deleteAllByComponentId(@Nonnull UUID componentId);

}
