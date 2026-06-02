package hu.szegedibibliaszol.scraper;

import hu.szegedibibliaszol.scraper.export.VerseExportService;
import hu.szegedibibliaszol.scraper.model.ScraperConfig;
import hu.szegedibibliaszol.scraper.service.DynamicSiteScraper;
import hu.szegedibibliaszol.scraper.service.ScraperCoordinator;
import hu.szegedibibliaszol.scraper.service.StaticSiteScraper;
import hu.szegedibibliaszol.scraper.support.SimpleRateLimiter;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScraperApplication {

	private static final Path DEFAULT_DATABASE_PATH = Path.of(System.getProperty("user.home"), "bible-verses.db");
	private static final String OUTPUT_DATABASE_PATH_PROPERTY = "scraper.outputDatabasePath";
	private static final String REQUEST_DELAY_PROPERTY = "scraper.requestDelayMillis";
	private static final String STATIC_ENABLED_PROPERTY = "scraper.staticScrapingEnabled";
	private static final String DYNAMIC_ENABLED_PROPERTY = "scraper.dynamicScrapingEnabled";

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
		Path outputDatabasePath = Path.of(System.getProperty(OUTPUT_DATABASE_PATH_PROPERTY, DEFAULT_DATABASE_PATH.toString()));
		long requestDelayMillis = Long.parseLong(System.getProperty(REQUEST_DELAY_PROPERTY, "250"));
		boolean staticScrapingEnabled = Boolean.parseBoolean(System.getProperty(STATIC_ENABLED_PROPERTY, "true"));
		boolean dynamicScrapingEnabled = Boolean.parseBoolean(System.getProperty(DYNAMIC_ENABLED_PROPERTY, "true"));

		return new ScraperConfig(outputDatabasePath, requestDelayMillis, staticScrapingEnabled, dynamicScrapingEnabled);
	}

	static ScraperCoordinator createCoordinator(ScraperConfig config) {
		return new ScraperCoordinator(
				config,
				new SimpleRateLimiter(config.requestDelayMillis()),
				new StaticSiteScraper(),
				new DynamicSiteScraper(),
				new VerseExportService()
		);
	}
}

