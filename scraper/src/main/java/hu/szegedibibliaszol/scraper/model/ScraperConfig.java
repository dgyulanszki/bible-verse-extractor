package hu.szegedibibliaszol.scraper.model;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public record ScraperConfig(
		Path outputDatabasePath,
		long requestDelayMillis,
		boolean staticScrapingEnabled,
		boolean dynamicScrapingEnabled,
		List<String> staticTranslations,
		List<String> dynamicTranslations
) {
	public ScraperConfig {
		Objects.requireNonNull(outputDatabasePath, "outputDatabasePath must not be null");
		Objects.requireNonNull(staticTranslations, "staticTranslations must not be null");
		Objects.requireNonNull(dynamicTranslations, "dynamicTranslations must not be null");
		staticTranslations = List.copyOf(staticTranslations);
		dynamicTranslations = List.copyOf(dynamicTranslations);
	}
}

