package one.epicsoft.vc.adapter;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import one.epicsoft.vc.enums.SourceType;
import one.epicsoft.vc.jpa.entity.ComponentEntity;
import one.epicsoft.vc.version.VersionCandidate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

import static one.epicsoft.vc.util.VcUtil.isConfigured;
import static one.epicsoft.vc.util.VcUtil.nonBlank;

/**
 * GitHub releases, and tags where a repository publishes none.
 *
 * <p>The release list rather than {@code /latest}: that endpoint answers with
 * whatever the maintainers last marked as latest, which skips prereleases in a
 * way nobody controls and returns nothing at all for repositories that never
 * set the flag. Reading the list keeps the decision here, where the rule is.
 *
 * <p>The token is optional and only raises the rate limit. Unset, the check
 * still works — sixty requests an hour is a lot for a handful of components
 * checked every two days.
 */
@Slf4j
@Component
public class GitHubReleaseAdapter implements SourceAdapter {

  private static final String RELEASES_URL = "https://api.github.com/repos/%s/releases?per_page=100";
  private static final String TAGS_URL = "https://api.github.com/repos/%s/tags?per_page=100";

  /** Its own, not the one the web layer uses: this reads a foreign payload. */
  private static final JsonMapper JSON = JsonMapper.builder().build();

  private final SourceClient sourceClient;
  private final String token;

  public GitHubReleaseAdapter(@Nonnull final SourceClient sourceClient,
                              @Value("${vc.github.token:}") @Nullable final String token) {
    this.sourceClient = sourceClient;
    // USE_ENV counts as unset: sent as a token it would turn a working anonymous
    // request into a guaranteed 401 on every check.
    this.token = isConfigured(token) ? token : null;
  }

  @Override
  @Nonnull
  public SourceType sourceType() {
    return SourceType.GITHUB_RELEASE;
  }

  @Override
  @Nonnull
  public List<VersionCandidate> fetchCandidates(@Nonnull final ComponentEntity component) {
    if (!nonBlank(component.getSourceRef())) {
      throw new SourceException("Component has no source reference; expected owner/repository");
    }
    final String repository = component.getSourceRef().trim();
    if (!repository.contains("/")) {
      throw new SourceException("Source reference '" + repository + "' is not an owner/repository pair");
    }

    final List<VersionCandidate> releases =
      this.parseReleases(this.sourceClient.get(RELEASES_URL.formatted(repository), SourceClient.bearer(this.token)));
    if (!releases.isEmpty()) {
      return releases;
    }
    // A repository that tags but never releases is common enough to be worth the
    // second request, and the alternative would be an error nobody can act on.
    log.debug("Repository {} publishes no releases, falling back to its tags", repository);
    return this.parseTags(this.sourceClient.get(TAGS_URL.formatted(repository), SourceClient.bearer(this.token)));
  }

  /** Reads the release list; separate from the request so it can be tested offline. */
  @Nonnull
  public List<VersionCandidate> parseReleases(@Nonnull final String payload) {
    final JsonNode root = this.read(payload);
    final List<VersionCandidate> candidates = new ArrayList<>();
    for (final JsonNode entry : root) {
      if (entry.path("draft").asBoolean(false)) {
        // A draft is not published; treating it as a candidate would propose a
        // version that nobody outside the project can pull.
        continue;
      }
      final String tag = entry.path("tag_name").asString(null);
      if (!nonBlank(tag)) {
        throw new SourceException("GitHub returned a release without a tag name; the API has changed");
      }
      candidates.add(VersionCandidate.of(tag, parseInstant(entry.path("published_at").asString(null)), null));
    }
    return candidates;
  }

  /** Reads the tag list, which carries no dates — only SemVer can order these. */
  @Nonnull
  public List<VersionCandidate> parseTags(@Nonnull final String payload) {
    final JsonNode root = this.read(payload);
    final List<VersionCandidate> candidates = new ArrayList<>();
    for (final JsonNode entry : root) {
      final String name = entry.path("name").asString(null);
      if (!nonBlank(name)) {
        throw new SourceException("GitHub returned a tag without a name; the API has changed");
      }
      candidates.add(VersionCandidate.of(name));
    }
    if (candidates.isEmpty()) {
      throw new SourceException("Repository publishes neither releases nor tags");
    }
    return candidates;
  }

  @Nonnull
  private JsonNode read(@Nonnull final String payload) {
    final JsonNode root;
    try {
      root = JSON.readTree(payload);
    } catch (final Exception exception) {
      throw new SourceException("GitHub answered with something that is not JSON", exception);
    }
    if (!root.isArray()) {
      throw new SourceException("GitHub answered with " + root.getNodeType() + " where a list was expected");
    }
    return root;
  }

  @Nullable
  private static Instant parseInstant(@Nullable final String value) {
    if (!nonBlank(value)) {
      return null;
    }
    try {
      return Instant.parse(value);
    } catch (final DateTimeParseException exception) {
      throw new SourceException("GitHub sent an unreadable date: " + value, exception);
    }
  }
}
