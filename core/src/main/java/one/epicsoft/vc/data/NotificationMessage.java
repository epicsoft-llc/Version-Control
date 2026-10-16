package one.epicsoft.vc.data;

import jakarta.annotation.Nonnull;

/**
 * The daily notification as every channel receives it.
 *
 * @param title the heading: the subject of a mail, the first line of a chat message
 * @param body  the lines below it
 */
public record NotificationMessage(@Nonnull String title, @Nonnull String body) {
}
