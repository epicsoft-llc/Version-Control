package one.epicsoft.vc.jpa.repository;

import jakarta.annotation.Nonnull;
import one.epicsoft.vc.jpa.entity.NotificationEntryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationEntryRepository extends JpaRepository<NotificationEntryEntity, UUID> {

  List<NotificationEntryEntity> findAllByProjectId(@Nonnull UUID projectId);

  void deleteAllByProjectIdAndEntryKeyIn(@Nonnull UUID projectId, @Nonnull Collection<String> entryKeys);
}
