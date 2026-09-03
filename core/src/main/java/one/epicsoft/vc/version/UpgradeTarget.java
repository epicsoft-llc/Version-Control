package one.epicsoft.vc.version;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * What can be done now, kept apart from what merely exists.
 *
 * @param target    the highest version the step limit allows from here — this is
 *                  the one that becomes a task
 * @param beyond    a version that exists but sits past the limit; a note, not a
 *                  demand, and deliberately never a task
 * @param limitedBy true when the limit is what stops the target from being the
 *                  newest available version
 */
public record UpgradeTarget(@Nullable String target, @Nullable String beyond, boolean limitedBy) {

  /**
   * Nothing to propose: either everything is current, or the component is set to
   * take no updates at all.
   */
  @Nonnull
  public static UpgradeTarget none() {
    return new UpgradeTarget(null, null, false);
  }

  public boolean hasTarget() {
    return target != null;
  }
}
