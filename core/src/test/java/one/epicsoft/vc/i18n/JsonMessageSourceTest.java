package one.epicsoft.vc.i18n;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class JsonMessageSourceTest {

  private final JsonMessageSource source = new JsonMessageSource(List.of("en", "de"), "en");

  @Test
  void resolvesTheRequestedLanguage() {
    assertEquals("Der angeforderte Eintrag existiert nicht.",
                 this.source.getMessage("error.notFound", null, Locale.GERMAN));
  }

  @Test
  void fallsBackToTheDefaultLanguage() {
    // French is not loaded at all — the reader still gets a sentence, not a key.
    assertEquals("The requested entry does not exist.",
                 this.source.getMessage("error.notFound", null, Locale.FRENCH));
  }

  @Test
  void returnsNoMessageForAnUnknownKey() {
    // Loud in the log, absent in the response: a raw key in the user interface
    // reads like a defect to whoever sees it.
    assertNull(this.source.getMessage("error.doesNotExist", null, null, Locale.ENGLISH));
  }
}
