package one.epicsoft.vc.adapter;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The message limit, which is the difference between a long notification and no
 * notification at all: Telegram rejects the whole message above 4096 characters
 * rather than cutting it, so a first run against a full inventory would fail
 * every morning and deliver nothing.
 */
class TelegramClientTest {

  @Test
  void keepsAShortMessageInOnePiece() {
    final List<String> parts = TelegramClient.split("Two\nlines");

    assertEquals(List.of("Two\nlines"), parts);
  }

  @Test
  void splitsALongMessageOnLineBoundaries() {
    final String text = IntStream.range(0, 400)
                                 .mapToObj(index -> "• PostgreSQL (Production) 17.2 -> 17.4 #" + index)
                                 .collect(Collectors.joining("\n"));

    final List<String> parts = TelegramClient.split(text);

    assertTrue(parts.size() > 1);
    parts.forEach(part -> assertTrue(part.length() <= 3900, "Part longer than the limit: " + part.length()));
    // Every line survives somewhere, and none of them is cut: a line broken in
    // the middle of a component name is a line nobody can act on.
    assertEquals(text.lines().count(), parts.stream().mapToLong(part -> part.lines().count()).sum());
    assertFalse(parts.stream().anyMatch(part -> part.startsWith("\n") || part.endsWith("\n")));
  }

  @Test
  void cutsASingleLineThatIsLongerThanTheLimitWithoutLosingAnyOfIt() {
    final String line = "x".repeat(5000);

    final List<String> parts = TelegramClient.split(line);

    // Cut mid-line only here, where there is no boundary to use — but cut, not
    // truncated: what is dropped is written down as reported all the same and
    // would never come back.
    assertEquals(2, parts.size());
    assertEquals(line, String.join("", parts));
  }

  @Test
  void staysOffWithoutATokenAndForThePlaceholder() {
    assertFalse(new TelegramClient(null, "42").isConfiguredForSending());
    assertFalse(new TelegramClient("  ", "42").isConfiguredForSending());
    // USE_ENV is the placeholder of the compose file. Sent as a token it would
    // turn a switched-off feature into a guaranteed failure every day.
    assertFalse(new TelegramClient("USE_ENV", "42").isConfiguredForSending());
    assertFalse(new TelegramClient("a-token", null).isConfiguredForSending());
    assertTrue(new TelegramClient("a-token", "42").isConfiguredForSending());
  }
}
