package one.epicsoft.vc.adapter;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

import static one.epicsoft.vc.util.VcUtil.nonBlank;

/**
 * The one place this application talks to an upstream source.
 *
 * <p>Kept deliberately small: a GET, a timeout, and the body as text. Everything
 * about what the body means belongs to the adapter that asked, and everything
 * about which version wins belongs to the rule.
 *
 * <p>The timeouts matter more than they look. The check runs in a scheduled job,
 * one component per tick; a source that accepts the connection and then says
 * nothing would otherwise hold the job until the process is restarted.
 */
@Slf4j
@Component
public class SourceClient {

  /**
   * Short on purpose. The check runs inside a transaction, so a source that
   * accepts the connection and then says nothing holds a database connection for
   * as long as this allows. Fifteen seconds is the worst case for one component,
   * and the check is retried on the next tick anyway.
   */
  private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
  private static final Duration READ_TIMEOUT = Duration.ofSeconds(10);

  private final RestClient restClient;

  public SourceClient() {
    final SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(CONNECT_TIMEOUT);
    factory.setReadTimeout(READ_TIMEOUT);
    // Built here rather than from an injected builder: this client talks to
    // foreign servers and wants its own timeouts, not whatever the application
    // configures for anything else it might call later.
    this.restClient = RestClient.builder().requestFactory(factory).build();
  }

  @Nonnull
  public String get(@Nonnull final String url) {
    return this.get(url, Map.of());
  }

  /**
   * @param headers sent as given; a value that is blank is left out, so an
   *                unconfigured token means an anonymous request rather than one
   *                carrying the word "USE_ENV"
   */
  @Nonnull
  public String get(@Nonnull final String url, @Nonnull final Map<String, String> headers) {
    final Map<String, String> used = new LinkedHashMap<>();
    headers.forEach((name, value) -> {
      if (nonBlank(value)) {
        used.put(name, value);
      }
    });
    log.debug("Fetching {} with {} header(s)", url, used.size());
    try {
      final String body = this.restClient.get()
                                         .uri(url)
                                         .headers(target -> used.forEach(target::add))
                                         .retrieve()
                                         .body(String.class);
      return requireBody(body, url);
    } catch (final SourceException exception) {
      throw exception;
    } catch (final Exception exception) {
      // The message names the address, never a header: a token in a log line is a
      // token in a backup.
      throw new SourceException("Request to " + url + " failed: " + exception.getMessage(), exception);
    }
  }

  @Nonnull
  private static String requireBody(@Nullable final String body, @Nonnull final String url) {
    if (!nonBlank(body)) {
      throw new SourceException("Request to " + url + " returned an empty body");
    }
    return body;
  }

  /** Bearer header for the sources that take one, empty when nothing is configured. */
  @Nonnull
  public static Map<String, String> bearer(@Nullable final String token) {
    return nonBlank(token) ? Map.of(HttpHeaders.AUTHORIZATION, "Bearer " + token.trim()) : Map.of();
  }
}
