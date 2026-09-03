package one.epicsoft.vc.i18n;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import java.util.Locale;

/**
 * Text for everything the application writes outside an HTTP response.
 *
 * <p>A notification has no request and therefore no locale of its own, so it
 * uses the configured default language. What it must not do is carry sentences
 * written into the code: a message somebody reads is translated text, whether it
 * arrives in a browser or in a chat.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TranslationService {

  private final MessageSource messageSource;

  @Value("${vc.i18n.default-lang}")
  private String defaultLanguage;

  /**
   * The translated text, or the key itself when nothing is stored for it.
   *
   * <p>The fallback is loud. A missing key is not a formatting detail: the key
   * goes out verbatim to whoever reads the message, and without a log line the
   * only way to notice is to receive one. It is a warning rather than an
   * exception because half a notification is still worth more than none.
   */
  @Nonnull
  public String get(@Nonnull final String key, @Nullable final Object... arguments) {
    final String message = this.messageSource.getMessage(key, arguments, null, Locale.of(this.defaultLanguage));
    if (message == null) {
      log.warn("No text for key {} in {}, so the key itself is sent", key, this.defaultLanguage);
      return key;
    }
    return message;
  }
}
