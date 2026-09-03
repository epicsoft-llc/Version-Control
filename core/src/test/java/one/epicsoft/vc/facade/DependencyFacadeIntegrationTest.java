package one.epicsoft.vc.facade;

import one.epicsoft.vc.AbstractIntegrationTest;
import one.epicsoft.vc.dto.ComponentDependencyRequest;
import one.epicsoft.vc.dto.ComponentDto;
import one.epicsoft.vc.dto.ComponentEnvironmentRequest;
import one.epicsoft.vc.dto.ComponentRequest;
import one.epicsoft.vc.dto.DependencyCheckDto;
import one.epicsoft.vc.dto.EnvironmentDto;
import one.epicsoft.vc.dto.EnvironmentRequest;
import one.epicsoft.vc.dto.ProjectRequest;
import one.epicsoft.vc.enums.ConstraintState;
import one.epicsoft.vc.enums.SourceType;
import one.epicsoft.vc.exception.AppValidationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Dependencies, evaluated per environment — which is the whole point: production
 * on PostgreSQL 16 and staging on 17 give the same dependency two different
 * answers, and a single verdict would be wrong in one of the two places.
 */
@Transactional
class DependencyFacadeIntegrationTest extends AbstractIntegrationTest {

  @Autowired
  private ProjectFacade projectFacade;

  @Autowired
  private EnvironmentFacade environmentFacade;

  @Autowired
  private ComponentFacade componentFacade;

  @Autowired
  private ComponentEnvironmentFacade componentEnvironmentFacade;

  @Autowired
  private DependencyFacade dependencyFacade;

  @Test
  void answersTheSameDependencyDifferentlyPerEnvironment() {
    final UUID projectId = this.project();
    final ComponentDto database = this.component(projectId, "PostgreSQL");
    final ComponentDto app = this.component(projectId, "Zitadel");
    final EnvironmentDto production = this.environment(projectId, "Production", true);
    final EnvironmentDto staging = this.environment(projectId, "Staging", false);
    this.cell(database.getId(), production.getId(), "16.4");
    this.cell(database.getId(), staging.getId(), "18.0");
    this.cell(app.getId(), production.getId(), "3.1.0");
    this.cell(app.getId(), staging.getId(), "3.1.0");
    this.dependsOn(app, database, ">=14 <18");

    final List<DependencyCheckDto> checks = this.dependencyFacade.checkComponent(app.getId());

    assertEquals(ConstraintState.SATISFIED, this.forEnvironment(checks, production.getId()).getState());
    // Staging is already outside the supported combination, production is not.
    assertEquals(ConstraintState.VIOLATED, this.forEnvironment(checks, staging.getId()).getState());
    assertEquals("18.0", this.forEnvironment(checks, staging.getId()).getActualVersion());
  }

  @Test
  void saysUnknownRatherThanSatisfiedForAVersionItCannotRead() {
    final UUID projectId = this.project();
    final ComponentDto database = this.component(projectId, "PostgreSQL");
    final ComponentDto app = this.component(projectId, "Zitadel");
    final EnvironmentDto production = this.environment(projectId, "Production", true);
    // latest carries no version at all — the case nobody checked by hand either,
    // and passing it silently is how an unsupported combination runs for a year.
    this.cell(database.getId(), production.getId(), "latest");
    this.cell(app.getId(), production.getId(), "3.1.0");
    this.dependsOn(app, database, ">=14 <18");

    assertEquals(ConstraintState.UNKNOWN,
                 this.forEnvironment(this.dependencyFacade.checkComponent(app.getId()), production.getId()).getState());
  }

