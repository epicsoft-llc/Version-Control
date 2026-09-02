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
import one.epicsoft.vc.dto.VersionChangeDto;
import one.epicsoft.vc.dto.VersionChangePackageRequest;
import one.epicsoft.vc.dto.VersionChangeRequest;
import one.epicsoft.vc.enums.SourceType;
import one.epicsoft.vc.exception.AppNotFoundException;
import one.epicsoft.vc.exception.AppValidationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The one path that changes a version, and the history it has to leave behind. */
@Transactional
class VersionChangeFacadeIntegrationTest extends AbstractIntegrationTest {

  @Autowired
  private ProjectFacade projectFacade;

  @Autowired
  private EnvironmentFacade environmentFacade;

  @Autowired
  private ComponentFacade componentFacade;

  @Autowired
  private ComponentEnvironmentFacade componentEnvironmentFacade;

  @Autowired
  private VersionChangeFacade versionChangeFacade;

  @Test
  void writesTheVersionAndItsHistoryEntryTogether() {
    final ComponentEnvironmentDto cell = this.cell("17.2");

    final VersionChangeRequest request = new VersionChangeRequest();
    request.setComponentEnvironmentId(cell.getId());
    request.setToVersion("18.0");
    request.setNote("Ran the migration first");
    request.setBackupTaken(Boolean.TRUE);
    request.setBackupRef("snapshot-4711");
    request.setRollbackPath("Restore the snapshot, then start 17.2 again");
    request.setDowntimeMinutes(12);
    request.setIssues("The extension had to be reinstalled");

    final VersionChangeDto change = this.versionChangeFacade.changeVersion(request);

    assertEquals("17.2", change.getFromVersion());
    assertEquals("18.0", change.getToVersion());
    assertTrue(change.getBackupTaken());
    assertEquals(12, change.getDowntimeMinutes());
    // The record and the entry are written in the same step, or not at all.
    assertEquals("18.0", this.componentEnvironmentFacade.getById(cell.getId()).getCurrentVersion());
    assertEquals(1, this.versionChangeFacade.findAllByComponentEnvironment(cell.getId()).size());
  }

  @Test
  void recordsTheFirstVersionWithoutAPredecessor() {
    final ComponentEnvironmentDto cell = this.cell(null);

    final VersionChangeRequest request = new VersionChangeRequest();
    request.setComponentEnvironmentId(cell.getId());
    request.setToVersion("17.2");

    final VersionChangeDto change = this.versionChangeFacade.changeVersion(request);

    assertNull(change.getFromVersion());
    assertEquals("17.2", change.getToVersion());
    // The checkbox starts unchecked, and nothing may quietly tick it.
    assertFalse(change.getBackupTaken());
  }

  @Test
  void listsTheHistoryNewestFirst() {
    final ComponentEnvironmentDto cell = this.cell("17.0");
    this.change(cell.getId(), "17.2");
    this.change(cell.getId(), "18.0");

    final List<VersionChangeDto> history = this.versionChangeFacade.findAllByComponentEnvironment(cell.getId());

    assertEquals(2, history.size());
    assertEquals("18.0", history.getFirst().getToVersion());
    assertEquals("17.2", history.getLast().getToVersion());
    assertNotNull(history.getFirst().getEnvironmentId());
  }

  @Test
  void refusesAnEntryThatRecordsNoChange() {
    final ComponentEnvironmentDto cell = this.cell("17.2");

    final VersionChangeRequest request = new VersionChangeRequest();
    request.setComponentEnvironmentId(cell.getId());
    request.setToVersion("17.2");

    // An entry saying nothing happened is noise in the list that has to stay readable.
    assertThrows(AppValidationException.class, () -> this.versionChangeFacade.changeVersion(request));
  }

  @Test
  void refusesAVersionForACellThatIsDeliberatelyEmpty() {
    final ComponentEnvironmentDto cell = this.cell(null);
    final ComponentEnvironmentRequest deactivate = new ComponentEnvironmentRequest();
    deactivate.setComponentId(cell.getComponentId());
    deactivate.setEnvironmentId(cell.getEnvironmentId());
    deactivate.setActive(Boolean.FALSE);
    this.componentEnvironmentFacade.createOrUpdate(deactivate);

    final VersionChangeRequest request = new VersionChangeRequest();
    request.setComponentEnvironmentId(cell.getId());
    request.setToVersion("17.2");

    // "Not present here" and "runs here with this version" cannot both hold.
    assertThrows(AppValidationException.class, () -> this.versionChangeFacade.changeVersion(request));
  }

