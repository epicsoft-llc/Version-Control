package one.epicsoft.vc.facade;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import one.epicsoft.vc.dto.DependencyCheckDto;
import one.epicsoft.vc.enums.ConstraintState;
import one.epicsoft.vc.jpa.entity.ComponentDependencyEntity;
import one.epicsoft.vc.jpa.entity.ComponentEntity;
import one.epicsoft.vc.jpa.entity.ComponentEnvironmentEntity;
import one.epicsoft.vc.service.ComponentDependencyService;
import one.epicsoft.vc.service.ComponentEnvironmentService;
import one.epicsoft.vc.service.ComponentService;
import one.epicsoft.vc.version.ConstraintEvaluator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;
import static one.epicsoft.vc.util.VcUtil.nonBlank;

/**
 * Whether the versions that are deployed fit together, and whether an update
 * would break that.
 *
 * <p>Every answer is per environment. Production can run PostgreSQL 16 while
 * staging is already on 17, and the same dependency then has two different
 * results — a single verdict for the component would be wrong in one of the two
 * places, which is worse than none.
 *
 * <p>The two directions answer different questions:
 *
 * <ul>
 *   <li><strong>Backwards</strong> — is what runs right now a supported
 *       combination? A violation here means something is already running outside
 *       what its vendor supports, which is why it sits high on the task list.</li>
 *   <li><strong>Forwards</strong> — would the proposed target break something
 *       that depends on this component? If so the update is reported as blocked
 *       rather than available, and the answer names the component and the range
 *       that stand in the way. That question is the one this application answers
 *       instead of leaving it to somebody's memory.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DependencyFacade {

  private final ComponentService componentService;
  private final ComponentEnvironmentService componentEnvironmentService;
  private final ComponentDependencyService componentDependencyService;
  private final ConstraintEvaluator constraintEvaluator;

  /**
   * Backwards, for one project: every dependency of every environment, with the
   * version that is actually there.
   */
  @Nonnull
  @Transactional(readOnly = true, rollbackFor = Throwable.class)
  public List<DependencyCheckDto> checkProject(@Nonnull final UUID projectId) {
    final List<ComponentDependencyEntity> dependencies = this.componentDependencyService.findAllByProject(projectId);
    if (dependencies.isEmpty()) {
      return List.of();
    }
    final List<ComponentEnvironmentEntity> cells = this.componentEnvironmentService.findAllByProject(projectId);
    return this.evaluate(dependencies, cells);
  }

  /** Backwards, for one component: what it needs, per environment. */
  @Nonnull
  @Transactional(readOnly = true, rollbackFor = Throwable.class)
  public List<DependencyCheckDto> checkComponent(@Nonnull final UUID componentId) {
    final ComponentEntity component = this.componentService.getById(componentId);
    return this.evaluate(this.componentDependencyService.findAllByComponent(componentId),
                         this.componentEnvironmentService.findAllByProject(component.getProjectId()));
  }

  /** The other direction: what needs this component, per environment. */
  @Nonnull
  @Transactional(readOnly = true, rollbackFor = Throwable.class)
  public List<DependencyCheckDto> checkDependents(@Nonnull final UUID componentId) {
    final ComponentEntity component = this.componentService.getById(componentId);
    return this.evaluate(this.componentDependencyService.findAllByDependsOn(componentId),
                         this.componentEnvironmentService.findAllByProject(component.getProjectId()));
  }

  /**
   * Forwards: what would stand in the way of moving this component to a version.
   *
   * <p>Only violations come back. A caller that gets an empty list may propose
   * the target; one that gets entries has to say why it cannot, and the entries
   * carry the name and the range to say it with.
   */
  @Nonnull
  @Transactional(readOnly = true, rollbackFor = Throwable.class)
  public List<DependencyCheckDto> blockers(@Nonnull final UUID componentId, @Nonnull final UUID environmentId,
                                           @Nullable final String target) {
    final ComponentEntity component = this.componentService.getById(componentId);
    return this.blockers(this.index(component.getProjectId()), componentId, environmentId, target);
  }

  /**
   * Everything the forward check needs, read once.
   *
   * <p>The alternative is two queries per environment and component: at twenty
   * components across three environments that is a hundred round trips for one
   * page, all of them asking for the same handful of rows.
   */
  @Nonnull
  @Transactional(readOnly = true, rollbackFor = Throwable.class)
  public DependencyIndex index(@Nonnull final UUID projectId) {
    final List<ComponentDependencyEntity> dependencies = this.componentDependencyService.findAllByProject(projectId);
    final Map<UUID, List<ComponentDependencyEntity>> byPrerequisite =
        dependencies.stream().collect(Collectors.groupingBy(ComponentDependencyEntity::getDependsOnId));
    final Map<UUID, Set<UUID>> presentPerEnvironment =
        this.componentEnvironmentService.findAllByProject(projectId).stream()
            .filter(ComponentEnvironmentEntity::isActive)
            .collect(Collectors.groupingBy(ComponentEnvironmentEntity::getEnvironmentId,
                                           Collectors.mapping(ComponentEnvironmentEntity::getComponentId,
                                                              Collectors.toSet())));
    // The names come along: answering from the index happens outside the
    // transaction that built it, and reaching through a lazy relation there
    // would throw rather than answer.
    final Map<UUID, String> componentNames = this.componentService.findAllByProject(projectId).stream()
                                                 .collect(Collectors.toMap(ComponentEntity::getId,
                                                                           ComponentEntity::getName));
    return new DependencyIndex(byPrerequisite, presentPerEnvironment, componentNames);
  }

  /** The same question as above, answered from an index and without a query. */
  @Nonnull
  public List<DependencyCheckDto> blockers(@Nonnull final DependencyIndex index, @Nonnull final UUID componentId,
                                           @Nonnull final UUID environmentId, @Nullable final String target) {
    if (!nonBlank(target)) {
      return List.of();
    }
    // Only the components that actually run in this environment. A dependent
    // that lives in production says nothing about an upgrade in dev, and the
    // backwards check filters the same way — the two directions have to agree.
    final Set<UUID> presentHere = index.presentPerEnvironment().getOrDefault(environmentId, Set.of());

    final List<DependencyCheckDto> blocking = new ArrayList<>();
    for (final ComponentDependencyEntity dependency : index.byPrerequisite()
                                                           .getOrDefault(componentId, List.of())) {
      if (!presentHere.contains(dependency.getComponentId())) {
        continue;
      }
      final ConstraintState state = this.constraintEvaluator.evaluate(target, dependency.getVersionRange());
      if (ConstraintState.SATISFIED.equals(state)) {
        continue;
      }
      // Unknown counts as blocking here on purpose: the forward check decides
      // whether to propose an action, and proposing one whose effect nobody can
      // work out is worse than saying so.
      blocking.add(named(this.toDto(dependency, index, environmentId, target, state), null));
    }
    return blocking;
  }

  @Nonnull
  private List<DependencyCheckDto> evaluate(@Nonnull final List<ComponentDependencyEntity> dependencies,
                                            @Nonnull final List<ComponentEnvironmentEntity> cells) {
    if (dependencies.isEmpty()) {
      return List.of();
    }
    // By component and environment, so the version of the prerequisite in that
    // one environment can be looked up without another query per pair — and by
    // component alone, so a dependency only walks its own cells instead of every
    // cell in the project.
    final Map<String, ComponentEnvironmentEntity> byPair =
        cells.stream().collect(Collectors.toMap(cell -> key(cell.getComponentId(), cell.getEnvironmentId()),
                                                Function.identity(), (first, second) -> first));
    final Map<UUID, List<ComponentEnvironmentEntity>> byComponent =
        cells.stream().collect(Collectors.groupingBy(ComponentEnvironmentEntity::getComponentId));

    final List<DependencyCheckDto> results = new ArrayList<>();
    for (final ComponentDependencyEntity dependency : dependencies) {
      for (final ComponentEnvironmentEntity cell : byComponent.getOrDefault(dependency.getComponentId(), List.of())) {
        if (!cell.isActive()) {
          continue;
        }
        final ComponentEnvironmentEntity prerequisite =
            byPair.get(key(dependency.getDependsOnId(), cell.getEnvironmentId()));
        final String environmentName = cell.getEnvironment().getName();
        if (isNull(prerequisite) || !prerequisite.isActive()) {
          // The prerequisite does not run here at all. That is a statement about
          // the setup, not about a version, and it is reported as unknown rather
          // than silently passed.
          results.add(named(this.toDto(dependency, cell.getEnvironmentId(), null, ConstraintState.UNKNOWN),
                            environmentName));
          continue;
        }
        results.add(named(this.toDto(dependency, cell.getEnvironmentId(), prerequisite.getCurrentVersion(),
                                     this.constraintEvaluator.evaluate(prerequisite.getCurrentVersion(),
                                                                       dependency.getVersionRange())),
                          environmentName));
      }
    }
    return results;
  }

  /** From the index, without touching a lazy relation. */
  @Nonnull
  private DependencyCheckDto toDto(@Nonnull final ComponentDependencyEntity dependency,
                                   @Nonnull final DependencyIndex index,
                                   @Nonnull final UUID environmentId,
                                   @Nullable final String version,
                                   @Nonnull final ConstraintState state) {
    final DependencyCheckDto dto = new DependencyCheckDto();
    dto.setDependencyId(dependency.getId());
    dto.setComponentId(dependency.getComponentId());
    dto.setComponentName(index.componentNames().getOrDefault(dependency.getComponentId(), ""));
    dto.setDependsOnId(dependency.getDependsOnId());
    dto.setDependsOnName(index.componentNames().getOrDefault(dependency.getDependsOnId(), ""));
    dto.setEnvironmentId(environmentId);
    dto.setVersionRange(dependency.getVersionRange());
    dto.setNote(dependency.getNote());
    dto.setActualVersion(version);
    dto.setState(state);
    return dto;
  }

  @Nonnull
  private DependencyCheckDto toDto(@Nonnull final ComponentDependencyEntity dependency,
                                   @Nonnull final UUID environmentId,
                                   @Nullable final String version,
                                   @Nonnull final ConstraintState state) {
    final DependencyCheckDto dto = new DependencyCheckDto();
    dto.setDependencyId(dependency.getId());
    dto.setComponentId(dependency.getComponentId());
    dto.setComponentName(dependency.getComponent().getName());
    dto.setDependsOnId(dependency.getDependsOnId());
    dto.setDependsOnName(dependency.getDependsOn().getName());
    dto.setEnvironmentId(environmentId);
    dto.setVersionRange(dependency.getVersionRange());
    dto.setNote(dependency.getNote());
    dto.setActualVersion(version);
    dto.setState(state);
    return dto;
  }

  @Nonnull
  private static DependencyCheckDto named(@Nonnull final DependencyCheckDto check,
                                          @Nullable final String environmentName) {
    check.setEnvironmentName(environmentName);
    return check;
  }

  /**
   * What the forward check needs, read once per project.
   *
   * @param byPrerequisite        the dependencies that name a component as their
   *                              prerequisite, by that component
   * @param presentPerEnvironment which components are actually active in which
   *                              environment
   * @param componentNames        carried along so a name may be read outside the
   *                              transaction the index was built in
   */
  public record DependencyIndex(@Nonnull Map<UUID, List<ComponentDependencyEntity>> byPrerequisite,
                                @Nonnull Map<UUID, Set<UUID>> presentPerEnvironment,
                                @Nonnull Map<UUID, String> componentNames) {
  }

  @Nonnull
  private static String key(@Nonnull final UUID componentId, @Nonnull final UUID environmentId) {
    return componentId + "/" + environmentId;
  }
}
