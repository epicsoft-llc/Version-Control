package one.epicsoft.vc.adapter;

import one.epicsoft.vc.config.MailProperties;
import one.epicsoft.vc.data.NotificationMessage;
import one.epicsoft.vc.enums.MailSecurity;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Which connection the settings produce. The sending itself needs a real server; what has to hold
 * here is that an unset channel stays off and a set one never falls back to clear text by itself.
 */
class MailClientTest {

  @Test
  void staysOffUntilServerSenderAndRecipientAreSet() {
    assertTrue(new MailClient(properties()).isConfiguredForSending());

    // USE_ENV is the placeholder of application.yml, not a host name.
    final MailProperties placeholder = properties();
    placeholder.setHost("USE_ENV");
    assertFalse(new MailClient(placeholder).isConfiguredForSending());

    final MailProperties noSender = properties();
    noSender.setFrom("  ");
    assertFalse(new MailClient(noSender).isConfiguredForSending());

    final MailProperties noRecipient = properties();
    noRecipient.setTo(List.of("USE_ENV", ""));
    assertFalse(new MailClient(noRecipient).isConfiguredForSending());
  }

  @Test
  void takesAnEmptyVariableAsUnset() {
    // The compose file passes every optional variable, empty when nobody set it.
    final MapConfigurationPropertySource environment = new MapConfigurationPropertySource(Map.of("vc.mail.host", "smtp.example.com",
                                                                                                 "vc.mail.port", "",
                                                                                                 "vc.mail.security", ""));

    final MailProperties properties = new Binder(environment).bindOrCreate("vc.mail", MailProperties.class);

    assertNull(properties.getPort());
    assertEquals(MailSecurity.STARTTLS, properties.getSecurity());
    assertEquals(587, MailClient.buildSender(properties).getPort());
  }

  @Test
  void takesTheStandardPortOfTheSecurityUnlessOneIsSet() {
    final MailProperties properties = properties();
    assertEquals(587, MailClient.buildSender(properties).getPort());

    properties.setSecurity(MailSecurity.TLS);
    assertEquals(465, MailClient.buildSender(properties).getPort());

    properties.setSecurity(MailSecurity.NONE);
    assertEquals(25, MailClient.buildSender(properties).getPort());

    properties.setPort(2525);
    assertEquals(2525, MailClient.buildSender(properties).getPort());
  }

  @Test
  void requiresStartTlsInsteadOfAttemptingIt() {
    final Properties settings = MailClient.buildSender(properties()).getJavaMailProperties();

    // Only attempted, a server that offers no STARTTLS would receive the login in clear text.
    assertEquals("true", settings.getProperty("mail.smtp.starttls.enable"));
    assertEquals("true", settings.getProperty("mail.smtp.starttls.required"));
    assertEquals("true", settings.getProperty("mail.smtp.ssl.checkserveridentity"));
    assertNull(settings.getProperty("mail.smtp.ssl.enable"));
  }

  @Test
  void speaksTlsFromTheFirstByteOrPlainWhenConfiguredSo() {
    final MailProperties properties = properties();

    properties.setSecurity(MailSecurity.TLS);
    final Properties tls = MailClient.buildSender(properties).getJavaMailProperties();
    assertEquals("true", tls.getProperty("mail.smtp.ssl.enable"));
    assertEquals("true", tls.getProperty("mail.smtp.ssl.checkserveridentity"));
    assertNull(tls.getProperty("mail.smtp.starttls.enable"));

    properties.setSecurity(MailSecurity.NONE);
    final Properties plain = MailClient.buildSender(properties).getJavaMailProperties();
    assertNull(plain.getProperty("mail.smtp.ssl.enable"));
    assertNull(plain.getProperty("mail.smtp.starttls.enable"));
  }

  @Test
  void logsInOnlyWithAUsername() {
    final JavaMailSenderImpl anonymous = MailClient.buildSender(properties());
    assertNull(anonymous.getUsername());
    assertNull(anonymous.getJavaMailProperties().getProperty("mail.smtp.auth"));

    final MailProperties properties = properties();
    properties.setUsername(" vc@example.com ");
    properties.setPassword("a password");
    final JavaMailSenderImpl login = MailClient.buildSender(properties);
    assertEquals("vc@example.com", login.getUsername());
    // Never trimmed: a space can be part of a password.
    assertEquals("a password", login.getPassword());
    assertEquals("true", login.getJavaMailProperties().getProperty("mail.smtp.auth"));
  }

  @Test
  void sendsTheTitleAsSubjectAndTheLinesAsText() {
    final MailProperties properties = properties();
    properties.setTo(List.of("ops@example.com", " dev@example.com "));
    final NotificationMessage message = new NotificationMessage("New since last notification", "Project\n• Update open: PostgreSQL 17.2 → 17.4");

    final SimpleMailMessage mail = new MailClient(properties).build(message);

    assertEquals("Version Control <vc@example.com>", mail.getFrom());
    assertArrayEquals(new String[]{"ops@example.com", "dev@example.com"}, mail.getTo());
    assertEquals("New since last notification", mail.getSubject());
    assertEquals("Project\n• Update open: PostgreSQL 17.2 → 17.4", mail.getText());
  }

  private static MailProperties properties() {
    final MailProperties properties = new MailProperties();
    properties.setHost("smtp.example.com");
    properties.setFrom("Version Control <vc@example.com>");
    properties.setTo(List.of("ops@example.com"));
    return properties;
  }
}
