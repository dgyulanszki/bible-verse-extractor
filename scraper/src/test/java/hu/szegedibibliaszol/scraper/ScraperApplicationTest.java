package hu.szegedibibliaszol.scraper;

import hu.szegedibibliaszol.scraper.model.ScraperConfig;
import hu.szegedibibliaszol.scraper.service.AbstractDynamicSiteScraper;
import hu.szegedibibliaszol.scraper.service.AbstractStaticSiteScraper;
import hu.szegedibibliaszol.scraper.service.ScraperCoordinator;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScraperApplicationTest {

  private static final Path DEFAULT_DATABASE_PATH = ScraperApplication.resolveDefaultDatabasePath(Path.of(System.getProperty("user.dir")));
  private static final String OUTPUT_DATABASE_PATH_PROPERTY = "scraper.outputDatabasePath";
  private static final String REQUEST_DELAY_PROPERTY = "scraper.requestDelayMillis";
  private static final String STATIC_ENABLED_PROPERTY = "scraper.staticScrapingEnabled";
  private static final String DYNAMIC_ENABLED_PROPERTY = "scraper.dynamicScrapingEnabled";
  private static final String STATIC_TRANSLATIONS_PROPERTY = "scraper.staticTranslations";
  private static final String DYNAMIC_TRANSLATIONS_PROPERTY = "scraper.dynamicTranslations";
  private static final String DYNAMIC_START_URL_PROPERTY = "scraper.dynamicStartUrl";

    @Test
    void resolveDefaultDatabasePathUsesRepositoryLocalAppDataDirectory() throws IOException {
        Path repositoryRoot = Files.createTempDirectory("scraper-repo-root");
        Files.createFile(repositoryRoot.resolve("pom.xml"));
        Files.createDirectories(repositoryRoot.resolve("app"));
        Files.createDirectories(repositoryRoot.resolve("scraper").resolve("nested"));

        Path resolvedPath = ScraperApplication.resolveDefaultDatabasePath(repositoryRoot.resolve("scraper").resolve("nested"));

        assertEquals(repositoryRoot.resolve("app").resolve("data").resolve("bible-verses.db"), resolvedPath);
    }

    @Test
    void resolveDefaultDatabasePathFallsBackToUserHomeOutsideRepository() throws IOException {
        Path unrelatedDirectory = Files.createTempDirectory("scraper-non-repo");

        Path resolvedPath = ScraperApplication.resolveDefaultDatabasePath(unrelatedDirectory);

        assertEquals(Path.of(System.getProperty("user.home"), "bible-verses.db"), resolvedPath);
    }

    @Test
    void findRepositoryRootReturnsEmptyWhenRepositoryMarkersAreMissing() throws IOException {
        Path unrelatedDirectory = Files.createTempDirectory("scraper-no-markers");

        assertTrue(ScraperApplication.findRepositoryRoot(unrelatedDirectory).isEmpty());
    }

    @Test
    void createDefaultConfigUsesExpectedValues() {
    withScraperProperties(
        DEFAULT_DATABASE_PATH.toString(),
        "250",
        "true",
        "true",
        "all",
        "all",
        null,
        () -> {
          ScraperConfig config = ScraperApplication.createDefaultConfig();

          assertEquals(DEFAULT_DATABASE_PATH, config.outputDatabasePath());
          assertEquals(250, config.requestDelayMillis());
          assertTrue(config.staticScrapingEnabled());
          assertTrue(config.dynamicScrapingEnabled());
          assertEquals(List.of("all"), config.staticTranslations());
          assertEquals(List.of("all"), config.dynamicTranslations());
          assertEquals(Optional.empty(), config.dynamicStartUrl());
        }
    );
    }

    @Test
    void createDefaultConfigParsesSelectedStaticTranslations() {
    withScraperProperties(
        DEFAULT_DATABASE_PATH.toString(),
        "250",
        "true",
        "false",
        "revidealt-karoli, karoli-gaspar, revidealt-uj-forditas",
        "efo",
        null,
        () -> {
          ScraperConfig config = ScraperApplication.createDefaultConfig();

          assertEquals(List.of("revidealt-karoli", "karoli-gaspar", "revidealt-uj-forditas"), config.staticTranslations());
          assertEquals(List.of("efo"), config.dynamicTranslations());
        }
    );
    }

    @Test
    void createDefaultConfigIgnoresBlankStaticTranslationEntries() {
    withScraperProperties(
        DEFAULT_DATABASE_PATH.toString(),
        "250",
        "true",
        "false",
        " revidealt-karoli, , karoli-gaspar ,, revidealt-uj-forditas ",
        " efo, , niv ,, kjv ",
        null,
        () -> {
          ScraperConfig config = ScraperApplication.createDefaultConfig();

          assertEquals(List.of("revidealt-karoli", "karoli-gaspar", "revidealt-uj-forditas"), config.staticTranslations());
          assertEquals(List.of("efo", "niv", "kjv"), config.dynamicTranslations());
        }
    );
    }

    @Test
    void createDefaultConfigUsesConfiguredDynamicStartUrl() {
    withScraperProperties(
        DEFAULT_DATABASE_PATH.toString(),
        "250",
        "true",
        "true",
        "all",
        "efo",
        " https://www.bible.com/bible/198/DAN.12.EFO ",
        () -> {
          ScraperConfig config = ScraperApplication.createDefaultConfig();

          assertEquals(Optional.of("https://www.bible.com/bible/198/DAN.12.EFO"), config.dynamicStartUrl());
        }
    );
    }

    @Test
    void createCoordinatorBuildsCoordinator() throws Exception {
        ScraperCoordinator coordinator = ScraperApplication.createCoordinator(
                new ScraperConfig(Path.of("target", "custom.db"), 0, false, false, List.of("all"), List.of("all"), Optional.empty())
        );

        Field staticSiteScrapersField = ScraperCoordinator.class.getDeclaredField("staticSiteScrapers");
        staticSiteScrapersField.setAccessible(true);
        Field dynamicSiteScrapersField = ScraperCoordinator.class.getDeclaredField("dynamicSiteScrapers");
        dynamicSiteScrapersField.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<AbstractStaticSiteScraper> staticSiteScrapers = (List<AbstractStaticSiteScraper>) staticSiteScrapersField.get(coordinator);
        @SuppressWarnings("unchecked")
        List<AbstractDynamicSiteScraper> dynamicSiteScrapers = (List<AbstractDynamicSiteScraper>) dynamicSiteScrapersField.get(coordinator);

        assertNotNull(coordinator);
        assertEquals(List.of("revidealt-karoli", "karoli-gaspar", "revidealt-uj-forditas"),
                staticSiteScrapers.stream().map(AbstractStaticSiteScraper::id).toList());
        assertEquals(List.of("efo"), dynamicSiteScrapers.stream().map(AbstractDynamicSiteScraper::id).toList());
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
        "all",
        "all",
        null,
        () -> {
          ScraperApplication.main(new String[0]);
          assertTrue(Files.exists(databasePath));
        }
    );
    }

    @Test
    void createDefaultConfigUsesResolvedDefaultPathWhenOutputPropertyIsMissing() {
        String previousOutputDatabasePath = System.getProperty(OUTPUT_DATABASE_PATH_PROPERTY);

        try {
            System.clearProperty(OUTPUT_DATABASE_PATH_PROPERTY);
            System.setProperty(REQUEST_DELAY_PROPERTY, "250");
            System.setProperty(STATIC_ENABLED_PROPERTY, "true");
            System.setProperty(DYNAMIC_ENABLED_PROPERTY, "true");
            System.setProperty(STATIC_TRANSLATIONS_PROPERTY, "all");
            System.setProperty(DYNAMIC_TRANSLATIONS_PROPERTY, "all");
            System.clearProperty(DYNAMIC_START_URL_PROPERTY);

            ScraperConfig config = ScraperApplication.createDefaultConfig();

            assertEquals(DEFAULT_DATABASE_PATH, config.outputDatabasePath());
            assertEquals(Optional.empty(), config.dynamicStartUrl());
        } finally {
            restoreProperty(OUTPUT_DATABASE_PATH_PROPERTY, previousOutputDatabasePath);
            System.clearProperty(REQUEST_DELAY_PROPERTY);
            System.clearProperty(STATIC_ENABLED_PROPERTY);
            System.clearProperty(DYNAMIC_ENABLED_PROPERTY);
            System.clearProperty(STATIC_TRANSLATIONS_PROPERTY);
            System.clearProperty(DYNAMIC_TRANSLATIONS_PROPERTY);
            System.clearProperty(DYNAMIC_START_URL_PROPERTY);
        }
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
      String staticTranslations,
      String dynamicTranslations,
      String dynamicStartUrl,
      ThrowingRunnable action
  ) {
    String previousOutputDatabasePath = System.getProperty(OUTPUT_DATABASE_PATH_PROPERTY);
    String previousRequestDelay = System.getProperty(REQUEST_DELAY_PROPERTY);
    String previousStaticEnabled = System.getProperty(STATIC_ENABLED_PROPERTY);
    String previousDynamicEnabled = System.getProperty(DYNAMIC_ENABLED_PROPERTY);
    String previousStaticTranslations = System.getProperty(STATIC_TRANSLATIONS_PROPERTY);
    String previousDynamicTranslations = System.getProperty(DYNAMIC_TRANSLATIONS_PROPERTY);
    String previousDynamicStartUrl = System.getProperty(DYNAMIC_START_URL_PROPERTY);

    try {
      System.setProperty(OUTPUT_DATABASE_PATH_PROPERTY, outputDatabasePath);
      System.setProperty(REQUEST_DELAY_PROPERTY, requestDelayMillis);
      System.setProperty(STATIC_ENABLED_PROPERTY, staticScrapingEnabled);
      System.setProperty(DYNAMIC_ENABLED_PROPERTY, dynamicScrapingEnabled);
      System.setProperty(STATIC_TRANSLATIONS_PROPERTY, staticTranslations);
      System.setProperty(DYNAMIC_TRANSLATIONS_PROPERTY, dynamicTranslations);
      restoreProperty(DYNAMIC_START_URL_PROPERTY, dynamicStartUrl);
      action.run();
    } catch (Exception ex) {
      throw new IllegalStateException("Test setup failed.", ex);
    } finally {
      restoreProperty(OUTPUT_DATABASE_PATH_PROPERTY, previousOutputDatabasePath);
      restoreProperty(REQUEST_DELAY_PROPERTY, previousRequestDelay);
      restoreProperty(STATIC_ENABLED_PROPERTY, previousStaticEnabled);
      restoreProperty(DYNAMIC_ENABLED_PROPERTY, previousDynamicEnabled);
      restoreProperty(STATIC_TRANSLATIONS_PROPERTY, previousStaticTranslations);
      restoreProperty(DYNAMIC_TRANSLATIONS_PROPERTY, previousDynamicTranslations);
      restoreProperty(DYNAMIC_START_URL_PROPERTY, previousDynamicStartUrl);
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