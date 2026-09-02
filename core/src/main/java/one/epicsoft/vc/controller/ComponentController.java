package one.epicsoft.vc.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import one.epicsoft.vc.dto.ComponentDto;
import one.epicsoft.vc.dto.ComponentRequest;
import one.epicsoft.vc.facade.ComponentFacade;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping(value = ComponentController.PATH_PREFIX,
                consumes = MediaType.APPLICATION_JSON_VALUE,
                produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class ComponentController {

  public static final String PATH_PREFIX = "api/v1/components";

  private final ComponentFacade componentFacade;

  @GetMapping(consumes = MediaType.ALL_VALUE)
  public ResponseEntity<List<ComponentDto>> findAllByProject(@RequestParam final UUID projectId) {
    log.debug("Incoming GET 'api/v1/components?projectId={}'", projectId);
    return ResponseEntity.ok(this.componentFacade.findAllByProject(projectId));
  }

  @GetMapping(path = "/{id}", consumes = MediaType.ALL_VALUE)
  public ResponseEntity<ComponentDto> getById(@PathVariable final UUID id) {
    log.debug("Incoming GET 'api/v1/components/{}'", id);
    return ResponseEntity.ok(this.componentFacade.getById(id));
  }

  @PostMapping
  public ResponseEntity<ComponentDto> create(@RequestBody @Valid final ComponentRequest request) {
    log.debug("Incoming POST 'api/v1/components' with: {}", request);
    return ResponseEntity.ok(this.componentFacade.create(request));
  }

  @PutMapping("/{id}")
  public ResponseEntity<ComponentDto> update(@PathVariable final UUID id,
                                             @RequestBody @Valid final ComponentRequest request) {
    log.debug("Incoming PUT 'api/v1/components/{}' with: {}", id, request);
    return ResponseEntity.ok(this.componentFacade.update(id, request));
  }

  @DeleteMapping(path = "/{id}", consumes = MediaType.ALL_VALUE)
  public ResponseEntity<Void> delete(@PathVariable final UUID id) {
    log.debug("Incoming DELETE 'api/v1/components/{}'", id);
    this.componentFacade.delete(id);
    return ResponseEntity.noContent().build();
  }
}
