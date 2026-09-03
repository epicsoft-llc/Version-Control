package one.epicsoft.vc.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import one.epicsoft.vc.dto.EolStatusDto;
import one.epicsoft.vc.exception.AppValidationException;
import one.epicsoft.vc.facade.EolStatusFacade;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.UUID;

import static java.util.Objects.nonNull;

/**
 * When support ends for what is running.
 *
 * <p>A component that names no product is not checked and answers with an empty
 * list. That is somebody saying the question does not apply, not a fault.
 */
@Slf4j
@RestController
@RequestMapping(value = EolStatusController.PATH_PREFIX, produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class EolStatusController {

  public static final String PATH_PREFIX = "api/v1/eol-statuses";

  private final EolStatusFacade eolStatusFacade;

  @GetMapping
  public ResponseEntity<List<EolStatusDto>> find(@RequestParam(required = false) final UUID projectId,
                                                 @RequestParam(required = false) final UUID componentId) {
    log.debug("Incoming GET end-of-life status for project {}, component {}", projectId, componentId);
    if (nonNull(componentId)) {
      return ResponseEntity.ok(this.eolStatusFacade.findAllByComponent(componentId));
    }
    if (nonNull(projectId)) {
      return ResponseEntity.ok(this.eolStatusFacade.findAllByProjectAsDto(projectId));
    }
    throw AppValidationException.parameterRequired("projectId or componentId");
  }

  /** Asks the end-of-life database now, on its own cycle and its own error state. */
  @PostMapping
  public ResponseEntity<List<EolStatusDto>> checkNow(@RequestParam final UUID componentId) {
    log.debug("Incoming POST end-of-life lookup for component {}", componentId);
    return ResponseEntity.ok(this.eolStatusFacade.checkAsDto(componentId));
  }
}
