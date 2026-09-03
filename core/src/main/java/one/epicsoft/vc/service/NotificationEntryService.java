package one.epicsoft.vc.service;

import jakarta.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import one.epicsoft.vc.jpa.entity.NotificationEntryEntity;
import one.epicsoft.vc.jpa.entity.ProjectEntity;
import one.epicsoft.vc.jpa.repository.NotificationEntryRepository;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationEntryService {

  private final NotificationEntryRepository notificationEntryRepository;

  @Nonnull
  public List<NotificationEntryEntity> findAllByProject(@Nonnull final UUID projectId) {
    return this.notificationEntryRepository.findAllByProjectId(projectId);
  }

  /** Writes down that this was reported, so tomorrow it is not news again. */
  @Nonnull
  public NotificationEntryEntity remember(@Nonnull final ProjectEntity project, @Nonnull final String entryKey,
                                          @Nonnull final Instant reportedAt) {
    final NotificationEntryEntity entry = new NotificationEntryEntity();
    entry.setProject(project);
    entry.setEntryKey(entryKey);
    entry.setReportedAt(reportedAt);
    return this.notificationEntryRepository.save(entry);
  }

  /** Forgets what is no longer true, so that its return is news again. */
  public void forget(@Nonnull final UUID projectId, @Nonnull final Collection<String> entryKeys) {
    if (entryKeys.isEmpty()) {
      return;
    }
    this.notificationEntryRepository.deleteAllByProjectIdAndEntryKeyIn(projectId, entryKeys);
  }
}
