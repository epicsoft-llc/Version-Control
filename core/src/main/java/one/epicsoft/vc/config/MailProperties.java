package one.epicsoft.vc.config;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import lombok.Getter;
import lombok.Setter;
import one.epicsoft.vc.enums.MailSecurity;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import java.util.List;

/**
 * The mail channel of the daily notification, for any SMTP server.
 *
 * <p>Off until a server, a sender and at least one recipient are set — like an unset Telegram
 * token, that is a decision and not an error.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "vc.mail")
public class MailProperties {

  @Nullable
  private String host;

  /** Unset means the standard port of the chosen security. */
  @Nullable
  private Integer port;

  @Nonnull
  private MailSecurity security = MailSecurity.STARTTLS;

  /** Without a username no login is attempted. */
  @Nullable
  private String username;

  @Nullable
  private String password;

  /** The sender, optionally with a display name: {@code Version Control <vc@example.com>}. */
  @Nullable
  private String from;

  @Nonnull
  private List<String> to = List.of();
}
