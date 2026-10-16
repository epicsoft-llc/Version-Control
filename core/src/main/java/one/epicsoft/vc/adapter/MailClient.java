package one.epicsoft.vc.adapter;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import one.epicsoft.vc.config.MailProperties;
import one.epicsoft.vc.data.NotificationMessage;
import one.epicsoft.vc.enums.ChannelType;
import one.epicsoft.vc.enums.MailSecurity;
import one.epicsoft.vc.util.VcUtil;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNullElse;
import static one.epicsoft.vc.util.VcUtil.isConfigured;
import static one.epicsoft.vc.util.VcUtil.trimmed;

/**
 * Sends the daily notification by mail, through any SMTP server.
 *
 * <p>The sender is built here rather than by Spring Boot's mail auto-configuration: that one would
 * also add a health check, and a mail server that is down for an hour would mark the whole
 * application as down.
 */
@Component
public class MailClient implements NotificationChannel {

  private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
  private static final Duration READ_TIMEOUT = Duration.ofSeconds(30);

  @Nonnull
  private final JavaMailSenderImpl sender;

  @Nullable
  private final String from;

  @Nonnull
  private final List<String> to;

  public MailClient(@Nonnull final MailProperties properties) {
    this.sender = buildSender(properties);
    this.from = isConfigured(properties.getFrom()) ? trimmed(properties.getFrom()) : null;
    this.to = properties.getTo().stream().filter(VcUtil::isConfigured).map(String::trim).toList();
  }

  @Nonnull
  @Override
  public ChannelType channelType() {
    return ChannelType.MAIL;
  }

  /** True when a server, a sender and at least one recipient are configured. */
  @Override
  public boolean isConfiguredForSending() {
    return nonNull(this.sender.getHost()) && nonNull(this.from) && !this.to.isEmpty();
  }

  /**
   * Sends one plain text mail to all recipients.
   *
   * @throws SourceException when the server did not accept it; unlike a token in an address, the
   *                         server's answer carries no secret and says what to fix
   */
  @Override
  public void send(@Nonnull final NotificationMessage message) {
    if (this.isConfiguredForSending()) {
      try {
        this.sender.send(this.build(message));
      } catch (final MailException exception) {
        throw new SourceException("Sending the notification by mail failed: %s".formatted(exception.getMessage()), exception);
      }
    }
  }

  /** The title is the subject, the lines are the text. Visible to the test. */
  @Nonnull
  SimpleMailMessage build(@Nonnull final NotificationMessage message) {
    final SimpleMailMessage mail = new SimpleMailMessage();
    mail.setFrom(this.from);
    mail.setTo(this.to.toArray(String[]::new));
    mail.setSubject(message.title());
    mail.setText(message.body());
    return mail;
  }

  /** Visible to the test: what has to hold is which connection the settings produce. */
  @Nonnull
  static JavaMailSenderImpl buildSender(@Nonnull final MailProperties properties) {
    final MailSecurity security = properties.getSecurity();
    final JavaMailSenderImpl sender = new JavaMailSenderImpl();
    sender.setHost(isConfigured(properties.getHost()) ? trimmed(properties.getHost()) : null);
    sender.setPort(requireNonNullElse(properties.getPort(), security.getDefaultPort()));
    sender.setDefaultEncoding(StandardCharsets.UTF_8.name());

    final Properties settings = sender.getJavaMailProperties();
    // Without timeouts a server that accepts the connection and then says nothing holds the job forever.
    settings.setProperty("mail.smtp.connectiontimeout", String.valueOf(CONNECT_TIMEOUT.toMillis()));
    settings.setProperty("mail.smtp.timeout", String.valueOf(READ_TIMEOUT.toMillis()));
    settings.setProperty("mail.smtp.writetimeout", String.valueOf(READ_TIMEOUT.toMillis()));
    if (isConfigured(properties.getUsername())) {
      sender.setUsername(trimmed(properties.getUsername()));
      sender.setPassword(isConfigured(properties.getPassword()) ? properties.getPassword() : null);
      settings.setProperty("mail.smtp.auth", "true");
    }
    settings.putAll(switch (security) {
      // Required, not attempted: a server that offers no STARTTLS would otherwise get the login in clear text.
      case STARTTLS -> Map.of("mail.smtp.starttls.enable", "true",
                              "mail.smtp.starttls.required", "true",
                              "mail.smtp.ssl.checkserveridentity", "true");
      case TLS -> Map.of("mail.smtp.ssl.enable", "true",
                         "mail.smtp.ssl.checkserveridentity", "true");
      case NONE -> Map.of();
    });
    return sender;
  }
}
