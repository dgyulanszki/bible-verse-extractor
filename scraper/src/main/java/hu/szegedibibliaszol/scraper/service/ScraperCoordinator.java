package hu.szegedibibliaszol.scraper.service;

import hu.szegedibibliaszol.scraper.export.VerseExportService;
import hu.szegedibibliaszol.scraper.model.ScraperConfig;
import hu.szegedibibliaszol.scraper.model.VerseRecord;
import hu.szegedibibliaszol.scraper.support.SimpleRateLimiter;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScraperCoordinator {

    private static final Logger log = LoggerFactory.getLogger(ScraperCoordinator.class);

    private final ScraperConfig config;
    private final SimpleRateLimiter rateLimiter;
    private final StaticSiteScraper staticSiteScraper;
    private final DynamicSiteScraper dynamicSiteScraper;
    private final VerseExportService verseExportService;

    public ScraperCoordinator(
            ScraperConfig config,
            SimpleRateLimiter rateLimiter,
            StaticSiteScraper staticSiteScraper,
            DynamicSiteScraper dynamicSiteScraper,
            VerseExportService verseExportService
    ) {
        this.config = config;
        this.rateLimiter = rateLimiter;
        this.staticSiteScraper = staticSiteScraper;
        this.dynamicSiteScraper = dynamicSiteScraper;
        this.verseExportService = verseExportService;
    }

    public void run() {
        List<VerseRecord> collectedVerses = List.of();

        if (config.staticScrapingEnabled()) {
            rateLimiter.acquire();
            collectedVerses = staticSiteScraper.scrape();
        }

        if (config.dynamicScrapingEnabled()) {
            rateLimiter.acquire();
            log.info("Dynamic scraper returned {} verse candidates.", dynamicSiteScraper.scrape().size());
        }

        verseExportService.exportToSqlite(config.outputDatabasePath(), collectedVerses);
        log.info("Exported {} verses to {}", collectedVerses.size(), config.outputDatabasePath());
    }
}

