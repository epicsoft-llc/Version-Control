package one.epicsoft.vc.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import one.epicsoft.vc.dto.VersionChangeDto;
import one.epicsoft.vc.dto.VersionChangeRequest;
import one.epicsoft.vc.exception.AppValidationException;
import one.epicsoft.vc.facade.VersionChangeFacade;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.UUID;

import static java.util.Objects.nonNull;

/**
 * The change history — and, by writing one, the only way to change a version.
 *
 * <p>There is no endpoint that updates a version without recording it. That is
 * the point: the entry cannot be forgotten because it is not a separate step.
 */
@Slf4j
@RestController
@RequestMapping(value = VersionChangeController.PATH_PREFIX,
                consumes = MediaType.APPLICATION_JSON_VALUE,
                produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class VersionChangeController {

  public static final String PATH_PREFIX = "api/v1/version-changes";

  private final VersionChangeFacade versionChangeFacade;

  /** The history of one cell, one component or a whole project — newest first. */
  @GetMapping(consumes = MediaType.ALL_VALUE)
  public ResponseEntity<List<VersionChangeDto>> find(@RequestParam(required = false) final UUID componentEnvironmentId,
                                                     @RequestParam(required = false) final UUID componentId,
                                                     @RequestParam(required = false) final UUID projectId) {
    log.debug("Incoming GET version changes for cell {}, component {}, project {}",
              componentEnvironmentId, componentId, projectId);
    if (nonNull(componentEnvironmentId)) {
      return ResponseEntity.ok(this.versionChangeFacade.findAllByComponentEnvironment(componentEnvironmentId));
    }
    if (nonNull(componentId)) {
      return ResponseEntity.ok(this.versionChangeFacade.findAllByComponent(componentId));
    }
    if (nonNull(projectId)) {
      return ResponseEntity.ok(this.versionChangeFacade.findAllByProject(projectId));
    }
    // Answering with every change ever recorded would be a list nobody asked for.
    throw AppValidationException.parameterRequired("componentEnvironmentId, componentId or projectId");
  }

  @GetMapping(path = "/{id}", consumes = MediaType.ALL_VALUE)
  public ResponseEntity<VersionChangeDto> getById(@PathVariable final UUID id) {
    log.debug("Incoming GET version change {}", id);
    return ResponseEntity.ok(this.versionChangeFacade.getById(id));
  }

  /** Saves the new version and the history entry in one step. */
  @PostMapping
  public ResponseEntity<VersionChangeDto> changeVersion(@RequestBody @Valid final VersionChangeRequest request) {
    // The id, not the body: note, rollback path and backup reference are free
    // text and routinely carry host names or snapshot ids that belong in the
    // database, not in a log file.
    log.debug("Incoming POST version change for cell {}", request.getComponentEnvironmentId());
    return ResponseEntity.ok(this.versionChangeFacade.changeVersion(request));
  }
}
