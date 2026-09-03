package one.epicsoft.vc.service;

import jakarta.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import one.epicsoft.vc.exception.AppNotFoundException;
import one.epicsoft.vc.jpa.entity.ComponentEnvironmentEntity;
import one.epicsoft.vc.jpa.entity.ComponentEnvironmentPackageEntity;
import one.epicsoft.vc.jpa.repository.ComponentEnvironmentPackageRepository;
import one.epicsoft.vc.jpa.repository.ComponentEnvironmentRepository;
import org.springframework.stereotype.Service;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ComponentEnvironmentService {

  private final ComponentEnvironmentRepository componentEnvironmentRepository;
  private final ComponentEnvironmentPackageRepository componentEnvironmentPackageRepository;

  @Nonnull
  public ComponentEnvironmentEntity getById(@Nonnull final UUID id) {
    return this.componentEnvironmentRepository.findByIdAndDeletedIsNull(id)
                                              .orElseThrow(() -> AppNotFoundException.byId("ComponentEnvironment", id));
  }

  @Nonnull
  public Optional<ComponentEnvironmentEntity> findByComponentAndEnvironment(@Nonnull final UUID componentId,
                                                                            @Nonnull final UUID environmentId) {
    return this.componentEnvironmentRepository.findByComponentIdAndEnvironmentIdAndDeletedIsNull(componentId, environmentId);
  }

  @Nonnull
  public List<ComponentEnvironmentEntity> findAllByComponent(@Nonnull final UUID componentId) {
    return this.componentEnvironmentRepository.findAllByComponent(componentId);
  }

  @Nonnull
  public List<ComponentEnvironmentEntity> findAllByEnvironment(@Nonnull final UUID environmentId) {
    return this.componentEnvironmentRepository.findAllByEnvironment(environmentId);
  }

  @Nonnull
  public List<ComponentEnvironmentEntity> findAllByProject(@Nonnull final UUID projectId) {
    return this.componentEnvironmentRepository.findAllByProject(projectId);
  }

  @Nonnull
  public ComponentEnvironmentEntity save(@Nonnull final ComponentEnvironmentEntity entity) {
    return this.componentEnvironmentRepository.save(entity);
  }

  public void delete(@Nonnull final ComponentEnvironmentEntity entity) {
    entity.delete();
    this.componentEnvironmentRepository.save(entity);
    log.debug("Soft deleted component environment '{}'", entity.getId());
  }

  @Nonnull
  public List<ComponentEnvironmentPackageEntity> findPackages(@Nonnull final UUID componentEnvironmentId) {
    return this.componentEnvironmentPackageRepository.findAllByComponentEnvironmentIdOrderByPackageNameAsc(componentEnvironmentId);
  }

  @Nonnull
  public List<ComponentEnvironmentPackageEntity> findPackages(@Nonnull final Collection<UUID> componentEnvironmentIds) {
    if (componentEnvironmentIds.isEmpty()) {
      return List.of();
    }
    return this.componentEnvironmentPackageRepository.findAllByComponentEnvironmentIdInOrderByPackageNameAsc(componentEnvironmentIds);
  }

  @Nonnull
  public List<ComponentEnvironmentPackageEntity> savePackages(@Nonnull final List<ComponentEnvironmentPackageEntity> entities) {
    return this.componentEnvironmentPackageRepository.saveAll(entities);
  }
}
