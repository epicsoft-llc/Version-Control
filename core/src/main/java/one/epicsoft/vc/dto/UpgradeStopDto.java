package one.epicsoft.vc.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

/** A version that cannot be skipped on the way up. */
@Data
@NoArgsConstructor
public class UpgradeStopDto {

  @Nonnull
  @JsonProperty("id")
  private UUID id;

  @Nonnull
  @JsonProperty("version")
  private String version;

  /** Why it cannot be skipped — the sentence that saves the next reader an hour. */
  @Nullable
  @JsonProperty("note")
  private String note;
}
