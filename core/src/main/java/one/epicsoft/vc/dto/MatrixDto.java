package one.epicsoft.vc.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nonnull;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.UUID;

/**
 * The version overview of one project: components as rows, environments as
 * columns. Assembled on the server so that the interface does not have to join
 * three lists and guess what an empty cell means.
 */
@Data
@NoArgsConstructor
public class MatrixDto {

  @Nonnull
  @JsonProperty("projectId")
  private UUID projectId;

  /** The columns, in their configured order. */
  @Nonnull
  @JsonProperty("environments")
  private List<EnvironmentDto> environments;

  @Nonnull
  @JsonProperty("rows")
  private List<MatrixRowDto> rows;
}
