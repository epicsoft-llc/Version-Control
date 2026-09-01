package one.epicsoft.vc.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.OffsetDateTime;
import java.util.List;

/** Single error shape for every failed request. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ErrorDto {

  @Nonnull
  @JsonProperty("timestamp")
  private OffsetDateTime timestamp;

  @Nonnull
  @JsonProperty("status")
  private Integer status;

  /** Translated text, ready to display. */
  @Nonnull
  @JsonProperty("message")
  private String message;

  /** Field errors of a failed validation; empty otherwise, never null. */
  @Nonnull
  @JsonProperty("fields")
  private List<FieldErrorDto> fields;

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class FieldErrorDto {

    @Nonnull
    @JsonProperty("field")
    private String field;

    @Nullable
    @JsonProperty("message")
    private String message;
  }
}
