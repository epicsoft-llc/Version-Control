package one.epicsoft.vc.adapter;

import jakarta.annotation.Nonnull;
import one.epicsoft.vc.enums.SourceType;
import one.epicsoft.vc.jpa.entity.ComponentEntity;
import one.epicsoft.vc.version.VersionCandidate;
import java.util.List;

/**
 * One way of asking an upstream source which versions exist.
 *
 * <p>An adapter reads and reports; it decides nothing. Which of the candidates
 * wins is the rule's business, so that the same rule applies to every source and
 * a new adapter cannot quietly bring its own idea of "newest".
 *
 * <p>Every adapter is defensive about the shape of what it receives: a field that
 * is missing, a payload that changed — both end as an exception and therefore as
 * an error on the check. Guessing would produce a version number nobody can trace.
 */
public interface SourceAdapter {

  @Nonnull
  SourceType sourceType();

  /**
   * Every version the source offers for this component, unfiltered and unsorted.
   *
   * @throws SourceException when the source cannot be reached or its answer cannot be read
   */
  @Nonnull
  List<VersionCandidate> fetchCandidates(@Nonnull ComponentEntity component);
}
