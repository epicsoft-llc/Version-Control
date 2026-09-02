package one.epicsoft.vc.enums;

/**
 * The sections of the task list, in the order they are shown.
 *
 * <p>The order is the point: what is out of support comes before what is merely
 * behind, and a check that has been failing for days comes before a cosmetic
 * warning. The list answers "what has to be done" rather than leaving that to be
 * worked out from a table.
 *
 * <p>Two sections cannot be filled yet — the dependency check and the
 * end-of-life watch are later steps. They are named here so that the order is
 * decided once, in one place, rather than rearranged later.
 */
public enum TaskSection {

  /** Out of support, or below the escalation threshold. */
  EOL_EXCEEDED,

  /** A dependency that is violated right now. */
  DEPENDENCY_VIOLATED,

  /** A newer version is available and nobody has decided anything. */
  UPDATE_OPEN,

  /** Postponed, and the date it was postponed to has passed. */
  SNOOZE_EXPIRED,

  /** Blocked by another component; the entry names it. */
  BLOCKED,

  /** The check has been failing longer than the grace period. */
  CHECK_ERROR,

  /** A tag that moves, running in a production environment. */
  MOVING_TAG_IN_PRODUCTION,

  /** Support ends within the warning threshold. */
  EOL_WARNING
}
