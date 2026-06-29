package hu.szegedibibliaszol.scraper.model;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record ScraperConfig(
		Path outputDatabasePath,
		long requestDelayMillis,
		boolean staticScrapingEnabled,
		boolean dynamicScrapingEnabled,
		List<String> staticTranslations,
		List<String> dynamicTranslations,
		Optional<String> dynamicStartUrl
) {
	public ScraperConfig {
		Objects.requireNonNull(outputDatabasePath, "outputDatabasePath must not be null");
		Objects.requireNonNull(staticTranslations, "staticTranslations must not be null");
		Objects.requireNonNull(dynamicTranslations, "dynamicTranslations must not be null");
		Objects.requireNonNull(dynamicStartUrl, "dynamicStartUrl must not be null");
		staticTranslations = List.copyOf(staticTranslations);
		dynamicTranslations = List.copyOf(dynamicTranslations);
		dynamicStartUrl = dynamicStartUrl.map(String::trim).filter(value -> !value.isEmpty());
	}
}

