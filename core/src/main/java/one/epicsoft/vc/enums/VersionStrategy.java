package one.epicsoft.vc.enums;

/** How the newest of the matching candidates is determined. */
public enum VersionStrategy {

  /** Newest by publication date, for sources that carry timestamps. */
  LATEST_BY_DATE,

  /** Semantic version ordering, for sources that do not. */
  SEMVER,

  /** Manifest digest instead of the tag — the only way to notice a moving tag changing. */
  DIGEST,

  /** Debian version ordering: epoch first, and a tilde sorts before an empty remainder. */
  DEB_VERSION
}
