package one.epicsoft.vc.config;

import jakarta.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;
import one.epicsoft.vc.i18n.JsonMessageSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.List;

@Slf4j
@Configuration
public class I18nConfig {

  @Value("${vc.i18n.langs}")
  private List<String> languages;

  @Value("${vc.i18n.default-lang}")
  private String defaultLanguage;

  @Nonnull
  @Bean
  public MessageSource messageSource() {
    log.info("Configuring JsonMessageSource with languages {} and fallback '{}'", this.languages, this.defaultLanguage);
    return new JsonMessageSource(this.languages, this.defaultLanguage);
  }
}
