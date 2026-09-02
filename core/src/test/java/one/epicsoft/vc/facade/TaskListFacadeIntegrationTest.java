package one.epicsoft.vc.facade;

import one.epicsoft.vc.AbstractIntegrationTest;
import one.epicsoft.vc.dto.ComponentDto;
import one.epicsoft.vc.dto.ComponentEnvironmentRequest;
import one.epicsoft.vc.dto.ComponentRequest;
import one.epicsoft.vc.dto.EnvironmentDto;
import one.epicsoft.vc.dto.EnvironmentRequest;
import one.epicsoft.vc.dto.ProjectRequest;
import one.epicsoft.vc.dto.TaskListDto;
import one.epicsoft.vc.dto.TaskSectionDto;
import one.epicsoft.vc.dto.UpdateDecisionRequest;
import one.epicsoft.vc.enums.CheckState;
import one.epicsoft.vc.enums.DecisionState;
import one.epicsoft.vc.enums.SourceType;
import one.epicsoft.vc.enums.TaskSection;
import one.epicsoft.vc.exception.AppValidationException;
import one.epicsoft.vc.jpa.entity.ComponentEntity;
import one.epicsoft.vc.jpa.entity.UpdateCheckEntity;
import one.epicsoft.vc.service.ComponentService;
import one.epicsoft.vc.service.UpdateCheckService;
import one.epicsoft.vc.service.UpdateDecisionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The list that answers "what has to be done", and the decisions that keep
 * entries off it — for exactly as long as they are about the version that is on
 * the table.
 */
@Transactional
class TaskListFacadeIntegrationTest extends AbstractIntegrationTest {

  @Autowired
  private ProjectFacade projectFacade;

  @Autowired
  private EnvironmentFacade environmentFacade;

  @Autowired
  private ComponentFacade componentFacade;

  @Autowired
  private ComponentEnvironmentFacade componentEnvironmentFacade;

  @Autowired
  private UpdateDecisionFacade updateDecisionFacade;

  @Autowired
  private TaskListFacade taskListFacade;

  @Autowired
  private UpdateCheckService updateCheckService;

  @Autowired
  private UpdateDecisionService updateDecisionService;

  @Autowired
  private ComponentService componentService;

  @Test
  void listsAnAvailableUpdateNobodyDecidedAbout() {
    final UUID projectId = this.project();
    final ComponentDto component = this.componentWithVersion(projectId, "17.2");
    this.recordCheck(component, "17.4", CheckState.OK, Instant.now());

    final TaskListDto list = this.taskListFacade.getByProject(projectId);

    final TaskSectionDto section = this.section(list, TaskSection.UPDATE_OPEN);
    assertEquals(1, section.getEntries().size());
    assertEquals("17.2", section.getEntries().getFirst().getCurrentVersion());
    assertEquals("17.4", section.getEntries().getFirst().getLatestVersion());
  }

  @Test
  void takesAPlannedUpdateOffTheList() {
    final UUID projectId = this.project();
    final ComponentDto component = this.componentWithVersion(projectId, "17.2");
    this.recordCheck(component, "17.4", CheckState.OK, Instant.now());
    this.decide(component.getId(), DecisionState.PLANNED, null);

    // A decision is an answer. Leaving it on the list would train people to
    // ignore the list.
    assertTrue(this.sections(this.taskListFacade.getByProject(projectId)).stream()
                   .noneMatch(section -> TaskSection.UPDATE_OPEN.equals(section.getSection())));
  }

  @Test
  void bringsBackAPostponementWhoseDateHasPassed() {
    final UUID projectId = this.project();
    final ComponentDto component = this.componentWithVersion(projectId, "17.2");
    this.recordCheck(component, "17.4", CheckState.OK, Instant.now());
    this.decide(component.getId(), DecisionState.POSTPONED, Instant.now().plus(7, ChronoUnit.DAYS));
    // Postponing into the past is refused, so the date is moved instead — which
    // is what the passing of a week does anyway.
    this.expireSnooze(component.getId());

    assertEquals(1, this.section(this.taskListFacade.getByProject(projectId), TaskSection.SNOOZE_EXPIRED)
                        .getEntries().size());
  }

  @Test
  void reopensADecisionWhenANewerVersionArrives() {
    final UUID projectId = this.project();
    final ComponentDto component = this.componentWithVersion(projectId, "17.2");
    this.recordCheck(component, "17.4", CheckState.OK, Instant.now());
    this.decide(component.getId(), DecisionState.SKIPPED, null);

    final boolean reopened = this.updateDecisionFacade.reopenIfNewerVersion(component.getId(), "17.5");

    // Otherwise skipping 17.4 quietly skips everything after it as well.
    assertTrue(reopened);
    assertEquals(DecisionState.OPEN,
                 this.updateDecisionService.findByComponent(component.getId()).orElseThrow().getState());
    // The note survives: what somebody wrote about the last version is the most
    // useful thing there is when deciding about the next.
    assertEquals("Not this quarter",
                 this.updateDecisionService.findByComponent(component.getId()).orElseThrow().getNote());
  }

