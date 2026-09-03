package one.epicsoft.vc.enums;

/** Where the update check looks for newer versions. */
public enum SourceType {

  DOCKER_HUB,
  GHCR,
  GENERIC_REGISTRY,
  GITHUB_RELEASE,
  APT_REPOSITORY,

  /**
   * No machine readable source. The component is maintained by hand and never
   * produces an error state — this case is guaranteed to occur.
   */
  MANUAL
}
