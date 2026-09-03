package one.epicsoft.vc.adapter;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static one.epicsoft.vc.util.VcUtil.isConfigured;

/**
 * Sends the daily notification, where one is configured.
 *
 * <p>Without a token the feature is off and stays quiet about it. It is not an
 * error state: somebody who did not set a token did not want notifications, and
 * a component that complains about that every morning is worse than none.
 *
 * <p><strong>The token is part of the address.</strong> Telegram puts it in the
 * path, so every I/O error message from the HTTP client contains it verbatim —
 * which is why nothing from that exception is ever passed on or logged here. A
 * token in a log line is a token in a backup.
 */
@Slf4j
@Component
public class TelegramClient {

  private static final String URL = "https://api.telegram.org/bot%s/sendMessage";

  private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
  private static final Duration READ_TIMEOUT = Duration.ofSeconds(10);

  /**
   * What Telegram accepts in one message, minus room for the last line.
   *
   * <p>The limit is 4096 characters. Exceeding it is not a partial failure: the
   * whole message is rejected, so a first run against a full inventory would
   * fail every morning and never deliver anything at all.
   */
  private static final int MAX_LENGTH = 3900;

  private static final JsonMapper JSON = JsonMapper.builder().build();

  private final RestClient restClient;
  private final String token;
  private final String chatId;

  public TelegramClient(@Value("${vc.telegram.bot-token:}") @Nullable final String token,
                        @Value("${vc.telegram.chat-id:}") @Nullable final String chatId) {
    // USE_ENV counts as unset: sent as a token it would turn a switched-off
    // feature into a guaranteed failure every single day.
    this.token = isConfigured(token) ? token.trim() : null;
    this.chatId = isConfigured(chatId) ? chatId.trim() : null;

    final SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(CONNECT_TIMEOUT);
    factory.setReadTimeout(READ_TIMEOUT);
    this.restClient = RestClient.builder().requestFactory(factory).build();
  }

  /** True when a token and a chat are configured; false switches the job off. */
  public boolean isConfiguredForSending() {
    return this.token != null && this.chatId != null;
  }

  /**
   * Sends the text, split into as many messages as the limit requires.
   *
   * @throws SourceException when a part could not be delivered; the message of
   *                         that exception never carries anything from the
   *                         client, because that would carry the token
   */
  public void send(@Nonnull final String text) {
    if (!this.isConfiguredForSending()) {
      return;
    }
    for (final String part : split(text)) {
      this.sendOne(part);
    }
  }

  private void sendOne(@Nonnull final String text) {
    try {
      this.restClient.post()
                     .uri(URL.formatted(this.token))
                     .contentType(MediaType.APPLICATION_JSON)
                     .body(JSON.writeValueAsString(Map.of("chat_id", this.chatId,
                                                          "text", text,
                                                          "disable_web_page_preview", true)))
                     .retrieve()
                     .toBodilessEntity();
    } catch (final Exception exception) {
      // Neither the message nor the cause is passed on: the client puts the
      // request URI into both, and the URI is the token.
      log.debug("Sending the notification failed with {}", exception.getClass().getSimpleName());
      throw new SourceException("Sending the notification failed; see the Telegram bot configuration");
    }
  }

  /**
   * Splits on line boundaries where it can, and mid-line only for a single line
   * longer than the limit — a message cut in the middle of a component name is
   * still better than one that is never sent. Nothing is dropped either way.
   *
   * <p>Visible to the test: the sending itself needs a real bot, and the rule
   * that has to hold is which text ends up in which part.
   */
  @Nonnull
  static List<String> split(@Nonnull final String text) {
    if (text.length() <= MAX_LENGTH) {
      return List.of(text);
    }
    final List<String> parts = new ArrayList<>();
    final StringBuilder part = new StringBuilder();
    for (final String line : text.split("\n", -1)) {
      if (!part.isEmpty() && part.length() + line.length() + 1 > MAX_LENGTH) {
        parts.add(part.toString());
        part.setLength(0);
      }
      if (line.length() > MAX_LENGTH) {
        // Cut into as many parts as it takes, never truncated: what is dropped
        // here is also written down as reported, so it would never come back.
        for (int start = 0; start < line.length(); start += MAX_LENGTH) {
          parts.add(line.substring(start, Math.min(start + MAX_LENGTH, line.length())));
        }
        continue;
      }
      part.append(part.isEmpty() ? "" : "\n").append(line);
    }
    if (!part.isEmpty()) {
      parts.add(part.toString());
    }
    return parts;
  }
}
