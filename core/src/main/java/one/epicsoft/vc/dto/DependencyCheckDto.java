package one.epicsoft.vc.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import lombok.Data;
import lombok.NoArgsConstructor;
import one.epicsoft.vc.enums.ConstraintState;
import java.util.UUID;

/**
 * One dependency, evaluated in one environment.
 *
 * <p>Everything a reader needs to judge it without opening anything else: who
 * needs what, in which range, what is actually there, and where that was read.
 */
@Data
@NoArgsConstructor
public class DependencyCheckDto {

  @Nonnull
  @JsonProperty("dependencyId")
  private UUID dependencyId;

  /** The dependent component — Zitadel in the example. */
  @Nonnull
  @JsonProperty("componentId")
  private UUID componentId;

  @Nonnull
  @JsonProperty("componentName")
  private String componentName;

  /** What it needs — PostgreSQL in the example. */
  @Nonnull
  @JsonProperty("dependsOnId")
  private UUID dependsOnId;

  @Nonnull
  @JsonProperty("dependsOnName")
  private String dependsOnName;

  @Nonnull
  @JsonProperty("environmentId")
  private UUID environmentId;

  /** Named as well as identified: a list of results is unreadable without it. */
  @Nullable
  @JsonProperty("environmentName")
  private String environmentName;

  @Nonnull
  @JsonProperty("versionRange")
  private String versionRange;

  /** The version of the prerequisite in that environment, where there is one. */
  @Nullable
  @JsonProperty("actualVersion")
  private String actualVersion;

  @Nonnull
  @JsonProperty("state")
  private ConstraintState state;

  /** Where the requirement was read — the sentence that saves looking it up again. */
  @Nullable
  @JsonProperty("note")
  private String note;
}
