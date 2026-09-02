package one.epicsoft.vc.facade;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import one.epicsoft.vc.dto.ComponentDto;
import one.epicsoft.vc.dto.ComponentPackageRequest;
import one.epicsoft.vc.dto.ComponentRequest;
import one.epicsoft.vc.dto.UpgradeStopRequest;
import one.epicsoft.vc.exception.AppConflictException;
import one.epicsoft.vc.exception.AppValidationException;
import one.epicsoft.vc.jpa.entity.ComponentEntity;
import one.epicsoft.vc.jpa.entity.ComponentPackageEntity;
import one.epicsoft.vc.jpa.entity.UpgradeStopEntity;
import one.epicsoft.vc.jpa.entity.ProjectEntity;
import one.epicsoft.vc.mapper.ComponentMapper;
import one.epicsoft.vc.mapper.UpgradeStopMapper;
import one.epicsoft.vc.service.ComponentEnvironmentService;
import one.epicsoft.vc.service.ComponentService;
import one.epicsoft.vc.service.UpgradeStopService;
import one.epicsoft.vc.service.ProjectService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;
import static one.epicsoft.vc.util.VcUtil.nonBlank;
import static java.util.Objects.nonNull;
import static one.epicsoft.vc.util.VcUtil.nonEquals;

@Slf4j
@Service
@RequiredArgsConstructor
public class ComponentFacade {

  private final ComponentService componentService;
  private final ProjectService projectService;
  private final ComponentEnvironmentService componentEnvironmentService;
  private final ComponentMapper componentMapper;
  private final UpgradeStopService upgradeStopService;
  private final UpgradeStopMapper upgradeStopMapper;

  @Nonnull
  @Transactional(readOnly = true, rollbackFor = Throwable.class)
  public List<ComponentDto> findAllByProject(@Nonnull final UUID projectId) {
    return this.toDtos(this.componentService.findAllByProject(projectId));
  }

  @Nonnull
  @Transactional(readOnly = true, rollbackFor = Throwable.class)
  public ComponentDto getById(@Nonnull final UUID id) {
    return this.toDtos(List.of(this.componentService.getById(id))).getFirst();
  }

  @Nonnull
  @Transactional(rollbackFor = Throwable.class)
  public ComponentDto create(@Nonnull final ComponentRequest request) {
    final ProjectEntity project = this.projectService.getById(request.getProjectId());
    if (this.componentService.existsByProjectAndName(project.getId(), request.getName())) {
      throw AppConflictException.duplicateName("Component", request.getName());
    }
    final ComponentEntity entity = new ComponentEntity();
    this.componentMapper.updateEntity(entity, request);
    normalise(entity);
    entity.setProject(project);
    final ComponentEntity saved = this.componentService.save(entity);
    this.applyUpgradeStops(saved, request.getUpgradeStops());
    this.applyPackages(saved, request.getPackages());
    return this.toDtos(List.of(saved)).getFirst();
  }

  @Nonnull
  @Transactional(rollbackFor = Throwable.class)
  public ComponentDto update(@Nonnull final UUID id, @Nonnull final ComponentRequest request) {
    final ComponentEntity entity = this.componentService.getById(id);
    if (nonEquals(entity.getProjectId(), request.getProjectId())) {
      // Silently ignoring it would answer 200 while the component stayed put.
      throw AppValidationException.projectCannotChange("Component", id);
    }
    if (nonEquals(entity.getName(), request.getName())
        && this.componentService.existsByProjectAndName(entity.getProjectId(), request.getName())) {
      throw AppConflictException.duplicateName("Component", request.getName());
    }
    this.componentMapper.updateEntity(entity, request);
    normalise(entity);
    final ComponentEntity saved = this.componentService.save(entity);
    this.applyUpgradeStops(saved, request.getUpgradeStops());
    this.applyPackages(saved, request.getPackages());
    return this.toDtos(List.of(saved)).getFirst();
  }

  /** Takes the version records with it; the package rows stay until the component is removed for good. */
  @Transactional(rollbackFor = Throwable.class)
  public void delete(@Nonnull final UUID id) {
    final ComponentEntity component = this.componentService.getById(id);
    this.componentEnvironmentService.findAllByComponent(id).forEach(this.componentEnvironmentService::delete);
    this.componentService.delete(component);
  }

