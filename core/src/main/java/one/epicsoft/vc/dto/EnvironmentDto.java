package one.epicsoft.vc.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nonnull;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Data
@NoArgsConstructor
public class EnvironmentDto {

  @Nonnull
  @JsonProperty("id")
  private UUID id;

  @Nonnull
  @JsonProperty("projectId")
  private UUID projectId;

  @Nonnull
  @JsonProperty("name")
  private String name;

  @Nonnull
  @JsonProperty("sortOrder")
  private Integer sortOrder;

  @Nonnull
  @JsonProperty("production")
  private Boolean production;
}