  @Test
  void readsTheVersionOutOfATagThatAlsoNamesAVariant() {
    final UUID projectId = this.project();
    final ComponentDto database = this.component(projectId, "PostgreSQL");
    final ComponentDto app = this.component(projectId, "Zitadel");
    final EnvironmentDto production = this.environment(projectId, "Production", true);
    this.cell(database.getId(), production.getId(), "16.4-alpine");
    this.cell(app.getId(), production.getId(), "3.1.0");
    this.dependsOn(app, database, ">=14 <18");

    // The variant says nothing about the version. Reporting unknown here would
    // leave a violation nobody can clear and block every upgrade behind it.
    assertEquals(ConstraintState.SATISFIED,
                 this.forEnvironment(this.dependencyFacade.checkComponent(app.getId()), production.getId()).getState());
  }

  @Test
  void saysUnknownWhereThePrerequisiteDoesNotRunAtAll() {
    final UUID projectId = this.project();
    final ComponentDto database = this.component(projectId, "PostgreSQL");
    final ComponentDto app = this.component(projectId, "Zitadel");
    final EnvironmentDto production = this.environment(projectId, "Production", true);
    this.cell(app.getId(), production.getId(), "3.1.0");
    this.dependsOn(app, database, ">=14");

    assertEquals(ConstraintState.UNKNOWN,
                 this.forEnvironment(this.dependencyFacade.checkComponent(app.getId()), production.getId()).getState());
  }

  @Test
  void namesWhatBlocksAProposedTarget() {
    final UUID projectId = this.project();
    final ComponentDto database = this.component(projectId, "PostgreSQL");
    final ComponentDto app = this.component(projectId, "Zitadel");
    final EnvironmentDto production = this.environment(projectId, "Production", true);
    this.cell(database.getId(), production.getId(), "16.4");
    this.cell(app.getId(), production.getId(), "3.1.0");
    this.dependsOn(app, database, ">=14 <18");

    final List<DependencyCheckDto> blockers =
      this.dependencyFacade.blockers(database.getId(), production.getId(), "18.0");

    // "Why can I not just do this" is answered by name, not left to memory.
    assertEquals(1, blockers.size());
    assertEquals("Zitadel", blockers.getFirst().getComponentName());
    assertEquals(">=14 <18", blockers.getFirst().getVersionRange());
    assertEquals(ConstraintState.VIOLATED, blockers.getFirst().getState());
  }

  @Test
  void blocksNothingWhenTheTargetStaysInsideEveryRange() {
    final UUID projectId = this.project();
    final ComponentDto database = this.component(projectId, "PostgreSQL");
    final ComponentDto app = this.component(projectId, "Zitadel");
    final EnvironmentDto production = this.environment(projectId, "Production", true);
    this.cell(database.getId(), production.getId(), "16.4");
    this.cell(app.getId(), production.getId(), "3.1.0");
    this.dependsOn(app, database, ">=14 <18");

    assertTrue(this.dependencyFacade.blockers(database.getId(), production.getId(), "17.6").isEmpty());
  }

  @Test
  void blocksNothingWhereTheDependentDoesNotRun() {
    final UUID projectId = this.project();
    final ComponentDto database = this.component(projectId, "PostgreSQL");
    final ComponentDto app = this.component(projectId, "Zitadel");
    final EnvironmentDto production = this.environment(projectId, "Production", true);
    final EnvironmentDto development = this.environment(projectId, "Development", false);
    this.cell(database.getId(), production.getId(), "16.4");
    this.cell(database.getId(), development.getId(), "16.4");
    // The dependent runs in production only.
    this.cell(app.getId(), production.getId(), "3.1.0");
    this.dependsOn(app, database, ">=14 <18");

    assertEquals(1, this.dependencyFacade.blockers(database.getId(), production.getId(), "18.0").size());
    // A dependent that lives in production says nothing about an upgrade in dev.
    assertTrue(this.dependencyFacade.blockers(database.getId(), development.getId(), "18.0").isEmpty());
  }

