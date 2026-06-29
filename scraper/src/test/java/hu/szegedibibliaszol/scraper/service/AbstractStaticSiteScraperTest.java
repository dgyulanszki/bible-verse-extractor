package hu.szegedibibliaszol.scraper.service;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AbstractStaticSiteScraperTest {

    @Test
    void readDocumentLoadsUtf8Html() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        String responseBody = "<html><body><p>árvíztűrő tükörfúrógép</p></body></html>";
        server.createContext("/chapter", exchange -> {
            byte[] responseBytes = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, responseBytes.length);
            try (OutputStream responseStream = exchange.getResponseBody()) {
                responseStream.write(responseBytes);
            }
        });
        server.start();

        try {
            TestAbstractStaticSiteScraper scraper = new TestAbstractStaticSiteScraper();

            Document document = scraper.readDocument("http://127.0.0.1:" + server.getAddress().getPort() + "/chapter");

            assertTrue(document.text().contains("árvíztűrő tükörfúrógép"));
        } finally {
            server.stop(0);
        }
    }

    @Test
    void loadDocumentDelegatesToReadDocument() {
        Document expectedDocument = Jsoup.parse("<html><body><p>betöltve</p></body></html>");
        TestAbstractStaticSiteScraper scraper = new TestAbstractStaticSiteScraper() {
            @Override
            protected Document readDocument(String url) {
                return expectedDocument;
            }
        };

        Document document = scraper.loadDocument("http://example.com/test");

        assertEquals(expectedDocument, document);
    }

    @Test
    void extractBookNameFailsWhenBlank() {
        TestAbstractStaticSiteScraper scraper = new TestAbstractStaticSiteScraper();
        Element link = Jsoup.parse("<a href=\"/test/GEN\">   </a>").selectFirst("a");
        assertNotNull(link);

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> scraper.extractBookName(link));

        assertEquals("Blank book name for link: /test/GEN", exception.getMessage());
    }

    @Test
    void extractChapterNumberReturnsNullWhenParsedValueIsInvalid() {
        TestAbstractStaticSiteScraper scraper = new TestAbstractStaticSiteScraper();
        Element link = Jsoup.parse("<a href=\"/test/GEN/ABC\">ABC</a>").selectFirst("a");
        assertNotNull(link);

        Integer chapterNumber = scraper.extractChapterNumber("/test/GEN/ABC", link, "GEN");

        assertNull(chapterNumber);
    }

    @Test
    void locateVerseRowsFailsWhenVerseTextElementIsMissing() {
        TestAbstractStaticSiteScraper scraper = new TestAbstractStaticSiteScraper();
        Element chapterContent = Jsoup.parse("<dl class=\"bible-chapter-content\"><dt>1</dt></dl>")
                .selectFirst("dl.bible-chapter-content");
        assertNotNull(chapterContent);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> scraper.locateVerseRows(chapterContent, "1. Mózes", 1));

        assertEquals("Missing verse text for 1. Mózes 1:1", exception.getMessage());
    }

    @Test
    void scrapeExposesAlreadyCollectedVersesWhenALaterChapterFails() {
        TestAbstractStaticSiteScraper scraper = new TestAbstractStaticSiteScraper(Map.of(
                "http://example.com/test", "<a href='/test/GEN'>1. Mózes</a>",
                "http://example.com/test/GEN", "<a href='/test/GEN/1'>1</a><a href='/test/GEN/2'>2</a>",
                "http://example.com/test/GEN/1", "<dl class='bible-chapter-content'><dt>1</dt><dd><a class='vers'>Kezdetben</a></dd></dl>",
                "http://example.com/test/GEN/2", "<div>Nincs vers tartalom</div>"
        ));

        PartialScrapeException exception = assertThrows(PartialScrapeException.class, scraper::scrape);

        assertEquals("Could not find verse container for 1. Mózes 2", exception.getMessage());
        assertEquals(List.of(
                new hu.szegedibibliaszol.scraper.model.VerseRecord("Test Translation", "1. Mózes", 1, 1, "Kezdetben")
        ), exception.partialVerses());
    }

    private static class TestAbstractStaticSiteScraper extends AbstractStaticSiteScraper {

        private final Map<String, String> htmlByUrl;

        private TestAbstractStaticSiteScraper() {
            this(Map.of());
        }

        private TestAbstractStaticSiteScraper(Map<String, String> htmlByUrl) {
            this.htmlByUrl = htmlByUrl;
        }

        @Override
        public String id() {
            return "test";
        }

        @Override
        public String translation() {
            return "Test Translation";
        }

        @Override
        protected String baseUrl() {
            return "http://example.com";
        }

        @Override
        protected String rootPath() {
            return "/test";
        }

        @Override
        protected Pattern bookLinkPattern() {
            return Pattern.compile("^/test/([A-Z0-9]+)$");
        }

        @Override
        protected Pattern chapterLinkPattern(String bookCode) {
            return Pattern.compile("^/test/" + Pattern.quote(bookCode) + "/(.+)$");
        }

        @Override
        protected Document loadDocument(String url) {
            String html = htmlByUrl.get(url);
            if (html == null) {
                return super.loadDocument(url);
            }
            return Jsoup.parse(html, url);
        }
    }
}


