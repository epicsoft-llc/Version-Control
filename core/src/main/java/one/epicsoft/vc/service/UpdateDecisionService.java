package one.epicsoft.vc.service;

import jakarta.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import one.epicsoft.vc.jpa.entity.UpdateDecisionEntity;
import one.epicsoft.vc.jpa.repository.UpdateDecisionRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UpdateDecisionService {

  private final UpdateDecisionRepository updateDecisionRepository;

  @Nonnull
  public Optional<UpdateDecisionEntity> findByComponent(@Nonnull final UUID componentId) {
    return this.updateDecisionRepository.findByComponentId(componentId);
  }

  @Nonnull
  public List<UpdateDecisionEntity> findAllByProject(@Nonnull final UUID projectId) {
    return this.updateDecisionRepository.findAllByProject(projectId);
  }

  @Nonnull
  public UpdateDecisionEntity save(@Nonnull final UpdateDecisionEntity entity) {
    return this.updateDecisionRepository.save(entity);
  }
}
