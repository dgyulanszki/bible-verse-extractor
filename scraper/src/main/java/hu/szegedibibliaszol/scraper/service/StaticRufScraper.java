package hu.szegedibibliaszol.scraper.service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class StaticRufScraper extends AbstractStaticSiteScraper {

    private static final String ID = "revidealt-uj-forditas";
    private static final String BASE_URL = "https://abibliamindenkie.hu";
    private static final String TRANSLATION = "Revideált Új Fordítás (RÚF)";
    private static final String ROOT_PATH = "/uj";
    private static final Pattern BOOK_LINK_PATTERN = Pattern.compile("^/uj/([A-Z0-9]+)$");
    private static final Pattern VERSE_NUMBER_SUFFIX_PATTERN = Pattern.compile(".*?(\\d+)$");

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
        return Pattern.compile("^/uj/" + Pattern.quote(bookCode) + "/(\\d+)$");
    }

    @Override
    protected Element findVerseContainer(Document chapterPage) {
        return chapterPage.selectFirst("div.chapter__body");
    }

    @Override
    protected List<VerseTextLocation> locateVerseRows(Element chapterContent, String bookName, int chapterNumber) {
        List<VerseTextLocation> verseRows = new ArrayList<>();
        for (Element verseRow : chapterContent.children()) {
            if (!verseRow.is("p.verse")) {
                continue;
            }
            Element verseNumberElement = verseRow.selectFirst("a.verse__number");
            if (verseNumberElement == null) {
                throw new IllegalStateException("Missing verse number for " + bookName + " " + chapterNumber);
            }
            verseRows.add(new VerseTextLocation(verseNumberElement, verseRow));
        }
        return List.copyOf(verseRows);
    }

    @Override
    protected String extractVerseText(Element verseTextElement, String bookName, int chapterNumber, int verseNumber) {
        Element verseContent = verseTextElement.clone();
        verseContent.select("a.verse__number, span").remove();
        String verseText = ScraperTextSupport.normalizeText(verseContent.text());
        if (verseText.isBlank()) {
            throw new IllegalStateException("Blank verse text for " + bookName + " " + chapterNumber + ":" + verseNumber);
        }
        return verseText;
    }

    @Override
    protected int extractVerseNumber(Element verseNumberElement, String bookName, int chapterNumber) {
        String verseNumberText = extractVerseNumberCandidate(verseNumberElement.text());
        if (verseNumberText.isBlank()) {
            verseNumberText = extractVerseNumberCandidate(verseNumberElement.id());
        }
        if (verseNumberText.isBlank()) {
            verseNumberText = extractVerseNumberCandidate(verseNumberElement.attr("href"));
        }
        return parseNumber(verseNumberText, "verse number");
    }

    private String extractVerseNumberCandidate(String rawValue) {
        String normalizedValue = ScraperTextSupport.normalizeText(rawValue);
        if (normalizedValue.isBlank()) {
            return normalizedValue;
        }

        Matcher matcher = VERSE_NUMBER_SUFFIX_PATTERN.matcher(normalizedValue);
        if (matcher.matches()) {
            return matcher.group(1);
        }

        return normalizedValue;
    }
}

