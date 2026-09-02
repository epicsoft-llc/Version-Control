package one.epicsoft.vc.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import one.epicsoft.vc.dto.ProjectDto;
import one.epicsoft.vc.dto.ProjectRequest;
import one.epicsoft.vc.facade.ProjectFacade;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping(value = ProjectController.PATH_PREFIX,
                consumes = MediaType.APPLICATION_JSON_VALUE,
                produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class ProjectController {

  public static final String PATH_PREFIX = "api/v1/projects";

  private final ProjectFacade projectFacade;

  @GetMapping(consumes = MediaType.ALL_VALUE)
  public ResponseEntity<List<ProjectDto>> findAll() {
    log.debug("Incoming GET 'api/v1/projects'");
    return ResponseEntity.ok(this.projectFacade.findAll());
  }

  @GetMapping(path = "/{id}", consumes = MediaType.ALL_VALUE)
  public ResponseEntity<ProjectDto> getById(@PathVariable final UUID id) {
    log.debug("Incoming GET 'api/v1/projects/{}'", id);
    return ResponseEntity.ok(this.projectFacade.getById(id));
  }

  @PostMapping
  public ResponseEntity<ProjectDto> create(@RequestBody @Valid final ProjectRequest request) {
    log.debug("Incoming POST 'api/v1/projects' with: {}", request);
    return ResponseEntity.ok(this.projectFacade.create(request));
  }

  @PutMapping("/{id}")
  public ResponseEntity<ProjectDto> update(@PathVariable final UUID id,
                                           @RequestBody @Valid final ProjectRequest request) {
    log.debug("Incoming PUT 'api/v1/projects/{}' with: {}", id, request);
    return ResponseEntity.ok(this.projectFacade.update(id, request));
  }

  @DeleteMapping(path = "/{id}", consumes = MediaType.ALL_VALUE)
  public ResponseEntity<Void> delete(@PathVariable final UUID id) {
    log.debug("Incoming DELETE 'api/v1/projects/{}'", id);
    this.projectFacade.delete(id);
    return ResponseEntity.noContent().build();
  }
}
