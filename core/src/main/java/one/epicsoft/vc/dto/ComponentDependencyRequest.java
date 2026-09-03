package one.epicsoft.vc.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

/** One dependency, as the master data form sends it. */
@Data
@NoArgsConstructor
public class ComponentDependencyRequest {

  @Nonnull
  @NotNull
  @JsonProperty("dependsOnId")
  private UUID dependsOnId;

  /** A version range in the npm style, for example {@code >=14 <18}. */
  @Nonnull
  @NotBlank
  @Size(max = 200)
  @JsonProperty("versionRange")
  private String versionRange;

  @Nullable
  @JsonProperty("note")
  private String note;
}
