package one.epicsoft.vc.enums;

/**
 * How the connection to the mail server is secured, and the standard port that goes with it.
 */
public enum MailSecurity {

  /** A plain connection upgraded with STARTTLS — required, never just attempted. */
  STARTTLS(587),

  /** TLS from the first byte. */
  TLS(465),

  /** No encryption, for a relay on the same host or network: the login travels in clear text. */
  NONE(25);

  private final int defaultPort;

  MailSecurity(final int defaultPort) {
    this.defaultPort = defaultPort;
  }

  /** The port used when none is configured. */
  public int getDefaultPort() {
    return this.defaultPort;
  }
}
