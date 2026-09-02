package one.epicsoft.vc.service;

import jakarta.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import one.epicsoft.vc.exception.AppNotFoundException;
import one.epicsoft.vc.jpa.entity.ComponentEntity;
import one.epicsoft.vc.jpa.entity.ComponentPackageEntity;
import one.epicsoft.vc.jpa.repository.ComponentPackageRepository;
import one.epicsoft.vc.jpa.repository.ComponentRepository;
import org.springframework.stereotype.Service;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ComponentService {

  private final ComponentRepository componentRepository;
  private final ComponentPackageRepository componentPackageRepository;

  @Nonnull
  public ComponentEntity getById(@Nonnull final UUID id) {
    return this.componentRepository.findByIdAndDeletedIsNull(id)
                                   .orElseThrow(() -> AppNotFoundException.byId("Component", id));
  }

  @Nonnull
  public List<ComponentEntity> findAllByProject(@Nonnull final UUID projectId) {
    return this.componentRepository.findAllByProjectIdAndDeletedIsNullOrderByNameAsc(projectId);
  }

  public boolean existsByProjectAndName(@Nonnull final UUID projectId, @Nonnull final String name) {
    return this.componentRepository.existsByProjectIdAndNameAndDeletedIsNull(projectId, name);
  }

  @Nonnull
  public ComponentEntity save(@Nonnull final ComponentEntity entity) {
    return this.componentRepository.save(entity);
  }

  public void delete(@Nonnull final ComponentEntity entity) {
    entity.delete();
    this.componentRepository.save(entity);
    log.debug("Soft deleted component '{}'", entity.getId());
  }

  @Nonnull
  public List<ComponentPackageEntity> findPackages(@Nonnull final UUID componentId) {
    return this.componentPackageRepository.findAllByComponentIdOrderBySortOrderAscNameAsc(componentId);
  }

  @Nonnull
  public List<ComponentPackageEntity> findPackages(@Nonnull final Collection<UUID> componentIds) {
    if (componentIds.isEmpty()) {
      return List.of();
    }
    return this.componentPackageRepository.findAllByComponentIdInOrderBySortOrderAscNameAsc(componentIds);
  }

  /** The submitted list replaces the stored one; packages carry no soft delete. */
  @Nonnull
  public List<ComponentPackageEntity> replacePackages(@Nonnull final ComponentEntity component,
                                                      @Nonnull final List<ComponentPackageEntity> packages) {
    this.componentPackageRepository.deleteAllByComponentId(component.getId());
    // Hibernate orders inserts before deletes inside one transaction, so a name
    // that survives the replacement would collide with its own old row. The flush
    // puts the deletes on the wire first.
    this.componentPackageRepository.flush();
    packages.forEach(entry -> entry.setComponent(component));
    return this.componentPackageRepository.saveAll(packages);
  }
}
