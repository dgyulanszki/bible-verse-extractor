package hu.szegedibibliaszol.scraper.service;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import hu.szegedibibliaszol.scraper.model.VerseRecord;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DynamicEfoScraperTest {

    @Test
    void startUrlPointsToTheEfoGenesisEntryChapter() {
        DynamicEfoScraper dynamicSiteScraper = new DynamicEfoScraper();

        assertEquals("https://www.bible.com/bible/198/GEN.1.EFO", dynamicSiteScraper.startUrl());
    }

    @Test
    void scrapeCollectsVersesAcrossLinkedChapters() throws IOException {
        Map<String, String> responses = new LinkedHashMap<>();
        responses.put("/bible/198/GEN.1.EFO", chapterPage(
                "Mózes első könyve 1",
                "EFO",
                """
                        <div class="Paragraph__p">
                            <span class="Verse__ignored" data-usfm="GEN.1.99">
                                <span class="Verse__content">Ezt figyelmen kívül kell hagyni.</span>
                            </span>
                            <span class="Verse__verse" data-usfm="GEN.1.1">
                                <span class="Verse__label">1</span>
                                <span class="Verse__content">Kezdetben</span>
                            </span>
                            <span class="Verse__verse" data-usfm="GEN.1.2">
                                <span class="Verse__label">2</span>
                                <span class="Verse__content">Az első rész</span>
                                <span class="Verse__note">
                                    <span class="Verse__label">#</span>
                                    <span class="Verse__content">Jegyzet</span>
                                </span>
                                <span class="Verse__content">a folytatás.</span>
                            </span>
                        </div>
                        <div class="Paragraph__q1">
                            <span class="Verse__verse" data-usfm="GEN.1.2">
                                <span class="Verse__content">Még egy sor.</span>
                            </span>
                            <span class="Verse__verse" data-usfm="GEN.1.2">
                                <span class="Verse__content"> </span>
                            </span>
                        </div>
                        """,
                "/bible/198/GEN.2.EFO"
        ));
        responses.put("/bible/198/GEN.2.EFO", chapterPage(
                "Mózes első könyve 2",
                "EFO",
                """
                        <div class="Paragraph__p">
                            <span class="Verse__verse" data-usfm="GEN.2.1">
                                <span class="Verse__label">1</span>
                                <span class="Verse__content">Így készült el az ég és a föld.</span>
                            </span>
                        </div>
                        <a href="/bible/198/GEN.1.EFO"><svg><title>Previous Chapter</title></svg></a>
                        """,
                null
        ));

        try (TestHttpServer server = startServer(responses)) {
            DynamicEfoScraper dynamicSiteScraper = new TestDynamicEfoScraper(server.url("/bible/198/GEN.1.EFO"));

            List<VerseRecord> verses = dynamicSiteScraper.scrape();

            assertEquals(List.of(
                    new VerseRecord("Egyszerű fordítás (EFO)", "Mózes első könyve", 1, 1, "Kezdetben"),
                    new VerseRecord("Egyszerű fordítás (EFO)", "Mózes első könyve", 1, 2, "Az első rész a folytatás. Még egy sor."),
                    new VerseRecord("Egyszerű fordítás (EFO)", "Mózes első könyve", 2, 1, "Így készült el az ég és a föld.")
            ), verses);
        }
    }

    @Test
    void scrapeWrapsDocumentLoadFailures() {
        DynamicEfoScraper dynamicSiteScraper = new TestDynamicEfoScraper("http://127.0.0.1:1/bible/198/GEN.1.EFO");

        IllegalStateException exception = assertThrows(IllegalStateException.class, dynamicSiteScraper::scrape);

        assertEquals("Could not load dynamic page: http://127.0.0.1:1/bible/198/GEN.1.EFO", exception.getMessage());
    }

    @Test
    void scrapeFailsWhenTheSelectedTranslationIsNotEfo() throws IOException {
        try (TestHttpServer server = startServer(Map.of(
                "/bible/198/GEN.1.EFO",
                chapterPage("Mózes első könyve 1", "RÚF", validVerseMarkup("GEN.1.1", "Kezdetben"), null)
        ))) {
            DynamicEfoScraper dynamicSiteScraper = new TestDynamicEfoScraper(server.url("/bible/198/GEN.1.EFO"));

            IllegalStateException exception = assertThrows(IllegalStateException.class, dynamicSiteScraper::scrape);

            assertEquals(
                    "Expected translation dropdown to show EFO for " + server.url("/bible/198/GEN.1.EFO"),
                    exception.getMessage()
            );
        }
    }

    @Test
    void scrapeFailsWhenTheChapterHeadingIsMissing() throws IOException {
        try (TestHttpServer server = startServer(Map.of(
                "/bible/198/GEN.1.EFO",
                chapterPageWithoutHeading("EFO", validVerseMarkup("GEN.1.1", "Kezdetben"))
        ))) {
            DynamicEfoScraper dynamicSiteScraper = new TestDynamicEfoScraper(server.url("/bible/198/GEN.1.EFO"));

            IllegalStateException exception = assertThrows(IllegalStateException.class, dynamicSiteScraper::scrape);

            assertEquals("Could not find chapter heading for " + server.url("/bible/198/GEN.1.EFO"), exception.getMessage());
        }
    }

    @Test
    void scrapeFailsWhenTheHeadingDoesNotContainABookName() throws IOException {
        try (TestHttpServer server = startServer(Map.of(
                "/bible/198/GEN.1.EFO",
                chapterPage("", "EFO", validVerseMarkup("GEN.1.1", "Kezdetben"), null)
        ))) {
            DynamicEfoScraper dynamicSiteScraper = new TestDynamicEfoScraper(server.url("/bible/198/GEN.1.EFO"));

            IllegalStateException exception = assertThrows(IllegalStateException.class, dynamicSiteScraper::scrape);

            assertEquals(
                    "Could not determine book name from heading for " + server.url("/bible/198/GEN.1.EFO"),
                    exception.getMessage()
            );
        }
    }

    @Test
    void scrapeFailsWhenTheVerseContainerIsMissing() throws IOException {
        try (TestHttpServer server = startServer(Map.of(
                "/bible/198/GEN.1.EFO",
                pageWithoutVerseContainer("Mózes első könyve 1", "EFO")
        ))) {
            DynamicEfoScraper dynamicSiteScraper = new TestDynamicEfoScraper(server.url("/bible/198/GEN.1.EFO"));

            IllegalStateException exception = assertThrows(IllegalStateException.class, dynamicSiteScraper::scrape);

            assertEquals("Could not find verse container for " + server.url("/bible/198/GEN.1.EFO"), exception.getMessage());
        }
    }

    @Test
    void scrapeFailsWhenNoNonBlankVersesAreFound() throws IOException {
        try (TestHttpServer server = startServer(Map.of(
                "/bible/198/GEN.1.EFO",
                chapterPage(
                        "Mózes első könyve 1",
                        "EFO",
                        """
                                <div class="Paragraph__p">
                                    <span class="Verse__verse" data-usfm="GEN.1.1">
                                        <span class="Verse__content"> </span>
                                    </span>
                                </div>
                                """,
                        null
                )
        ))) {
            DynamicEfoScraper dynamicSiteScraper = new TestDynamicEfoScraper(server.url("/bible/198/GEN.1.EFO"));

            IllegalStateException exception = assertThrows(IllegalStateException.class, dynamicSiteScraper::scrape);

            assertEquals("Could not find non-blank verses for " + server.url("/bible/198/GEN.1.EFO"), exception.getMessage());
        }
    }

    @Test
    void scrapeFailsWhenAUsfmReferenceHasAnUnexpectedShape() throws IOException {
        try (TestHttpServer server = startServer(Map.of(
                "/bible/198/GEN.1.EFO",
                chapterPage("Mózes első könyve 1", "EFO", validVerseMarkup("GEN.1", "Kezdetben"), null)
        ))) {
            DynamicEfoScraper dynamicSiteScraper = new TestDynamicEfoScraper(server.url("/bible/198/GEN.1.EFO"));

            IllegalStateException exception = assertThrows(IllegalStateException.class, dynamicSiteScraper::scrape);

            assertEquals(
                    "Unexpected verse reference 'GEN.1' for " + server.url("/bible/198/GEN.1.EFO"),
                    exception.getMessage()
            );
        }
    }

    @Test
    void scrapeFailsWhenAUsfmReferenceContainsNonNumericParts() throws IOException {
        try (TestHttpServer server = startServer(Map.of(
                "/bible/198/GEN.1.EFO",
                chapterPage("Mózes első könyve 1", "EFO", validVerseMarkup("GEN.one.1", "Kezdetben"), null)
        ))) {
            DynamicEfoScraper dynamicSiteScraper = new TestDynamicEfoScraper(server.url("/bible/198/GEN.1.EFO"));

            IllegalStateException exception = assertThrows(IllegalStateException.class, dynamicSiteScraper::scrape);

            assertEquals(
                    "Unexpected verse reference 'GEN.one.1' for " + server.url("/bible/198/GEN.1.EFO"),
                    exception.getMessage()
            );
        }
    }

    @Test
    void scrapeUsesTheFirstVerseNumberFromCombinedUsfmReferences() throws IOException {
        try (TestHttpServer server = startServer(Map.of(
                "/bible/198/GEN.2.EFO",
                chapterPage(
                        "Mózes első könyve 2",
                        "EFO",
                        """
                                <div class="Paragraph__p">
                                    <span class="Verse__verse" data-usfm="GEN.2.11+GEN.2.12">
                                        <span class="Verse__label">11-12</span>
                                        <span class="Verse__content">Az első folyó a Písón.</span>
                                    </span>
                                </div>
                                """,
                        null
                )
        ))) {
            DynamicEfoScraper dynamicSiteScraper = new TestDynamicEfoScraper(server.url("/bible/198/GEN.2.EFO"));

            List<VerseRecord> verses = dynamicSiteScraper.scrape();

            assertEquals(List.of(
                    new VerseRecord("Egyszerű fordítás (EFO)", "Mózes első könyve", 2, 11, "Az első folyó a Písón.")
            ), verses);
        }
    }

    @Test
    void scrapeDetectsNavigationLoops() throws IOException {
        try (TestHttpServer server = startServer(Map.of(
                "/bible/198/GEN.1.EFO",
                chapterPage("Mózes első könyve 1", "EFO", validVerseMarkup("GEN.1.1", "Kezdetben"), "/bible/198/GEN.1.EFO")
        ))) {
            DynamicEfoScraper dynamicSiteScraper = new TestDynamicEfoScraper(server.url("/bible/198/GEN.1.EFO"));

            IllegalStateException exception = assertThrows(IllegalStateException.class, dynamicSiteScraper::scrape);

            assertEquals(
                    "Detected navigation loop while scraping Egyszerű fordítás (EFO) at " + server.url("/bible/198/GEN.1.EFO"),
                    exception.getMessage()
            );
        }
    }

    private static TestHttpServer startServer(Map<String, String> responsesByPath) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        for (Map.Entry<String, String> response : responsesByPath.entrySet()) {
            server.createContext(response.getKey(), exchange -> writeHtmlResponse(exchange, response.getValue()));
        }
        server.start();
        return new TestHttpServer(server);
    }

    private static void writeHtmlResponse(HttpExchange exchange, String html) throws IOException {
        byte[] body = html.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
        exchange.sendResponseHeaders(200, body.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(body);
        }
    }

    private static String chapterPage(String heading, String translationButton, String chapterMarkup, String nextHref) {
        String nextLinkMarkup = nextHref == null
                ? ""
                : "<a href=\"" + nextHref + "\"><svg><title>Next Chapter</title></svg></a>";
        return """
                <!DOCTYPE html>
                <html>
                <body>
                    <button><div>%s</div></button>
                    <h1>%s</h1>
                    <div data-testid="chapter-content">%s</div>
                    %s
                </body>
                </html>
                """.formatted(translationButton, heading, chapterMarkup, nextLinkMarkup);
    }

    private static String chapterPageWithoutHeading(String translationButton, String chapterMarkup) {
        return """
                <!DOCTYPE html>
                <html>
                <body>
                    <button><div>%s</div></button>
                    <div data-testid="chapter-content">%s</div>
                </body>
                </html>
                """.formatted(translationButton, chapterMarkup);
    }

    private static String pageWithoutVerseContainer(String heading, String translationButton) {
        return """
                <!DOCTYPE html>
                <html>
                <body>
                    <button><div>%s</div></button>
                    <h1>%s</h1>
                </body>
                </html>
                """.formatted(translationButton, heading);
    }

    private static String validVerseMarkup(String dataUsfm, String text) {
        return """
                <div class="Paragraph__p">
                    <span class="Verse__verse" data-usfm="%s">
                        <span class="Verse__label">1</span>
                        <span class="Verse__content">%s</span>
                    </span>
                </div>
                """.formatted(dataUsfm, text);
    }

    private static final class TestDynamicEfoScraper extends DynamicEfoScraper {

        private final String startUrl;

        private TestDynamicEfoScraper(String startUrl) {
            this.startUrl = startUrl;
        }

        @Override
        protected String startUrl() {
            return startUrl;
        }
    }

    private static final class TestHttpServer implements AutoCloseable {

        private final HttpServer server;

        private TestHttpServer(HttpServer server) {
            this.server = server;
        }

        private String url(String path) {
            return "http://127.0.0.1:" + server.getAddress().getPort() + path;
        }

        @Override
        public void close() {
            server.stop(0);
        }
    }
}

