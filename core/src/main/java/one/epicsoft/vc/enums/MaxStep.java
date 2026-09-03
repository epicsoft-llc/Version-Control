package one.epicsoft.vc.enums;

/**
 * How far a single suggested step may go. MAJOR — no limit — is a legitimate and
 * frequent choice, not an exception.
 */
public enum MaxStep {

  PATCH,
  MINOR,
  MAJOR,

  /** Show what is available, suggest nothing. */
  NONE
}
