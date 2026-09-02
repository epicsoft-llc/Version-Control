package one.epicsoft.vc.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.util.List;

/**
 * What a rule would select, shown while it is being written.
 *
 * <p>Not optional and not a convenience: without it a regular expression is
 * written against a tag list nobody can see, and that is where rule maintenance
 * fails in practice.
 */
@Data
@NoArgsConstructor
public class VersionPreviewDto {

  /**
   * Why nothing matched, where nothing matched. A half written pattern is the
   * normal state of this screen, so it answers with an explanation rather than
   * an error — the reader is in the middle of writing the rule.
   */
  @Nullable
  @JsonProperty("message")
  private String message;

  /** The tag that would win, first of the matching ones. */
  @Nullable
  @JsonProperty("winner")
  private String winner;

  @Nonnull
  @JsonProperty("matching")
  private List<Candidate> matching;

  @Data
  @NoArgsConstructor
  public static class Candidate {

    @Nonnull
    @JsonProperty("tag")
    private String tag;

    @Nullable
    @JsonProperty("releasedAt")
    private Instant releasedAt;

    @Nullable
    @JsonProperty("packageName")
    private String packageName;
  }
}
