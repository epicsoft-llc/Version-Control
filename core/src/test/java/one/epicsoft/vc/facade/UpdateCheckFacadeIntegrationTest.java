package one.epicsoft.vc.facade;

import one.epicsoft.vc.AbstractIntegrationTest;
import one.epicsoft.vc.dto.ComponentDto;
import one.epicsoft.vc.dto.ComponentEnvironmentRequest;
import one.epicsoft.vc.dto.ComponentRequest;
import one.epicsoft.vc.dto.EnvironmentDto;
import one.epicsoft.vc.dto.EnvironmentRequest;
import one.epicsoft.vc.dto.ProjectRequest;
import one.epicsoft.vc.dto.VersionPreviewDto;
import one.epicsoft.vc.dto.VersionPreviewRequest;
import one.epicsoft.vc.enums.CheckState;
import one.epicsoft.vc.enums.SourceType;
import one.epicsoft.vc.enums.VersionStrategy;
import one.epicsoft.vc.exception.AppValidationException;
import one.epicsoft.vc.jpa.entity.UpdateCheckEntity;
import one.epicsoft.vc.service.ComponentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The parts of a check that do not need a network: what a source type of MANUAL
 * means, how the minimum interval behaves, and which components the scheduler
 * considers due. The adapters themselves are covered offline in their own test.
 */
@Transactional
class UpdateCheckFacadeIntegrationTest extends AbstractIntegrationTest {

  @Autowired
  private ProjectFacade projectFacade;

  @Autowired
  private EnvironmentFacade environmentFacade;

  @Autowired
  private ComponentFacade componentFacade;

  @Autowired
  private ComponentEnvironmentFacade componentEnvironmentFacade;

  @Autowired
  private UpdateCheckFacade updateCheckFacade;

  @Autowired
  private ComponentService componentService;

  @Test
  void treatsAManualSourceAsARegularCaseRatherThanAFault() {
    final ComponentDto component = this.component(SourceType.MANUAL, null);

    final UpdateCheckEntity check = this.updateCheckFacade.check(component.getId(), false);

    // Nobody publishes this component anywhere; that is a decision, not an error.
    assertEquals(CheckState.NOT_APPLICABLE, check.getCheckState());
    assertNotNull(check.getCheckedAt());
    assertEquals(0, this.componentService.getById(component.getId()).getCheckFailureCount());
  }

  @Test
  void refusesASecondManualCheckInsideTheMinimumInterval() {
    final ComponentDto component = this.component(SourceType.MANUAL, null);
    this.updateCheckFacade.check(component.getId(), true);

    // The bound holds for a person with a button as much as for the scheduler;
    // an exception here is the path by which it stops existing.
    assertThrows(AppValidationException.class, () -> this.updateCheckFacade.checkNow(component.getId()));
  }

  @Test
  void schedulesTheNextCheckNoEarlierThanTheMinimumInterval() {
    final ComponentDto component = this.component(SourceType.MANUAL, null);

    this.updateCheckFacade.check(component.getId(), false);

    final Instant due = this.componentService.getById(component.getId()).getNextCheckDue();
    assertNotNull(due);
    // The jitter may push a check later, never earlier.
    assertTrue(due.isAfter(Instant.now().plusSeconds(47 * 3600)), "next check due at " + due);
  }

  @Test
  void countsAFailureAndBacksOff() {
    // An address that cannot resolve is the cheapest way to reach the failure
    // path without asking a real source anything.
    final ComponentDto component = this.component(SourceType.APT_REPOSITORY, "http://localhost:1/repository");

    final UpdateCheckEntity check = this.updateCheckFacade.check(component.getId(), false);

    assertEquals(CheckState.ERROR, check.getCheckState());
    assertNotNull(check.getErrorMessage());
    assertEquals(1, this.componentService.getById(component.getId()).getCheckFailureCount());
    // The back-off is shorter than the regular interval: a failed check is retried
    // sooner than a successful one is repeated.
    final Instant due = this.componentService.getById(component.getId()).getNextCheckDue();
    assertNotNull(due);
    assertTrue(due.isBefore(Instant.now().plusSeconds(3 * 3600)), "next check due at " + due);
  }

  @Test
  void allowsARetryAfterAFailedCheck() {
    final ComponentDto component = this.component(SourceType.APT_REPOSITORY, "http://localhost:1/repository");
    assertEquals(CheckState.ERROR, this.updateCheckFacade.check(component.getId(), false).getCheckState());

    // A failed check is not an answer. Counting it against the interval would
    // leave the person who just fixed the rule waiting two days to find out.
    assertEquals(CheckState.ERROR, this.updateCheckFacade.checkNow(component.getId()).getCheckState());
  }

