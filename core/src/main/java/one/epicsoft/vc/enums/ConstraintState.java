package one.epicsoft.vc.enums;

/**
 * Whether a dependency holds — with a third answer that has to stay visible.
 *
 * <p>A version that cannot be read is {@link #UNKNOWN} and never quietly
 * {@link #SATISFIED}. A moving tag or an unusual scheme is exactly the case
 * where nobody checked by hand either, and passing it silently is how an
 * unsupported combination runs for a year without anybody noticing.
 */
public enum ConstraintState {

  SATISFIED,
  VIOLATED,
  UNKNOWN
}
