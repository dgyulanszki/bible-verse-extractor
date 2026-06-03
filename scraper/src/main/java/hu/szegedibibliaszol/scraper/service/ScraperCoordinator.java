package hu.szegedibibliaszol.scraper.service;

import hu.szegedibibliaszol.scraper.export.VerseExportService;
import hu.szegedibibliaszol.scraper.model.ScraperConfig;
import hu.szegedibibliaszol.scraper.model.VerseRecord;
import hu.szegedibibliaszol.scraper.support.SimpleRateLimiter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScraperCoordinator {

    private static final Logger log = LoggerFactory.getLogger(ScraperCoordinator.class);

    private final ScraperConfig config;
    private final SimpleRateLimiter rateLimiter;
    private final List<AbstractStaticSiteScraper> staticSiteScrapers;
    private final List<AbstractDynamicSiteScraper> dynamicSiteScrapers;
    private final VerseExportService verseExportService;

    public ScraperCoordinator(
            ScraperConfig config,
            SimpleRateLimiter rateLimiter,
            List<AbstractStaticSiteScraper> staticSiteScrapers,
            List<AbstractDynamicSiteScraper> dynamicSiteScrapers,
            VerseExportService verseExportService
    ) {
        this.config = config;
        this.rateLimiter = rateLimiter;
        this.staticSiteScrapers = List.copyOf(staticSiteScrapers);
        this.dynamicSiteScrapers = List.copyOf(dynamicSiteScrapers);
        this.verseExportService = verseExportService;
    }

    public void run() {
        List<VerseRecord> collectedVerses = new ArrayList<>();

        if (config.staticScrapingEnabled()) {
            for (AbstractStaticSiteScraper staticSiteScraper : selectedStaticSiteScrapers()) {
                rateLimiter.acquire();
                collectedVerses.addAll(staticSiteScraper.scrape());
            }
        }

        if (config.dynamicScrapingEnabled()) {
            for (AbstractDynamicSiteScraper dynamicSiteScraper : selectedDynamicSiteScrapers()) {
                rateLimiter.acquire();
                List<VerseRecord> dynamicVerses = dynamicSiteScraper.scrape();
                collectedVerses.addAll(dynamicVerses);
                log.info("Dynamic scraper '{}' collected {} verses.", dynamicSiteScraper.id(), dynamicVerses.size());
            }
        }

        verseExportService.exportToSqlite(config.outputDatabasePath(), List.copyOf(collectedVerses));
        log.info("Exported {} verses to {}", collectedVerses.size(), config.outputDatabasePath());
    }

    private List<AbstractStaticSiteScraper> selectedStaticSiteScrapers() {
        Map<String, AbstractStaticSiteScraper> scrapersById = new LinkedHashMap<>();
        for (AbstractStaticSiteScraper staticSiteScraper : staticSiteScrapers) {
            scrapersById.put(staticSiteScraper.id(), staticSiteScraper);
        }

        if (config.staticTranslations().isEmpty()) {
            return List.copyOf(staticSiteScrapers);
        }

        List<AbstractStaticSiteScraper> selectedScrapers = new ArrayList<>();
        for (String configuredTranslationId : config.staticTranslations()) {
            String normalizedTranslationId = configuredTranslationId.toLowerCase(Locale.ROOT);
            if ("all".equals(normalizedTranslationId)) {
                return List.copyOf(staticSiteScrapers);
            }

            AbstractStaticSiteScraper staticSiteScraper = scrapersById.get(normalizedTranslationId);
            if (staticSiteScraper == null) {
                throw new IllegalStateException(
                        "Unknown static translation id: " + configuredTranslationId + ". Available ids: " + scrapersById.keySet()
                );
            }
            selectedScrapers.add(staticSiteScraper);
        }

        return List.copyOf(selectedScrapers);
    }

    private List<AbstractDynamicSiteScraper> selectedDynamicSiteScrapers() {
        Map<String, AbstractDynamicSiteScraper> scrapersById = new LinkedHashMap<>();
        for (AbstractDynamicSiteScraper dynamicSiteScraper : dynamicSiteScrapers) {
            scrapersById.put(dynamicSiteScraper.id(), dynamicSiteScraper);
        }

        if (config.dynamicTranslations().isEmpty()) {
            return List.copyOf(dynamicSiteScrapers);
        }

        List<AbstractDynamicSiteScraper> selectedScrapers = new ArrayList<>();
        for (String configuredTranslationId : config.dynamicTranslations()) {
            String normalizedTranslationId = configuredTranslationId.toLowerCase(Locale.ROOT);
            if ("all".equals(normalizedTranslationId)) {
                return List.copyOf(dynamicSiteScrapers);
            }

            AbstractDynamicSiteScraper dynamicSiteScraper = scrapersById.get(normalizedTranslationId);
            if (dynamicSiteScraper == null) {
                throw new IllegalStateException(
                        "Unknown dynamic translation id: " + configuredTranslationId + ". Available ids: " + scrapersById.keySet()
                );
            }
            selectedScrapers.add(dynamicSiteScraper);
        }

        return List.copyOf(selectedScrapers);
    }
}

