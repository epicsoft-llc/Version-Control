package one.epicsoft.vc.jpa;

import one.epicsoft.vc.AbstractIntegrationTest;
import one.epicsoft.vc.jpa.entity.ProjectEntity;
import one.epicsoft.vc.jpa.repository.ProjectRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Also the place where the Flyway migration is proven to run at all. */
@Transactional
class ProjectRepositoryIntegrationTest extends AbstractIntegrationTest {

  @Autowired
  private ProjectRepository projectRepository;

  @Test
  void assignsIdAndCreatedOnPersist() {
    final ProjectEntity saved = this.projectRepository.save(project("Infrastructure"));

    assertNotNull(saved.getId());
    assertNotNull(saved.getCreated());
    assertFalse(saved.isDeleted());
  }

  @Test
  void findsOnlyWhatIsNotDeleted() {
    final ProjectEntity saved = this.projectRepository.save(project("Alive"));
    final ProjectEntity gone = this.projectRepository.save(project("Gone"));
    gone.delete();
    this.projectRepository.save(gone);

    assertTrue(this.projectRepository.findByIdAndDeletedIsNull(saved.getId()).isPresent());
    assertTrue(this.projectRepository.findByIdAndDeletedIsNull(gone.getId()).isEmpty());
  }

  @Test
  void reusesTheNameOfADeletedProject() {
    // The partial unique index is what allows this; a plain constraint would let
    // the deleted row block the name for good.
    final String name = "Same name " + UUID.randomUUID();
    final ProjectEntity first = this.projectRepository.save(project(name));
    first.delete();
    this.projectRepository.saveAndFlush(first);

    final ProjectEntity second = this.projectRepository.saveAndFlush(project(name));

    assertNotNull(second.getId());
    // Counted by name, not globally: other test classes commit rows of their own.
    assertEquals(1, this.projectRepository.findAllByDeletedIsNullOrderBySortOrderAscNameAsc()
                                          .stream().filter(entry -> name.equals(entry.getName())).count());
  }

  private ProjectEntity project(final String name) {
    final ProjectEntity entity = new ProjectEntity();
    entity.setName(name);
    entity.setSortOrder(0);
    return entity;
  }
}
