package one.epicsoft.vc.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nonnull;
import lombok.Data;
import lombok.NoArgsConstructor;
import one.epicsoft.vc.enums.TaskSection;
import java.util.List;

/** One section of the task list. Sections without entries are not sent. */
@Data
@NoArgsConstructor
public class TaskSectionDto {

  @Nonnull
  @JsonProperty("section")
  private TaskSection section;

  @Nonnull
  @JsonProperty("entries")
  private List<TaskEntryDto> entries;
}
