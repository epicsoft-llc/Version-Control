package one.epicsoft.vc.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Data
@NoArgsConstructor
public class ProjectDto {

  @Nonnull
  @JsonProperty("id")
  private UUID id;

  @Nonnull
  @JsonProperty("name")
  private String name;

  @Nullable
  @JsonProperty("description")
  private String description;

  @Nonnull
  @JsonProperty("sortOrder")
  private Integer sortOrder;
}
