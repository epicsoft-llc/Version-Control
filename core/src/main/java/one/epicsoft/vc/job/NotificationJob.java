package one.epicsoft.vc.job;

import jakarta.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import one.epicsoft.vc.adapter.NotificationChannel;
import one.epicsoft.vc.data.NotificationDraft;
import one.epicsoft.vc.data.NotificationMessage;
import one.epicsoft.vc.facade.NotificationFacade;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.util.List;

/**
 * The daily message, once, at a fixed time, to every configured channel.
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
  private final List<NotificationChannel> notificationChannels;

  /**
   * Collect, send, then write down what went out — in that order and in three
   * steps.
   *
   * <p>The order is the point: nothing counts as reported until it has actually
   * been delivered, so a failed send costs a day rather than a whole message.
   * And the send sits between two transactions rather than inside one, so no
   * database connection waits on a foreign server.
   *
   * <p>Written down only when every configured channel delivered: a broken
   * channel repeats the news on the others rather than losing it for its own
   * readers.
   */
  @Scheduled(cron = "#{@notificationProperties.cron}")
  public void sendDailyNotification() {
    final NotificationDraft draft = this.notificationFacade.collect();
    if (draft.isEmpty()) {
      log.debug("Nothing to notify about");
      return;
    }
    boolean delivered = true;
    for (final NotificationChannel channel : this.notificationChannels) {
      if (channel.isConfiguredForSending()) {
        delivered &= this.send(channel, draft.message());
      }
    }
    if (delivered) {
      this.notificationFacade.remember(draft);
      log.info("Sent the daily notification");
    }
  }

  private boolean send(@Nonnull final NotificationChannel channel, @Nonnull final NotificationMessage message) {
    try {
      channel.send(message);
      return true;
    } catch (final RuntimeException exception) {
      // The channel words this message; Telegram's never carries the token of its address.
      log.error("The daily notification could not be sent by {}: {}", channel.channelType(), exception.getMessage());
      return false;
    }
  }
}
