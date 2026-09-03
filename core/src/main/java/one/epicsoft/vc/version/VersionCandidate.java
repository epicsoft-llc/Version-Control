package one.epicsoft.vc.version;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.time.Instant;

/**
 * One version a source offers, as far as the source says anything about it.
 *
 * <p>Two of the three fields are optional because the sources differ: a registry
 * that only lists tag names gives neither a date nor a digest, and the selection
 * has to say so rather than fall back on a guess.
 *
 * @param tag         the tag or package version as published
 * @param releasedAt  when it was published, where the source says
 * @param digest      the image digest, where the source says
 * @param packageName the package it belongs to, for a component made of several
 */
public record VersionCandidate(@Nonnull String tag,
                               @Nullable Instant releasedAt,
                               @Nullable String digest,
                               @Nullable String packageName) {

  @Nonnull
  public static VersionCandidate of(@Nonnull final String tag) {
    return new VersionCandidate(tag, null, null, null);
  }

  @Nonnull
  public static VersionCandidate of(@Nonnull final String tag, @Nullable final Instant releasedAt,
                                    @Nullable final String digest) {
    return new VersionCandidate(tag, releasedAt, digest, null);
  }

  @Nonnull
  public static VersionCandidate ofPackage(@Nonnull final String packageName, @Nonnull final String version) {
    return new VersionCandidate(version, null, null, packageName);
  }
}
