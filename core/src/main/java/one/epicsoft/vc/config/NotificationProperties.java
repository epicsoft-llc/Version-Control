package one.epicsoft.vc.config;

import jakarta.annotation.Nonnull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import java.time.LocalTime;

/**
 * When the daily notification goes out.
 *
 * <p>A fixed time of day, deliberately decoupled from the check scheduler: the
 * checks run all day at their own pace, the message arrives once, when somebody
 * is there to read it.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "vc.notification")
public class NotificationProperties {

  @Nonnull
  private LocalTime dailyTime = LocalTime.of(8, 0);

  /** The configured time as a cron expression, for the scheduled job. */
  @Nonnull
  public String getCron() {
    return "0 %d %d * * *".formatted(this.dailyTime.getMinute(), this.dailyTime.getHour());
  }
}
