package one.epicsoft.vc.service;

import jakarta.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import one.epicsoft.vc.exception.AppNotFoundException;
import one.epicsoft.vc.jpa.entity.EnvironmentEntity;
import one.epicsoft.vc.jpa.repository.EnvironmentRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EnvironmentService {

  private final EnvironmentRepository environmentRepository;

  @Nonnull
  public EnvironmentEntity getById(@Nonnull final UUID id) {
    return this.environmentRepository.findByIdAndDeletedIsNull(id)
                                     .orElseThrow(() -> AppNotFoundException.byId("Environment", id));
  }

  @Nonnull
  public List<EnvironmentEntity> findAllByProject(@Nonnull final UUID projectId) {
    return this.environmentRepository.findAllByProjectIdAndDeletedIsNullOrderBySortOrderAscNameAsc(projectId);
  }

  public boolean existsByProjectAndName(@Nonnull final UUID projectId, @Nonnull final String name) {
    return this.environmentRepository.existsByProjectIdAndNameAndDeletedIsNull(projectId, name);
  }

  @Nonnull
  public EnvironmentEntity save(@Nonnull final EnvironmentEntity entity) {
    return this.environmentRepository.save(entity);
  }

  public void delete(@Nonnull final EnvironmentEntity entity) {
    entity.delete();
    this.environmentRepository.save(entity);
    log.debug("Soft deleted environment '{}'", entity.getId());
  }
}