  @Test
  void leavesManualAndDisabledComponentsOutOfTheDueList() {
    final UUID projectId = this.project();
    final ComponentDto manual = this.componentFacade.create(this.request(projectId, "Manual", SourceType.MANUAL, null));
    final ComponentRequest disabledRequest = this.request(projectId, "Disabled", SourceType.DOCKER_HUB,
                                                          "library/postgres");
    disabledRequest.setCheckEnabled(Boolean.FALSE);
    final ComponentDto disabled = this.componentFacade.create(disabledRequest);
    final ComponentDto due = this.componentFacade.create(this.request(projectId, "Due", SourceType.DOCKER_HUB,
                                                                      "library/postgres"));

    final List<UUID> ids = this.componentService.findDue(Instant.now(), 100).stream()
                               .map(entity -> entity.getId())
                               .toList();

    assertTrue(ids.contains(due.getId()));
    // Nothing to ask for a manual component, and a disabled check is a decision.
    assertTrue(!ids.contains(manual.getId()));
    assertTrue(!ids.contains(disabled.getId()));
  }

  @Test
  void answersAnUnusableRuleWithAnExplanationRatherThanAnError() {
    final ComponentDto component = this.component(SourceType.APT_REPOSITORY, "http://localhost:1/repository");
    final VersionPreviewRequest request = new VersionPreviewRequest();
    request.setComponentId(component.getId());
    request.setTagPattern("^[");

    final VersionPreviewDto preview = this.updateCheckFacade.previewRule(request);

    // Writing a rule means half finished patterns. Answering those with a server
    // error would make the one screen that exists for writing rules unusable.
    assertTrue(preview.getMatching().isEmpty());
    assertNotNull(preview.getMessage());
    assertNull(preview.getWinner());
  }

  @Test
  void doesNotLetThePreviewAskTheSourceAgainInsideTheInterval() {
    final ComponentDto component = this.component(SourceType.APT_REPOSITORY, "http://localhost:1/repository");
    final VersionPreviewRequest request = new VersionPreviewRequest();
    request.setComponentId(component.getId());

    this.updateCheckFacade.previewRule(request);
    final Instant firstAttempt = this.updateCheckFacade.findAllByComponent(component.getId())
                                     .getFirst().getCheckedAt();
    this.updateCheckFacade.previewRule(request);

    // The second preview reuses what the first one brought back. Otherwise fifty
    // clicks on the preview button are fifty requests to a foreign server.
    assertEquals(firstAttempt, this.updateCheckFacade.findAllByComponent(component.getId())
                                   .getFirst().getCheckedAt());
  }

  @Test
  void saysAManualComponentHasNoSourceRatherThanBlamingTheRule() {
    final ComponentDto component = this.component(SourceType.MANUAL, null);
    final VersionPreviewRequest request = new VersionPreviewRequest();
    request.setComponentId(component.getId());

    final VersionPreviewDto preview = this.updateCheckFacade.previewRule(request);

    // Pointing at the pattern would send the reader looking in the wrong place.
    assertNotNull(preview.getMessage());
    assertTrue(preview.getMatching().isEmpty());
  }

  @Test
  void previewsTheRuleAsItStandsInTheFormRatherThanTheStoredOne() {
    final ComponentDto component = this.component(SourceType.APT_REPOSITORY, "http://localhost:1/repository");
    final VersionPreviewRequest request = new VersionPreviewRequest();
    request.setComponentId(component.getId());
    request.setMajorConstraint("29");
    request.setAllowPrerelease(Boolean.TRUE);
    request.setVersionStrategy(VersionStrategy.SEMVER);

    // The request is accepted with every rule field, not only the pattern; the
    // source itself is unreachable here, so the answer is the explanation.
    assertNotNull(this.updateCheckFacade.previewRule(request).getMessage());
  }

  @Test
  void refusesToCheckAComponentThatDoesNotExist() {
    assertThrows(RuntimeException.class, () -> this.updateCheckFacade.check(UUID.randomUUID(), false));
  }

  private ComponentDto component(final SourceType sourceType, final String sourceRef) {
    final UUID projectId = this.project();
    final ComponentDto component =
      this.componentFacade.create(this.request(projectId, "Component " + UUID.randomUUID(), sourceType, sourceRef));
    final EnvironmentDto environment = this.environment(projectId);

    final ComponentEnvironmentRequest cell = new ComponentEnvironmentRequest();
    cell.setComponentId(component.getId());
    cell.setEnvironmentId(environment.getId());
    cell.setCurrentVersion("17.2");
    this.componentEnvironmentFacade.createOrUpdate(cell);
    return component;
  }

  private ComponentRequest request(final UUID projectId, final String name, final SourceType sourceType,
                                   final String sourceRef) {
    final ComponentRequest request = new ComponentRequest();
    request.setProjectId(projectId);
    request.setName(name);
    request.setSourceType(sourceType);
    request.setSourceRef(sourceRef);
    if (SourceType.APT_REPOSITORY.equals(sourceType)) {
      request.setAptSuite("noble");
      request.setAptComponent("stable");
      request.setAptArchitecture("amd64");
      request.setVersionStrategy(VersionStrategy.DEB_VERSION);
    }
    return request;
  }

  private UUID project() {
    final ProjectRequest request = new ProjectRequest();
    request.setName("Project " + UUID.randomUUID());
    return this.projectFacade.create(request).getId();
  }

  private EnvironmentDto environment(final UUID projectId) {
    final EnvironmentRequest request = new EnvironmentRequest();
    request.setProjectId(projectId);
    request.setName("Production");
    request.setProduction(Boolean.TRUE);
    return this.environmentFacade.create(request);
  }
}
