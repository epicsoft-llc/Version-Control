package one.epicsoft.vc.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import one.epicsoft.vc.dto.UpdateCheckDto;
import one.epicsoft.vc.dto.VersionPreviewDto;
import one.epicsoft.vc.dto.VersionPreviewRequest;
import one.epicsoft.vc.exception.AppValidationException;
import one.epicsoft.vc.facade.UpdateCheckFacade;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.UUID;

import static java.util.Objects.nonNull;

/**
 * What the last check found, and the two ways of asking for more.
 *
 * <p>A check triggered here obeys the same minimum interval as the scheduler.
 * Letting a button bypass it would mean the bound only holds while nobody is
 * looking, which is the opposite of what a bound is for.
 */
@Slf4j
@RestController
@RequestMapping(value = UpdateCheckController.PATH_PREFIX,
                consumes = MediaType.APPLICATION_JSON_VALUE,
                produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class UpdateCheckController {

  public static final String PATH_PREFIX = "api/v1/update-checks";

  private final UpdateCheckFacade updateCheckFacade;

  /** A component that was never checked answers with an empty list, not an error. */
  @GetMapping(consumes = MediaType.ALL_VALUE)
  public ResponseEntity<List<UpdateCheckDto>> find(@RequestParam(required = false) final UUID projectId,
                                                   @RequestParam(required = false) final UUID componentId) {
    log.debug("Incoming GET update checks for project {}, component {}", projectId, componentId);
    if (nonNull(componentId)) {
      return ResponseEntity.ok(this.updateCheckFacade.findAllByComponent(componentId));
    }
    if (nonNull(projectId)) {
      return ResponseEntity.ok(this.updateCheckFacade.findAllByProject(projectId));
    }
    throw AppValidationException.parameterRequired("projectId or componentId");
  }

  @PostMapping(consumes = MediaType.ALL_VALUE)
  public ResponseEntity<UpdateCheckDto> checkNow(@RequestParam final UUID componentId) {
    log.debug("Incoming POST check of component {}", componentId);
    return ResponseEntity.ok(this.updateCheckFacade.checkNow(componentId));
  }

  /** Which tags a rule accepts, and which of them wins — before the rule is saved. */
  @PostMapping("/preview")
  public ResponseEntity<VersionPreviewDto> preview(@RequestBody @Valid final VersionPreviewRequest request) {
    log.debug("Incoming POST rule preview for component {}", request.getComponentId());
    return ResponseEntity.ok(this.updateCheckFacade.previewRule(request));
  }
}