  @Test
  void forgetsADependencyWhenItsComponentIsDeleted() {
    final UUID projectId = this.project();
    final ComponentDto database = this.component(projectId, "PostgreSQL");
    final ComponentDto app = this.component(projectId, "Zitadel");
    final EnvironmentDto production = this.environment(projectId, "Production", true);
    this.cell(database.getId(), production.getId(), "16.4");
    this.cell(app.getId(), production.getId(), "3.1.0");
    this.dependsOn(app, database, ">=14 <18");

    this.componentFacade.delete(app.getId());

    // Otherwise a component nobody can open any more blocks every upgrade of
    // the one it used to need, for good.
    assertTrue(this.dependencyFacade.blockers(database.getId(), production.getId(), "18.0").isEmpty());
    assertTrue(this.dependencyFacade.checkDependents(database.getId()).isEmpty());
  }

  @Test
  void namesTheEnvironmentOnEveryResult() {
    final UUID projectId = this.project();
    final ComponentDto database = this.component(projectId, "PostgreSQL");
    final ComponentDto app = this.component(projectId, "Zitadel");
    final EnvironmentDto production = this.environment(projectId, "Production", true);
    this.cell(database.getId(), production.getId(), "16.4");
    this.cell(app.getId(), production.getId(), "3.1.0");
    this.dependsOn(app, database, ">=14 <18");

    // Without the name the task list carries rows nobody can tell apart.
    assertEquals("Production",
                 this.dependencyFacade.checkComponent(app.getId()).getFirst().getEnvironmentName());
  }

  @Test
  void refusesARangeNothingCanRead() {
    final UUID projectId = this.project();
    final ComponentDto database = this.component(projectId, "PostgreSQL");
    final ComponentDto app = this.component(projectId, "Zitadel");

    // Stored unchecked it would block every upgrade of the prerequisite for
    // good, with nothing but a detail string to explain why.
    assertThrows(AppValidationException.class, () -> this.dependsOn(app, database, ">= 14, < 18"));
  }

  @Test
  void refusesADependencyOnItselfAndOneAcrossProjects() {
    final UUID projectId = this.project();
    final ComponentDto app = this.component(projectId, "Zitadel");
    final ComponentDto elsewhere = this.component(this.project(), "PostgreSQL");

    assertThrows(AppValidationException.class, () -> this.dependsOn(app, app, ">=1"));
    // A dependency across projects would be evaluated against environments that
    // do not exist on the other side.
    assertThrows(AppValidationException.class, () -> this.dependsOn(app, elsewhere, ">=14"));
  }

  private DependencyCheckDto forEnvironment(final List<DependencyCheckDto> checks, final UUID environmentId) {
    return checks.stream()
                 .filter(check -> environmentId.equals(check.getEnvironmentId()))
                 .findFirst()
                 .orElseThrow(() -> new AssertionError("no check for environment " + environmentId));
  }

  private void dependsOn(final ComponentDto component, final ComponentDto dependsOn, final String range) {
    final ComponentDependencyRequest dependency = new ComponentDependencyRequest();
    dependency.setDependsOnId(dependsOn.getId());
    dependency.setVersionRange(range);
    dependency.setNote("Read in the release notes");

    final ComponentRequest request = this.componentRequest(component.getProjectId(), component.getName());
    request.setDependencies(List.of(dependency));
    this.componentFacade.update(component.getId(), request);
  }

  private ComponentDto component(final UUID projectId, final String name) {
    return this.componentFacade.create(this.componentRequest(projectId, name));
  }

  private void cell(final UUID componentId, final UUID environmentId, final String version) {
    final ComponentEnvironmentRequest request = new ComponentEnvironmentRequest();
    request.setComponentId(componentId);
    request.setEnvironmentId(environmentId);
    request.setCurrentVersion(version);
    this.componentEnvironmentFacade.createOrUpdate(request);
  }

  private UUID project() {
    final ProjectRequest request = new ProjectRequest();
    request.setName("Project " + UUID.randomUUID());
    return this.projectFacade.create(request).getId();
  }

  private EnvironmentDto environment(final UUID projectId, final String name, final boolean production) {
    final EnvironmentRequest request = new EnvironmentRequest();
    request.setProjectId(projectId);
    request.setName(name);
    request.setProduction(production);
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
