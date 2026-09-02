package one.epicsoft.vc.jpa.repository;

import jakarta.annotation.Nonnull;
import one.epicsoft.vc.jpa.entity.ComponentPackageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface ComponentPackageRepository extends JpaRepository<ComponentPackageEntity, UUID> {

  List<ComponentPackageEntity> findAllByComponentIdOrderBySortOrderAscNameAsc(@Nonnull UUID componentId);

  /** One query for a whole list of components instead of one per component. */
  List<ComponentPackageEntity> findAllByComponentIdInOrderBySortOrderAscNameAsc(@Nonnull Collection<UUID> componentIds);

  void deleteAllByComponentId(@Nonnull UUID componentId);
}
