package hu.szegedibibliaszol.scraper.service;

import hu.szegedibibliaszol.scraper.model.VerseRecord;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StaticRevidealtKaroliScraperTest {

    @Test
    void scrapeTraversesBooksChaptersAndVerses() {
        TestStaticRevidealtKaroliScraper staticSiteScraper = new TestStaticRevidealtKaroliScraper(Map.ofEntries(
                Map.entry("https://www.online-biblia.ro/bible/4", document("""
                        <html><body>
                        <a href="/bible/4/GEN">1. Mózes</a>
                        <a href="/bible/4/GEN">1. Mózes duplicate</a>
                        <a href="/bible/4/EXO">2. Mózes</a>
                        <a href="/bible/3/GEN">ignore other translation</a>
                        </body></html>
                        """)),
                Map.entry("https://www.online-biblia.ro/bible/4/GEN", document("""
                        <html><body>
                        <a href="/bible/4/GEN/1">1</a>
                        <a href="/bible/4/GEN/1">1 again</a>
                        <a href="/bible/4/GEN/2">2</a>
                        </body></html>
                        """)),
                Map.entry("https://www.online-biblia.ro/bible/4/EXO", document("""
                        <html><body>
                        <a href="/bible/4/EXO/1">1</a>
                        </body></html>
                        """)),
                Map.entry("https://www.online-biblia.ro/bible/4/GEN/1", document("""
                        <html><body>
                        <dl class="bible-chapter-content">
                          <dt><a href="/bible/4/GEN/1#v1">1</a></dt>
                          <dd class="bible-context-0"><a class="vers" name="v1">Kezdetben teremtette Isten az eget és a földet.</a></dd>
                          <dt><a href="/bible/4/GEN/1#v2">2</a></dt>
                          <dd class="bible-context-0"> <a class="vers" name="v2"> A föld pedig kietlen volt. </a> </dd>
                        </dl>
                        </body></html>
                        """)),
                Map.entry("https://www.online-biblia.ro/bible/4/GEN/2", document("""
                        <html><body>
                        <dl class="bible-chapter-content">
                          <dt><a href="/bible/4/GEN/2#v1">1</a></dt>
                          <dd class="bible-context-0">És elkészült a menny és a föld.</dd>
                        </dl>
                        </body></html>
                        """)),
                Map.entry("https://www.online-biblia.ro/bible/4/EXO/1", document("""
                        <html><body>
                        <dl class="bible-chapter-content">
                          <dt><a href="/bible/4/EXO/1#v1">1</a></dt>
                          <dd class="bible-context-0"><a class="vers" name="v1">Ezek pedig Izráel fiainak nevei.</a></dd>
                        </dl>
                        </body></html>
                        """))
        ));

        List<VerseRecord> verses = staticSiteScraper.scrape();

        assertEquals(List.of(
                "https://www.online-biblia.ro/bible/4",
                "https://www.online-biblia.ro/bible/4/GEN",
                "https://www.online-biblia.ro/bible/4/GEN/1",
                "https://www.online-biblia.ro/bible/4/GEN/2",
                "https://www.online-biblia.ro/bible/4/EXO",
                "https://www.online-biblia.ro/bible/4/EXO/1"
        ), staticSiteScraper.visitedUrls());
        assertEquals(4, verses.size());
        assertEquals(new VerseRecord("Revideált Károli", "1. Mózes", 1, 1, "Kezdetben teremtette Isten az eget és a földet."), verses.get(0));
        assertEquals(new VerseRecord("Revideált Károli", "1. Mózes", 1, 2, "A föld pedig kietlen volt."), verses.get(1));
        assertEquals(new VerseRecord("Revideált Károli", "1. Mózes", 2, 1, "És elkészült a menny és a föld."), verses.get(2));
        assertEquals(new VerseRecord("Revideált Károli", "2. Mózes", 1, 1, "Ezek pedig Izráel fiainak nevei."), verses.get(3));
    }

    @Test
    void scrapeWrapsDocumentLoadingFailures() {
        StaticRevidealtKaroliScraper staticSiteScraper = new StaticRevidealtKaroliScraper() {
            @Override
            protected Document readDocument(String url) throws IOException {
                throw new IOException("boom");
            }
        };

        IllegalStateException exception = assertThrows(IllegalStateException.class, staticSiteScraper::scrape);

        assertEquals("Could not load static page: https://www.online-biblia.ro/bible/4", exception.getMessage());
        assertInstanceOf(IOException.class, exception.getCause());
    }

    @Test
    void scrapeFailsWhenVerseContainerIsMissing() {
        TestStaticRevidealtKaroliScraper staticSiteScraper = new TestStaticRevidealtKaroliScraper(Map.of(
                "https://www.online-biblia.ro/bible/4", document("<a href=\"/bible/4/GEN\">1. Mózes</a>"),
                "https://www.online-biblia.ro/bible/4/GEN", document("<a href=\"/bible/4/GEN/1\">1</a>"),
                "https://www.online-biblia.ro/bible/4/GEN/1", document("<html><body>missing content</body></html>")
        ));

        IllegalStateException exception = assertThrows(IllegalStateException.class, staticSiteScraper::scrape);

        assertEquals("Could not find verse container for 1. Mózes 1", exception.getMessage());
    }

    @Test
    void scrapeFailsWhenVerseTextIsMissing() {
        TestStaticRevidealtKaroliScraper staticSiteScraper = new TestStaticRevidealtKaroliScraper(Map.of(
                "https://www.online-biblia.ro/bible/4", document("<a href=\"/bible/4/GEN\">1. Mózes</a>"),
                "https://www.online-biblia.ro/bible/4/GEN", document("<a href=\"/bible/4/GEN/1\">1</a>"),
                "https://www.online-biblia.ro/bible/4/GEN/1", document("""
                        <dl class="bible-chapter-content">
                          <dt><a href="/bible/4/GEN/1#v1">1</a></dt>
                          <span>wrong element</span>
                        </dl>
                        """)
        ));

        IllegalStateException exception = assertThrows(IllegalStateException.class, staticSiteScraper::scrape);

        assertEquals("Missing verse text for 1. Mózes 1:1", exception.getMessage());
    }

    @Test
    void scrapeFailsWhenVerseNumberCannotBeParsed() {
        TestStaticRevidealtKaroliScraper staticSiteScraper = new TestStaticRevidealtKaroliScraper(Map.of(
                "https://www.online-biblia.ro/bible/4", document("<a href=\"/bible/4/GEN\">1. Mózes</a>"),
                "https://www.online-biblia.ro/bible/4/GEN", document("<a href=\"/bible/4/GEN/1\">1</a>"),
                "https://www.online-biblia.ro/bible/4/GEN/1", document("""
                        <dl class="bible-chapter-content">
                          <dt><a href="/bible/4/GEN/1#vX">X</a></dt>
                          <dd class="bible-context-0"><a class="vers" name="vX">text</a></dd>
                        </dl>
                        """)
        ));

        IllegalStateException exception = assertThrows(IllegalStateException.class, staticSiteScraper::scrape);

        assertEquals("Could not parse verse number: X", exception.getMessage());
        assertInstanceOf(IllegalStateException.class, exception.getCause());
        assertInstanceOf(NumberFormatException.class, exception.getCause().getCause());
    }

    @Test
    void scrapeFailsWhenBookChapterCannotBeParsed() {
        TestStaticRevidealtKaroliScraper staticSiteScraper = new TestStaticRevidealtKaroliScraper(Map.of(
                "https://www.online-biblia.ro/bible/4", document("<a href=\"/bible/4/GEN\">1. Mózes</a>"),
                "https://www.online-biblia.ro/bible/4/GEN", document("<a href=\"/bible/4/GEN/ABC\">ABC</a>")
        ));

        List<VerseRecord> verses = staticSiteScraper.scrape();

        assertEquals(List.of(), verses);
        assertEquals(List.of(
                "https://www.online-biblia.ro/bible/4",
                "https://www.online-biblia.ro/bible/4/GEN"
        ), staticSiteScraper.visitedUrls());
    }

    @Test
    void scrapeFailsWhenVerseTextIsBlank() {
        TestStaticRevidealtKaroliScraper staticSiteScraper = new TestStaticRevidealtKaroliScraper(Map.of(
                "https://www.online-biblia.ro/bible/4", document("<a href=\"/bible/4/GEN\">1. Mózes</a>"),
                "https://www.online-biblia.ro/bible/4/GEN", document("<a href=\"/bible/4/GEN/1\">1</a>"),
                "https://www.online-biblia.ro/bible/4/GEN/1", document("""
                        <dl class="bible-chapter-content">
                          <dt><a href="/bible/4/GEN/1#v1">1</a></dt>
                          <dd class="bible-context-0"><a class="vers" name="v1">   </a></dd>
                        </dl>
                        """)
        ));

        IllegalStateException exception = assertThrows(IllegalStateException.class, staticSiteScraper::scrape);

        assertEquals("Blank verse text for 1. Mózes 1:1", exception.getMessage());
    }

    private static Document document(String html) {
        return Jsoup.parse(html, "https://www.online-biblia.ro");
    }

    private static final class TestStaticRevidealtKaroliScraper extends StaticRevidealtKaroliScraper {

        private final Map<String, Document> documents;
        private final List<String> visitedUrls = new ArrayList<>();

        private TestStaticRevidealtKaroliScraper(Map<String, Document> documents) {
            this.documents = new HashMap<>(documents);
        }

        @Override
        protected Document loadDocument(String url) {
            visitedUrls.add(url);
            Document document = documents.get(url);
            if (document == null) {
                throw new IllegalStateException("Unexpected URL: " + url);
            }
            return document;
        }

        private List<String> visitedUrls() {
            return List.copyOf(visitedUrls);
        }
    }
}


