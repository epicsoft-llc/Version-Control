package one.epicsoft.vc.version;

import jakarta.annotation.Nonnull;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;
import static one.epicsoft.vc.util.VcUtil.nonBlank;

/**
 * How the candidates of a check are stored, and read back.
 *
 * <p>One place for the format. It was written in one class and read in two
 * others, and a format that is written once and parsed twice drifts apart the
 * first time somebody adds a field.
 *
 * <p>The date and the digest ride along, not only the tag: a reader that dropped
 * them could not order by date, and the strategy would then work on fresh data
 * and fail on stored data.
 */
public final class CandidateTags {

  private static final String CANDIDATE_SEPARATOR = "\n";
  private static final String FIELD_SEPARATOR = "\t";

  /** Enough for a rule preview; the full list of an old repository is noise. */
  private static final int STORED_CANDIDATES = 200;

  private CandidateTags() {
    // utility class
  }

  @Nonnull
  public static String serialize(@Nonnull final List<VersionCandidate> candidates) {
    return candidates.stream()
                     .limit(STORED_CANDIDATES)
                     .map(CandidateTags::line)
                     .collect(Collectors.joining(CANDIDATE_SEPARATOR));
  }

  @Nonnull
  public static List<VersionCandidate> parse(final String stored) {
    if (!nonBlank(stored)) {
      return List.of();
    }
    final List<VersionCandidate> candidates = new ArrayList<>();
    for (final String line : stored.split(CANDIDATE_SEPARATOR)) {
      if (line.isBlank()) {
        continue;
      }
      final String[] fields = line.split(FIELD_SEPARATOR, -1);
      candidates.add(new VersionCandidate(fields[0],
                                          fields.length > 1 && nonBlank(fields[1]) ? Instant.parse(fields[1]) : null,
                                          fields.length > 2 && nonBlank(fields[2]) ? fields[2] : null,
                                          fields.length > 3 && nonBlank(fields[3]) ? fields[3] : null));
    }
    return candidates;
  }

  @Nonnull
  private static String line(@Nonnull final VersionCandidate candidate) {
    return String.join(FIELD_SEPARATOR,
                       candidate.tag(),
                       isNull(candidate.releasedAt()) ? "" : candidate.releasedAt().toString(),
                       isNull(candidate.digest()) ? "" : candidate.digest(),
                       isNull(candidate.packageName()) ? "" : candidate.packageName());
  }
}