  @Test
  void leavesADecisionAloneWhileItIsAboutTheVersionOnTheTable() {
    final UUID projectId = this.project();
    final ComponentDto component = this.componentWithVersion(projectId, "17.2");
    this.recordCheck(component, "17.4", CheckState.OK, Instant.now());
    this.decide(component.getId(), DecisionState.SKIPPED, null);

    assertFalse(this.updateDecisionFacade.reopenIfNewerVersion(component.getId(), "17.4"));
  }

  @Test
  void waitsOutTheGracePeriodBeforeReportingAFailingCheck() {
    final UUID projectId = this.project();
    final ComponentDto fresh = this.componentWithVersion(projectId, "17.2");
    this.recordCheck(fresh, null, CheckState.ERROR, Instant.now());

    // A single network hiccup is not a task, and a list full of them stops being read.
    assertTrue(this.sections(this.taskListFacade.getByProject(projectId)).stream()
                   .noneMatch(section -> TaskSection.CHECK_ERROR.equals(section.getSection())));
  }

  @Test
  void reportsACheckThatHasBeenBlindForLongerThanTheGracePeriod() {
    final UUID projectId = this.project();
    final ComponentDto component = this.componentWithVersion(projectId, "17.2");
    this.recordCheck(component, null, CheckState.ERROR, Instant.now().minus(10, ChronoUnit.DAYS));

    assertEquals(1, this.section(this.taskListFacade.getByProject(projectId), TaskSection.CHECK_ERROR)
                        .getEntries().size());
  }

  @Test
  void reportsAMovingTagOnlyInProduction() {
    final UUID projectId = this.project();
    final ComponentDto component = this.componentFacade.create(this.componentRequest(projectId, "PostgreSQL"));
    final EnvironmentDto production = this.environment(projectId, "Production", true);
    final EnvironmentDto staging = this.environment(projectId, "Staging", false);
    this.cell(component.getId(), production.getId(), "17-alpine");
    this.cell(component.getId(), staging.getId(), "17-alpine");

    final TaskSectionDto section = this.section(this.taskListFacade.getByProject(projectId),
                                                TaskSection.MOVING_TAG_IN_PRODUCTION);

    // Allowed anywhere, worth a task only where the image can change unnoticed.
    assertEquals(1, section.getEntries().size());
    assertEquals("Production", section.getEntries().getFirst().getEnvironmentName());
  }

  @Test
  void keepsTheSectionsInTheOrderTheyAreRead() {
    final UUID projectId = this.project();
    final ComponentDto behind = this.componentWithVersion(projectId, "17.2");
    this.recordCheck(behind, "17.4", CheckState.OK, Instant.now());
    final ComponentDto failing = this.componentWithVersion(projectId, "1.0");
    this.recordCheck(failing, null, CheckState.ERROR, Instant.now().minus(10, ChronoUnit.DAYS));

    final List<TaskSection> order = this.sections(this.taskListFacade.getByProject(projectId)).stream()
                                        .map(TaskSectionDto::getSection).toList();

    // What is behind comes before what is merely not answering.
    assertEquals(List.of(TaskSection.UPDATE_OPEN, TaskSection.CHECK_ERROR), order);
  }

  @Test
  void keepsAPendingUpdateOnTheListWhileTheCheckIsFailing() {
    final UUID projectId = this.project();
    final ComponentDto component = this.componentWithVersion(projectId, "17.2");
    this.recordCheck(component, "17.6", CheckState.OK, Instant.now());
    // The source answers with a 503 on the next tick; the version it found last
    // time is still what is available.
    this.recordCheck(component, "17.6", CheckState.ERROR, Instant.now());

    // Dropping the entry would leave the start page reporting nothing to do for
    // a component that is two versions behind.
    assertEquals(1, this.section(this.taskListFacade.getByProject(projectId), TaskSection.UPDATE_OPEN)
                        .getEntries().size());
  }

  @Test
  void leavesAMovingTagOutOfTheUpdateSection() {
    final UUID projectId = this.project();
    final ComponentDto component = this.componentFacade.create(this.componentRequest(projectId, "PostgreSQL"));
    final EnvironmentDto staging = this.environment(projectId, "Staging", false);
    this.cell(component.getId(), staging.getId(), "17-alpine");
    this.recordCheck(component, "17.6", CheckState.OK, Instant.now());

    // 17-alpine never equals 17.6, so the entry could never be cleared; the
    // moving tag has a section of its own where it can be acted on.
    assertTrue(this.sections(this.taskListFacade.getByProject(projectId)).stream()
                   .noneMatch(section -> TaskSection.UPDATE_OPEN.equals(section.getSection())));
  }

  @Test
  void doesNotCallAnEnvironmentBehindWhenItIsAhead() {
    final UUID projectId = this.project();
    final ComponentDto component = this.componentWithVersion(projectId, "18.0");
    // A rule pinned to major 17 reports 17.6 as the newest allowed version.
    this.recordCheck(component, "17.6", CheckState.OK, Instant.now());

    assertTrue(this.sections(this.taskListFacade.getByProject(projectId)).stream()
                   .noneMatch(section -> TaskSection.UPDATE_OPEN.equals(section.getSection())));
  }

