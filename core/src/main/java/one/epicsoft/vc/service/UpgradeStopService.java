package one.epicsoft.vc.service;

import jakarta.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import one.epicsoft.vc.jpa.entity.ComponentEntity;
import one.epicsoft.vc.jpa.entity.UpgradeStopEntity;
import one.epicsoft.vc.jpa.repository.UpgradeStopRepository;
import org.springframework.stereotype.Service;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UpgradeStopService {

  private final UpgradeStopRepository upgradeStopRepository;

  @Nonnull
  public List<UpgradeStopEntity> findAllByComponent(@Nonnull final UUID componentId) {
    return this.upgradeStopRepository.findAllByComponentIdOrderByVersionAsc(componentId);
  }

  @Nonnull
  public List<UpgradeStopEntity> findAllByComponents(@Nonnull final Collection<UUID> componentIds) {
    if (componentIds.isEmpty()) {
      return List.of();
    }
    return this.upgradeStopRepository.findAllByComponentIdInOrderByVersionAsc(componentIds);
  }

  /** The submitted list replaces the stored one; stops carry no soft delete. */
  @Nonnull
  public List<UpgradeStopEntity> replaceStops(@Nonnull final ComponentEntity component,
                                              @Nonnull final List<UpgradeStopEntity> stops) {
    this.upgradeStopRepository.deleteAllByComponentId(component.getId());
    // Hibernate orders inserts before deletes inside one transaction, so a version
    // that survives the replacement would collide with its own old row.
    this.upgradeStopRepository.flush();
    stops.forEach(stop -> stop.setComponent(component));
    return this.upgradeStopRepository.saveAll(stops);
  }
}
