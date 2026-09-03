package one.epicsoft.vc.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

/** A dependency as it is configured, without any evaluation. */
@Data
@NoArgsConstructor
public class ComponentDependencyDto {

  @Nonnull
  @JsonProperty("id")
  private UUID id;

  @Nonnull
  @JsonProperty("dependsOnId")
  private UUID dependsOnId;

  @Nonnull
  @JsonProperty("versionRange")
  private String versionRange;

  @Nullable
  @JsonProperty("note")
  private String note;
}
