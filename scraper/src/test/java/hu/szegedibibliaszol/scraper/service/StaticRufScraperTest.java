package hu.szegedibibliaszol.scraper.service;

import hu.szegedibibliaszol.scraper.model.VerseRecord;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StaticRufScraperTest {

    @Test
    void scrapeTraversesBooksChaptersAndVerses() {
        TestStaticRufScraper scraper = new TestStaticRufScraper(Map.ofEntries(
                Map.entry("https://abibliamindenkie.hu/uj", document("""
                        <html><body>
                        <a href="https://abibliamindenkie.hu/uj/GEN">Mózes első könyve</a>
                        <a href="/uj/GEN">Mózes első könyve duplicate</a>
                        <a href="/uj/EXO">Mózes második könyve</a>
                        <a href="/karoli/GEN">ignore other translation</a>
                        </body></html>
                        """)),
                Map.entry("https://abibliamindenkie.hu/uj/GEN", document("""
                        <html><body>
                        <a href="https://abibliamindenkie.hu/uj/GEN/1">1. fejezet</a>
                        <a href="/uj/GEN/1">1. fejezet duplicate</a>
                        <a href="/uj/GEN/2">2. fejezet</a>
                        </body></html>
                        """)),
                Map.entry("https://abibliamindenkie.hu/uj/EXO", document("""
                        <html><body>
                        <a href="/uj/EXO/1">1. fejezet</a>
                        </body></html>
                        """)),
                Map.entry("https://abibliamindenkie.hu/uj/GEN/1", document("""
                        <html><body>
                        <div class="chapter__body">
                          <p class="verse"><a class="verse__number">1</a>Kezdetben teremtette Isten az eget és a földet.<span class="verse__crossreference"><a href="/uj/JHN/1/1#1">Jn 1,1</a></span></p>
                          <p class="verse"><a class="verse__number">2</a>A föld pedig kietlen és puszta volt.<span class="br"></span></p>
                        </div>
                        </body></html>
                        """)),
                Map.entry("https://abibliamindenkie.hu/uj/GEN/2", document("""
                        <html><body>
                        <div class="chapter__body">
                          <p class="verse"><a class="verse__number">1</a>Így készült el az ég és a föld.</p>
                        </div>
                        </body></html>
                        """)),
                Map.entry("https://abibliamindenkie.hu/uj/EXO/1", document("""
                        <html><body>
                        <div class="chapter__body">
                          <p class="verse"><a class="verse__number">1</a>Ezek annak a tizenkét fiúnak a nevei.</p>
                        </div>
                        </body></html>
                        """))
        ));

        List<VerseRecord> verses = scraper.scrape();

        assertEquals(List.of(
                "https://abibliamindenkie.hu/uj",
                "https://abibliamindenkie.hu/uj/GEN",
                "https://abibliamindenkie.hu/uj/GEN/1",
                "https://abibliamindenkie.hu/uj/GEN/2",
                "https://abibliamindenkie.hu/uj/EXO",
                "https://abibliamindenkie.hu/uj/EXO/1"
        ), scraper.visitedUrls());
        assertEquals(List.of(
                new VerseRecord("Revideált Új Fordítás (RÚF)", "Mózes első könyve", 1, 1, "Kezdetben teremtette Isten az eget és a földet."),
                new VerseRecord("Revideált Új Fordítás (RÚF)", "Mózes első könyve", 1, 2, "A föld pedig kietlen és puszta volt."),
                new VerseRecord("Revideált Új Fordítás (RÚF)", "Mózes első könyve", 2, 1, "Így készült el az ég és a föld."),
                new VerseRecord("Revideált Új Fordítás (RÚF)", "Mózes második könyve", 1, 1, "Ezek annak a tizenkét fiúnak a nevei.")
        ), verses);
    }

    @Test
    void scrapeWrapsDocumentLoadingFailures() {
        StaticRufScraper scraper = new StaticRufScraper() {
            @Override
            protected Document readDocument(String url) throws IOException {
                throw new IOException("boom");
            }
        };

        IllegalStateException exception = assertThrows(IllegalStateException.class, scraper::scrape);

        assertEquals("Could not load static page: https://abibliamindenkie.hu/uj", exception.getMessage());
        assertInstanceOf(IOException.class, exception.getCause());
    }

    @Test
    void exposesExpectedIdsAndUrlPatterns() {
        StaticRufScraper scraper = new StaticRufScraper();

        assertEquals("revidealt-uj-forditas", scraper.id());
        assertEquals("Revideált Új Fordítás (RÚF)", scraper.translation());
        assertEquals("https://abibliamindenkie.hu", scraper.baseUrl());
        assertEquals("/uj", scraper.rootPath());
        assertTrue(scraper.bookLinkPattern().matcher("/uj/GEN").matches());
        assertTrue(scraper.chapterLinkPattern("GEN").matcher("/uj/GEN/1").matches());
    }

    @Test
    void scrapeFailsWhenVerseNumberIsMissing() {
        TestStaticRufScraper scraper = new TestStaticRufScraper(Map.of(
                "https://abibliamindenkie.hu/uj", document("<a href=\"/uj/GEN\">Mózes első könyve</a>"),
                "https://abibliamindenkie.hu/uj/GEN", document("<a href=\"/uj/GEN/1\">1. fejezet</a>"),
                "https://abibliamindenkie.hu/uj/GEN/1", document("<div class=\"chapter__body\"><p class=\"verse\">Kezdetben teremtette Isten az eget és a földet.</p></div>")
        ));

        IllegalStateException exception = assertThrows(IllegalStateException.class, scraper::scrape);

        assertEquals("Missing verse number for Mózes első könyve 1", exception.getMessage());
    }

    @Test
    void scrapeFailsWhenVerseNumberCannotBeParsed() {
        TestStaticRufScraper scraper = new TestStaticRufScraper(Map.of(
                "https://abibliamindenkie.hu/uj", document("<a href=\"/uj/GEN\">Mózes első könyve</a>"),
                "https://abibliamindenkie.hu/uj/GEN", document("<a href=\"/uj/GEN/1\">1. fejezet</a>"),
                "https://abibliamindenkie.hu/uj/GEN/1", document("<div class=\"chapter__body\"><p class=\"verse\"><a class=\"verse__number\">X</a>text</p></div>")
        ));

        IllegalStateException exception = assertThrows(IllegalStateException.class, scraper::scrape);

        assertEquals("Could not parse verse number: X", exception.getMessage());
        assertInstanceOf(IllegalStateException.class, exception.getCause());
        assertInstanceOf(NumberFormatException.class, exception.getCause().getCause());
    }

    @Test
    void scrapeAcceptsUnderscorePrefixedVerseNumberText() {
        TestStaticRufScraper scraper = new TestStaticRufScraper(Map.of(
                "https://abibliamindenkie.hu/uj", document("<a href=\"/uj/GEN\">Mózes első könyve</a>"),
                "https://abibliamindenkie.hu/uj/GEN", document("<a href=\"/uj/GEN/1\">1. fejezet</a>"),
                "https://abibliamindenkie.hu/uj/GEN/1", document("<div class=\"chapter__body\"><p class=\"verse\"><a class=\"verse__number\">_40</a>szöveg</p></div>")
        ));

        List<VerseRecord> verses = scraper.scrape();

        assertEquals(List.of(new VerseRecord("Revideált Új Fordítás (RÚF)", "Mózes első könyve", 1, 40, "szöveg")), verses);
    }

    @Test
    void scrapeIgnoresBookChaptersThatCannotBeParsed() {
        TestStaticRufScraper scraper = new TestStaticRufScraper(Map.of(
                "https://abibliamindenkie.hu/uj", document("<a href=\"/uj/GEN\">Mózes első könyve</a>"),
                "https://abibliamindenkie.hu/uj/GEN", document("<a href=\"/uj/GEN/ABC\">ABC</a>")
        ));

        List<VerseRecord> verses = scraper.scrape();

        assertEquals(List.of(), verses);
        assertEquals(List.of(
                "https://abibliamindenkie.hu/uj",
                "https://abibliamindenkie.hu/uj/GEN"
        ), scraper.visitedUrls());
    }

    @Test
    void scrapeFailsWhenVerseTextIsBlank() {
        TestStaticRufScraper scraper = new TestStaticRufScraper(Map.of(
                "https://abibliamindenkie.hu/uj", document("<a href=\"/uj/GEN\">Mózes első könyve</a>"),
                "https://abibliamindenkie.hu/uj/GEN", document("<a href=\"/uj/GEN/1\">1. fejezet</a>"),
                "https://abibliamindenkie.hu/uj/GEN/1", document("<div class=\"chapter__body\"><p class=\"verse\"><a class=\"verse__number\">1</a><span class=\"verse__crossreference\">xref</span></p></div>")
        ));

        IllegalStateException exception = assertThrows(IllegalStateException.class, scraper::scrape);

        assertEquals("Blank verse text for Mózes első könyve 1:1", exception.getMessage());
    }

    @Test
    void scrapeIgnoresVerseLikeParagraphsOutsideChapterBody() {
        TestStaticRufScraper scraper = new TestStaticRufScraper(Map.of(
                "https://abibliamindenkie.hu/uj", document("<a href=\"/uj/GEN\">Mózes első könyve</a>"),
                "https://abibliamindenkie.hu/uj/GEN", document("<a href=\"/uj/GEN/1\">1. fejezet</a>"),
                "https://abibliamindenkie.hu/uj/GEN/1", document("""
                        <html><body>
                        <aside>
                          <p class="verse"><a class="verse__number" id="999"></a>sidebar preview</p>
                        </aside>
                        <div class="chapter__body">
                          <p class="verse"><a class="verse__number">1</a>Kezdetben.</p>
                        </div>
                        </body></html>
                        """)
        ));

        List<VerseRecord> verses = scraper.scrape();

        assertEquals(List.of(new VerseRecord("Revideált Új Fordítás (RÚF)", "Mózes első könyve", 1, 1, "Kezdetben.")), verses);
    }

    @Test
    void scrapeIgnoresNonVerseChildrenInsideChapterBody() {
        TestStaticRufScraper scraper = new TestStaticRufScraper(Map.of(
                "https://abibliamindenkie.hu/uj", document("<a href=\"/uj/GEN\">Mózes első könyve</a>"),
                "https://abibliamindenkie.hu/uj/GEN", document("<a href=\"/uj/GEN/1\">1. fejezet</a>"),
                "https://abibliamindenkie.hu/uj/GEN/1", document("""
                        <div class="chapter__body">
                          <h2 class="chapter__title">A világ teremtése</h2>
                          <p class="verse"><a class="verse__number">1</a>Kezdetben.</p>
                        </div>
                        """)
        ));

        List<VerseRecord> verses = scraper.scrape();

        assertEquals(List.of(new VerseRecord("Revideált Új Fordítás (RÚF)", "Mózes első könyve", 1, 1, "Kezdetben.")), verses);
    }

    @Test
    void scrapeFallsBackToVerseAnchorIdWhenVerseNumberTextIsBlank() {
        TestStaticRufScraper scraper = new TestStaticRufScraper(Map.of(
                "https://abibliamindenkie.hu/uj", document("<a href=\"/uj/GEN\">Mózes első könyve</a>"),
                "https://abibliamindenkie.hu/uj/GEN", document("<a href=\"/uj/GEN/1\">1. fejezet</a>"),
                "https://abibliamindenkie.hu/uj/GEN/1", document("""
                        <div class="chapter__body">
                          <p class="verse"><a class="verse__number" id="1" href="#1"></a>Kezdetben.</p>
                        </div>
                        """)
        ));

        List<VerseRecord> verses = scraper.scrape();

        assertEquals(List.of(new VerseRecord("Revideált Új Fordítás (RÚF)", "Mózes első könyve", 1, 1, "Kezdetben.")), verses);
    }

    @Test
    void scrapeFallsBackToUnderscorePrefixedVerseAnchorIdWhenVerseNumberTextIsBlank() {
        TestStaticRufScraper scraper = new TestStaticRufScraper(Map.of(
                "https://abibliamindenkie.hu/uj", document("<a href=\"/uj/GEN\">Mózes első könyve</a>"),
                "https://abibliamindenkie.hu/uj/GEN", document("<a href=\"/uj/GEN/1\">1. fejezet</a>"),
                "https://abibliamindenkie.hu/uj/GEN/1", document("""
                        <div class="chapter__body">
                          <p class="verse"><a class="verse__number" id="_40" href="#_40"></a>Kezdetben.</p>
                        </div>
                        """)
        ));

        List<VerseRecord> verses = scraper.scrape();

        assertEquals(List.of(new VerseRecord("Revideált Új Fordítás (RÚF)", "Mózes első könyve", 1, 40, "Kezdetben.")), verses);
    }

    @Test
    void scrapeFallsBackToVerseAnchorHrefWhenVerseNumberTextAndIdAreBlank() {
        TestStaticRufScraper scraper = new TestStaticRufScraper(Map.of(
                "https://abibliamindenkie.hu/uj", document("<a href=\"/uj/GEN\">Mózes első könyve</a>"),
                "https://abibliamindenkie.hu/uj/GEN", document("<a href=\"/uj/GEN/1\">1. fejezet</a>"),
                "https://abibliamindenkie.hu/uj/GEN/1", document("""
                        <div class="chapter__body">
                          <p class="verse"><a class="verse__number" href="#1"></a>Kezdetben.</p>
                        </div>
                        """)
        ));

        List<VerseRecord> verses = scraper.scrape();

        assertEquals(List.of(new VerseRecord("Revideált Új Fordítás (RÚF)", "Mózes első könyve", 1, 1, "Kezdetben.")), verses);
    }

    @Test
    void scrapeFallsBackToUnderscorePrefixedVerseAnchorHrefWhenVerseNumberTextAndIdAreBlank() {
        TestStaticRufScraper scraper = new TestStaticRufScraper(Map.of(
                "https://abibliamindenkie.hu/uj", document("<a href=\"/uj/GEN\">Mózes első könyve</a>"),
                "https://abibliamindenkie.hu/uj/GEN", document("<a href=\"/uj/GEN/1\">1. fejezet</a>"),
                "https://abibliamindenkie.hu/uj/GEN/1", document("""
                        <div class="chapter__body">
                          <p class="verse"><a class="verse__number" href="#_40"></a>Kezdetben.</p>
                        </div>
                        """)
        ));

        List<VerseRecord> verses = scraper.scrape();

        assertEquals(List.of(new VerseRecord("Revideált Új Fordítás (RÚF)", "Mózes első könyve", 1, 40, "Kezdetben.")), verses);
    }

    private static Document document(String html) {
        return Jsoup.parse(html, "https://abibliamindenkie.hu");
    }

    private static final class TestStaticRufScraper extends StaticRufScraper {

        private final Map<String, Document> documentsByUrl;
        private final List<String> visitedUrls = new ArrayList<>();

        private TestStaticRufScraper(Map<String, Document> documentsByUrl) {
            Map<String, Document> parsedDocumentsByUrl = new LinkedHashMap<>();
            parsedDocumentsByUrl.putAll(documentsByUrl);
            this.documentsByUrl = new HashMap<>(parsedDocumentsByUrl);
        }

        @Override
        protected Document loadDocument(String url) {
            visitedUrls.add(url);
            Document document = documentsByUrl.get(url);
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


