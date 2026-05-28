package hu.szegedibibliaszol.scraper.model;

import java.nio.file.Path;

public record ScraperConfig(
		Path outputDatabasePath,
		long requestDelayMillis,
		boolean staticScrapingEnabled,
		boolean dynamicScrapingEnabled
) {
}

