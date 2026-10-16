package one.epicsoft.vc.data;

import jakarta.annotation.Nonnull;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * A notification waiting to be sent, and what to write down once it has gone out.
 *
 * @param message       blank when there is nothing to say
 * @param keysByProject what was reported, per project
 */
public record NotificationDraft(@Nonnull NotificationMessage message, @Nonnull Map<UUID, List<String>> keysByProject) {

  @Nonnull
  public static NotificationDraft empty() {
    return new NotificationDraft(new NotificationMessage("", ""), Map.of());
  }

  public boolean isEmpty() {
    return this.message.body().isBlank();
  }
}
