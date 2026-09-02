package one.epicsoft.vc.facade;

import jakarta.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import one.epicsoft.vc.dto.EnvironmentDto;
import one.epicsoft.vc.dto.EnvironmentRequest;
import one.epicsoft.vc.exception.AppConflictException;
import one.epicsoft.vc.exception.AppValidationException;
import one.epicsoft.vc.jpa.entity.EnvironmentEntity;
import one.epicsoft.vc.jpa.entity.ProjectEntity;
import one.epicsoft.vc.mapper.EnvironmentMapper;
import one.epicsoft.vc.service.ComponentEnvironmentService;
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
public class EnvironmentFacade {

  private final EnvironmentService environmentService;
  private final ProjectService projectService;
  private final ComponentEnvironmentService componentEnvironmentService;
  private final EnvironmentMapper environmentMapper;

  @Nonnull
  @Transactional(readOnly = true, rollbackFor = Throwable.class)
  public List<EnvironmentDto> findAllByProject(@Nonnull final UUID projectId) {
    return this.environmentService.findAllByProject(projectId).stream().map(this.environmentMapper::toDto).toList();
  }

  @Nonnull
  @Transactional(readOnly = true, rollbackFor = Throwable.class)
  public EnvironmentDto getById(@Nonnull final UUID id) {
    return this.environmentMapper.toDto(this.environmentService.getById(id));
  }

  @Nonnull
  @Transactional(rollbackFor = Throwable.class)
  public EnvironmentDto create(@Nonnull final EnvironmentRequest request) {
    final ProjectEntity project = this.projectService.getById(request.getProjectId());
    if (this.environmentService.existsByProjectAndName(project.getId(), request.getName())) {
      throw AppConflictException.duplicateName("Environment", request.getName());
    }
    final EnvironmentEntity entity = new EnvironmentEntity();
    this.environmentMapper.updateEntity(entity, request);
    entity.setProject(project);
    return this.environmentMapper.toDto(this.environmentService.save(entity));
  }

  @Nonnull
  @Transactional(rollbackFor = Throwable.class)
  public EnvironmentDto update(@Nonnull final UUID id, @Nonnull final EnvironmentRequest request) {
    final EnvironmentEntity entity = this.environmentService.getById(id);
    if (nonEquals(entity.getProjectId(), request.getProjectId())) {
      // Answering 200 while the environment stayed put would describe a move that
      // did not happen — and the duplicate check below would ask the wrong project.
      throw AppValidationException.projectCannotChange("Environment", id);
    }
    if (nonEquals(entity.getName(), request.getName())
        && this.environmentService.existsByProjectAndName(entity.getProjectId(), request.getName())) {
      throw AppConflictException.duplicateName("Environment", request.getName());
    }
    this.environmentMapper.updateEntity(entity, request);
    return this.environmentMapper.toDto(this.environmentService.save(entity));
  }

  /** Takes the version records of that environment with it; otherwise they hang off a column nobody lists. */
  @Transactional(rollbackFor = Throwable.class)
  public void delete(@Nonnull final UUID id) {
    final EnvironmentEntity environment = this.environmentService.getById(id);
    this.componentEnvironmentService.findAllByEnvironment(id).forEach(this.componentEnvironmentService::delete);
    this.environmentService.delete(environment);
  }
}
