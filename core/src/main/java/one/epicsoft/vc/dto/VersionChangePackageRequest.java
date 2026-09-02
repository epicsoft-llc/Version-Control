package one.epicsoft.vc.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nonnull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * The new version of one package of a component that has several. All of them
 * move in a single maintenance action, and that action is one history entry.
 */
@Data
@NoArgsConstructor
public class VersionChangePackageRequest {

  @Nonnull
  @NotBlank
  @Size(max = 200)
  @JsonProperty("packageName")
  private String packageName;

  @Nonnull
  @NotBlank
  @Size(max = 200)
  @JsonProperty("version")
  private String version;
}
