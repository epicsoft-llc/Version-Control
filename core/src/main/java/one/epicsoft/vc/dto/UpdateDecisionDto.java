package one.epicsoft.vc.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import lombok.Data;
import lombok.NoArgsConstructor;
import one.epicsoft.vc.enums.DecisionState;
import java.time.Instant;
import java.util.UUID;

/** What somebody decided about an available update. */
@Data
@NoArgsConstructor
public class UpdateDecisionDto {

  @Nonnull
  @JsonProperty("id")
  private UUID id;

  @Nonnull
  @JsonProperty("componentId")
  private UUID componentId;

  @Nonnull
  @JsonProperty("state")
  private DecisionState state;

  /** The version this was decided about; a newer one reopens the decision. */
  @Nullable
  @JsonProperty("decidedForVersion")
  private String decidedForVersion;

  @Nullable
  @JsonProperty("snoozeUntil")
  private Instant snoozeUntil;

  @Nullable
  @JsonProperty("note")
  private String note;

  @Nullable
  @JsonProperty("releaseUrl")
  private String releaseUrl;

  @Nonnull
  @JsonProperty("decidedAt")
  private Instant decidedAt;
}
