package one.epicsoft.vc.facade;

import one.epicsoft.vc.AbstractIntegrationTest;
import one.epicsoft.vc.dto.ComponentDto;
import one.epicsoft.vc.dto.ComponentEnvironmentDto;
import one.epicsoft.vc.dto.ComponentEnvironmentRequest;
import one.epicsoft.vc.dto.ComponentPackageRequest;
import one.epicsoft.vc.dto.ComponentRequest;
import one.epicsoft.vc.dto.EnvironmentDto;
import one.epicsoft.vc.dto.EnvironmentRequest;
import one.epicsoft.vc.dto.ProjectRequest;
import one.epicsoft.vc.enums.SourceType;
import one.epicsoft.vc.exception.AppValidationException;
import one.epicsoft.vc.jpa.entity.ComponentEnvironmentEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The findings of the second review, each with the case that produced it. */
@Transactional
class GuardSecondRoundIntegrationTest extends AbstractIntegrationTest {

  @Autowired
  private ProjectFacade projectFacade;

  @Autowired
  private EnvironmentFacade environmentFacade;

  @Autowired
  private ComponentFacade componentFacade;

  @Autowired
  private ComponentEnvironmentFacade componentEnvironmentFacade;

  @Test
  void countsAPinnedPrereleaseAsPinned() {
    final ComponentEnvironmentEntity entity = new ComponentEnvironmentEntity();

    entity.setCurrentVersion("2.0.0-rc.1");
    assertFalse(entity.isMovingTag());
    entity.setCurrentVersion("6.4.1-15");
    assertFalse(entity.isMovingTag());
    // A variant on a version that names its minor is pinned as well: the
    // publisher stopped changing 1.29.2-alpine the day it appeared. Reported as
    // moving, every alpine image in production would sit on the task list for
    // good — and a warning that is wrong every day is one nobody reads.
    entity.setCurrentVersion("1.29.2-alpine");
    assertFalse(entity.isMovingTag());
    entity.setCurrentVersion("17.2-alpine");
    assertFalse(entity.isMovingTag());
    // The v of a release tag and the epoch of a Debian version sit in front of
    // the number without being part of it.
    entity.setCurrentVersion("v4.2.1");
    assertFalse(entity.isMovingTag());
    entity.setCurrentVersion("5:29.7.2");
    assertFalse(entity.isMovingTag());

    // Only the major named: that tag gets rebuilt with every release of the line.
    entity.setCurrentVersion("17-alpine");
    assertTrue(entity.isMovingTag());
    entity.setCurrentVersion("17");
    assertTrue(entity.isMovingTag());
    entity.setCurrentVersion("latest");
    assertTrue(entity.isMovingTag());
    entity.setCurrentVersion("stable-slim");
    assertTrue(entity.isMovingTag());
  }

  @Test
  void refusesToMoveAnEnvironmentToAnotherProject() {
    final UUID projectId = this.project();
    final EnvironmentDto environment = this.environment(projectId, "Production");

    final EnvironmentRequest request = new EnvironmentRequest();
    request.setProjectId(this.project());
    request.setName("Production");

    assertThrows(AppValidationException.class, () -> this.environmentFacade.update(environment.getId(), request));
  }

  @Test
  void refusesAPackageListedTwice() {
    final ComponentRequest request = this.componentRequest(this.project(), "Docker");
    request.setSourceType(SourceType.APT_REPOSITORY);
    request.setPackages(List.of(this.packageRequest("docker-ce"), this.packageRequest("docker-ce")));

    assertThrows(AppValidationException.class, () -> this.componentFacade.create(request));
  }

  @Test
  void keepsTheTimestampWhenTheVersionIsOnlyEchoed() {
    final UUID projectId = this.project();
    final ComponentDto component = this.componentFacade.create(this.componentRequest(projectId, "PostgreSQL"));
    final EnvironmentDto environment = this.environment(projectId, "Production");

    final ComponentEnvironmentRequest request = new ComponentEnvironmentRequest();
    request.setComponentId(component.getId());
    request.setEnvironmentId(environment.getId());
    request.setCurrentVersion("17.2");
    final ComponentEnvironmentDto first = this.componentEnvironmentFacade.createOrUpdate(request);

    // Toggling `active` while echoing the version must not move the timestamp
    // away from the deployment it records.
    request.setActive(Boolean.FALSE);
    final ComponentEnvironmentDto second = this.componentEnvironmentFacade.createOrUpdate(request);

    assertEquals(first.getLastChangedAt(), second.getLastChangedAt());
    assertFalse(second.getActive());
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

  private ComponentPackageRequest packageRequest(final String name) {
    final ComponentPackageRequest request = new ComponentPackageRequest();
    request.setName(name);
    return request;
  }
}
