package one.epicsoft.vc.exception;

import jakarta.annotation.Nonnull;
import java.io.Serial;
import java.util.UUID;

/** The request is well formed but asks for something the domain does not allow. */
public class AppValidationException extends AppException {

  @Serial
  private static final long serialVersionUID = 1L;

  protected AppValidationException(@Nonnull final String message, @Nonnull final String messageKey) {
    super(message, messageKey);
  }

  @Nonnull
  public static AppValidationException differentProjects(@Nonnull final UUID componentId, @Nonnull final UUID environmentId) {
    return new AppValidationException(
        "Component '%s' and environment '%s' belong to different projects".formatted(componentId, environmentId),
        "error.differentProjects");
  }

  @Nonnull
  public static AppValidationException projectCannotChange(@Nonnull final String type, @Nonnull final UUID id) {
    return new AppValidationException("%s '%s' cannot be moved to another project".formatted(type, id),
                                      "error.projectCannotChange");
  }

  @Nonnull
  public static AppValidationException parameterRequired(@Nonnull final String parameter) {
    return new AppValidationException("Request is missing '%s'".formatted(parameter), "error.badRequest");
  }

  @Nonnull
  public static AppValidationException duplicatePackage(@Nonnull final String name) {
    return new AppValidationException("Package '%s' is listed twice".formatted(name), "error.duplicatePackage");
  }

  /**
   * Changing a version that is already recorded belongs in the dialog that writes
   * the history entry in the same step — there is deliberately no second way in.
   */
  @Nonnull
  public static AppValidationException versionNeedsHistory(@Nonnull final UUID componentEnvironmentId) {
    return new AppValidationException(
        "Version of '%s' is already recorded and can only be changed through the version change dialog"
            .formatted(componentEnvironmentId),
        "error.versionNeedsHistory");
  }
}
