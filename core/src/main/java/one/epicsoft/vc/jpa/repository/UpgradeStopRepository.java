package one.epicsoft.vc.jpa.repository;

import jakarta.annotation.Nonnull;
import one.epicsoft.vc.jpa.entity.UpgradeStopEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface UpgradeStopRepository extends JpaRepository<UpgradeStopEntity, UUID> {

  List<UpgradeStopEntity> findAllByComponentIdOrderByVersionAsc(@Nonnull UUID componentId);

  List<UpgradeStopEntity> findAllByComponentIdInOrderByVersionAsc(@Nonnull Collection<UUID> componentIds);

  void deleteAllByComponentId(@Nonnull UUID componentId);
}
