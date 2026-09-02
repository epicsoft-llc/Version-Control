package one.epicsoft.vc.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nonnull;
import lombok.Data;
import lombok.NoArgsConstructor;
import one.epicsoft.vc.enums.SourceType;
import java.util.List;
import java.util.UUID;

/** One component and its version in every environment of the project. */
@Data
@NoArgsConstructor
public class MatrixRowDto {

  @Nonnull
  @JsonProperty("componentId")
  private UUID componentId;

  @Nonnull
  @JsonProperty("componentName")
  private String componentName;

  @Nonnull
  @JsonProperty("sourceType")
  private SourceType sourceType;

  /** More than one version is in use across the environments of this component. */
  @Nonnull
  @JsonProperty("deviating")
  private Boolean deviating;

  /** One entry per environment, in the order of the environment list. */
  @Nonnull
  @JsonProperty("cells")
  private List<MatrixCellDto> cells;
}
