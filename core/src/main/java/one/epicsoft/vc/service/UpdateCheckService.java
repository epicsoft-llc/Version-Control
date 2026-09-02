package one.epicsoft.vc.service;

import jakarta.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import one.epicsoft.vc.jpa.entity.UpdateCheckEntity;
import one.epicsoft.vc.jpa.entity.UpdateCheckPackageEntity;
import one.epicsoft.vc.jpa.repository.UpdateCheckPackageRepository;
import one.epicsoft.vc.jpa.repository.UpdateCheckRepository;
import org.springframework.stereotype.Service;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UpdateCheckService {

  private final UpdateCheckRepository updateCheckRepository;
  private final UpdateCheckPackageRepository updateCheckPackageRepository;

  @Nonnull
  public Optional<UpdateCheckEntity> findByComponent(@Nonnull final UUID componentId) {
    return this.updateCheckRepository.findByComponentId(componentId);
  }

  @Nonnull
  public List<UpdateCheckEntity> findAllByProject(@Nonnull final UUID projectId) {
    return this.updateCheckRepository.findAllByProject(projectId);
  }

  @Nonnull
  public UpdateCheckEntity save(@Nonnull final UpdateCheckEntity entity) {
    return this.updateCheckRepository.save(entity);
  }

  @Nonnull
  public List<UpdateCheckPackageEntity> findPackages(@Nonnull final UUID updateCheckId) {
    return this.updateCheckPackageRepository.findAllByUpdateCheckIdOrderByPackageNameAsc(updateCheckId);
  }

  @Nonnull
  public List<UpdateCheckPackageEntity> findPackages(@Nonnull final Collection<UUID> updateCheckIds) {
    if (updateCheckIds.isEmpty()) {
      return List.of();
    }
    return this.updateCheckPackageRepository.findAllByUpdateCheckIdInOrderByPackageNameAsc(updateCheckIds);
  }

  /** The result of a check replaces the one before it; there is no history here. */
  @Nonnull
  public List<UpdateCheckPackageEntity> replacePackages(@Nonnull final UpdateCheckEntity updateCheck,
                                                        @Nonnull final List<UpdateCheckPackageEntity> packages) {
    this.updateCheckPackageRepository.deleteAllByUpdateCheckId(updateCheck.getId());
    // Hibernate orders inserts before deletes inside one transaction, so a name
    // that survives the replacement would collide with its own old row.
    this.updateCheckPackageRepository.flush();
    packages.forEach(entry -> entry.setUpdateCheck(updateCheck));
    return this.updateCheckPackageRepository.saveAll(packages);
  }
}
