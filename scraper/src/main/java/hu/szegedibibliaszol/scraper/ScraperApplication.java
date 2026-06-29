package hu.szegedibibliaszol.scraper;

import hu.szegedibibliaszol.scraper.export.VerseExportService;
import hu.szegedibibliaszol.scraper.model.ScraperConfig;
import hu.szegedibibliaszol.scraper.service.DynamicEfoScraper;
import hu.szegedibibliaszol.scraper.service.ScraperCoordinator;
import hu.szegedibibliaszol.scraper.service.StaticKaroliScraper;
import hu.szegedibibliaszol.scraper.service.StaticRevidealtKaroliScraper;
import hu.szegedibibliaszol.scraper.service.StaticRufScraper;
import hu.szegedibibliaszol.scraper.support.SimpleRateLimiter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScraperApplication {

	private static final String DATABASE_FILE_NAME = "bible-verses.db";
	private static final Path USER_HOME_FALLBACK_DATABASE_PATH = Path.of(System.getProperty("user.home"), DATABASE_FILE_NAME);
	private static final String OUTPUT_DATABASE_PATH_PROPERTY = "scraper.outputDatabasePath";
	private static final String REQUEST_DELAY_PROPERTY = "scraper.requestDelayMillis";
	private static final String STATIC_ENABLED_PROPERTY = "scraper.staticScrapingEnabled";
	private static final String DYNAMIC_ENABLED_PROPERTY = "scraper.dynamicScrapingEnabled";
	private static final String STATIC_TRANSLATIONS_PROPERTY = "scraper.staticTranslations";
	private static final String DYNAMIC_TRANSLATIONS_PROPERTY = "scraper.dynamicTranslations";
	private static final String DYNAMIC_START_URL_PROPERTY = "scraper.dynamicStartUrl";

	private static final Logger log = LoggerFactory.getLogger(ScraperApplication.class);

	private ScraperApplication() {
		throw new IllegalStateException("Utility class");
	}

	public static void main(String[] args) {
		ScraperConfig config = createDefaultConfig();
		ScraperCoordinator coordinator = createCoordinator(config);

		log.info("Starting scraper module...");
		coordinator.run();
		log.info("Scraper module finished.");
	}

	static ScraperConfig createDefaultConfig() {
		Path outputDatabasePath = Path.of(System.getProperty(
				OUTPUT_DATABASE_PATH_PROPERTY,
				resolveDefaultDatabasePath(Path.of(System.getProperty("user.dir"))).toString()
		));
		long requestDelayMillis = Long.parseLong(System.getProperty(REQUEST_DELAY_PROPERTY, "250"));
		boolean staticScrapingEnabled = Boolean.parseBoolean(System.getProperty(STATIC_ENABLED_PROPERTY, "true"));
		boolean dynamicScrapingEnabled = Boolean.parseBoolean(System.getProperty(DYNAMIC_ENABLED_PROPERTY, "true"));
		List<String> staticTranslations = parseTranslationSelection(System.getProperty(STATIC_TRANSLATIONS_PROPERTY, "all"));
		List<String> dynamicTranslations = parseTranslationSelection(System.getProperty(DYNAMIC_TRANSLATIONS_PROPERTY, "all"));
		Optional<String> dynamicStartUrl = Optional.ofNullable(System.getProperty(DYNAMIC_START_URL_PROPERTY));

		return new ScraperConfig(
				outputDatabasePath,
				requestDelayMillis,
				staticScrapingEnabled,
				dynamicScrapingEnabled,
				staticTranslations,
				dynamicTranslations,
				dynamicStartUrl
		);
	}

	static ScraperCoordinator createCoordinator(ScraperConfig config) {
		return new ScraperCoordinator(
				config,
				new SimpleRateLimiter(config.requestDelayMillis()),
				List.of(new StaticRevidealtKaroliScraper(), new StaticKaroliScraper(), new StaticRufScraper()),
				List.of(new DynamicEfoScraper()),
				new VerseExportService()
		);
	}

	private static List<String> parseTranslationSelection(String configuredTranslations) {
		return Arrays.stream(configuredTranslations.split(","))
				.map(String::trim)
				.filter(value -> !value.isEmpty())
				.toList();
	}

	static Path resolveDefaultDatabasePath(Path searchStart) {
		return findRepositoryRoot(searchStart)
				.map(repositoryRoot -> repositoryRoot.resolve("app").resolve("data").resolve(DATABASE_FILE_NAME))
				.orElse(USER_HOME_FALLBACK_DATABASE_PATH);
	}

	static Optional<Path> findRepositoryRoot(Path searchStart) {
		for (Path current = searchStart.toAbsolutePath(); current != null; current = current.getParent()) {
			if (Files.isRegularFile(current.resolve("pom.xml"))
					&& Files.isDirectory(current.resolve("app"))
					&& Files.isDirectory(current.resolve("scraper"))) {
				return Optional.of(current);
			}
		}

		return Optional.empty();
	}
}
