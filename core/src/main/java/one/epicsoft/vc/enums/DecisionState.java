package one.epicsoft.vc.enums;

/** What the user decided about an available update. */
public enum DecisionState {

  OPEN,
  PLANNED,
  POSTPONED,
  SKIPPED,

  /** Derived from the dependency check, never set by hand. */
  BLOCKED
}
