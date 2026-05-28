package hu.szegedibibliaszol.scraper.service;

import hu.szegedibibliaszol.scraper.export.VerseExportService;
import hu.szegedibibliaszol.scraper.model.ScraperConfig;
import hu.szegedibibliaszol.scraper.model.VerseRecord;
import hu.szegedibibliaszol.scraper.support.SimpleRateLimiter;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ScraperCoordinatorTest {

    @Test
    void runInvokesEnabledScrapersAndExportsCollectedVerses() {
        CountingRateLimiter rateLimiter = new CountingRateLimiter();
        CountingStaticSiteScraper staticSiteScraper = new CountingStaticSiteScraper(
                List.of(new VerseRecord("KJV", "John", 3, 16, "For God so loved the world"))
        );
        CountingDynamicSiteScraper dynamicSiteScraper = new CountingDynamicSiteScraper(List.of("dynamic-result"));
        CapturingVerseExportService verseExportService = new CapturingVerseExportService();
        ScraperConfig config = new ScraperConfig(Path.of("target", "scraper-coordinator.db"), 0, true, true);

        ScraperCoordinator scraperCoordinator = new ScraperCoordinator(
                config,
                rateLimiter,
                staticSiteScraper,
                dynamicSiteScraper,
                verseExportService
        );

        scraperCoordinator.run();

        assertEquals(2, rateLimiter.acquireCalls);
        assertEquals(1, staticSiteScraper.scrapeCalls);
        assertEquals(1, dynamicSiteScraper.scrapeCalls);
        assertEquals(config.outputDatabasePath(), verseExportService.exportPath);
        assertEquals(1, verseExportService.exportedVerses.size());
    }

    @Test
    void runSkipsDisabledScrapersAndStillExportsEmptyList() {
        CountingRateLimiter rateLimiter = new CountingRateLimiter();
        CountingStaticSiteScraper staticSiteScraper = new CountingStaticSiteScraper(List.of());
        CountingDynamicSiteScraper dynamicSiteScraper = new CountingDynamicSiteScraper(List.of());
        CapturingVerseExportService verseExportService = new CapturingVerseExportService();
        ScraperConfig config = new ScraperConfig(Path.of("target", "scraper-coordinator-empty.db"), 0, false, false);

        ScraperCoordinator scraperCoordinator = new ScraperCoordinator(
                config,
                rateLimiter,
                staticSiteScraper,
                dynamicSiteScraper,
                verseExportService
        );

        scraperCoordinator.run();

        assertEquals(0, rateLimiter.acquireCalls);
        assertEquals(0, staticSiteScraper.scrapeCalls);
        assertEquals(0, dynamicSiteScraper.scrapeCalls);
        assertEquals(0, verseExportService.exportedVerses.size());
    }

    private static final class CountingRateLimiter extends SimpleRateLimiter {

        private int acquireCalls;

        private CountingRateLimiter() {
            super(0);
        }

        @Override
        public synchronized void acquire() {
            acquireCalls++;
        }
    }

    private static final class CountingStaticSiteScraper extends StaticSiteScraper {

        private final List<VerseRecord> verses;
        private int scrapeCalls;

        private CountingStaticSiteScraper(List<VerseRecord> verses) {
            this.verses = verses;
        }

        @Override
        public List<VerseRecord> scrape() {
            scrapeCalls++;
            return verses;
        }
    }

    private static final class CountingDynamicSiteScraper extends DynamicSiteScraper {

        private final List<String> scrapeResults;
        private int scrapeCalls;

        private CountingDynamicSiteScraper(List<String> scrapeResults) {
            this.scrapeResults = scrapeResults;
        }

        @Override
        public List<String> scrape() {
            scrapeCalls++;
            return scrapeResults;
        }
    }

    private static final class CapturingVerseExportService extends VerseExportService {

        private Path exportPath = Path.of(".");
        private List<VerseRecord> exportedVerses = List.of();

        @Override
        public void exportToSqlite(Path databasePath, List<VerseRecord> verses) {
            exportPath = databasePath;
            exportedVerses = verses;
        }
    }
}

