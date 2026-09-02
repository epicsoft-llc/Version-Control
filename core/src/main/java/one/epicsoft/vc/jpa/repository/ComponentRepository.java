package one.epicsoft.vc.jpa.repository;

import jakarta.annotation.Nonnull;
import one.epicsoft.vc.jpa.entity.ComponentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ComponentRepository extends JpaRepository<ComponentEntity, UUID> {

  Optional<ComponentEntity> findByIdAndDeletedIsNull(@Nonnull UUID id);

  List<ComponentEntity> findAllByProjectIdAndDeletedIsNullOrderByNameAsc(@Nonnull UUID projectId);

  boolean existsByProjectIdAndNameAndDeletedIsNull(@Nonnull UUID projectId, @Nonnull String name);
}
