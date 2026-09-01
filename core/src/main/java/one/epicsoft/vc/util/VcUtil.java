package one.epicsoft.vc.util;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.Objects;

/**
 * The handful of null-safe helpers the code conventions assume.
 *
 * <p>Kept deliberately small: everything here exists because a plain
 * {@code equals} or {@code isBlank} call would need a null check at every call
 * site, not because a utility class is a good place for logic.
 */
public final class VcUtil {

  /** Placeholder in {@code application.yml} for values that only ever come from the environment. */
  public static final String USE_ENV = "USE_ENV";

  private VcUtil() {
    // utility class
  }

  public static boolean nonBlank(@Nullable final String value) {
    return Objects.nonNull(value) && !value.isBlank();
  }

  public static boolean isBlank(@Nullable final String value) {
    return Objects.isNull(value) || value.isBlank();
  }

  /**
   * True when the value is a usable configuration value.
   *
   * <p>{@link #USE_ENV} counts as absent. Without this check a component believes
   * it is configured and calls the upstream API with the literal {@code USE_ENV}
   * as its token — turning a cleanly disabled feature into a guaranteed failure
   * on every single call.
   */
  public static boolean isConfigured(@Nullable final String value) {
    return nonBlank(value) && !USE_ENV.equals(value.trim());
  }

  public static boolean isTrue(@Nullable final Boolean value) {
    return Boolean.TRUE.equals(value);
  }

  public static boolean isEquals(@Nullable final Object first, @Nullable final Object second) {
    return Objects.equals(first, second);
  }

  public static boolean nonEquals(@Nullable final Object first, @Nullable final Object second) {
    return !Objects.equals(first, second);
  }

  @Nonnull
  public static String trimmed(@Nullable final String value) {
    return Objects.isNull(value) ? "" : value.trim();
  }
}
