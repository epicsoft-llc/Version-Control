package one.epicsoft.vc.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** The version of one component in one environment. */
@Data
@NoArgsConstructor
public class ComponentEnvironmentDto {

  @Nonnull
  @JsonProperty("id")
  private UUID id;

  @Nonnull
  @JsonProperty("componentId")
  private UUID componentId;

  @Nonnull
  @JsonProperty("environmentId")
  private UUID environmentId;

  /** False means "deliberately not present here". */
  @Nonnull
  @JsonProperty("active")
  private Boolean active;

  @Nullable
  @JsonProperty("currentVersion")
  private String currentVersion;

  @Nullable
  @JsonProperty("currentDigest")
  private String currentDigest;

  @Nullable
  @JsonProperty("lastChangedAt")
  private Instant lastChangedAt;

  /** Active, but no version recorded — a gap, which an inactive cell is not. */
  @Nonnull
  @JsonProperty("versionUnknown")
  private Boolean versionUnknown;

  /** latest, stable, 17-alpine: allowed, but worth a warning. */
  @Nonnull
  @JsonProperty("movingTag")
  private Boolean movingTag;

  @Nonnull
  @JsonProperty("packages")
  private List<ComponentEnvironmentPackageDto> packages;
}
