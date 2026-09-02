package one.epicsoft.vc.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.UUID;

/**
 * What the dialog "change version" sends: the new state and the story around it,
 * saved in one step. Everything but the target version is optional, but all of it
 * is on the form — asked for later, none of it would ever be filled in.
 *
 * <p>A component with several packages sends {@code packages} instead of
 * {@code toVersion}; the entry that comes out of it is still a single one,
 * because note, backup and rollback belong to the action, not to a package.
 */
@Data
@NoArgsConstructor
public class VersionChangeRequest {

  @Nonnull
  @NotNull
  @JsonProperty("componentEnvironmentId")
  private UUID componentEnvironmentId;

  /** Required unless {@code packages} carries the new versions. */
  @Nullable
  @Size(max = 200)
  @JsonProperty("toVersion")
  private String toVersion;

  // The annotation belongs on the type argument: on the list itself Hibernate
  // Validator warns that the form is deprecated.
  @Nullable
  @JsonProperty("packages")
  private List<@Valid VersionChangePackageRequest> packages;

  @Nullable
  @JsonProperty("note")
  private String note;

  /** Defaults to false — the checkbox starts unchecked and is meant to nag. */
  @Nullable
  @JsonProperty("backupTaken")
  private Boolean backupTaken;

  @Nullable
  @Size(max = 500)
  @JsonProperty("backupRef")
  private String backupRef;

  @Nullable
  @JsonProperty("rollbackPath")
  private String rollbackPath;

  @Nullable
  @Min(0)
  @JsonProperty("downtimeMinutes")
  private Integer downtimeMinutes;

  @Nullable
  @JsonProperty("issues")
  private String issues;
}
