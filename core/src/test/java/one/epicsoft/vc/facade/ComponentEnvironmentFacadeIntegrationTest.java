package one.epicsoft.vc.facade;

import one.epicsoft.vc.AbstractIntegrationTest;
import one.epicsoft.vc.dto.ComponentDto;
import one.epicsoft.vc.dto.ComponentEnvironmentDto;
import one.epicsoft.vc.dto.ComponentEnvironmentRequest;
import one.epicsoft.vc.dto.ComponentPackageRequest;
import one.epicsoft.vc.dto.ComponentRequest;
import one.epicsoft.vc.dto.EnvironmentDto;
import one.epicsoft.vc.dto.EnvironmentRequest;
import one.epicsoft.vc.dto.ProjectDto;
import one.epicsoft.vc.dto.ProjectRequest;
import one.epicsoft.vc.enums.SourceType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Transactional
class ComponentEnvironmentFacadeIntegrationTest extends AbstractIntegrationTest {

  @Autowired
  private ProjectFacade projectFacade;

  @Autowired
  private EnvironmentFacade environmentFacade;

  @Autowired
  private ComponentFacade componentFacade;

  @Autowired
  private ComponentEnvironmentFacade componentEnvironmentFacade;

  @Test
  void tellsAnUnknownVersionApartFromAnInactiveCell() {
    final ComponentEnvironmentDto record = this.componentEnvironment(project(), null);

    // Active without a version: the component runs here, nobody wrote down which
    // version — a gap in the matrix.
    assertTrue(record.getActive());
    assertTrue(record.getVersionUnknown());

    final ComponentEnvironmentRequest disable = new ComponentEnvironmentRequest();
    disable.setComponentId(record.getComponentId());
    disable.setEnvironmentId(record.getEnvironmentId());
    disable.setActive(Boolean.FALSE);
    final ComponentEnvironmentDto disabled = this.componentEnvironmentFacade.createOrUpdate(disable);

    // Deliberately not present here, which is a different statement and must not
    // read as a gap.
    assertFalse(disabled.getActive());
    assertFalse(disabled.getVersionUnknown());
    assertEquals(record.getId(), disabled.getId());
  }

  @Test
  void recognisesAMovingTag() {
    final UUID projectId = project();

    assertFalse(this.componentEnvironment(projectId, "17.2").getMovingTag());
    assertTrue(this.componentEnvironment(project(), "17-alpine").getMovingTag());
    assertTrue(this.componentEnvironment(project(), "latest").getMovingTag());
  }

  @Test
  void replacesThePackagesOfAComponent() {
    final UUID projectId = project();
    final ComponentRequest request = componentRequest(projectId, "Docker");
    request.setSourceType(SourceType.APT_REPOSITORY);
    request.setPackages(List.of(packageRequest("docker-ce", true), packageRequest("containerd.io", false)));

    final ComponentDto created = this.componentFacade.create(request);
    assertEquals(2, created.getPackages().size());
    assertTrue(created.getPackages().stream().anyMatch(entry -> Boolean.TRUE.equals(entry.getLeadingPackage())));

    request.setPackages(List.of(packageRequest("docker-ce", true)));
    final ComponentDto updated = this.componentFacade.update(created.getId(), request);

    assertEquals(1, updated.getPackages().size());
    assertEquals("docker-ce", updated.getPackages().getFirst().getName());
  }

  private UUID project() {
    final ProjectRequest request = new ProjectRequest();
    request.setName("Project " + UUID.randomUUID());
    final ProjectDto project = this.projectFacade.create(request);
    return project.getId();
  }

  private ComponentEnvironmentDto componentEnvironment(final UUID projectId, final String version) {
    final EnvironmentRequest environmentRequest = new EnvironmentRequest();
    environmentRequest.setProjectId(projectId);
    environmentRequest.setName("Production");
    environmentRequest.setProduction(Boolean.TRUE);
    final EnvironmentDto environment = this.environmentFacade.create(environmentRequest);

    final ComponentDto component = this.componentFacade.create(componentRequest(projectId, "PostgreSQL"));

    final ComponentEnvironmentRequest request = new ComponentEnvironmentRequest();
    request.setComponentId(component.getId());
    request.setEnvironmentId(environment.getId());
    request.setCurrentVersion(version);
    return this.componentEnvironmentFacade.createOrUpdate(request);
  }

  private ComponentRequest componentRequest(final UUID projectId, final String name) {
    final ComponentRequest request = new ComponentRequest();
    request.setProjectId(projectId);
    request.setName(name);
    request.setSourceType(SourceType.DOCKER_HUB);
    request.setSourceRef("library/postgres");
    return request;
  }

  private ComponentPackageRequest packageRequest(final String name, final boolean leading) {
    final ComponentPackageRequest request = new ComponentPackageRequest();
    request.setName(name);
    request.setLeadingPackage(leading);
    return request;
  }
}
