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

  @Nonnull
  public static AppValidationException unknownPackage(@Nonnull final String name) {
    return new AppValidationException("Package '%s' is not configured for this component".formatted(name),
                                      "error.unknownPackage");
  }

  /** An entry that records no change would be noise in the one list that must stay readable. */
  @Nonnull
  public static AppValidationException versionUnchanged(@Nonnull final UUID componentEnvironmentId,
                                                        @Nonnull final String version) {
    return new AppValidationException("Version '%s' of '%s' is already the recorded one"
                                          .formatted(version, componentEnvironmentId),
                                      "error.versionUnchanged");
  }

  /**
   * The minimum interval holds for a check somebody asked for by hand as well.
   * An exception for the manual case is the path by which the interval stops
   * existing — and the upstream sources are the ones that pay for it.
   */
  @Nonnull
  public static AppValidationException checkTooSoon(@Nonnull final UUID componentId, final long hours) {
    return new AppValidationException("Component '%s' was checked less than %d hours ago"
                                          .formatted(componentId, hours),
                                      "error.checkTooSoon");
  }

  /** BLOCKED comes from the dependency check; by hand it would never clear. */
  @Nonnull
  public static AppValidationException decisionNotSettable(@Nonnull final UUID componentId) {
    return new AppValidationException("State BLOCKED cannot be set by hand for component '%s'".formatted(componentId),
                                      "error.decisionNotSettable");
  }

  /**
   * A postponement without a date, or with one that has passed, is a note nobody
   * comes back to — which is the state this feature exists to replace.
   */
  @Nonnull
  public static AppValidationException snoozeDateRequired(@Nonnull final UUID componentId) {
    return new AppValidationException(
        "Postponing component '%s' needs a date in the future to come back on".formatted(componentId),
        "error.snoozeDateRequired");
  }

  /** A cell marked as deliberately absent cannot carry a deployed version. */
  @Nonnull
  public static AppValidationException inactiveCell(@Nonnull final UUID componentEnvironmentId) {
    return new AppValidationException("Component environment '%s' is inactive and holds no version"
                                          .formatted(componentEnvironmentId),
                                      "error.inactiveCell");
  }

  /** The same refusal for a cell that is being created and has no id yet. */
  @Nonnull
  public static AppValidationException inactiveCell(@Nonnull final UUID componentId, @Nonnull final UUID environmentId) {
    return new AppValidationException("Component '%s' is inactive in environment '%s' and holds no version"
                                          .formatted(componentId, environmentId),
                                      "error.inactiveCell");
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
