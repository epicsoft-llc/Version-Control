package one.epicsoft.vc.jpa.repository;

import jakarta.annotation.Nonnull;
import one.epicsoft.vc.jpa.entity.UpdateDecisionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UpdateDecisionRepository extends JpaRepository<UpdateDecisionEntity, UUID> {

  Optional<UpdateDecisionEntity> findByComponentId(@Nonnull UUID componentId);

  /** Everything the task list of one project needs, in one query. */
  @Query("""
      SELECT ud FROM UpdateDecisionEntity ud
      WHERE ud.component.projectId = :projectId
        AND ud.component.deleted IS NULL
      """)
  List<UpdateDecisionEntity> findAllByProject(@Nonnull @Param("projectId") UUID projectId);
}