  @Test
  void listsEveryEnvironmentThatIsBehind() {
    final UUID projectId = this.project();
    final ComponentDto component = this.componentFacade.create(this.componentRequest(projectId, "PostgreSQL"));
    final EnvironmentDto production = this.environment(projectId, "Production", true);
    final EnvironmentDto staging = this.environment(projectId, "Staging", false);
    this.cell(component.getId(), production.getId(), "17.2");
    this.cell(component.getId(), staging.getId(), "17.4");
    this.recordCheck(component, "17.6", CheckState.OK, Instant.now());

    final TaskSectionDto section = this.section(this.taskListFacade.getByProject(projectId), TaskSection.UPDATE_OPEN);

    // Reporting one of them would hide the other and change between two reloads.
    assertEquals(2, section.getEntries().size());
    // What is live comes first.
    assertEquals("Production", section.getEntries().getFirst().getEnvironmentName());
  }

  @Test
  void leavesADecisionAloneWhenTheReportedVersionGotLower() {
    final UUID projectId = this.project();
    final ComponentDto component = this.componentWithVersion(projectId, "17.2");
    this.recordCheck(component, "18.0", CheckState.OK, Instant.now());
    this.decide(component.getId(), DecisionState.SKIPPED, null);

    // Tightening a rule lowers what the check reports; nothing new happened.
    assertFalse(this.updateDecisionFacade.reopenIfNewerVersion(component.getId(), "17.6"));
  }

  @Test
  void refusesAPostponementIntoThePast() {
    final UUID projectId = this.project();
    final ComponentDto component = this.componentWithVersion(projectId, "17.2");

    // Same as no date: the entry is back before the reader has closed the dialog.
    assertThrows(AppValidationException.class,
                 () -> this.decide(component.getId(), DecisionState.POSTPONED,
                                   Instant.now().minus(1, ChronoUnit.HOURS)));
  }

  @Test
  void refusesToSetBlockedByHandAndAPostponementWithoutADate() {
    final UUID projectId = this.project();
    final ComponentDto component = this.componentWithVersion(projectId, "17.2");

    assertThrows(AppValidationException.class,
                 () -> this.decide(component.getId(), DecisionState.BLOCKED, null));
    // A postponement without a date is a note nobody comes back to.
    assertThrows(AppValidationException.class,
                 () -> this.decide(component.getId(), DecisionState.POSTPONED, null));
  }

  private TaskSectionDto section(final TaskListDto list, final TaskSection section) {
    return this.sections(list).stream()
               .filter(entry -> section.equals(entry.getSection()))
               .findFirst()
               .orElseThrow(() -> new AssertionError("section " + section + " is missing"));
  }

  private List<TaskSectionDto> sections(final TaskListDto list) {
    return list.getSections();
  }

  /** Lets the postponement run out, without waiting a week for it. */
  private void expireSnooze(final UUID componentId) {
    final var decision = this.updateDecisionService.findByComponent(componentId).orElseThrow();
    decision.setSnoozeUntil(Instant.now().minus(1, ChronoUnit.DAYS));
    this.updateDecisionService.save(decision);
  }

  private void decide(final UUID componentId, final DecisionState state, final Instant snoozeUntil) {
    final UpdateDecisionRequest request = new UpdateDecisionRequest();
    request.setComponentId(componentId);
    request.setState(state);
    request.setSnoozeUntil(snoozeUntil);
    request.setNote("Not this quarter");
    this.updateDecisionFacade.decide(request);
  }

  /** Writes a check result directly: what the adapters do is tested without a network elsewhere. */
  private void recordCheck(final ComponentDto component, final String latestVersion, final CheckState state,
                           final Instant lastSuccess) {
    final ComponentEntity entity = this.componentService.getById(component.getId());
    final UpdateCheckEntity check = Optional.ofNullable(this.updateCheckService.findByComponent(component.getId())
                                                            .orElse(null))
                                            .orElseGet(UpdateCheckEntity::new);
    check.setComponent(entity);
    check.setCheckState(state);
    check.setLatestVersion(latestVersion);
    check.setCheckedAt(Instant.now());
    check.setLastSuccessfulCheckAt(CheckState.OK.equals(state) ? Instant.now() : lastSuccess);
    check.setErrorMessage(CheckState.ERROR.equals(state) ? "Request failed" : null);
    this.updateCheckService.save(check);
  }

  private ComponentDto componentWithVersion(final UUID projectId, final String version) {
    final ComponentDto component =
      this.componentFacade.create(this.componentRequest(projectId, "Component " + UUID.randomUUID()));
    final EnvironmentDto environment = this.environment(projectId, "Production " + UUID.randomUUID(), true);
    this.cell(component.getId(), environment.getId(), version);
    return component;
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
