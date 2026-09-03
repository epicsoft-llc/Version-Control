package one.epicsoft.vc.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import one.epicsoft.vc.dto.ComponentEnvironmentDto;
import one.epicsoft.vc.dto.ComponentEnvironmentRequest;
import one.epicsoft.vc.exception.AppValidationException;
import one.epicsoft.vc.facade.ComponentEnvironmentFacade;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.UUID;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@Slf4j
@RestController
@RequestMapping(value = ComponentEnvironmentController.PATH_PREFIX,
                consumes = MediaType.APPLICATION_JSON_VALUE,
                produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class ComponentEnvironmentController {

  public static final String PATH_PREFIX = "api/v1/component-environments";

  private final ComponentEnvironmentFacade componentEnvironmentFacade;

  /** Everything the matrix of one project needs, or the rows of one component. */
  @GetMapping(consumes = MediaType.ALL_VALUE)
  public ResponseEntity<List<ComponentEnvironmentDto>> find(@RequestParam(required = false) final UUID projectId,
                                                            @RequestParam(required = false) final UUID componentId) {
    log.debug("Incoming GET 'api/v1/component-environments?projectId={}&componentId={}'", projectId, componentId);
    if (nonNull(componentId)) {
      return ResponseEntity.ok(this.componentEnvironmentFacade.findAllByComponent(componentId));
    }
    if (isNull(projectId)) {
      // Falling through would answer 200 with an empty matrix — the hardest kind
      // of defect to find in the interface that consumes it.
      throw AppValidationException.parameterRequired("projectId or componentId");
    }
    return ResponseEntity.ok(this.componentEnvironmentFacade.findAllByProject(projectId));
  }

  @GetMapping(path = "/{id}", consumes = MediaType.ALL_VALUE)
  public ResponseEntity<ComponentEnvironmentDto> getById(@PathVariable final UUID id) {
    log.debug("Incoming GET 'api/v1/component-environments/{}'", id);
    return ResponseEntity.ok(this.componentEnvironmentFacade.getById(id));
  }

  @PostMapping
  public ResponseEntity<ComponentEnvironmentDto> createOrUpdate(@RequestBody @Valid final ComponentEnvironmentRequest request) {
    log.debug("Incoming POST 'api/v1/component-environments' with: {}", request);
    return ResponseEntity.ok(this.componentEnvironmentFacade.createOrUpdate(request));
  }

  @DeleteMapping(path = "/{id}", consumes = MediaType.ALL_VALUE)
  public ResponseEntity<Void> delete(@PathVariable final UUID id) {
    log.debug("Incoming DELETE 'api/v1/component-environments/{}'", id);
    this.componentEnvironmentFacade.delete(id);
    return ResponseEntity.noContent().build();
  }
}
