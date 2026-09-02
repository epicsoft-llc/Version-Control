package one.epicsoft.vc.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.util.UUID;

/** One thing to do, with what it takes to judge it without opening anything. */
@Data
@NoArgsConstructor
public class TaskEntryDto {

  @Nonnull
  @JsonProperty("componentId")
  private UUID componentId;

  @Nonnull
  @JsonProperty("componentName")
  private String componentName;

  /** The environment this is about, where it is about one. */
  @Nullable
  @JsonProperty("environmentName")
  private String environmentName;

  @Nullable
  @JsonProperty("currentVersion")
  private String currentVersion;

  @Nullable
  @JsonProperty("latestVersion")
  private String latestVersion;

  /** Free text the section decides: an error message, a blocker, a date. */
  @Nullable
  @JsonProperty("detail")
  private String detail;

  @Nullable
  @JsonProperty("since")
  private Instant since;
}
