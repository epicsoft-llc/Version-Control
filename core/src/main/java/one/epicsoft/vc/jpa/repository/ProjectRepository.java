package one.epicsoft.vc.jpa.repository;

import jakarta.annotation.Nonnull;
import one.epicsoft.vc.jpa.entity.ProjectEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProjectRepository extends JpaRepository<ProjectEntity, UUID> {

  Optional<ProjectEntity> findByIdAndDeletedIsNull(@Nonnull UUID id);

  List<ProjectEntity> findAllByDeletedIsNullOrderBySortOrderAscNameAsc();

  boolean existsByNameAndDeletedIsNull(@Nonnull String name);
}
