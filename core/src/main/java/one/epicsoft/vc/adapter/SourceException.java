package one.epicsoft.vc.adapter;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.io.Serial;

/**
 * The source could not be reached, or its answer could not be read.
 *
 * <p>Always ends as an ERROR on the check. The message is technical and for the
 * log and the detail page, never a translated sentence — the reader of a failed
 * check is the person who has to fix the rule or the address.
 */
public class SourceException extends RuntimeException {

  @Serial
  private static final long serialVersionUID = 1L;

  public SourceException(@Nonnull final String message) {
    super(message);
  }

  public SourceException(@Nonnull final String message, @Nullable final Throwable cause) {
    super(message, cause);
  }
}
