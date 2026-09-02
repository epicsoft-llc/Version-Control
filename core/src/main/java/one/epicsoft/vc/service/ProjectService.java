package one.epicsoft.vc.service;

import jakarta.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import one.epicsoft.vc.exception.AppNotFoundException;
import one.epicsoft.vc.jpa.entity.ProjectEntity;
import one.epicsoft.vc.jpa.repository.ProjectRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectService {

  private final ProjectRepository projectRepository;

  @Nonnull
  public ProjectEntity getById(@Nonnull final UUID id) {
    return this.projectRepository.findByIdAndDeletedIsNull(id)
                                 .orElseThrow(() -> AppNotFoundException.byId("Project", id));
  }

  @Nonnull
  public List<ProjectEntity> findAll() {
    return this.projectRepository.findAllByDeletedIsNullOrderBySortOrderAscNameAsc();
  }

  public boolean existsByName(@Nonnull final String name) {
    return this.projectRepository.existsByNameAndDeletedIsNull(name);
  }

  @Nonnull
  public ProjectEntity save(@Nonnull final ProjectEntity entity) {
    return this.projectRepository.save(entity);
  }

  public void delete(@Nonnull final ProjectEntity entity) {
    entity.delete();
    this.projectRepository.save(entity);
    log.debug("Soft deleted project '{}'", entity.getId());
  }
}
