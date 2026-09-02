package one.epicsoft.vc.exception;

import jakarta.annotation.Nonnull;
import java.io.Serial;

/** The request collides with something that already exists or with the current state. */
public class AppConflictException extends AppException {

  @Serial
  private static final long serialVersionUID = 1L;

  protected AppConflictException(@Nonnull final String message, @Nonnull final String messageKey) {
    super(message, messageKey);
  }

  @Nonnull
  public static AppConflictException duplicateName(@Nonnull final String type, @Nonnull final String name) {
    return new AppConflictException("%s with name '%s' already exists".formatted(type, name), "error.duplicateName");
  }
}
