package one.epicsoft.vc.version;

import jakarta.annotation.Nonnull;
import java.io.Serial;

/**
 * The rule and the data cannot answer the question together.
 *
 * <p>Caught by the check, which turns it into an ERROR carrying this message.
 * The one thing it must never become is a silent "up to date".
 */
public class VersionRuleException extends RuntimeException {

  @Serial
  private static final long serialVersionUID = 1L;

  public VersionRuleException(@Nonnull final String message) {
    super(message);
  }
}