  @Test
  void foldsSeveralPackagesIntoOneEntry() {
    final UUID projectId = this.project();
    final ComponentRequest componentRequest = this.componentRequest(projectId, "Docker");
    componentRequest.setSourceType(SourceType.APT_REPOSITORY);
    componentRequest.setPackages(List.of(this.packageRequest("docker-ce", 0, true),
                                         this.packageRequest("containerd.io", 1, false)));
    final ComponentDto component = this.componentFacade.create(componentRequest);
    final ComponentEnvironmentDto cell = this.cell(component.getId(), this.environment(projectId, "Production").getId(), null);

    this.versionChangeFacade.changeVersion(this.packageChange(cell.getId(), "29.7.1", "2.3.3"));
    final VersionChangeDto second = this.versionChangeFacade.changeVersion(this.packageChange(cell.getId(), "29.7.2", "2.3.4"));

    assertEquals("docker-ce 29.7.1, containerd.io 2.3.3", second.getFromVersion());
    assertEquals("docker-ce 29.7.2, containerd.io 2.3.4", second.getToVersion());
    // One maintenance action, one entry — four would be noise.
    assertEquals(2, this.versionChangeFacade.findAllByComponentEnvironment(cell.getId()).size());
    // The matrix cell follows the leading package.
    assertEquals("29.7.2", this.componentEnvironmentFacade.getById(cell.getId()).getCurrentVersion());
  }

  @Test
  void namesOnlyThePackagesThatMoved() {
    final UUID projectId = this.project();
    final ComponentRequest componentRequest = this.componentRequest(projectId, "Docker");
    componentRequest.setSourceType(SourceType.APT_REPOSITORY);
    componentRequest.setPackages(List.of(this.packageRequest("docker-ce", 0, true),
                                         this.packageRequest("containerd.io", 1, false)));
    final ComponentDto component = this.componentFacade.create(componentRequest);
    final ComponentEnvironmentDto cell = this.cell(component.getId(), this.environment(projectId, "Production").getId(), null);

    this.versionChangeFacade.changeVersion(this.packageChange(cell.getId(), "29.7.1", "2.3.3"));
    final VersionChangeDto second = this.versionChangeFacade.changeVersion(this.packageChange(cell.getId(), "29.7.2", "2.3.3"));

    assertEquals("docker-ce 29.7.1", second.getFromVersion());
    assertEquals("docker-ce 29.7.2", second.getToVersion());
  }

  @Test
  void refusesAPackageThatIsNotConfigured() {
    final UUID projectId = this.project();
    final ComponentRequest componentRequest = this.componentRequest(projectId, "Docker");
    componentRequest.setSourceType(SourceType.APT_REPOSITORY);
    componentRequest.setPackages(List.of(this.packageRequest("docker-ce", 0, true)));
    final ComponentDto component = this.componentFacade.create(componentRequest);
    final ComponentEnvironmentDto cell = this.cell(component.getId(), this.environment(projectId, "Production").getId(), null);

    final VersionChangeRequest request = new VersionChangeRequest();
    request.setComponentEnvironmentId(cell.getId());
    request.setPackages(List.of(this.versionPackage("docker-ce-cli", "29.7.2")));

    // Otherwise a typo silently creates a package row nobody configured.
    assertThrows(AppValidationException.class, () -> this.versionChangeFacade.changeVersion(request));
  }

  @Test
  void refusesASingleVersionForAComponentMaintainedPerPackage() {
    final ComponentEnvironmentDto cell = this.packageCell(true);

    final VersionChangeRequest request = new VersionChangeRequest();
    request.setComponentEnvironmentId(cell.getId());
    request.setToVersion("29.7.2");

    // Accepting it would move the cell while the package rows keep the old
    // versions, and the next summary would report a change that never happened.
    assertThrows(AppValidationException.class, () -> this.versionChangeFacade.changeVersion(request));
  }

  @Test
  void refusesPackagesForAComponentThatCarriesOneVersion() {
    final ComponentEnvironmentDto cell = this.cell("17.2");

    final VersionChangeRequest request = new VersionChangeRequest();
    request.setComponentEnvironmentId(cell.getId());
    request.setPackages(List.of(this.versionPackage("postgresql", "18.0")));

    assertThrows(AppValidationException.class, () -> this.versionChangeFacade.changeVersion(request));
  }

