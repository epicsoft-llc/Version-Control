package one.epicsoft.vc.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

/**
 * Creates or updates the record of a component in an environment. The deployed
 * version is not changed here — that happens in the dedicated dialog, which
 * writes a history entry in the same step.
 */
@Data
@NoArgsConstructor
public class ComponentEnvironmentRequest {

  @Nonnull
  @NotNull
  @JsonProperty("componentId")
  private UUID componentId;

  @Nonnull
  @NotNull
  @JsonProperty("environmentId")
  private UUID environmentId;

  @Nullable
  @JsonProperty("active")
  private Boolean active;

  @Nullable
  @Size(max = 200)
  @JsonProperty("currentVersion")
  private String currentVersion;
}
