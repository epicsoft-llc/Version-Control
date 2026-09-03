package one.epicsoft.vc.version;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import one.epicsoft.vc.enums.ConstraintState;
import org.semver4j.Semver;
import org.semver4j.range.RangeList;
import org.semver4j.range.RangeListFactory;
import org.springframework.stereotype.Component;
import java.util.Objects;
import java.util.regex.Pattern;

import static one.epicsoft.vc.util.VcUtil.nonBlank;

/**
 * Whether a version satisfies a range.
 *
 * <p>Ranges are written in the npm style — {@code >=14 <18}, {@code ^17.2},
 * {@code 16 || 17} — which is what {@code semver4j} understands and what most
 * people writing them have seen before.
 *
 * <p>The important part is the third answer. A version that cannot be read as a
 * semantic version, and a range that will not parse, both give
 * {@link ConstraintState#UNKNOWN}, and that is carried through to the screen.
 * Treating either as satisfied is how an unsupported combination runs for a year
 * with nobody noticing — the check would be reporting that everything is fine
 * precisely where it knows nothing at all.
 */
@Slf4j
@Component
public class ConstraintEvaluator {

  /**
   * ">=14", "^17.2", "1.2.3-rc.1", "x", "*", "||", the hyphen of a range — and a
   * bare operator, because ">= 14 < 18" is written that way as often as not and
   * means exactly the same thing. Rejecting it would file a permanent violation
   * and block every upgrade of the prerequisite behind it.
   */
  private static final Pattern TOKEN =
    Pattern.compile("^(\\|\\||-|\\*|[<>=~^]{1,2}|[<>=~^]{0,2}v?[0-9xX*][0-9a-zA-Z.+*xX-]*)$");

  @Nonnull
  public ConstraintState evaluate(@Nullable final String version, @Nullable final String range) {
    if (!nonBlank(version) || !nonBlank(range)) {
      return ConstraintState.UNKNOWN;
    }
    // What of the tag can be compared at all: the number, and a preview marker
    // where there is one. The rule lives in TagRule, because the variant of
    // 17.2-alpine and the revision of 6.4.1-15 have to mean the same thing here
    // as they do everywhere else.
    final Semver semver = Semver.coerce(TagRule.versionForComparison(version));
    if (Objects.isNull(semver)) {
      // latest, stable: no version in there at all. Exactly the case where
      // nobody checked by hand either, so the answer is that nobody knows.
      log.debug("Version {} carries no version core, so the constraint stays unknown", version);
      return ConstraintState.UNKNOWN;
    }
    final RangeList parsed = parseRange(range.trim());
    if (Objects.isNull(parsed)) {
      return ConstraintState.UNKNOWN;
    }
    return parsed.isSatisfiedBy(semver) ? ConstraintState.SATISFIED : ConstraintState.VIOLATED;
  }

  /** Whether a range can be evaluated at all — asked before one is stored. */
  public boolean isReadable(@Nullable final String range) {
    return nonBlank(range) && Objects.nonNull(parseRange(range.trim()));
  }

  /**
   * The range, or null where it cannot be read.
   *
   * <p>Parsed rather than handed straight to {@code satisfies}: that method
   * answers false for a range it did not understand, which would report a
   * violation where the truth is that nobody can tell — and the typo behind it
   * would stay hidden behind a red mark that looks like a real finding.
   */
  @Nullable
  private static RangeList parseRange(@Nonnull final String range) {
    if (!looksLikeRange(range)) {
      // The library is forgiving to a fault: it reads "at least 14" as
      // ">=14.0.0 <15.0.0", quietly turning a sentence into a constraint that
      // was never written. A wrong constraint is worse than an unreadable one,
      // because it looks like an answer.
      log.debug("Range {} does not look like a version range", range);
      return null;
    }
    try {
      // A bare x means "any version", but the library parses it to a range that
      // nothing satisfies — a dependency written that way would report a
      // violation for every version there will ever be.
      final String normalised = range.replaceAll("(?i)(^|\\s)[xX](\\s|$)", "$1*$2");
      // Prereleases count: the tool records what is deployed, and a component
      // running 2.0.0-rc.1 is inside ">=1 <3" whatever the default of the
      // library says. Excluding them would put a red mark on a combination that
      // is exactly what somebody decided to run.
      final RangeList parsed = RangeListFactory.create(normalised, true);
      return parsed.get().isEmpty() ? null : parsed;
    } catch (final RuntimeException exception) {
      log.debug("Range {} cannot be read: {}", range, exception.getMessage());
      return null;
    }
  }

  /** Every token has to be an operator with a version, a wildcard, or a joiner. */
  private static boolean looksLikeRange(@Nonnull final String range) {
    for (final String token : range.split("\\s+")) {
      if (!token.isEmpty() && !TOKEN.matcher(token).matches()) {
        return false;
      }
    }
    return true;
  }
}
