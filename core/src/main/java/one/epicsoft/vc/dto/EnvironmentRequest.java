package one.epicsoft.vc.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Data
@NoArgsConstructor
public class EnvironmentRequest {

  @Nonnull
  @NotNull
  @JsonProperty("projectId")
  private UUID projectId;

  @Nonnull
  @NotBlank
  @Size(max = 100)
  @JsonProperty("name")
  private String name;

  @Nullable
  @JsonProperty("sortOrder")
  private Integer sortOrder;

  @Nullable
  @JsonProperty("production")
  private Boolean production;
}
