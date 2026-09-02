package one.epicsoft.vc.adapter;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The end-of-life lookup against recorded answers. The database states a cycle
 * either with a date or with a plain yes/no, and both have to come out the same
 * way — a deadline that is read wrongly is worse than none at all.
 */
class EndOfLifeAdapterTest {

  private final EndOfLifeAdapter adapter = new EndOfLifeAdapter(new SourceClient());

  private static final String PAYLOAD = """
      [
        {"cycle": "18", "eol": false, "support": "2030-11-14"},
        {"cycle": "17", "eol": "2029-11-08", "support": "2029-11-08"},
        {"cycle": "13", "eol": "2025-11-13", "support": "2025-11-13"}
      ]
      """;

  @Test
  void readsTheCycleThatWasAskedFor() {
    final EndOfLifeAdapter.EndOfLife found = this.adapter.parse(PAYLOAD, "17");

    assertEquals(LocalDate.parse("2029-11-08"), found.eolDate());
    assertEquals(LocalDate.parse("2029-11-08"), found.supportedUntil());
    assertFalse(found.eol());
  }

  @Test
  void recognisesACycleWhoseSupportHasEnded() {
    assertTrue(this.adapter.parse(PAYLOAD, "13").eol());
  }

  @Test
  void readsAPlainYesOrNoWhereNoDateIsGiven() {
    final EndOfLifeAdapter.EndOfLife found = this.adapter.parse(PAYLOAD, "18");

    // Still supported, and no day named — the date stays empty rather than being
    // invented, and nothing may read the missing date as "over".
    assertNull(found.eolDate());
    assertFalse(found.eol());
  }

  @Test
  void takesTheNewestCycleWhenNoneWasNamed() {
    assertEquals(LocalDate.parse("2030-11-14"), this.adapter.parse(PAYLOAD, null).supportedUntil());
  }

  @Test
  void namesTheCycleItCouldNotFind() {
    // The product is right and the release line is not; only the name says which
    // one to correct.
    final SourceException exception = assertThrows(SourceException.class, () -> this.adapter.parse(PAYLOAD, "99"));
    assertTrue(exception.getMessage().contains("99"));
  }

  @Test
  void reportsAnAnswerItCannotRead() {
    assertThrows(SourceException.class, () -> this.adapter.parse("{\"message\": \"Not Found\"}", "17"));
    assertThrows(SourceException.class, () -> this.adapter.parse("[]", "17"));
    assertThrows(SourceException.class, () -> this.adapter.parse("<html>404</html>", "17"));
  }
}
