package hu.szegedibibliaszol.scraper;

import hu.szegedibibliaszol.scraper.model.ScraperConfig;
import hu.szegedibibliaszol.scraper.service.ScraperCoordinator;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScraperApplicationTest {

  private static final Path DEFAULT_DATABASE_PATH = Path.of(System.getProperty("user.home"), "bible-verses.db");
  private static final String OUTPUT_DATABASE_PATH_PROPERTY = "scraper.outputDatabasePath";
  private static final String REQUEST_DELAY_PROPERTY = "scraper.requestDelayMillis";
  private static final String STATIC_ENABLED_PROPERTY = "scraper.staticScrapingEnabled";
  private static final String DYNAMIC_ENABLED_PROPERTY = "scraper.dynamicScrapingEnabled";

    @Test
    void createDefaultConfigUsesExpectedValues() {
    withScraperProperties(
        DEFAULT_DATABASE_PATH.toString(),
        "250",
        "true",
        "true",
        () -> {
          ScraperConfig config = ScraperApplication.createDefaultConfig();

          assertEquals(DEFAULT_DATABASE_PATH, config.outputDatabasePath());
          assertEquals(250, config.requestDelayMillis());
          assertTrue(config.staticScrapingEnabled());
          assertTrue(config.dynamicScrapingEnabled());
        }
    );
    }

    @Test
    void createCoordinatorBuildsCoordinator() {
        ScraperCoordinator coordinator = ScraperApplication.createCoordinator(
                new ScraperConfig(Path.of("target", "custom.db"), 0, false, false)
        );

        assertNotNull(coordinator);
    }

    @Test
    void mainCreatesSqliteDatabaseFile() throws IOException {
    Path tempDirectory = Files.createTempDirectory("scraper-application-test");
    Path databasePath = tempDirectory.resolve("verses.db");

    withScraperProperties(
        databasePath.toString(),
        "0",
        "false",
        "false",
        () -> {
          ScraperApplication.main(new String[0]);
          assertTrue(Files.exists(databasePath));
        }
    );
    }

    @Test
    void constructorIsNotInstantiable() throws Exception {
        Constructor<ScraperApplication> constructor = ScraperApplication.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        InvocationTargetException exception = assertThrows(InvocationTargetException.class, constructor::newInstance);

        assertInstanceOf(IllegalStateException.class, exception.getCause());
    }

  private void withScraperProperties(
      String outputDatabasePath,
      String requestDelayMillis,
      String staticScrapingEnabled,
      String dynamicScrapingEnabled,
      ThrowingRunnable action
  ) {
    String previousOutputDatabasePath = System.getProperty(OUTPUT_DATABASE_PATH_PROPERTY);
    String previousRequestDelay = System.getProperty(REQUEST_DELAY_PROPERTY);
    String previousStaticEnabled = System.getProperty(STATIC_ENABLED_PROPERTY);
    String previousDynamicEnabled = System.getProperty(DYNAMIC_ENABLED_PROPERTY);

    try {
      System.setProperty(OUTPUT_DATABASE_PATH_PROPERTY, outputDatabasePath);
      System.setProperty(REQUEST_DELAY_PROPERTY, requestDelayMillis);
      System.setProperty(STATIC_ENABLED_PROPERTY, staticScrapingEnabled);
      System.setProperty(DYNAMIC_ENABLED_PROPERTY, dynamicScrapingEnabled);
      action.run();
    } catch (Exception ex) {
      throw new IllegalStateException("Test setup failed.", ex);
    } finally {
      restoreProperty(OUTPUT_DATABASE_PATH_PROPERTY, previousOutputDatabasePath);
      restoreProperty(REQUEST_DELAY_PROPERTY, previousRequestDelay);
      restoreProperty(STATIC_ENABLED_PROPERTY, previousStaticEnabled);
      restoreProperty(DYNAMIC_ENABLED_PROPERTY, previousDynamicEnabled);
    }
  }

  private void restoreProperty(String key, String value) {
    if (value == null) {
      System.clearProperty(key);
      return;
    }

    System.setProperty(key, value);
  }

  @FunctionalInterface
  private interface ThrowingRunnable {

    void run() throws Exception;
  }
}