package one.epicsoft.vc.adapter;

import jakarta.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import one.epicsoft.vc.enums.SourceType;
import one.epicsoft.vc.jpa.entity.ComponentEntity;
import one.epicsoft.vc.version.VersionCandidate;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;

import static one.epicsoft.vc.util.VcUtil.nonBlank;
import static one.epicsoft.vc.util.VcUtil.trimmed;

/**
 * An APT repository, read straight from its package index.
 *
 * <p>One request answers for every package of the component: the index of a
 * suite is a single file, and Docker on Ubuntu is four packages in it. Four
 * requests for one component would be four times the traffic for the same file.
 *
 * <p>Two properties of that file decide how this is written. It lists
 * <em>every</em> version, not only the newest, and it is not reliably ordered —
 * so the newest is found by comparing versions, never by taking the first or the
 * last stanza. And it carries no timestamps at all, which is why the date
 * strategy cannot be used here and the Debian comparison has to be.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AptRepositoryAdapter implements SourceAdapter {

  private static final String URL = "%s/dists/%s/%s/binary-%s/Packages";

  private final SourceClient sourceClient;

  @Override
  @Nonnull
  public SourceType sourceType() {
    return SourceType.APT_REPOSITORY;
  }

  @Override
  @Nonnull
  public List<VersionCandidate> fetchCandidates(@Nonnull final ComponentEntity component) {
    if (!nonBlank(component.getSourceRef())) {
      throw new SourceException("Component has no source reference; expected the base URL of the repository");
    }
    if (!nonBlank(component.getAptSuite()) || !nonBlank(component.getAptComponent())
        || !nonBlank(component.getAptArchitecture())) {
      throw new SourceException("Component is missing suite, repository section or architecture");
    }
    final String url = URL.formatted(trimmed(component.getSourceRef()).replaceAll("/+$", ""),
                                     trimmed(component.getAptSuite()),
                                     trimmed(component.getAptComponent()),
                                     trimmed(component.getAptArchitecture()));
    return this.parse(this.sourceClient.get(url));
  }

  /**
   * Reads every {@code Package}/{@code Version} pair out of the index. Separate
   * from the request so that it can be tested against a recorded file.
   *
   * <p>The format is one stanza per package version, fields separated by a blank
   * line. Continuation lines start with a space and belong to the field above,
   * which is why a line that does not look like a field is skipped rather than
   * parsed.
   */
  @Nonnull
  public List<VersionCandidate> parse(@Nonnull final String payload) {
    final List<VersionCandidate> candidates = new ArrayList<>();
    String packageName = null;
    String version = null;

    for (final String rawLine : payload.split("\\R")) {
      if (rawLine.isBlank()) {
        addCandidate(candidates, packageName, version);
        packageName = null;
        version = null;
        continue;
      }
      if (rawLine.startsWith(" ") || rawLine.startsWith("\t")) {
        continue;
      }
      if (rawLine.startsWith("Package:")) {
        packageName = value(rawLine);
      } else if (rawLine.startsWith("Version:")) {
        version = value(rawLine);
      }
    }
    // A file that does not end with a blank line still holds a stanza.
    addCandidate(candidates, packageName, version);

    if (candidates.isEmpty()) {
      throw new SourceException("Package index lists no package with a version; the address may be wrong");
    }
    log.debug("APT index carried {} package versions", candidates.size());
    return candidates;
  }

  private static void addCandidate(@Nonnull final List<VersionCandidate> candidates, final String packageName,
                                   final String version) {
    if (nonBlank(packageName) && nonBlank(version)) {
      candidates.add(VersionCandidate.ofPackage(packageName, version));
    }
  }

  @Nonnull
  private static String value(@Nonnull final String line) {
    return line.substring(line.indexOf(':') + 1).trim();
  }
}
