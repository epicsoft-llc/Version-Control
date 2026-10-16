package one.epicsoft.vc.adapter;

import jakarta.annotation.Nonnull;
import one.epicsoft.vc.data.NotificationMessage;
import one.epicsoft.vc.enums.ChannelType;

/**
 * One way of delivering the daily notification.
 *
 * <p>Each channel switches itself on through its own configuration, and the job sends to every
 * channel that is configured. A new channel is a new implementation and a constant in
 * {@link ChannelType}; nothing else changes.
 */
public interface NotificationChannel {

  @Nonnull
  ChannelType channelType();

  /** True when everything the channel needs is configured. */
  boolean isConfiguredForSending();

  /**
   * Delivers the whole message; does nothing while the channel is not configured.
   *
   * @throws SourceException when the message could not be delivered
   */
  void send(@Nonnull NotificationMessage message);
}
