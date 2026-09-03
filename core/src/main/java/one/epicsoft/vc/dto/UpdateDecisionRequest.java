package one.epicsoft.vc.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import one.epicsoft.vc.enums.DecisionState;
import java.time.Instant;
import java.util.UUID;

/**
 * Records a decision about an available update. BLOCKED is not accepted here:
 * it is derived from the dependency check, and setting it by hand would claim a
 * blocker that nothing points at.
 */
@Data
@NoArgsConstructor
public class UpdateDecisionRequest {

  @Nonnull
  @NotNull
  @JsonProperty("componentId")
  private UUID componentId;

  @Nonnull
  @NotNull
  @JsonProperty("state")
  private DecisionState state;

  /** Required for a postponement — that is what makes it come back. */
  @Nullable
  @JsonProperty("snoozeUntil")
  private Instant snoozeUntil;

  @Nullable
  @JsonProperty("note")
  private String note;

  @Nullable
  @Size(max = 500)
  @JsonProperty("releaseUrl")
  private String releaseUrl;
}