  @Test
  void fillsTheCellEvenWhenNoPackageWasMarkedAsLeading() {
    final ComponentEnvironmentDto cell = this.packageCell(false);

    this.versionChangeFacade.changeVersion(this.packageChange(cell.getId(), "29.7.1", "2.3.3"));

    // Without a leading package the cell would stay empty for good, however many
    // versions are recorded; the first package in order takes the role.
    assertEquals("29.7.1", this.componentEnvironmentFacade.getById(cell.getId()).getCurrentVersion());
  }

  @Test
  void refusesToListTheHistoryOfSomethingThatDoesNotExist() {
    // An empty list would read as "nothing was ever changed here".
    assertThrows(AppNotFoundException.class,
                 () -> this.versionChangeFacade.findAllByComponentEnvironment(UUID.randomUUID()));
    assertThrows(AppNotFoundException.class, () -> this.versionChangeFacade.findAllByComponent(UUID.randomUUID()));
    assertThrows(AppNotFoundException.class, () -> this.versionChangeFacade.findAllByProject(UUID.randomUUID()));
  }

  @Test
  void dropsTheHistoryOfADeletedEnvironmentFromTheComponentList() {
    final UUID projectId = this.project();
    final ComponentDto component = this.componentFacade.create(this.componentRequest(projectId, "PostgreSQL"));
    final EnvironmentDto environment = this.environment(projectId, "Staging");
    final ComponentEnvironmentDto cell = this.cell(component.getId(), environment.getId(), null);
    this.change(cell.getId(), "17.2");

    this.environmentFacade.delete(environment.getId());

    // The entry names a column the environment list no longer has.
    assertTrue(this.versionChangeFacade.findAllByComponent(component.getId()).isEmpty());
  }

  private ComponentEnvironmentDto packageCell(final boolean markLeading) {
    final UUID projectId = this.project();
    final ComponentRequest componentRequest = this.componentRequest(projectId, "Docker");
    componentRequest.setSourceType(SourceType.APT_REPOSITORY);
    componentRequest.setPackages(List.of(this.packageRequest("docker-ce", 0, markLeading),
                                         this.packageRequest("containerd.io", 1, false)));
    final ComponentDto component = this.componentFacade.create(componentRequest);
    return this.cell(component.getId(), this.environment(projectId, "Production").getId(), null);
  }

  private VersionChangeRequest packageChange(final UUID cellId, final String docker, final String containerd) {
    final VersionChangeRequest request = new VersionChangeRequest();
    request.setComponentEnvironmentId(cellId);
    request.setPackages(List.of(this.versionPackage("docker-ce", docker),
                                this.versionPackage("containerd.io", containerd)));
    return request;
  }

  private VersionChangePackageRequest versionPackage(final String name, final String version) {
    final VersionChangePackageRequest entry = new VersionChangePackageRequest();
    entry.setPackageName(name);
    entry.setVersion(version);
    return entry;
  }

  private ComponentPackageRequest packageRequest(final String name, final int sortOrder, final boolean leading) {
    final ComponentPackageRequest request = new ComponentPackageRequest();
    request.setName(name);
    request.setSortOrder(sortOrder);
    request.setLeadingPackage(leading);
    return request;
  }

  private void change(final UUID cellId, final String toVersion) {
    final VersionChangeRequest request = new VersionChangeRequest();
    request.setComponentEnvironmentId(cellId);
    request.setToVersion(toVersion);
    this.versionChangeFacade.changeVersion(request);
  }

  private ComponentEnvironmentDto cell(final String currentVersion) {
    final UUID projectId = this.project();
    final ComponentDto component = this.componentFacade.create(this.componentRequest(projectId, "PostgreSQL"));
    final EnvironmentDto environment = this.environment(projectId, "Production");
    return this.cell(component.getId(), environment.getId(), currentVersion);
  }

  private ComponentEnvironmentDto cell(final UUID componentId, final UUID environmentId, final String currentVersion) {
    final ComponentEnvironmentRequest request = new ComponentEnvironmentRequest();
    request.setComponentId(componentId);
    request.setEnvironmentId(environmentId);
    request.setCurrentVersion(currentVersion);
    return this.componentEnvironmentFacade.createOrUpdate(request);
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
    request.setProduction(Boolean.TRUE);
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
