package one.epicsoft.vc.service;

import jakarta.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import one.epicsoft.vc.jpa.entity.ComponentDependencyEntity;
import one.epicsoft.vc.jpa.entity.ComponentEntity;
import one.epicsoft.vc.jpa.repository.ComponentDependencyRepository;
import org.springframework.stereotype.Service;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ComponentDependencyService {

  private final ComponentDependencyRepository componentDependencyRepository;

  @Nonnull
  public List<ComponentDependencyEntity> findAllByComponent(@Nonnull final UUID componentId) {
    return this.componentDependencyRepository.findAllByComponent(componentId);
  }

  /** One query for a whole list instead of one per component. */
  @Nonnull
  public List<ComponentDependencyEntity> findAllByComponents(@Nonnull final Collection<UUID> componentIds) {
    if (componentIds.isEmpty()) {
      return List.of();
    }
    return this.componentDependencyRepository.findAllByComponents(componentIds);
  }

  @Nonnull
  public List<ComponentDependencyEntity> findAllByDependsOn(@Nonnull final UUID dependsOnId) {
    return this.componentDependencyRepository.findAllByDependsOn(dependsOnId);
  }

  /**
   * Removes what a component declared, when it goes.
   *
   * <p>Only its own rows. The other direction — what other components need from
   * it — belongs to them: their range and the note saying where it was read
   * would be gone for good, while the component itself is only deleted softly.
   * Those rows stay invisible on their own, because every query requires both
   * ends to be alive.
   */
  public void deleteAllOf(@Nonnull final UUID componentId) {
    this.componentDependencyRepository.deleteAllByComponentId(componentId);
  }

  @Nonnull
  public List<ComponentDependencyEntity> findAllByProject(@Nonnull final UUID projectId) {
    return this.componentDependencyRepository.findAllByProject(projectId);
  }

  /** The submitted list replaces the stored one; dependencies carry no soft delete. */
  @Nonnull
  public List<ComponentDependencyEntity> replaceDependencies(@Nonnull final ComponentEntity component,
                                                             @Nonnull final List<ComponentDependencyEntity> dependencies) {
    this.componentDependencyRepository.deleteAllByComponentId(component.getId());
    // Hibernate orders inserts before deletes inside one transaction, so a pair
    // that survives the replacement would collide with its own old row.
    this.componentDependencyRepository.flush();
    dependencies.forEach(entry -> entry.setComponent(component));
    return this.componentDependencyRepository.saveAll(dependencies);
  }
}
