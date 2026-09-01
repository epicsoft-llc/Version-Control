package one.epicsoft.vc.controller;

import jakarta.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import one.epicsoft.vc.dto.ErrorDto;
import one.epicsoft.vc.exception.AppException;
import one.epicsoft.vc.exception.AppNotFoundException;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.time.OffsetDateTime;
import java.util.List;

import static java.util.Objects.requireNonNullElse;

/**
 * Turns every exception into the same {@link ErrorDto}.
 *
 * <p>The response carries a translated sentence, the log carries the technical
 * message. Stack traces and class names never reach the client.
 */
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class AppExceptionHandler {

  private static final String KEY_VALIDATION = "error.validation";
  private static final String KEY_UNEXPECTED = "error.unexpected";

  private final MessageSource messageSource;

  @ExceptionHandler(AppNotFoundException.class)
  public ResponseEntity<ErrorDto> handleNotFound(@Nonnull final AppNotFoundException exception) {
    log.debug("Not found: {}", exception.getMessage());
    return this.build(HttpStatus.NOT_FOUND, exception.getMessageKey(), List.of());
  }

  @ExceptionHandler(AppException.class)
  public ResponseEntity<ErrorDto> handleApp(@Nonnull final AppException exception) {
    log.warn("Domain error: {}", exception.getMessage());
    return this.build(HttpStatus.BAD_REQUEST, exception.getMessageKey(), List.of());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorDto> handleValidation(@Nonnull final MethodArgumentNotValidException exception) {
    final List<ErrorDto.FieldErrorDto> fields = exception.getBindingResult()
                                                         .getFieldErrors()
                                                         .stream()
                                                         .map(error -> new ErrorDto.FieldErrorDto(error.getField(), error.getDefaultMessage()))
                                                         .toList();
    log.debug("Validation failed: {}", fields);
    return this.build(HttpStatus.BAD_REQUEST, KEY_VALIDATION, fields);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorDto> handleUnexpected(@Nonnull final Exception exception) {
    log.error("Unexpected error", exception);
    return this.build(HttpStatus.INTERNAL_SERVER_ERROR, KEY_UNEXPECTED, List.of());
  }

  @Nonnull
  private ResponseEntity<ErrorDto> build(@Nonnull final HttpStatus status,
                                         @Nonnull final String messageKey,
                                         @Nonnull final List<ErrorDto.FieldErrorDto> fields) {
    final String message = this.messageSource.getMessage(messageKey, null, null, LocaleContextHolder.getLocale());
    return ResponseEntity.status(status)
                         .body(new ErrorDto(OffsetDateTime.now(),
                                            status.value(),
                                            requireNonNullElse(message, messageKey),
                                            fields));
  }
}
