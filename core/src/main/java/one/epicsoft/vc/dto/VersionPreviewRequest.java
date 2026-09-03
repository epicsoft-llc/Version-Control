package one.epicsoft.vc.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import one.epicsoft.vc.enums.VersionStrategy;
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

  /**
   * The rest of the rule as it stands in the form. Each field falls back to the
   * stored one when it is not sent — a preview that silently used the saved
   * major constraint while the form shows a different one would answer a question
   * nobody asked.
   */
  @Nullable
  @Size(max = 50)
  @JsonProperty("majorConstraint")
  private String majorConstraint;

  @Nullable
  @JsonProperty("allowPrerelease")
  private Boolean allowPrerelease;

  @Nullable
  @JsonProperty("versionStrategy")
  private VersionStrategy versionStrategy;
}
