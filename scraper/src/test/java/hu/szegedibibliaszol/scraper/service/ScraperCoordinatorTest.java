package hu.szegedibibliaszol.scraper.service;

import hu.szegedibibliaszol.scraper.export.VerseExportService;
import hu.szegedibibliaszol.scraper.model.ScraperConfig;
import hu.szegedibibliaszol.scraper.model.VerseRecord;
import hu.szegedibibliaszol.scraper.support.SimpleRateLimiter;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ScraperCoordinatorTest {

    @Test
    void runInvokesEnabledScrapersAndExportsCollectedVerses() {
        CountingRateLimiter rateLimiter = new CountingRateLimiter();
        CountingStaticTranslationScraper firstStaticSiteScraper = new CountingStaticTranslationScraper(
                "revidealt-karoli",
                "Revideált Károli",
                List.of(new VerseRecord("KJV", "John", 3, 16, "For God so loved the world"))
        );
        CountingStaticTranslationScraper secondStaticSiteScraper = new CountingStaticTranslationScraper(
                "karoli-gaspar",
                "Károli",
                List.of(new VerseRecord("KAR", "Mózes I. könyve", 1, 1, "Kezdetben"))
        );
        CountingDynamicTranslationScraper dynamicSiteScraper = new CountingDynamicTranslationScraper(
                "efo",
                List.of(new VerseRecord("Egyszerű fordítás (EFO)", "Mózes első könyve", 1, 1, "Kezdetben"))
        );
        CapturingVerseExportService verseExportService = new CapturingVerseExportService();
        ScraperConfig config = new ScraperConfig(
                Path.of("target", "scraper-coordinator.db"),
                0,
                true,
                true,
                List.of("all"),
                List.of("all")
        );

        ScraperCoordinator scraperCoordinator = new ScraperCoordinator(
                config,
                rateLimiter,
                List.of(firstStaticSiteScraper, secondStaticSiteScraper),
                List.of(dynamicSiteScraper),
                verseExportService
        );

        scraperCoordinator.run();

        assertEquals(3, rateLimiter.acquireCalls);
        assertEquals(1, firstStaticSiteScraper.scrapeCalls);
        assertEquals(1, secondStaticSiteScraper.scrapeCalls);
        assertEquals(1, dynamicSiteScraper.scrapeCalls);
        assertEquals(config.outputDatabasePath(), verseExportService.exportPath);
        assertEquals(3, verseExportService.exportedVerses.size());
    }

    @Test
    void runSkipsDisabledScrapersAndStillExportsEmptyList() {
        CountingRateLimiter rateLimiter = new CountingRateLimiter();
        CountingStaticTranslationScraper staticSiteScraper = new CountingStaticTranslationScraper(
                "revidealt-karoli",
                "Revideált Károli",
                List.of()
        );
        CountingDynamicTranslationScraper dynamicSiteScraper = new CountingDynamicTranslationScraper("efo", List.of());
        CapturingVerseExportService verseExportService = new CapturingVerseExportService();
        ScraperConfig config = new ScraperConfig(
                Path.of("target", "scraper-coordinator-empty.db"),
                0,
                false,
                false,
                List.of("all"),
                List.of("all")
        );

        ScraperCoordinator scraperCoordinator = new ScraperCoordinator(
                config,
                rateLimiter,
                List.of(staticSiteScraper),
                List.of(dynamicSiteScraper),
                verseExportService
        );

        scraperCoordinator.run();

        assertEquals(0, rateLimiter.acquireCalls);
        assertEquals(0, staticSiteScraper.scrapeCalls);
        assertEquals(0, dynamicSiteScraper.scrapeCalls);
        assertEquals(0, verseExportService.exportedVerses.size());
    }

    @Test
    void runScrapesOnlySelectedStaticTranslation() {
        CountingRateLimiter rateLimiter = new CountingRateLimiter();
        CountingStaticTranslationScraper firstStaticSiteScraper = new CountingStaticTranslationScraper(
                "revidealt-karoli",
                "Revideált Károli",
                List.of(new VerseRecord("Revideált Károli", "1. Mózes", 1, 1, "Első"))
        );
        CountingStaticTranslationScraper secondStaticSiteScraper = new CountingStaticTranslationScraper(
                "karoli-gaspar",
                "Károli",
                List.of(new VerseRecord("Károli", "Mózes I. könyve", 1, 1, "Második"))
        );
        CapturingVerseExportService verseExportService = new CapturingVerseExportService();
        ScraperConfig config = new ScraperConfig(
                Path.of("target", "scraper-coordinator-selected.db"),
                0,
                true,
                false,
                List.of("karoli-gaspar"),
                List.of("all")
        );

        ScraperCoordinator scraperCoordinator = new ScraperCoordinator(
                config,
                rateLimiter,
                List.of(firstStaticSiteScraper, secondStaticSiteScraper),
                List.of(new CountingDynamicTranslationScraper("efo", List.of())),
                verseExportService
        );

        scraperCoordinator.run();

        assertEquals(1, rateLimiter.acquireCalls);
        assertEquals(0, firstStaticSiteScraper.scrapeCalls);
        assertEquals(1, secondStaticSiteScraper.scrapeCalls);
        assertEquals(List.of(new VerseRecord("Károli", "Mózes I. könyve", 1, 1, "Második")), verseExportService.exportedVerses);
    }

    @Test
    void runTreatsEmptyStaticTranslationSelectionAsAllStaticScrapers() {
        CountingRateLimiter rateLimiter = new CountingRateLimiter();
        CountingStaticTranslationScraper firstStaticSiteScraper = new CountingStaticTranslationScraper(
                "revidealt-karoli",
                "Revideált Károli",
                List.of(new VerseRecord("Revideált Károli", "1. Mózes", 1, 1, "Első"))
        );
        CountingStaticTranslationScraper secondStaticSiteScraper = new CountingStaticTranslationScraper(
                "karoli-gaspar",
                "Károli",
                List.of(new VerseRecord("Károli", "Mózes I. könyve", 1, 1, "Második"))
        );
        CapturingVerseExportService verseExportService = new CapturingVerseExportService();
        ScraperConfig config = new ScraperConfig(
                Path.of("target", "scraper-coordinator-empty-selection.db"),
                0,
                true,
                false,
                List.of(),
                List.of("all")
        );

        ScraperCoordinator scraperCoordinator = new ScraperCoordinator(
                config,
                rateLimiter,
                List.of(firstStaticSiteScraper, secondStaticSiteScraper),
                List.of(new CountingDynamicTranslationScraper("efo", List.of())),
                verseExportService
        );

        scraperCoordinator.run();

        assertEquals(2, rateLimiter.acquireCalls);
        assertEquals(1, firstStaticSiteScraper.scrapeCalls);
        assertEquals(1, secondStaticSiteScraper.scrapeCalls);
        assertEquals(List.of(
                new VerseRecord("Revideált Károli", "1. Mózes", 1, 1, "Első"),
                new VerseRecord("Károli", "Mózes I. könyve", 1, 1, "Második")
        ), verseExportService.exportedVerses);
    }

    @Test
    void runFailsForUnknownStaticTranslationId() {
        ScraperCoordinator scraperCoordinator = new ScraperCoordinator(
                new ScraperConfig(Path.of("target", "scraper-coordinator-invalid.db"), 0, true, false, List.of("unknown"), List.of("all")),
                new CountingRateLimiter(),
                List.of(new CountingStaticTranslationScraper("revidealt-karoli", "Revideált Károli", List.of())),
                List.of(new CountingDynamicTranslationScraper("efo", List.of())),
                new CapturingVerseExportService()
        );

        IllegalStateException exception = assertThrows(IllegalStateException.class, scraperCoordinator::run);

        assertEquals("Unknown static translation id: unknown. Available ids: [revidealt-karoli]", exception.getMessage());
    }

    @Test
    void runScrapesOnlySelectedDynamicTranslation() {
        CountingRateLimiter rateLimiter = new CountingRateLimiter();
        CountingDynamicTranslationScraper firstDynamicSiteScraper = new CountingDynamicTranslationScraper(
                "efo",
                List.of(new VerseRecord("Egyszerű fordítás (EFO)", "Mózes első könyve", 1, 1, "Első"))
        );
        CountingDynamicTranslationScraper secondDynamicSiteScraper = new CountingDynamicTranslationScraper(
                "niv",
                List.of(new VerseRecord("NIV", "Genesis", 1, 1, "Second"))
        );
        CapturingVerseExportService verseExportService = new CapturingVerseExportService();
        ScraperConfig config = new ScraperConfig(
                Path.of("target", "scraper-coordinator-dynamic-selected.db"),
                0,
                false,
                true,
                List.of("all"),
                List.of("niv")
        );

        ScraperCoordinator scraperCoordinator = new ScraperCoordinator(
                config,
                rateLimiter,
                List.of(),
                List.of(firstDynamicSiteScraper, secondDynamicSiteScraper),
                verseExportService
        );

        scraperCoordinator.run();

        assertEquals(1, rateLimiter.acquireCalls);
        assertEquals(0, firstDynamicSiteScraper.scrapeCalls);
        assertEquals(1, secondDynamicSiteScraper.scrapeCalls);
        assertEquals(List.of(new VerseRecord("NIV", "Genesis", 1, 1, "Second")), verseExportService.exportedVerses);
    }

    @Test
    void runTreatsEmptyDynamicTranslationSelectionAsAllDynamicScrapers() {
        CountingRateLimiter rateLimiter = new CountingRateLimiter();
        CountingDynamicTranslationScraper firstDynamicSiteScraper = new CountingDynamicTranslationScraper(
                "efo",
                List.of(new VerseRecord("Egyszerű fordítás (EFO)", "Mózes első könyve", 1, 1, "Első"))
        );
        CountingDynamicTranslationScraper secondDynamicSiteScraper = new CountingDynamicTranslationScraper(
                "niv",
                List.of(new VerseRecord("NIV", "Genesis", 1, 1, "Second"))
        );
        CapturingVerseExportService verseExportService = new CapturingVerseExportService();
        ScraperConfig config = new ScraperConfig(
                Path.of("target", "scraper-coordinator-dynamic-empty-selection.db"),
                0,
                false,
                true,
                List.of("all"),
                List.of()
        );

        ScraperCoordinator scraperCoordinator = new ScraperCoordinator(
                config,
                rateLimiter,
                List.of(),
                List.of(firstDynamicSiteScraper, secondDynamicSiteScraper),
                verseExportService
        );

        scraperCoordinator.run();

        assertEquals(2, rateLimiter.acquireCalls);
        assertEquals(1, firstDynamicSiteScraper.scrapeCalls);
        assertEquals(1, secondDynamicSiteScraper.scrapeCalls);
        assertEquals(List.of(
                new VerseRecord("Egyszerű fordítás (EFO)", "Mózes első könyve", 1, 1, "Első"),
                new VerseRecord("NIV", "Genesis", 1, 1, "Second")
        ), verseExportService.exportedVerses);
    }

    @Test
    void runFailsForUnknownDynamicTranslationId() {
        ScraperCoordinator scraperCoordinator = new ScraperCoordinator(
                new ScraperConfig(Path.of("target", "scraper-coordinator-dynamic-invalid.db"), 0, false, true, List.of("all"), List.of("unknown")),
                new CountingRateLimiter(),
                List.of(),
                List.of(new CountingDynamicTranslationScraper("efo", List.of())),
                new CapturingVerseExportService()
        );

        IllegalStateException exception = assertThrows(IllegalStateException.class, scraperCoordinator::run);

        assertEquals("Unknown dynamic translation id: unknown. Available ids: [efo]", exception.getMessage());
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

    private static final class CountingStaticTranslationScraper extends AbstractStaticSiteScraper {

        private final String id;
        private final String translation;
        private final List<VerseRecord> verses;
        private int scrapeCalls;

        private CountingStaticTranslationScraper(String id, String translation, List<VerseRecord> verses) {
            this.id = id;
            this.translation = translation;
            this.verses = verses;
        }

        @Override
        public String id() {
            return id;
        }

        @Override
        public String translation() {
            return translation;
        }

        @Override
        public List<VerseRecord> scrape() {
            scrapeCalls++;
            return verses;
        }

        @Override
        protected String baseUrl() {
            return "https://example.com";
        }

        @Override
        protected String rootPath() {
            return "/bible/example";
        }

        @Override
        protected java.util.regex.Pattern bookLinkPattern() {
            return java.util.regex.Pattern.compile("^$");
        }

        @Override
        protected java.util.regex.Pattern chapterLinkPattern(String bookCode) {
            return java.util.regex.Pattern.compile("^$");
        }
    }

    private static final class CountingDynamicTranslationScraper extends AbstractDynamicSiteScraper {

        private final String id;
        private final List<VerseRecord> scrapeResults;
        private int scrapeCalls;

        private CountingDynamicTranslationScraper(String id, List<VerseRecord> scrapeResults) {
            this.id = id;
            this.scrapeResults = scrapeResults;
        }

        @Override
        public String id() {
            return id;
        }

        @Override
        public String translation() {
            return id;
        }

        @Override
        public List<VerseRecord> scrape() {
            scrapeCalls++;
            return scrapeResults;
        }

        @Override
        protected String startUrl() {
            return "https://example.com/dynamic/" + id;
        }

        @Override
        protected String translationButtonText() {
            return id.toUpperCase();
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

