package one.epicsoft.vc.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import one.epicsoft.vc.enums.MaxStep;
import one.epicsoft.vc.enums.SourceType;
import one.epicsoft.vc.enums.VersionStrategy;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
public class ComponentRequest {

  @Nonnull
  @NotNull
  @JsonProperty("projectId")
  private UUID projectId;

  @Nonnull
  @NotBlank
  @Size(max = 100)
  @JsonProperty("name")
  private String name;

  @Nullable
  @JsonProperty("description")
  private String description;

  @Nonnull
  @NotNull
  @JsonProperty("sourceType")
  private SourceType sourceType;

  @Nullable
  @Size(max = 500)
  @JsonProperty("sourceRef")
  private String sourceRef;

  @Nullable
  @Size(max = 50)
  @JsonProperty("aptSuite")
  private String aptSuite;

  @Nullable
  @Size(max = 50)
  @JsonProperty("aptComponent")
  private String aptComponent;

  @Nullable
  @Size(max = 50)
  @JsonProperty("aptArchitecture")
  private String aptArchitecture;

  @Nullable
  @Size(max = 200)
  @JsonProperty("tagPattern")
  private String tagPattern;

  @Nullable
  @JsonProperty("versionStrategy")
  private VersionStrategy versionStrategy;

  @Nullable
  @Size(max = 50)
  @JsonProperty("majorConstraint")
  private String majorConstraint;

  @Nullable
  @JsonProperty("allowPrerelease")
  private Boolean allowPrerelease;

  @Nullable
  @JsonProperty("maxStep")
  private MaxStep maxStep;

  @Nullable
  @JsonProperty("upgradePathNotes")
  private String upgradePathNotes;

  @Nullable
  @Size(max = 500)
  @JsonProperty("upgradeDocUrl")
  private String upgradeDocUrl;

  @Nullable
  @JsonProperty("needsMaintenanceWindow")
  private Boolean needsMaintenanceWindow;

  @Nullable
  @Size(max = 100)
  @JsonProperty("eolProduct")
  private String eolProduct;

  @Nullable
  @Size(max = 50)
  @JsonProperty("eolCycle")
  private String eolCycle;

  @Nullable
  @JsonProperty("checkEnabled")
  private Boolean checkEnabled;

  /** Only for APT components; the whole list replaces the stored one. */
  @Nullable
  @Valid
  @JsonProperty("packages")
  private List<ComponentPackageRequest> packages;

  /**
   * Versions that cannot be skipped on the way up. Omitted leaves the stored ones
   * alone, an empty list clears them — the same shape as the package list.
   */
  @Nullable
  @JsonProperty("upgradeStops")
  private List<@Valid UpgradeStopRequest> upgradeStops;

  /** What this component needs, in the same shape again. */
  @Nullable
  @JsonProperty("dependencies")
  private List<@Valid ComponentDependencyRequest> dependencies;
}
