package one.epicsoft.vc.config;

import jakarta.annotation.Nonnull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import java.time.Duration;

/**
 * How often support dates are looked up, and when they start to matter.
 *
 * <p>Separate from the update check on purpose: an end of support moves slowly
 * and is worth a weekly question, while an available version is worth asking
 * about every other day. The two thresholds decide when a date turns into a
 * warning and when it turns into something that has to be done.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "vc.eol")
public class EolProperties {

  @Nonnull
  private Duration checkInterval = Duration.ofDays(7);

  /** Support ending within this many months is worth knowing about. */
  private int warnMonths = 6;

  /** Within this many months it stops being a note and becomes a task. */
  private int escalateMonths = 3;
}
