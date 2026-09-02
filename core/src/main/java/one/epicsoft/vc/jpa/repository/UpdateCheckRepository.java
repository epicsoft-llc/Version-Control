package one.epicsoft.vc.jpa.repository;

import jakarta.annotation.Nonnull;
import one.epicsoft.vc.jpa.entity.UpdateCheckEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UpdateCheckRepository extends JpaRepository<UpdateCheckEntity, UUID> {

  Optional<UpdateCheckEntity> findByComponentId(@Nonnull UUID componentId);

  /** Everything the overview of one project needs, in one query. */
  @Query("""
      SELECT uc FROM UpdateCheckEntity uc
      WHERE uc.component.projectId = :projectId
        AND uc.component.deleted IS NULL
      """)
  List<UpdateCheckEntity> findAllByProject(@Nonnull @Param("projectId") UUID projectId);
}
