package one.epicsoft.vc.enums;

/**
 * Outcome of the last check. A failure is ERROR and never silently "current":
 * a broken rule must not look like everything being in order.
 */
public enum CheckState {

  OK,
  ERROR,

  /** No machine readable source — not a failure. */
  NOT_APPLICABLE
}
