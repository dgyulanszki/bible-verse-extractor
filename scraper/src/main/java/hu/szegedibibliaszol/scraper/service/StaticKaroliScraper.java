package hu.szegedibibliaszol.scraper.service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class StaticKaroliScraper extends AbstractStaticSiteScraper {

    private static final String ID = "karoli-gaspar";
    private static final String BASE_URL = "https://biblia.hit.hu";
    private static final String TRANSLATION = "Károli";
    private static final String ROOT_PATH = "/bible/karoli";
    private static final Pattern BOOK_LINK_PATTERN = Pattern.compile("^/bible/karoli/([A-Z0-9]+)$");

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String translation() {
        return TRANSLATION;
    }

    @Override
    protected String baseUrl() {
        return BASE_URL;
    }

    @Override
    protected String rootPath() {
        return ROOT_PATH;
    }

    @Override
    protected Pattern bookLinkPattern() {
        return BOOK_LINK_PATTERN;
    }

    @Override
    protected Pattern chapterLinkPattern(String bookCode) {
        return Pattern.compile("^/bible/karoli/" + Pattern.quote(bookCode) + "/(\\d+)$");
    }

    @Override
    protected Element findVerseContainer(Document chapterPage) {
        return chapterPage.selectFirst("div.bible-verses tbody");
    }

    @Override
    protected List<VerseTextLocation> locateVerseRows(Element chapterContent, String bookName, int chapterNumber) {
        List<VerseTextLocation> verseRows = new ArrayList<>();
        for (Element verseRow : chapterContent.select("tr")) {
            Element verseNumberElement = verseRow.selectFirst("td.verse-num");
            if (verseNumberElement == null) {
                throw new IllegalStateException("Missing verse number for " + bookName + " " + chapterNumber);
            }
            Element verseTextElement = verseRow.selectFirst("td.verse-line");
            int verseNumber = extractVerseNumber(verseNumberElement, bookName, chapterNumber);
            if (verseTextElement == null) {
                throw new IllegalStateException("Missing verse text for " + bookName + " " + chapterNumber + ":" + verseNumber);
            }
            verseRows.add(new VerseTextLocation(verseNumberElement, verseTextElement));
        }
        return List.copyOf(verseRows);
    }

    @Override
    protected int extractVerseNumber(Element verseNumberElement, String bookName, int chapterNumber) {
        String verseNumberText = ScraperTextSupport.normalizeText(verseNumberElement.text());
        if (verseNumberText.endsWith(".")) {
            verseNumberText = verseNumberText.substring(0, verseNumberText.length() - 1);
        }
        return parseNumber(verseNumberText, "verse number");
    }
}



