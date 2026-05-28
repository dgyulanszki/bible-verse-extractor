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

	private static final Path DEFAULT_DATABASE_PATH = Path.of("target", "scraper-output", "bible-verses.db");

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
		return new ScraperConfig(DEFAULT_DATABASE_PATH, 250, true, true);
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

