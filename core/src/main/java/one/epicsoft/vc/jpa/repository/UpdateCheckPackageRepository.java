package one.epicsoft.vc.jpa.repository;

import jakarta.annotation.Nonnull;
import one.epicsoft.vc.jpa.entity.UpdateCheckPackageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface UpdateCheckPackageRepository extends JpaRepository<UpdateCheckPackageEntity, UUID> {

  List<UpdateCheckPackageEntity> findAllByUpdateCheckIdOrderByPackageNameAsc(@Nonnull UUID updateCheckId);

  /** One query for a whole project instead of one per component. */
  List<UpdateCheckPackageEntity> findAllByUpdateCheckIdInOrderByPackageNameAsc(@Nonnull Collection<UUID> updateCheckIds);

  void deleteAllByUpdateCheckId(@Nonnull UUID updateCheckId);
}
