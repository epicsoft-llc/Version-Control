package one.epicsoft.vc;

import jakarta.annotation.Nullable;
import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base for tests that need the real schema.
 *
 * <p>A real PostgreSQL, not an embedded database: the schema belongs to Flyway and
 * uses native uuid columns and partial indexes, so anything else would test a
 * different database than the one that runs.
 *
 * <p>Where that database comes from depends on the environment. With
 * {@code VC_DATABASE_HOST} set the connection configured in {@code application.yml}
 * is used as it stands — that is the pipeline, which attaches PostgreSQL as a
 * service. Without it a Testcontainers instance is started once and shared by
 * every test that extends this class, so a developer needs nothing but Docker.
 *
 * <p>Both ways the database is shared across test classes: the schema is migrated
 * once and rows committed by one class are visible to the next. Tests either roll
 * back through {@code @Transactional} or make their data unique.
 *
 * <p>Tagged {@code integration}. The build skips that tag only when neither a
 * Docker daemon nor an external database is there, and it refuses to skip it in
 * CI — see the root {@code build.gradle}.
 */
@Tag("integration")
@SpringBootTest
public abstract class AbstractIntegrationTest {

  private static final String EXTERNAL_HOST = System.getenv("VC_DATABASE_HOST");

  /** Null when an external database is configured — then nothing is started here. */
  @Nullable
  private static final PostgreSQLContainer<?> POSTGRES =
    isExternalDatabase() ? null : new PostgreSQLContainer<>("postgres:18-alpine");

  static {
    if (POSTGRES != null) {
      POSTGRES.start();
    }
  }

  @DynamicPropertySource
  static void datasource(final DynamicPropertyRegistry registry) {
    // No scheduled check during a test run: it would call upstream sources for
    // real, which is slow, flaky and somebody else's traffic.
    registry.add("vc.scheduler.enabled", () -> Boolean.FALSE);
    if (POSTGRES == null) {
      return;
    }
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
  }

  /** An empty variable counts as unset — a half-configured job must not look configured. */
  private static boolean isExternalDatabase() {
    return EXTERNAL_HOST != null && !EXTERNAL_HOST.isBlank();
  }
}
