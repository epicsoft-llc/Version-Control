package one.epicsoft.vc.adapter;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import one.epicsoft.vc.jpa.entity.ComponentEntity;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Objects;

import static one.epicsoft.vc.util.VcUtil.nonBlank;

/**
 * When support for a release ends, from the public end-of-life database.
 *
 * <p>Not a source adapter: this answers a different question on its own cycle,
 * and its failures are its own. An end of support has a deadline that no
 * decision can postpone, which is exactly why it is kept apart from the update
 * check rather than folded into it.
 *
 * <p>Each product publishes a list of cycles. Which cycle applies is the one the
 * component names, or the leading segments of the version it runs — a component
 * on 17.2 is asking about cycle 17.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EndOfLifeAdapter {

  private static final String URL = "https://endoflife.date/api/%s.json";

  private static final JsonMapper JSON = JsonMapper.builder().build();

  private final SourceClient sourceClient;

  /**
   * Looks the product up and picks the cycle in question.
   *
   * @param cycle the release line to ask about; falls back to the component's
   *              configured cycle, and then to the newest one published
   * @throws SourceException when the product is unknown or the answer cannot be read
   */
  @Nonnull
  public EndOfLife fetch(@Nonnull final ComponentEntity component, @Nullable final String cycle) {
    if (!nonBlank(component.getEolProduct())) {
      throw new SourceException("Component names no end-of-life product");
    }
    final String wanted = nonBlank(cycle) ? cycle
                                          : nonBlank(component.getEolCycle()) ? component.getEolCycle() : null;
    return this.parse(this.sourceClient.get(URL.formatted(component.getEolProduct().trim())), wanted);
  }

  /** Reads the cycle list; separate from the request so it can be tested offline. */
  @Nonnull
  public EndOfLife parse(@Nonnull final String payload, @Nullable final String cycle) {
    final JsonNode root;
    try {
      root = JSON.readTree(payload);
    } catch (final Exception exception) {
      throw new SourceException("The end-of-life database answered with something that is not JSON", exception);
    }
    if (!root.isArray() || root.isEmpty()) {
      throw new SourceException("The end-of-life database knows no cycles for this product");
    }

    final JsonNode entry = nonBlank(cycle) ? findCycle(root, cycle) : root.get(0);
    if (Objects.isNull(entry)) {
      // Naming the cycle beats a generic miss: the product is right and the
      // release line is not, and only the name says which one to correct.
      throw new SourceException("The end-of-life database has no cycle '" + cycle + "' for this product");
    }
    return new EndOfLife(readDate(entry.path("eol")), readDate(entry.path("support")), isEol(entry.path("eol")));
  }

  @Nullable
  private static JsonNode findCycle(@Nonnull final JsonNode root, @Nonnull final String cycle) {
    for (final JsonNode entry : root) {
      if (cycle.equals(entry.path("cycle").asString(null))) {
        return entry;
      }
    }
    return null;
  }

  /**
   * The field is a date, or a boolean where a product says "still supported" or
   * "over" without naming a day.
   */
  @Nullable
  private static LocalDate readDate(@Nonnull final JsonNode node) {
    if (!node.isString()) {
      return null;
    }
    try {
      return LocalDate.parse(node.asString());
    } catch (final DateTimeParseException exception) {
      throw new SourceException("The end-of-life database sent an unreadable date: " + node.asString(), exception);
    }
  }

  private static boolean isEol(@Nonnull final JsonNode node) {
    if (node.isBoolean()) {
      return node.asBoolean(false);
    }
    final LocalDate date = readDate(node);
    return Objects.nonNull(date) && date.isBefore(LocalDate.now());
  }

  /**
   * What the database says about one release line.
   *
   * @param eolDate        the day support ends, where a day is named
   * @param supportedUntil the end of active support, where the product separates
   *                       that from security fixes
   * @param eol            true when support has already ended
   */
  public record EndOfLife(@Nullable LocalDate eolDate, @Nullable LocalDate supportedUntil, boolean eol) {
  }
}
