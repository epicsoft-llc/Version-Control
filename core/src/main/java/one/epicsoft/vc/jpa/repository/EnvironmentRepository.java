package one.epicsoft.vc.jpa.repository;

import jakarta.annotation.Nonnull;
import one.epicsoft.vc.jpa.entity.EnvironmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EnvironmentRepository extends JpaRepository<EnvironmentEntity, UUID> {

  Optional<EnvironmentEntity> findByIdAndDeletedIsNull(@Nonnull UUID id);

  List<EnvironmentEntity> findAllByProjectIdAndDeletedIsNullOrderBySortOrderAscNameAsc(@Nonnull UUID projectId);

  boolean existsByProjectIdAndNameAndDeletedIsNull(@Nonnull UUID projectId, @Nonnull String name);
}
