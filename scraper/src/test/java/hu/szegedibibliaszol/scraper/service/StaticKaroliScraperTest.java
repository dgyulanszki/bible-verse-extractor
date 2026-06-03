package hu.szegedibibliaszol.scraper.service;

import hu.szegedibibliaszol.scraper.model.VerseRecord;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StaticKaroliScraperTest {

    @Test
    void scrapeTraversesBooksChaptersAndVerses() {
        TestStaticKaroliScraper scraper = new TestStaticKaroliScraper(Map.ofEntries(
                Map.entry("https://biblia.hit.hu/bible/karoli", document("""
                        <html><body>
                        <a href="https://biblia.hit.hu/bible/karoli/GEN">Mózes I. könyve</a>
                        <a href="/bible/karoli/GEN">Mózes I. könyve duplicate</a>
                        <a href="/bible/karoli/EXO">Mózes II. könyve</a>
                        <a href="/bible/kingjames/GEN">ignore other translation</a>
                        </body></html>
                        """)),
                Map.entry("https://biblia.hit.hu/bible/karoli/GEN", document("""
                        <html><body>
                        <a href="https://biblia.hit.hu/bible/karoli/GEN/1">1. fejezet</a>
                        <a href="/bible/karoli/GEN/1">1. fejezet duplicate</a>
                        <a href="/bible/karoli/GEN/2">2. fejezet</a>
                        </body></html>
                        """)),
                Map.entry("https://biblia.hit.hu/bible/karoli/EXO", document("""
                        <html><body>
                        <a href="/bible/karoli/EXO/1">1. fejezet</a>
                        </body></html>
                        """)),
                Map.entry("https://biblia.hit.hu/bible/karoli/GEN/1", document("""
                        <html><body>
                        <div class="bible-verses">
                          <table>
                            <tbody>
                              <tr id="#1"><td class="verse-num">1.</td><td class="verse-line">Kezdetben.</td></tr>
                              <tr id="#2"><td class="verse-num">2.</td><td class="verse-line">A föld pedig.</td></tr>
                            </tbody>
                          </table>
                        </div>
                        </body></html>
                        """)),
                Map.entry("https://biblia.hit.hu/bible/karoli/GEN/2", document("""
                        <html><body>
                        <div class="bible-verses">
                          <table>
                            <tbody>
                              <tr id="#1"><td class="verse-num">1.</td><td class="verse-line">Így készült el.</td></tr>
                            </tbody>
                          </table>
                        </div>
                        </body></html>
                        """)),
                Map.entry("https://biblia.hit.hu/bible/karoli/EXO/1", document("""
                        <html><body>
                        <div class="bible-verses">
                          <table>
                            <tbody>
                              <tr id="#1"><td class="verse-num">1.</td><td class="verse-line">Ezek pedig.</td></tr>
                            </tbody>
                          </table>
                        </div>
                        </body></html>
                        """))
        ));

        List<VerseRecord> verses = scraper.scrape();

        assertEquals(List.of(
                "https://biblia.hit.hu/bible/karoli",
                "https://biblia.hit.hu/bible/karoli/GEN",
                "https://biblia.hit.hu/bible/karoli/GEN/1",
                "https://biblia.hit.hu/bible/karoli/GEN/2",
                "https://biblia.hit.hu/bible/karoli/EXO",
                "https://biblia.hit.hu/bible/karoli/EXO/1"
        ), scraper.visitedUrls());
        assertEquals(List.of(
                new VerseRecord("Károli", "Mózes I. könyve", 1, 1, "Kezdetben."),
                new VerseRecord("Károli", "Mózes I. könyve", 1, 2, "A föld pedig."),
                new VerseRecord("Károli", "Mózes I. könyve", 2, 1, "Így készült el."),
                new VerseRecord("Károli", "Mózes II. könyve", 1, 1, "Ezek pedig.")
        ), verses);
    }

    @Test
    void scrapeFailsWhenVerseNumberIsMissing() {
        TestStaticKaroliScraper scraper = new TestStaticKaroliScraper(Map.of(
                "https://biblia.hit.hu/bible/karoli", document("<a href=\"/bible/karoli/GEN\">Mózes I. könyve</a>"),
                "https://biblia.hit.hu/bible/karoli/GEN", document("<a href=\"/bible/karoli/GEN/1\">1. fejezet</a>"),
                "https://biblia.hit.hu/bible/karoli/GEN/1", document("""
                        <div class="bible-verses"><table><tbody>
                        <tr><td class="verse-line">Kezdetben.</td></tr>
                        </tbody></table></div>
                        """)
        ));

        IllegalStateException exception = assertThrows(IllegalStateException.class, scraper::scrape);

        assertEquals("Missing verse number for Mózes I. könyve 1", exception.getMessage());
    }

    @Test
    void scrapeFailsWhenVerseTextIsMissing() {
        TestStaticKaroliScraper scraper = new TestStaticKaroliScraper(Map.of(
                "https://biblia.hit.hu/bible/karoli", document("<a href=\"/bible/karoli/GEN\">Mózes I. könyve</a>"),
                "https://biblia.hit.hu/bible/karoli/GEN", document("<a href=\"/bible/karoli/GEN/1\">1. fejezet</a>"),
                "https://biblia.hit.hu/bible/karoli/GEN/1", document("""
                        <div class="bible-verses"><table><tbody>
                        <tr><td class="verse-num">1.</td></tr>
                        </tbody></table></div>
                        """)
        ));

        IllegalStateException exception = assertThrows(IllegalStateException.class, scraper::scrape);

        assertEquals("Missing verse text for Mózes I. könyve 1:1", exception.getMessage());
    }

    @Test
    void scrapeFailsWhenVerseNumberCannotBeParsed() {
        TestStaticKaroliScraper scraper = new TestStaticKaroliScraper(Map.of(
                "https://biblia.hit.hu/bible/karoli", document("<a href=\"/bible/karoli/GEN\">Mózes I. könyve</a>"),
                "https://biblia.hit.hu/bible/karoli/GEN", document("<a href=\"/bible/karoli/GEN/1\">1. fejezet</a>"),
                "https://biblia.hit.hu/bible/karoli/GEN/1", document("""
                        <div class="bible-verses"><table><tbody>
                        <tr><td class="verse-num">X.</td><td class="verse-line">Kezdetben.</td></tr>
                        </tbody></table></div>
                        """)
        ));

        IllegalStateException exception = assertThrows(IllegalStateException.class, scraper::scrape);

        assertEquals("Could not parse verse number: X", exception.getMessage());
    }

    private static Document document(String html) {
        return Jsoup.parse(html, "https://biblia.hit.hu");
    }

    private static final class TestStaticKaroliScraper extends StaticKaroliScraper {

        private final Map<String, Document> documents;
        private final List<String> visitedUrls = new ArrayList<>();

        private TestStaticKaroliScraper(Map<String, Document> documents) {
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



