package one.epicsoft.vc.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import lombok.Data;
import lombok.NoArgsConstructor;
import one.epicsoft.vc.enums.CheckState;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * When support for the running release ends. Kept apart from the update check:
 * this has a deadline, and no decision postpones it.
 */
@Data
@NoArgsConstructor
public class EolStatusDto {

  @Nonnull
  @JsonProperty("id")
  private UUID id;

  @Nonnull
  @JsonProperty("componentId")
  private UUID componentId;

  @Nullable
  @JsonProperty("eolDate")
  private LocalDate eolDate;

  @Nullable
  @JsonProperty("supportedUntil")
  private LocalDate supportedUntil;

  @Nonnull
  @JsonProperty("eol")
  private Boolean eol;

  @Nonnull
  @JsonProperty("checkedAt")
  private Instant checkedAt;

  @Nonnull
  @JsonProperty("checkState")
  private CheckState checkState;

  @Nullable
  @JsonProperty("errorMessage")
  private String errorMessage;
}
