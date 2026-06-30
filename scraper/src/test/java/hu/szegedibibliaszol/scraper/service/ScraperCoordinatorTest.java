package hu.szegedibibliaszol.scraper.service;

import hu.szegedibibliaszol.scraper.export.VerseExportService;
import hu.szegedibibliaszol.scraper.model.ScraperConfig;
import hu.szegedibibliaszol.scraper.model.VerseRecord;
import hu.szegedibibliaszol.scraper.support.SimpleRateLimiter;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
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
                List.of("all"),
                Optional.empty()
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
                List.of("all"),
                Optional.empty()
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
                List.of("all"),
                Optional.empty()
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
                List.of("all"),
                Optional.empty()
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
                new ScraperConfig(Path.of("target", "scraper-coordinator-invalid.db"), 0, true, false, List.of("unknown"), List.of("all"), Optional.empty()),
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
                List.of("niv"),
                Optional.empty()
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
                List.of(),
                Optional.empty()
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
                new ScraperConfig(Path.of("target", "scraper-coordinator-dynamic-invalid.db"), 0, false, true, List.of("all"), List.of("unknown"), Optional.empty()),
                new CountingRateLimiter(),
                List.of(),
                List.of(new CountingDynamicTranslationScraper("efo", List.of())),
                new CapturingVerseExportService()
        );

        IllegalStateException exception = assertThrows(IllegalStateException.class, scraperCoordinator::run);

        assertEquals("Unknown dynamic translation id: unknown. Available ids: [efo]", exception.getMessage());
    }

    @Test
    void runUsesConfiguredDynamicStartUrlForSingleSelectedDynamicScraper() {
        CountingRateLimiter rateLimiter = new CountingRateLimiter();
        CountingDynamicTranslationScraper dynamicSiteScraper = new CountingDynamicTranslationScraper(
                "efo",
                List.of(new VerseRecord("Egyszerű fordítás (EFO)", "Dániel", 12, 1, "Abban az időben..."))
        );
        CapturingVerseExportService verseExportService = new CapturingVerseExportService();
        ScraperConfig config = new ScraperConfig(
                Path.of("target", "scraper-coordinator-dynamic-resume.db"),
                0,
                false,
                true,
                List.of("all"),
                List.of("efo"),
                Optional.of("https://www.bible.com/bible/198/DAN.12.EFO")
        );

        ScraperCoordinator scraperCoordinator = new ScraperCoordinator(
                config,
                rateLimiter,
                List.of(),
                List.of(dynamicSiteScraper),
                verseExportService
        );

        scraperCoordinator.run();

        assertEquals(1, rateLimiter.acquireCalls);
        assertEquals(0, dynamicSiteScraper.scrapeCalls);
        assertEquals(1, dynamicSiteScraper.scrapeFromCalls);
        assertEquals("https://www.bible.com/bible/198/DAN.12.EFO", dynamicSiteScraper.lastStartUrl);
        assertEquals(List.of(new VerseRecord("Egyszerű fordítás (EFO)", "Dániel", 12, 1, "Abban az időben...")), verseExportService.exportedVerses);
    }

    @Test
    void runRejectsDynamicStartUrlWhenMultipleDynamicScrapersAreSelected() {
        ScraperCoordinator scraperCoordinator = new ScraperCoordinator(
                new ScraperConfig(
                        Path.of("target", "scraper-coordinator-dynamic-resume-invalid.db"),
                        0,
                        false,
                        true,
                        List.of("all"),
                        List.of("all"),
                        Optional.of("https://www.bible.com/bible/198/DAN.12.EFO")
                ),
                new CountingRateLimiter(),
                List.of(),
                List.of(
                        new CountingDynamicTranslationScraper("efo", List.of()),
                        new CountingDynamicTranslationScraper("niv", List.of())
                ),
                new CapturingVerseExportService()
        );

        IllegalStateException exception = assertThrows(IllegalStateException.class, scraperCoordinator::run);

        assertEquals(
                "scraper.dynamicStartUrl can only be used when exactly one dynamic scraper is selected. Selected ids: [efo, niv]",
                exception.getMessage()
        );
    }

    @Test
    void runRejectsDynamicStartUrlWhenNoDynamicScraperIsSelected() {
        ScraperCoordinator scraperCoordinator = new ScraperCoordinator(
                new ScraperConfig(
                        Path.of("target", "scraper-coordinator-dynamic-resume-missing.db"),
                        0,
                        false,
                        true,
                        List.of("all"),
                        List.of("all"),
                        Optional.of("https://www.bible.com/bible/198/DAN.12.EFO")
                ),
                new CountingRateLimiter(),
                List.of(),
                List.of(),
                new CapturingVerseExportService()
        );

        IllegalStateException exception = assertThrows(IllegalStateException.class, scraperCoordinator::run);

        assertEquals(
                "scraper.dynamicStartUrl can only be used when exactly one dynamic scraper is selected. Selected ids: []",
                exception.getMessage()
        );
    }

    @Test
    void runPersistsAlreadyCollectedVersesOnFailureWhenFallbackOptionIsEnabled() {
        CountingRateLimiter rateLimiter = new CountingRateLimiter();
        CountingStaticTranslationScraper completedStaticSiteScraper = new CountingStaticTranslationScraper(
                "revidealt-karoli",
                "Revideált Károli",
                List.of(new VerseRecord("Revideált Károli", "1. Mózes", 1, 1, "Első"))
        );
        FailingStaticTranslationScraper failingStaticSiteScraper = new FailingStaticTranslationScraper(
                "karoli-gaspar",
                "Károli",
                List.of(new VerseRecord("Károli", "1. Mózes", 1, 2, "Második")),
                "Simulated static scrape failure"
        );
        CapturingVerseExportService verseExportService = new CapturingVerseExportService();
        ScraperCoordinator scraperCoordinator = new ScraperCoordinator(
                new ScraperConfig(
                        Path.of("target", "scraper-coordinator-partial-fallback.db"),
                        0,
                        true,
                        false,
                        List.of("all"),
                        List.of("all"),
                        Optional.empty()
                ),
                rateLimiter,
                List.of(completedStaticSiteScraper, failingStaticSiteScraper),
                List.of(),
                verseExportService
        );

        PartialScrapeException exception = assertThrows(PartialScrapeException.class, scraperCoordinator::run);

        assertEquals("Simulated static scrape failure", exception.getMessage());
        assertEquals(2, rateLimiter.acquireCalls);
        assertEquals(1, completedStaticSiteScraper.scrapeCalls);
        assertEquals(1, failingStaticSiteScraper.scrapeCalls);
        assertEquals(1, verseExportService.exportCalls);
        assertEquals(List.of(
                new VerseRecord("Revideált Károli", "1. Mózes", 1, 1, "Első"),
                new VerseRecord("Károli", "1. Mózes", 1, 2, "Második")
        ), verseExportService.exportedVerses);
    }

    @Test
    void runPersistsPartialResultsOnFailureByDefault() {
        CapturingVerseExportService verseExportService = new CapturingVerseExportService();
        ScraperCoordinator scraperCoordinator = new ScraperCoordinator(
                new ScraperConfig(
                        Path.of("target", "scraper-coordinator-partial-fallback-disabled.db"),
                        0,
                        true,
                        false,
                        List.of("all"),
                        List.of("all"),
                        Optional.empty()
                ),
                new CountingRateLimiter(),
                List.of(new FailingStaticTranslationScraper(
                        "revidealt-karoli",
                        "Revideált Károli",
                        List.of(new VerseRecord("Revideált Károli", "1. Mózes", 1, 1, "Első")),
                        "Simulated static scrape failure"
                )),
                List.of(),
                verseExportService
        );

        PartialScrapeException exception = assertThrows(PartialScrapeException.class, scraperCoordinator::run);

        assertEquals("Simulated static scrape failure", exception.getMessage());
        assertEquals(1, verseExportService.exportCalls);
        assertEquals(List.of(
                new VerseRecord("Revideált Károli", "1. Mózes", 1, 1, "Első")
        ), verseExportService.exportedVerses);
    }

    @Test
    void runPersistsAlreadyCollectedVersesWhenPartialFailureContainsNoVerses() {
        CapturingVerseExportService verseExportService = new CapturingVerseExportService();
        ScraperCoordinator scraperCoordinator = new ScraperCoordinator(
                new ScraperConfig(
                        Path.of("target", "scraper-coordinator-partial-no-extra-verses.db"),
                        0,
                        true,
                        false,
                        List.of("all"),
                        List.of("all"),
                        Optional.empty()
                ),
                new CountingRateLimiter(),
                List.of(
                        new CountingStaticTranslationScraper(
                                "revidealt-karoli",
                                "Revideált Károli",
                                List.of(new VerseRecord("Revideált Károli", "1. Mózes", 1, 1, "Első"))
                        ),
                        new FailingStaticTranslationScraper(
                                "karoli-gaspar",
                                "Károli",
                                List.of(),
                                "Simulated static scrape failure"
                        )
                ),
                List.of(),
                verseExportService
        );

        PartialScrapeException exception = assertThrows(PartialScrapeException.class, scraperCoordinator::run);

        assertEquals("Simulated static scrape failure", exception.getMessage());
        assertEquals(1, verseExportService.exportCalls);
        assertEquals(List.of(
                new VerseRecord("Revideált Károli", "1. Mózes", 1, 1, "Első")
        ), verseExportService.exportedVerses);
    }

    @Test
    void versesToPersistReturnsAlreadyCollectedVersesWhenPartialFailureProvidesNone() throws Exception {
        ScraperCoordinator scraperCoordinator = new ScraperCoordinator(
                new ScraperConfig(
                        Path.of("target", "scraper-coordinator-direct-partial-no-extra-verses.db"),
                        0,
                        false,
                        false,
                        List.of("all"),
                        List.of("all"),
                        Optional.empty()
                ),
                new CountingRateLimiter(),
                List.of(),
                List.of(),
                new CapturingVerseExportService()
        );
        List<VerseRecord> collectedVerses = List.of(new VerseRecord("Revideált Károli", "1. Mózes", 1, 1, "Első"));
        PartialScrapeException failure = new PartialScrapeException(
                "Simulated partial scrape failure",
                new IllegalStateException("Simulated partial scrape failure"),
                List.of()
        );

        Method method = ScraperCoordinator.class.getDeclaredMethod("versesToPersist", List.class, RuntimeException.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<VerseRecord> versesToPersist = (List<VerseRecord>) method.invoke(scraperCoordinator, collectedVerses, failure);

        assertEquals(collectedVerses, versesToPersist);
    }

    @Test
    void runSkipsFallbackExportWhenFailureHappensBeforeAnyVerseIsCollected() {
        CapturingVerseExportService verseExportService = new CapturingVerseExportService();
        ScraperCoordinator scraperCoordinator = new ScraperCoordinator(
                new ScraperConfig(
                        Path.of("target", "scraper-coordinator-partial-no-verses.db"),
                        0,
                        true,
                        false,
                        List.of("all"),
                        List.of("all"),
                        Optional.empty()
                ),
                new CountingRateLimiter(),
                List.of(new FailingStaticTranslationScraper(
                        "revidealt-karoli",
                        "Revideált Károli",
                        List.of(),
                        "Simulated static scrape failure"
                )),
                List.of(),
                verseExportService
        );

        PartialScrapeException exception = assertThrows(PartialScrapeException.class, scraperCoordinator::run);

        assertEquals("Simulated static scrape failure", exception.getMessage());
        assertEquals(0, verseExportService.exportCalls);
    }

    @Test
    void runAddsSuppressedFailureWhenFallbackExportAlsoFails() {
        RuntimeException exportFailure = new RuntimeException("fallback export failed");
        ScraperCoordinator scraperCoordinator = new ScraperCoordinator(
                new ScraperConfig(
                        Path.of("target", "scraper-coordinator-partial-export-failure.db"),
                        0,
                        true,
                        false,
                        List.of("all"),
                        List.of("all"),
                        Optional.empty()
                ),
                new CountingRateLimiter(),
                List.of(new FailingStaticTranslationScraper(
                        "revidealt-karoli",
                        "Revideált Károli",
                        List.of(new VerseRecord("Revideált Károli", "1. Mózes", 1, 1, "Első")),
                        "Simulated static scrape failure"
                )),
                List.of(),
                new FailingVerseExportService(exportFailure)
        );

        PartialScrapeException exception = assertThrows(PartialScrapeException.class, scraperCoordinator::run);

        assertEquals("Simulated static scrape failure", exception.getMessage());
        assertEquals(1, exception.getSuppressed().length);
        assertEquals(exportFailure, exception.getSuppressed()[0]);
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

    private static final class FailingStaticTranslationScraper extends AbstractStaticSiteScraper {

        private final String id;
        private final String translation;
        private final List<VerseRecord> partialVerses;
        private final String failureMessage;
        private int scrapeCalls;

        private FailingStaticTranslationScraper(String id, String translation, List<VerseRecord> verses, String failureMessage) {
            this.id = id;
            this.translation = translation;
            this.partialVerses = verses;
            this.failureMessage = failureMessage;
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
            throw new PartialScrapeException(failureMessage, new IllegalStateException(failureMessage), partialVerses);
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
            return java.util.regex.Pattern.compile("^");
        }

        @Override
        protected java.util.regex.Pattern chapterLinkPattern(String bookCode) {
            return java.util.regex.Pattern.compile("^");
        }
    }

    private static final class CountingDynamicTranslationScraper extends AbstractDynamicSiteScraper {

        private final String id;
        private final List<VerseRecord> scrapeResults;
        private int scrapeCalls;
        private int scrapeFromCalls;
        private String lastStartUrl;

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
        public List<VerseRecord> scrapeFrom(String startUrl) {
            scrapeFromCalls++;
            lastStartUrl = startUrl;
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
        private int exportCalls;

        @Override
        public void exportToSqlite(Path databasePath, List<VerseRecord> verses) {
            exportCalls++;
            exportPath = databasePath;
            exportedVerses = verses;
        }
    }

    private static final class FailingVerseExportService extends VerseExportService {

        private final RuntimeException exception;

        private FailingVerseExportService(RuntimeException exception) {
            this.exception = exception;
        }

        @Override
        public void exportToSqlite(Path databasePath, List<VerseRecord> verses) {
            throw exception;
        }
    }
}

