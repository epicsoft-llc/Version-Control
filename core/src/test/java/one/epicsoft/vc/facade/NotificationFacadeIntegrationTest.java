package one.epicsoft.vc.facade;

import one.epicsoft.vc.AbstractIntegrationTest;
import one.epicsoft.vc.adapter.NotificationChannel;
import one.epicsoft.vc.adapter.SourceException;
import one.epicsoft.vc.adapter.TelegramClient;
import one.epicsoft.vc.data.NotificationMessage;
import one.epicsoft.vc.dto.ComponentDependencyRequest;
import one.epicsoft.vc.dto.ComponentDto;
import one.epicsoft.vc.dto.ComponentEnvironmentRequest;
import one.epicsoft.vc.dto.ComponentRequest;
import one.epicsoft.vc.dto.EnvironmentDto;
import one.epicsoft.vc.dto.EnvironmentRequest;
import one.epicsoft.vc.dto.ProjectRequest;
import one.epicsoft.vc.enums.CheckState;
import one.epicsoft.vc.enums.SourceType;
import one.epicsoft.vc.jpa.entity.UpdateCheckEntity;
import one.epicsoft.vc.job.NotificationJob;
import one.epicsoft.vc.service.ComponentService;
import one.epicsoft.vc.service.UpdateCheckService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The daily message: what is new, once, and nothing at all when there is nothing
 * new. The channel itself is stubbed — what is worth testing is which lines get
 * into the message and which do not.
 *
 * <p>Driven through the job rather than the facade, because the order of collect,
 * send and remember is half of what makes the rule work. The job is built here by
 * hand: the scheduler is switched off in tests, so its bean does not exist.
 */
@Transactional
class NotificationFacadeIntegrationTest extends AbstractIntegrationTest {

  @MockitoBean
  private TelegramClient telegramClient;

  @Autowired
  private ProjectFacade projectFacade;

  @Autowired
  private EnvironmentFacade environmentFacade;

  @Autowired
  private ComponentFacade componentFacade;

  @Autowired
  private ComponentEnvironmentFacade componentEnvironmentFacade;

  @Autowired
  private NotificationFacade notificationFacade;

  @Autowired
  private UpdateCheckService updateCheckService;

  @Autowired
  private ComponentService componentService;

  private NotificationJob notificationJob;

  @BeforeEach
  void configureChannel() {
    when(this.telegramClient.isConfiguredForSending()).thenReturn(true);
    doNothing().when(this.telegramClient).send(any(NotificationMessage.class));
    this.notificationJob = new NotificationJob(this.notificationFacade, List.of(this.telegramClient));
  }

  @Test
  void reportsSomethingNewOnce() {
    final UUID projectId = this.project();
    final ComponentDto component = this.componentWithVersion(projectId, "17.2");
    this.recordCheck(component, "17.4");

    this.notificationJob.sendDailyNotification();
    this.notificationJob.sendDailyNotification();

    // Sent once, and the second morning has nothing to add: the same twelve
    // entries every day are read for a week and ignored after that.
    final ArgumentCaptor<NotificationMessage> sent = ArgumentCaptor.forClass(NotificationMessage.class);
    verify(this.telegramClient).send(sent.capture());
    assertTrue(sent.getValue().body().contains(component.getName()));
  }

  @Test
  void saysNothingWhenThereIsNothingToSay() {
    this.project();

    this.notificationJob.sendDailyNotification();

    // An empty notification teaches people not to look.
    verify(this.telegramClient, never()).send(any(NotificationMessage.class));
  }

  @Test
  void reportsAgainWhenTheSameThingComesBack() {
    final UUID projectId = this.project();
    final ComponentDto component = this.componentWithVersion(projectId, "17.2");
    this.recordCheck(component, "17.4");
    this.notificationJob.sendDailyNotification();

    // The update is carried out, so the entry disappears...
    this.recordCheck(component, "17.2");
    this.notificationJob.sendDailyNotification();
    // ...and when the next version shows up it is news again.
    this.recordCheck(component, "17.6");
    this.notificationJob.sendDailyNotification();

    verify(this.telegramClient, times(2)).send(any(NotificationMessage.class));
  }

  @Test
  void staysSilentWithoutAConfiguredChannel() {
    when(this.telegramClient.isConfiguredForSending()).thenReturn(false);
    final UUID projectId = this.project();
    final ComponentDto component = this.componentWithVersion(projectId, "17.2");
    this.recordCheck(component, "17.4");

    this.notificationJob.sendDailyNotification();

    // Nobody configured a channel, so nobody asked for notifications — that is a
    // decision, not a fault, and it is not worth a complaint every morning.
    verify(this.telegramClient, never()).send(any(NotificationMessage.class));
  }

  @Test
  void keepsTheNewsWhenTheSendFailed() {
    final UUID projectId = this.project();
    final ComponentDto component = this.componentWithVersion(projectId, "17.2");
    this.recordCheck(component, "17.4");
    doThrow(new SourceException("Sending the notification failed")).when(this.telegramClient).send(any(NotificationMessage.class));

    this.notificationJob.sendDailyNotification();

    // Written down before the send, the entry would now be reported for good and
    // the only trace of it would be one line in a log nobody reads.
    doNothing().when(this.telegramClient).send(any(NotificationMessage.class));
    this.notificationJob.sendDailyNotification();

    verify(this.telegramClient, times(2)).send(any(NotificationMessage.class));
  }

