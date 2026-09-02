package one.epicsoft.vc.facade;

import jakarta.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import one.epicsoft.vc.dto.ProjectDto;
import one.epicsoft.vc.dto.ProjectRequest;
import one.epicsoft.vc.exception.AppConflictException;
import one.epicsoft.vc.jpa.entity.ComponentEntity;
import one.epicsoft.vc.jpa.entity.EnvironmentEntity;
import one.epicsoft.vc.jpa.entity.ProjectEntity;
import one.epicsoft.vc.mapper.ProjectMapper;
import one.epicsoft.vc.service.ComponentEnvironmentService;
import one.epicsoft.vc.service.ComponentService;
import one.epicsoft.vc.service.EnvironmentService;
import one.epicsoft.vc.service.ProjectService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

import static one.epicsoft.vc.util.VcUtil.nonEquals;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectFacade {

  private final ProjectService projectService;
  private final EnvironmentService environmentService;
  private final ComponentService componentService;
  private final ComponentEnvironmentService componentEnvironmentService;
  private final ProjectMapper projectMapper;

  @Nonnull
  @Transactional(readOnly = true, rollbackFor = Throwable.class)
  public List<ProjectDto> findAll() {
    return this.projectService.findAll().stream().map(this.projectMapper::toDto).toList();
  }

  @Nonnull
  @Transactional(readOnly = true, rollbackFor = Throwable.class)
  public ProjectDto getById(@Nonnull final UUID id) {
    return this.projectMapper.toDto(this.projectService.getById(id));
  }

  @Nonnull
  @Transactional(rollbackFor = Throwable.class)
  public ProjectDto create(@Nonnull final ProjectRequest request) {
    if (this.projectService.existsByName(request.getName())) {
      throw AppConflictException.duplicateName("Project", request.getName());
    }
    final ProjectEntity entity = new ProjectEntity();
    this.projectMapper.updateEntity(entity, request);
    return this.projectMapper.toDto(this.projectService.save(entity));
  }

  @Nonnull
  @Transactional(rollbackFor = Throwable.class)
  public ProjectDto update(@Nonnull final UUID id, @Nonnull final ProjectRequest request) {
    final ProjectEntity entity = this.projectService.getById(id);
    if (nonEquals(entity.getName(), request.getName()) && this.projectService.existsByName(request.getName())) {
      throw AppConflictException.duplicateName("Project", request.getName());
    }
    this.projectMapper.updateEntity(entity, request);
    return this.projectMapper.toDto(this.projectService.save(entity));
  }

  /**
   * Soft deletes the project and everything below it. Without the cascade the
   * children stay reachable through their own endpoints and the interface offers
   * components of a project that no longer exists.
   */
  @Transactional(rollbackFor = Throwable.class)
  public void delete(@Nonnull final UUID id) {
    final ProjectEntity project = this.projectService.getById(id);

    for (final ComponentEntity component : this.componentService.findAllByProject(id)) {
      this.componentEnvironmentService.findAllByComponent(component.getId())
                                      .forEach(this.componentEnvironmentService::delete);
      this.componentService.delete(component);
    }
    for (final EnvironmentEntity environment : this.environmentService.findAllByProject(id)) {
      this.environmentService.delete(environment);
    }
    this.projectService.delete(project);
  }
}