  /**
   * Null leaves the stored packages alone, a list replaces them. An empty list is
   * therefore how a component loses its packages — distinct from "not mentioned".
   */
  private void applyPackages(@Nonnull final ComponentEntity component,
                             @Nullable final List<ComponentPackageRequest> requested) {
    if (isNull(requested)) {
      return;
    }
    final List<ComponentPackageEntity> packages = new ArrayList<>();
    final Set<String> names = new HashSet<>();
    int position = 0;
    for (final ComponentPackageRequest entry : requested) {
      if (!names.add(entry.getName())) {
        // Otherwise this reaches the unique index and answers 500, one level below
        // the component name that is already guarded.
        throw AppValidationException.duplicatePackage(entry.getName());
      }
      final ComponentPackageEntity entity = new ComponentPackageEntity();
      entity.setName(entry.getName());
      entity.setSortOrder(nonNull(entry.getSortOrder()) ? entry.getSortOrder() : position);
      entity.setLeadingPackage(nonNull(entry.getLeadingPackage()) ? entry.getLeadingPackage() : Boolean.FALSE);
      packages.add(entity);
      position++;
    }
    this.markLeadingPackage(packages);
    this.componentService.replacePackages(component, packages);
  }

  /**
   * Null leaves the stored stops alone, a list replaces them — same shape as the
   * package list, because they are edited in the same form.
   */
  /**
   * A cleared text field arrives as an empty string, and an empty string is not
   * a product. Left as it is, the end-of-life job would pick this component on
   * every tick, do nothing, write no row — and no other component would ever be
   * looked up again.
   */
  private static void normalise(@Nonnull final ComponentEntity component) {
    if (!nonBlank(component.getEolProduct())) {
      component.setEolProduct(null);
    }
    if (!nonBlank(component.getEolCycle())) {
      component.setEolCycle(null);
    }
  }

  private void applyUpgradeStops(@Nonnull final ComponentEntity component,
                                 @Nullable final List<UpgradeStopRequest> requested) {
    if (isNull(requested)) {
      return;
    }
    final List<UpgradeStopEntity> stops = new ArrayList<>();
    final Set<String> versions = new HashSet<>();
    for (final UpgradeStopRequest entry : requested) {
      if (!versions.add(entry.getVersion())) {
        // Otherwise this reaches the unique index and answers 500.
        throw AppValidationException.duplicateUpgradeStop(entry.getVersion());
      }
      final UpgradeStopEntity stop = new UpgradeStopEntity();
      stop.setVersion(entry.getVersion());
      stop.setNote(entry.getNote());
      stops.add(stop);
    }
    this.upgradeStopService.replaceStops(component, stops);
  }

  /**
   * The matrix cell of a multi package component shows the leading one, and the
   * version dialog writes it there. Without a package carrying the flag the cell
   * would stay empty for good, however many versions are recorded — so the first
   * package in the configured order takes the role until someone picks another.
   */
  private void markLeadingPackage(@Nonnull final List<ComponentPackageEntity> packages) {
    if (packages.isEmpty() || packages.stream().anyMatch(ComponentPackageEntity::isLeadingPackage)) {
      return;
    }
    packages.stream()
            .min(Comparator.comparing(ComponentPackageEntity::getSortOrder))
            .ifPresent(entry -> entry.setLeadingPackage(Boolean.TRUE));
  }

  /** One query for the packages of all components instead of one per component. */
  @Nonnull
  private List<ComponentDto> toDtos(@Nonnull final List<ComponentEntity> entities) {
    final Map<UUID, List<ComponentPackageEntity>> packages =
        this.componentService.findPackages(entities.stream().map(ComponentEntity::getId).toList()).stream()
            .collect(Collectors.groupingBy(ComponentPackageEntity::getComponentId));

    final Map<UUID, List<UpgradeStopEntity>> stops =
        this.upgradeStopService.findAllByComponents(entities.stream().map(ComponentEntity::getId).toList()).stream()
            .collect(Collectors.groupingBy(UpgradeStopEntity::getComponentId));

    return entities.stream().map(entity -> {
      final ComponentDto dto = this.componentMapper.toDto(entity);
      dto.setPackages(this.componentMapper.toPackageDtos(packages.getOrDefault(entity.getId(), List.of())));
      dto.setUpgradeStops(this.upgradeStopMapper.toDtos(stops.getOrDefault(entity.getId(), List.of())));
      return dto;
    }).toList();
  }
}
