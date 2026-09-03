package one.epicsoft.vc.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nonnull;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.UUID;

/**
 * What has to be done in one project, in the order it should be read.
 *
 * <p>This is the entry point of the application rather than the matrix: a table
 * leaves it to the reader to work out what needs doing, and the answer to that
 * question is the whole reason the inventory exists.
 */
@Data
@NoArgsConstructor
public class TaskListDto {

  @Nonnull
  @JsonProperty("projectId")
  private UUID projectId;

  /** Only sections that have entries, in the order of the enum. */
  @Nonnull
  @JsonProperty("sections")
  private List<TaskSectionDto> sections;
}
