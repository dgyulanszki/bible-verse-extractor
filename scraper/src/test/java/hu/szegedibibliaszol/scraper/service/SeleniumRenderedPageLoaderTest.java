package hu.szegedibibliaszol.scraper.service;

import java.io.IOException;
import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SeleniumRenderedPageLoaderTest {

    private static final String HEADLESS_BROWSER_PROPERTY = "scraper.dynamicBrowserHeadless";

    @Test
    void defaultBrowserCandidatesExposeAllSupportedBrowsers() {
        List<SeleniumRenderedPageLoader.BrowserDriverCandidate> browserCandidates = SeleniumRenderedPageLoader.defaultBrowserCandidates();

        assertEquals(List.of("Google Chrome"),
                browserCandidates.stream().map(SeleniumRenderedPageLoader.BrowserDriverCandidate::name).toList());
    }

    @Test
    void browserOptionFactoriesSetExpectedWindowArguments() {
        assertTrue(SeleniumRenderedPageLoader.chromeArguments().contains("--window-size=1920,1080"));
        assertFalse(SeleniumRenderedPageLoader.chromeOptions().asMap().toString().contains("--headless=new"));
    }

    @Test
    void chromeOptionsAddHeadlessArgumentWhenRequested() {
        withSystemProperty(HEADLESS_BROWSER_PROPERTY, "true", () ->
                assertTrue(SeleniumRenderedPageLoader.chromeOptions().asMap().toString().contains("--headless=new"))
        );
    }

    @Test
    void renderedChapterDetectionRecognizesRenderedAndChallengeMarkup() {
        assertTrue(SeleniumRenderedPageLoader.containsRenderedChapterContent(validRenderedHtml()));
        assertFalse(SeleniumRenderedPageLoader.containsRenderedChapterContent(clientChallengeHtml()));
        assertTrue(SeleniumRenderedPageLoader.isClientChallengePage(clientChallengeHtml()));
        assertFalse(SeleniumRenderedPageLoader.isClientChallengePage(validRenderedHtml()));
    }

    @Test
    void loadUsesNextAvailableBrowserCandidateAndParsesRenderedHtml() throws Exception {
        AtomicInteger navigateCount = new AtomicInteger();
        AtomicInteger quitCount = new AtomicInteger();
        AtomicInteger createdDrivers = new AtomicInteger();
        WebDriver webDriver = browserDriver(() -> validRenderedHtml(), navigateCount, quitCount, null);
        SeleniumRenderedPageLoader renderedPageLoader = new SeleniumRenderedPageLoader(
                List.of(
                        new SeleniumRenderedPageLoader.BrowserDriverCandidate("Broken Browser", () -> {
                            throw new IllegalStateException("missing browser");
                        }),
                        new SeleniumRenderedPageLoader.BrowserDriverCandidate("Stub Browser", () -> {
                            createdDrivers.incrementAndGet();
                            return webDriver;
                        })
                ),
                Duration.ofSeconds(1),
                Duration.ofMillis(20)
        );

        org.jsoup.nodes.Document chapterPage = renderedPageLoader.load("https://example.test/bible/198/GEN.1.EFO");
        renderedPageLoader.close();

        assertNotNull(chapterPage.selectFirst("div[data-testid=chapter-content]"));
        assertEquals(1, navigateCount.get());
        assertEquals(1, quitCount.get());
        assertEquals(1, createdDrivers.get());
    }

    @Test
    void loadReusesStartedBrowserAcrossMultipleRequests() throws Exception {
        AtomicInteger navigateCount = new AtomicInteger();
        AtomicInteger quitCount = new AtomicInteger();
        AtomicInteger createdDrivers = new AtomicInteger();
        SeleniumRenderedPageLoader renderedPageLoader = new SeleniumRenderedPageLoader(
                List.of(new SeleniumRenderedPageLoader.BrowserDriverCandidate("Stub Browser", () -> {
                    createdDrivers.incrementAndGet();
                    return browserDriver(() -> validRenderedHtml(), navigateCount, quitCount, null);
                })),
                Duration.ofSeconds(1),
                Duration.ofMillis(20)
        );

        renderedPageLoader.load("https://example.test/bible/198/GEN.1.EFO");
        renderedPageLoader.load("https://example.test/bible/198/GEN.2.EFO");
        renderedPageLoader.close();

        assertEquals(2, navigateCount.get());
        assertEquals(1, createdDrivers.get());
        assertEquals(1, quitCount.get());
    }

    @Test
    void loadThrowsClearMessageWhenAllBrowserCandidatesFail() {
        SeleniumRenderedPageLoader renderedPageLoader = new SeleniumRenderedPageLoader(
                List.of(
                        new SeleniumRenderedPageLoader.BrowserDriverCandidate("Browser A", () -> {
                            throw new IllegalStateException("failure A");
                        }),
                        new SeleniumRenderedPageLoader.BrowserDriverCandidate("Browser B", () -> {
                            throw new IllegalStateException("failure B");
                        })
                ),
                Duration.ofSeconds(1),
                Duration.ofMillis(20)
        );

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> renderedPageLoader.load("https://example.test/bible/198/GEN.1.EFO"));

        assertEquals(
                "Could not start a supported browser for dynamic scraping. Install Google Chrome, or disable the dynamic scraper.",
                exception.getMessage()
        );
        assertEquals("Could not start Browser B browser for dynamic scraping.", exception.getCause().getMessage());
    }

    @Test
    void defaultChromeCandidateFactoryCanStartAndCloseHeadlessChrome() {
        withSystemProperty(HEADLESS_BROWSER_PROPERTY, "true", () -> {
            WebDriver webDriver = SeleniumRenderedPageLoader.defaultBrowserCandidates().getFirst().factory().get();

            try {
                assertNotNull(webDriver);
            } finally {
                webDriver.quit();
            }
        });
    }

    @Test
    void loadThrowsClearMessageWhenChallengePagePersists() {
        AtomicInteger quitCount = new AtomicInteger();
        SeleniumRenderedPageLoader renderedPageLoader = new SeleniumRenderedPageLoader(
                List.of(new SeleniumRenderedPageLoader.BrowserDriverCandidate(
                        "Stub Browser",
                        () -> browserDriver(() -> clientChallengeHtml(), new AtomicInteger(), quitCount, null)
                )),
                Duration.ofSeconds(1),
                Duration.ofMillis(20)
        );

        IOException exception = assertThrows(IOException.class,
                () -> renderedPageLoader.load("https://example.test/bible/198/GEN.1.EFO"));
        renderedPageLoader.close();

        assertEquals(
                "Bible.com returned a client challenge page instead of chapter content for https://example.test/bible/198/GEN.1.EFO",
                exception.getMessage()
        );
        assertEquals(1, quitCount.get());
    }

    @Test
    void loadThrowsGenericTimeoutWhenRenderedMarkupNeverAppears() {
        AtomicInteger quitCount = new AtomicInteger();
        SeleniumRenderedPageLoader renderedPageLoader = new SeleniumRenderedPageLoader(
                List.of(new SeleniumRenderedPageLoader.BrowserDriverCandidate(
                        "Stub Browser",
                        () -> browserDriver(() -> "<html><body><h1>Loading...</h1></body></html>", new AtomicInteger(), quitCount, null)
                )),
                Duration.ofSeconds(1),
                Duration.ofMillis(20)
        );

        IOException exception = assertThrows(IOException.class,
                () -> renderedPageLoader.load("https://example.test/bible/198/GEN.1.EFO"));
        renderedPageLoader.close();

        assertEquals(
                "Timed out while waiting for rendered chapter content: https://example.test/bible/198/GEN.1.EFO",
                exception.getMessage()
        );
        assertEquals(1, quitCount.get());
    }

    @Test
    void loadWrapsBrowserNavigationFailures() {
        SeleniumRenderedPageLoader renderedPageLoader = new SeleniumRenderedPageLoader(
                List.of(new SeleniumRenderedPageLoader.BrowserDriverCandidate(
                        "Stub Browser",
                        () -> browserDriver(() -> validRenderedHtml(), new AtomicInteger(), new AtomicInteger(), new TimeoutException("boom"))
                )),
                Duration.ofSeconds(1),
                Duration.ofMillis(20)
        );

        IOException exception = assertThrows(IOException.class,
                () -> renderedPageLoader.load("https://example.test/bible/198/GEN.1.EFO"));

        assertEquals(
                "Could not render dynamic page in browser: https://example.test/bible/198/GEN.1.EFO",
                exception.getMessage()
        );
        assertTrue(exception.getCause().getMessage().contains("boom"));
    }

    @Test
    void closeDoesNothingBeforeAnyBrowserIsStarted() {
        SeleniumRenderedPageLoader renderedPageLoader = new SeleniumRenderedPageLoader(List.of(), Duration.ofSeconds(1), Duration.ofMillis(20));

        renderedPageLoader.close();
    }

    @Test
    void loadTreatsNullPageSourceAsBlankMarkup() {
        SeleniumRenderedPageLoader renderedPageLoader = new SeleniumRenderedPageLoader(
                List.of(new SeleniumRenderedPageLoader.BrowserDriverCandidate(
                        "Stub Browser",
                        () -> browserDriver(() -> null, new AtomicInteger(), new AtomicInteger(), null)
                )),
                Duration.ofSeconds(1),
                Duration.ofMillis(20)
        );

        IOException exception = assertThrows(IOException.class,
                () -> renderedPageLoader.load("https://example.test/bible/198/GEN.1.EFO"));

        assertEquals(
                "Timed out while waiting for rendered chapter content: https://example.test/bible/198/GEN.1.EFO",
                exception.getMessage()
        );
    }

    private static void withSystemProperty(String propertyName, String propertyValue, Runnable action) {
        String previousValue = System.getProperty(propertyName);

        try {
            if (propertyValue == null) {
                System.clearProperty(propertyName);
            } else {
                System.setProperty(propertyName, propertyValue);
            }
            action.run();
        } finally {
            if (previousValue == null) {
                System.clearProperty(propertyName);
            } else {
                System.setProperty(propertyName, previousValue);
            }
        }
    }

    private static WebDriver browserDriver(
            Supplier<String> pageSourceSupplier,
            AtomicInteger navigateCount,
            AtomicInteger quitCount,
            RuntimeException navigationFailure
    ) {
        WebDriver.Timeouts timeouts = proxy(WebDriver.Timeouts.class, (proxy, method, args) -> proxy);
        WebDriver.Options options = proxy(WebDriver.Options.class, (proxy, method, args) -> {
            if ("timeouts".equals(method.getName())) {
                return timeouts;
            }
            throw new UnsupportedOperationException(method.getName());
        });
        WebDriver.Navigation navigation = proxy(WebDriver.Navigation.class, (proxy, method, args) -> {
            if ("to".equals(method.getName())) {
                navigateCount.incrementAndGet();
                if (navigationFailure != null) {
                    throw navigationFailure;
                }
                return null;
            }
            throw new UnsupportedOperationException(method.getName());
        });
        return proxy(WebDriver.class, (proxy, method, args) -> switch (method.getName()) {
            case "navigate" -> navigation;
            case "manage" -> options;
            case "getPageSource" -> pageSourceSupplier.get();
            case "quit" -> {
                quitCount.incrementAndGet();
                yield null;
            }
            case "close" -> null;
            case "toString" -> "StubWebDriver";
            default -> throw new UnsupportedOperationException(method.getName());
        });
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, java.lang.reflect.InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler);
    }

    private static String validRenderedHtml() {
        return """
                <!DOCTYPE html>
                <html>
                <body>
                    <button><div>EFO</div></button>
                    <h1>Mózes első könyve 1</h1>
                    <div data-testid="chapter-content">
                        <span class="Verse__verse" data-usfm="GEN.1.1">
                            <span class="Verse__content">Kezdetben</span>
                        </span>
                    </div>
                </body>
                </html>
                """;
    }

    private static String clientChallengeHtml() {
        return """
                <!DOCTYPE html>
                <html>
                <head><title>Client Challenge</title></head>
                <body>
                    <noscript>
                        <p>JavaScript is disabled in your browser.</p>
                    </noscript>
                </body>
                </html>
                """;
    }
}
