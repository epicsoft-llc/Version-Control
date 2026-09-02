package one.epicsoft.vc.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nonnull;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Data
@NoArgsConstructor
public class ComponentPackageDto {

  @Nonnull
  @JsonProperty("id")
  private UUID id;

  @Nonnull
  @JsonProperty("name")
  private String name;

  @Nonnull
  @JsonProperty("sortOrder")
  private Integer sortOrder;

  /** Its version is the one the matrix cell shows. */
  @Nonnull
  @JsonProperty("leadingPackage")
  private Boolean leadingPackage;
}
