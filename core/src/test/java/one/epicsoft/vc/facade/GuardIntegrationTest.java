package one.epicsoft.vc.facade;

import one.epicsoft.vc.AbstractIntegrationTest;
import one.epicsoft.vc.dto.ComponentDto;
import one.epicsoft.vc.dto.ComponentEnvironmentRequest;
import one.epicsoft.vc.dto.ComponentRequest;
import one.epicsoft.vc.dto.EnvironmentDto;
import one.epicsoft.vc.dto.EnvironmentRequest;
import one.epicsoft.vc.dto.ProjectRequest;
import one.epicsoft.vc.enums.SourceType;
import one.epicsoft.vc.exception.AppConflictException;
import one.epicsoft.vc.exception.AppValidationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The refusals that keep the data honest. */
@Transactional
class GuardIntegrationTest extends AbstractIntegrationTest {

  @Autowired
  private ProjectFacade projectFacade;

  @Autowired
  private EnvironmentFacade environmentFacade;

  @Autowired
  private ComponentFacade componentFacade;

  @Autowired
  private ComponentEnvironmentFacade componentEnvironmentFacade;

  @Test
  void refusesAComponentAndAnEnvironmentFromDifferentProjects() {
    final UUID first = this.project();
    final UUID second = this.project();
    final ComponentDto component = this.componentFacade.create(this.componentRequest(first, "PostgreSQL"));
    final EnvironmentDto environment = this.environment(second, "Production");

    final ComponentEnvironmentRequest request = new ComponentEnvironmentRequest();
    request.setComponentId(component.getId());
    request.setEnvironmentId(environment.getId());

    // Otherwise the matrix of one project carries a column it cannot resolve.
    assertThrows(AppValidationException.class, () -> this.componentEnvironmentFacade.createOrUpdate(request));
  }

  @Test
  void refusesToOverwriteAKnownVersionWithoutHistory() {
    final UUID projectId = this.project();
    final ComponentDto component = this.componentFacade.create(this.componentRequest(projectId, "PostgreSQL"));
    final EnvironmentDto environment = this.environment(projectId, "Production");

    final ComponentEnvironmentRequest first = new ComponentEnvironmentRequest();
    first.setComponentId(component.getId());
    first.setEnvironmentId(environment.getId());
    first.setCurrentVersion("17.2");
    assertEquals("17.2", this.componentEnvironmentFacade.createOrUpdate(first).getCurrentVersion());

    final ComponentEnvironmentRequest second = new ComponentEnvironmentRequest();
    second.setComponentId(component.getId());
    second.setEnvironmentId(environment.getId());
    second.setCurrentVersion("18.0");

    // Changing a recorded version belongs in the dialog that writes the history
    // entry with it; a second way in is how history ends up with holes.
    assertThrows(AppValidationException.class, () -> this.componentEnvironmentFacade.createOrUpdate(second));
  }

  @Test
  void recordsWhenTheFirstVersionWasWrittenDown() {
    final UUID projectId = this.project();
    final ComponentDto component = this.componentFacade.create(this.componentRequest(projectId, "PostgreSQL"));
    final EnvironmentDto environment = this.environment(projectId, "Production");

    final ComponentEnvironmentRequest request = new ComponentEnvironmentRequest();
    request.setComponentId(component.getId());
    request.setEnvironmentId(environment.getId());
    request.setCurrentVersion("17.2");

    assertTrue(this.componentEnvironmentFacade.createOrUpdate(request).getLastChangedAt() != null);
  }

  @Test
  void refusesADuplicateName() {
    final UUID projectId = this.project();
    this.componentFacade.create(this.componentRequest(projectId, "PostgreSQL"));

    // Without the check this reached the unique index and answered 500.
    assertThrows(AppConflictException.class,
                 () -> this.componentFacade.create(this.componentRequest(projectId, "PostgreSQL")));
  }

  @Test
  void refusesToMoveAComponentToAnotherProject() {
    final UUID projectId = this.project();
    final ComponentDto component = this.componentFacade.create(this.componentRequest(projectId, "PostgreSQL"));

    final ComponentRequest request = this.componentRequest(this.project(), "PostgreSQL");

    assertThrows(AppValidationException.class, () -> this.componentFacade.update(component.getId(), request));
  }

  @Test
  void takesTheChildrenAlongWhenAProjectIsDeleted() {
    final UUID projectId = this.project();
    this.componentFacade.create(this.componentRequest(projectId, "PostgreSQL"));
    this.environment(projectId, "Production");

    this.projectFacade.delete(projectId);

    assertTrue(this.componentFacade.findAllByProject(projectId).isEmpty());
    assertTrue(this.environmentFacade.findAllByProject(projectId).isEmpty());
  }

  @Test
  void dropsTheCellsOfADeletedEnvironment() {
    final UUID projectId = this.project();
    final ComponentDto component = this.componentFacade.create(this.componentRequest(projectId, "PostgreSQL"));
    final EnvironmentDto environment = this.environment(projectId, "Staging");

    final ComponentEnvironmentRequest request = new ComponentEnvironmentRequest();
    request.setComponentId(component.getId());
    request.setEnvironmentId(environment.getId());
    this.componentEnvironmentFacade.createOrUpdate(request);

    this.environmentFacade.delete(environment.getId());

    // The details page must not offer a column the environment list no longer has.
    assertTrue(this.componentEnvironmentFacade.findAllByComponent(component.getId()).isEmpty());
  }

  private UUID project() {
    final ProjectRequest request = new ProjectRequest();
    request.setName("Project " + UUID.randomUUID());
    return this.projectFacade.create(request).getId();
  }

  private EnvironmentDto environment(final UUID projectId, final String name) {
    final EnvironmentRequest request = new EnvironmentRequest();
    request.setProjectId(projectId);
    request.setName(name);
    return this.environmentFacade.create(request);
  }

  private ComponentRequest componentRequest(final UUID projectId, final String name) {
    final ComponentRequest request = new ComponentRequest();
    request.setProjectId(projectId);
    request.setName(name);
    request.setSourceType(SourceType.DOCKER_HUB);
    request.setSourceRef("library/postgres");
    return request;
  }
}
