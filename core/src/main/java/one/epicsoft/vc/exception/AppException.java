package one.epicsoft.vc.exception;

import jakarta.annotation.Nonnull;
import java.io.Serial;

/**
 * Base of every domain exception. Carries the i18n key of the message shown to
 * the user; the exception message itself stays English and is for the log.
 */
public abstract class AppException extends RuntimeException {

  @Serial
  private static final long serialVersionUID = 1L;

  private final transient String messageKey;

  protected AppException(@Nonnull final String message, @Nonnull final String messageKey) {
    super(message);
    this.messageKey = messageKey;
  }

  @Nonnull
  public String getMessageKey() {
    return this.messageKey;
  }
}
