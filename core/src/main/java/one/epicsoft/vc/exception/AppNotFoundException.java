package one.epicsoft.vc.exception;

import jakarta.annotation.Nonnull;
import java.io.Serial;
import java.util.UUID;

/**
 * Thrown by every {@code get*} method when the resource is absent.
 * {@code find*} methods return an empty {@code Optional} instead and never throw.
 */
public class AppNotFoundException extends AppException {

  @Serial
  private static final long serialVersionUID = 1L;

  private static final String MESSAGE_KEY = "error.notFound";

  protected AppNotFoundException(@Nonnull final String message) {
    super(message, MESSAGE_KEY);
  }

  @Nonnull
  public static AppNotFoundException byId(@Nonnull final String type, @Nonnull final UUID id) {
    return new AppNotFoundException("%s not found by id '%s'".formatted(type, id));
  }

  @Nonnull
  public static AppNotFoundException byName(@Nonnull final String type, @Nonnull final String name) {
    return new AppNotFoundException("%s not found by name '%s'".formatted(type, name));
  }
}