  @Test
  void keepsTheNewsWhenOneOfTwoChannelsFailed() {
    final NotificationChannel mail = mock(NotificationChannel.class);
    when(mail.isConfiguredForSending()).thenReturn(true);
    doThrow(new SourceException("Sending the notification by mail failed")).when(mail).send(any(NotificationMessage.class));
    this.notificationJob = new NotificationJob(this.notificationFacade, List.of(this.telegramClient, mail));
    final UUID projectId = this.project();
    final ComponentDto component = this.componentWithVersion(projectId, "17.2");
    this.recordCheck(component, "17.4");

    this.notificationJob.sendDailyNotification();

    // Written down after Telegram alone, the readers of the mail would never hear of it.
    doNothing().when(mail).send(any(NotificationMessage.class));
    this.notificationJob.sendDailyNotification();

    verify(this.telegramClient, times(2)).send(any(NotificationMessage.class));
    verify(mail, times(2)).send(any(NotificationMessage.class));
  }

  @Test
  void reportsBothViolationsOfTheSameComponent() {
    final UUID projectId = this.project();
    final EnvironmentDto production = this.environment(projectId);
    final ComponentDto database = this.component(projectId, "PostgreSQL");
    final ComponentDto broker = this.component(projectId, "Valkey");
    final ComponentDto app = this.component(projectId, "Zitadel");
    this.cell(database.getId(), production.getId(), "18.0");
    this.cell(broker.getId(), production.getId(), "9.0.0");
    this.cell(app.getId(), production.getId(), "3.1.0");
    this.dependsOn(app, List.of(constraint(database, ">=14 <18"), constraint(broker, ">=7 <8")));

    final ArgumentCaptor<NotificationMessage> sent = ArgumentCaptor.forClass(NotificationMessage.class);
    this.notificationJob.sendDailyNotification();

    // Two prerequisites, one component, one environment: told apart only by the
    // dependency itself. A key that cannot do that reports one of them and
    // swallows the other for good.
    verify(this.telegramClient).send(sent.capture());
    assertEquals(2, sent.getValue().body().lines().filter(line -> line.contains(app.getName())).count());
  }

  private void dependsOn(final ComponentDto component, final List<ComponentDependencyRequest> dependencies) {
    final ComponentRequest request = new ComponentRequest();
    request.setProjectId(component.getProjectId());
    request.setName(component.getName());
    request.setSourceType(SourceType.DOCKER_HUB);
    request.setSourceRef("library/postgres");
    request.setDependencies(dependencies);
    this.componentFacade.update(component.getId(), request);
  }

  private static ComponentDependencyRequest constraint(final ComponentDto dependsOn, final String range) {
    final ComponentDependencyRequest dependency = new ComponentDependencyRequest();
    dependency.setDependsOnId(dependsOn.getId());
    dependency.setVersionRange(range);
    return dependency;
  }

  private ComponentDto component(final UUID projectId, final String name) {
    final ComponentRequest request = new ComponentRequest();
    request.setProjectId(projectId);
    request.setName(name + " " + UUID.randomUUID());
    request.setSourceType(SourceType.DOCKER_HUB);
    request.setSourceRef("library/postgres");
    return this.componentFacade.create(request);
  }

  private EnvironmentDto environment(final UUID projectId) {
    final EnvironmentRequest request = new EnvironmentRequest();
    request.setProjectId(projectId);
    request.setName("Production " + UUID.randomUUID());
    request.setProduction(Boolean.TRUE);
    return this.environmentFacade.create(request);
  }

  private void cell(final UUID componentId, final UUID environmentId, final String version) {
    final ComponentEnvironmentRequest request = new ComponentEnvironmentRequest();
    request.setComponentId(componentId);
    request.setEnvironmentId(environmentId);
    request.setCurrentVersion(version);
    this.componentEnvironmentFacade.createOrUpdate(request);
  }

  private void recordCheck(final ComponentDto component, final String latestVersion) {
    final UpdateCheckEntity check = this.updateCheckService.findByComponent(component.getId())
                                        .orElseGet(UpdateCheckEntity::new);
    check.setComponent(this.componentService.getById(component.getId()));
    check.setCheckState(CheckState.OK);
    check.setLatestVersion(latestVersion);
    check.setCheckedAt(Instant.now());
    check.setLastSuccessfulCheckAt(Instant.now());
    this.updateCheckService.save(check);
  }

  private ComponentDto componentWithVersion(final UUID projectId, final String version) {
    final ComponentRequest request = new ComponentRequest();
    request.setProjectId(projectId);
    request.setName("Component " + UUID.randomUUID());
    request.setSourceType(SourceType.DOCKER_HUB);
    request.setSourceRef("library/postgres");
    final ComponentDto component = this.componentFacade.create(request);

    final EnvironmentRequest environmentRequest = new EnvironmentRequest();
    environmentRequest.setProjectId(projectId);
    environmentRequest.setName("Production " + UUID.randomUUID());
    environmentRequest.setProduction(Boolean.TRUE);
    final EnvironmentDto environment = this.environmentFacade.create(environmentRequest);

    final ComponentEnvironmentRequest cell = new ComponentEnvironmentRequest();
    cell.setComponentId(component.getId());
    cell.setEnvironmentId(environment.getId());
    cell.setCurrentVersion(version);
    this.componentEnvironmentFacade.createOrUpdate(cell);
    return component;
  }

  private UUID project() {
    final ProjectRequest request = new ProjectRequest();
    request.setName("Project " + UUID.randomUUID());
    return this.projectFacade.create(request).getId();
  }
}
