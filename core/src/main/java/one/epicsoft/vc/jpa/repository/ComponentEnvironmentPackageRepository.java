package one.epicsoft.vc.jpa.repository;

import jakarta.annotation.Nonnull;
import one.epicsoft.vc.jpa.entity.ComponentEnvironmentPackageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface ComponentEnvironmentPackageRepository extends JpaRepository<ComponentEnvironmentPackageEntity, UUID> {

  List<ComponentEnvironmentPackageEntity> findAllByComponentEnvironmentIdOrderByPackageNameAsc(@Nonnull UUID componentEnvironmentId);

  /** One query for a whole matrix instead of one per cell. */
  List<ComponentEnvironmentPackageEntity> findAllByComponentEnvironmentIdInOrderByPackageNameAsc(@Nonnull Collection<UUID> componentEnvironmentIds);
}
