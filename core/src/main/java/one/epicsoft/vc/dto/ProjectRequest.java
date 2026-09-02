package one.epicsoft.vc.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ProjectRequest {

  @Nonnull
  @NotBlank
  @Size(max = 100)
  @JsonProperty("name")
  private String name;

  @Nullable
  @JsonProperty("description")
  private String description;

  @Nullable
  @JsonProperty("sortOrder")
  private Integer sortOrder;
}
