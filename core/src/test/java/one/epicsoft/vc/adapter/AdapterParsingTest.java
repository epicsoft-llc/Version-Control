package one.epicsoft.vc.adapter;

import one.epicsoft.vc.version.VersionCandidate;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The adapters against recorded answers.
 *
 * <p>No network: the payloads are pasted in, shortened to the fields that are
 * read. What is being tested is the half that breaks when an upstream API
 * changes shape — and that it then fails loudly instead of returning a version
 * nobody can trace.
 */
class AdapterParsingTest {

  private final DockerHubAdapter dockerHub = new DockerHubAdapter(new SourceClient());
  private final GitHubReleaseAdapter gitHub = new GitHubReleaseAdapter(new SourceClient(), null);
  private final AptRepositoryAdapter apt = new AptRepositoryAdapter(new SourceClient());

  @Test
  void readsTagsDatesAndDigestsFromDockerHub() {
    final String payload = """
        {"count": 2, "results": [
          {"name": "17.2-alpine", "last_updated": "2026-01-05T10:00:00Z", "digest": "sha256:aaa"},
          {"name": "17.3-alpine", "last_updated": "2026-02-05T10:00:00Z", "digest": "sha256:bbb"}
        ]}
        """;

    final List<VersionCandidate> candidates = this.dockerHub.parse(payload);

    assertEquals(2, candidates.size());
    assertEquals("17.2-alpine", candidates.getFirst().tag());
    assertEquals(Instant.parse("2026-01-05T10:00:00Z"), candidates.getFirst().releasedAt());
    assertEquals("sha256:aaa", candidates.getFirst().digest());
  }

  @Test
  void reportsAChangedDockerHubPayloadInsteadOfGuessing() {
    // The API is undocumented. When it changes, the check has to say so.
    assertThrows(SourceException.class, () -> this.dockerHub.parse("{\"tags\": []}"));
    assertThrows(SourceException.class, () -> this.dockerHub.parse("{\"results\": [{\"digest\": \"sha256:a\"}]}"));
    assertThrows(SourceException.class, () -> this.dockerHub.parse("<html>rate limited</html>"));
  }

  @Test
  void readsReleasesFromGitHubAndLeavesDraftsOut() {
    final String payload = """
        [
          {"tag_name": "v2.1.0", "published_at": "2026-02-01T00:00:00Z", "draft": false},
          {"tag_name": "v2.2.0", "published_at": null, "draft": true}
        ]
        """;

    final List<VersionCandidate> candidates = this.gitHub.parseReleases(payload);

    assertEquals(1, candidates.size());
    assertEquals("v2.1.0", candidates.getFirst().tag());
  }

  @Test
  void readsTagsFromGitHubWithoutDates() {
    final List<VersionCandidate> candidates = this.gitHub.parseTags("[{\"name\": \"v1.4.0\"}]");

    assertEquals("v1.4.0", candidates.getFirst().tag());
    // No timestamps here, which is why only SemVer can order this source.
    assertNull(candidates.getFirst().releasedAt());
  }

  @Test
  void reportsAChangedGitHubPayload() {
    assertThrows(SourceException.class, () -> this.gitHub.parseReleases("{\"message\": \"Not Found\"}"));
    assertThrows(SourceException.class, () -> this.gitHub.parseTags("[{\"commit\": {}}]"));
  }

  @Test
  void readsEveryPackageVersionFromAnAptIndex() {
    final String payload = """
        Package: docker-ce
        Version: 5:29.7.1-1~ubuntu.24.04~noble
        Architecture: amd64
        Description: Docker
         a continuation line that belongs to the field above

        Package: containerd.io
        Version: 2.3.3-1

        Package: docker-ce
        Version: 5:29.7.2-1~ubuntu.24.04~noble
        """;

    final List<VersionCandidate> candidates = this.apt.parse(payload);

    // Every version is in the file, not only the newest, and both versions of
    // docker-ce have to survive parsing for the comparison to find the newer one.
    assertEquals(3, candidates.size());
    assertTrue(candidates.stream().anyMatch(candidate -> "containerd.io".equals(candidate.packageName())));
    assertEquals(2, candidates.stream().filter(c -> "docker-ce".equals(c.packageName())).count());
    // The last stanza has no blank line after it and still counts.
    assertTrue(candidates.stream().anyMatch(c -> "5:29.7.2-1~ubuntu.24.04~noble".equals(c.tag())));
  }

  @Test
  void readsAPageThatIsNotTheLastOne() {
    // parse() answers per page and no longer decides whether a repository is
    // empty; that call belongs to the loop that walks the pages.
    final String payload = "{\"results\": [{\"name\": \"1.0\"}]}";

    assertEquals(1, this.dockerHub.parse(payload).size());
    assertEquals(0, this.dockerHub.parse("{\"results\": []}").size());
  }

  @Test
  void reportsAnAptIndexThatHoldsNothing() {
    assertThrows(SourceException.class, () -> this.apt.parse("<html>404</html>"));
  }
}
