package one.epicsoft.vc.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

/** Tries a rule out before it is saved. */
@Data
@NoArgsConstructor
public class VersionPreviewRequest {

  @Nonnull
  @NotNull
  @JsonProperty("componentId")
  private UUID componentId;

  /** Empty tries the rule the component would derive from its deployed tag. */
  @Nullable
  @Size(max = 200)
  @JsonProperty("tagPattern")
  private String tagPattern;
}
