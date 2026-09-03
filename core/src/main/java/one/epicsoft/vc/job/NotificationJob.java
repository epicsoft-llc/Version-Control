package one.epicsoft.vc.job;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import one.epicsoft.vc.adapter.TelegramClient;
import one.epicsoft.vc.facade.NotificationFacade;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * The daily message, once, at a fixed time.
 *
 * <p>Its own job, not a tail on the check scheduler: the checks are spread
 * across the day on purpose, and a message per check would be exactly the stream
 * of single events this is meant to replace.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "vc.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class NotificationJob {

  private final NotificationFacade notificationFacade;
  private final TelegramClient telegramClient;

  /**
   * Collect, send, then write down what went out — in that order and in three
   * steps.
   *
   * <p>The order is the point: nothing counts as reported until it has actually
   * been delivered, so a failed send costs a day rather than a whole message.
   * And the send sits between two transactions rather than inside one, so no
   * database connection waits on a foreign server.
   */
  @Scheduled(cron = "#{@notificationProperties.cron}")
  public void sendDailyNotification() {
    final NotificationFacade.NotificationDraft draft = this.notificationFacade.collect();
    if (draft.isEmpty()) {
      log.debug("Nothing to notify about");
      return;
    }
    try {
      this.telegramClient.send(draft.text());
    } catch (final RuntimeException exception) {
      // The message of this one carries nothing from the HTTP client on purpose:
      // the request address is the bot token.
      log.error("The daily notification could not be sent: {}", exception.getMessage());
      return;
    }
    this.notificationFacade.remember(draft);
    log.info("Sent the daily notification");
  }
}
