package hu.szegedibibliaszol.scraper.service;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
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

    private static class TestAbstractStaticSiteScraper extends AbstractStaticSiteScraper {

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
    }
}


