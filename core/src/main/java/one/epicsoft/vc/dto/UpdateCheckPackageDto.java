package one.epicsoft.vc.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import lombok.Data;
import lombok.NoArgsConstructor;

/** The newest version found for one package of a component that has several. */
@Data
@NoArgsConstructor
public class UpdateCheckPackageDto {

  @Nonnull
  @JsonProperty("packageName")
  private String packageName;

  @Nullable
  @JsonProperty("latestVersion")
  private String latestVersion;
}
