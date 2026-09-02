package one.epicsoft.vc.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import lombok.Data;
import lombok.NoArgsConstructor;
import one.epicsoft.vc.enums.CheckState;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * What the last check found. A failed check keeps the version of the last good
 * one and says how old that answer is — the alternative, an empty result, reads
 * like "nothing available" and is the wrong thing to tell anyone.
 */
@Data
@NoArgsConstructor
public class UpdateCheckDto {

  @Nonnull
  @JsonProperty("id")
  private UUID id;

  @Nonnull
  @JsonProperty("componentId")
  private UUID componentId;

  @Nullable
  @JsonProperty("latestVersion")
  private String latestVersion;

  @Nullable
  @JsonProperty("latestReleasedAt")
  private Instant latestReleasedAt;

  @Nullable
  @JsonProperty("latestDigest")
  private String latestDigest;

  @Nonnull
  @JsonProperty("checkedAt")
  private Instant checkedAt;

  @Nonnull
  @JsonProperty("checkState")
  private CheckState checkState;

  /** Technical, for the person who has to fix the rule or the address. */
  @Nullable
  @JsonProperty("errorMessage")
  private String errorMessage;

  @Nullable
  @JsonProperty("lastSuccessfulCheckAt")
  private Instant lastSuccessfulCheckAt;

  @Nonnull
  @JsonProperty("packages")
  private List<UpdateCheckPackageDto> packages;
}
