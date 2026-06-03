package hu.szegedibibliaszol.scraper.service;

import hu.szegedibibliaszol.scraper.model.VerseRecord;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractStaticSiteScraper {

    private static final Logger log = LoggerFactory.getLogger(AbstractStaticSiteScraper.class);

    public abstract String id();

    public abstract String translation();

    public List<VerseRecord> scrape() {
        String mainPageUrl = buildUrl(rootPath());
        Document mainPage = loadDocument(mainPageUrl);
        List<BookLink> books = extractBooks(mainPage);
        List<VerseRecord> verses = new ArrayList<>();

        log.info("Starting static scrape of translation '{}' with {} books from {}", translation(), books.size(), mainPageUrl);
        for (BookLink book : books) {
            String bookPageUrl = buildUrl(bookPath(book.code()));
            List<Integer> chapters = extractChapters(loadDocument(bookPageUrl), book.code());
            log.info("Scraping {} chapters from {} ({})", chapters.size(), book.name(), translation());

            for (Integer chapterNumber : chapters) {
                String chapterPageUrl = buildUrl(chapterPath(book.code(), chapterNumber));
                verses.addAll(extractVerses(loadDocument(chapterPageUrl), book.name(), chapterNumber));
            }
        }

        log.info("Static scraping collected {} verses for {}.", verses.size(), translation());
        return List.copyOf(verses);
    }

    protected Document loadDocument(String url) {
        try {
            return readDocument(url);
        } catch (IOException ex) {
            throw new IllegalStateException("Could not load static page: " + url, ex);
        }
    }

    protected Document readDocument(String url) throws IOException {
        Connection.Response response = Jsoup.connect(url)
                .userAgent("Mozilla/5.0")
                .timeout(30_000)
                .execute();

        try (InputStream inputStream = response.bodyStream()) {
            return Jsoup.parse(inputStream, StandardCharsets.UTF_8.name(), url);
        }
    }

    protected List<BookLink> extractBooks(Document mainPage) {
        Map<String, String> booksByCode = new LinkedHashMap<>();

        for (Element link : mainPage.select("a[href]")) {
            String bookCode = extractBookCode(link.attr("href"));
            if (bookCode != null) {
                String bookName = extractBookName(link);
                booksByCode.putIfAbsent(bookCode, bookName);
            }
        }

        return booksByCode.entrySet().stream()
                .map(entry -> new BookLink(entry.getKey(), entry.getValue()))
                .toList();
    }

    protected List<Integer> extractChapters(Document bookPage, String bookCode) {
        Map<Integer, Integer> chaptersByNumber = new LinkedHashMap<>();

        for (Element link : bookPage.select("a[href]")) {
            Integer chapterNumber = extractChapterNumber(link.attr("href"), link, bookCode);
            if (chapterNumber != null) {
                chaptersByNumber.putIfAbsent(chapterNumber, chapterNumber);
            }
        }

        return List.copyOf(chaptersByNumber.keySet());
    }

    protected List<VerseRecord> extractVerses(Document chapterPage, String bookName, int chapterNumber) {
        Element chapterContent = findVerseContainer(chapterPage);
        if (chapterContent == null) {
            throw new IllegalStateException("Could not find verse container for " + bookName + " " + chapterNumber);
        }

        List<VerseRecord> verses = new ArrayList<>();
        for (VerseTextLocation verseTextLocation : locateVerseRows(chapterContent, bookName, chapterNumber)) {
            int verseNumber = extractVerseNumber(verseTextLocation.numberElement(), bookName, chapterNumber);
            String verseText = extractVerseText(verseTextLocation.textElement(), bookName, chapterNumber, verseNumber);
            verses.add(new VerseRecord(translation(), bookName, chapterNumber, verseNumber, verseText));
        }
        return verses;
    }

    protected abstract String baseUrl();

    protected abstract String rootPath();

    protected abstract Pattern bookLinkPattern();

    protected abstract Pattern chapterLinkPattern(String bookCode);

    protected String bookPath(String bookCode) {
        return rootPath() + "/" + bookCode;
    }

    protected String chapterPath(String bookCode, int chapterNumber) {
        return bookPath(bookCode) + "/" + chapterNumber;
    }

    protected String extractBookCode(String href) {
        String normalizedHref = normalizeHref(href);
        Matcher matcher = bookLinkPattern().matcher(normalizedHref);
        if (!matcher.matches()) {
            return null;
        }
        return matcher.group(1);
    }

    protected String extractBookName(Element link) {
        String bookName = normalizeText(link.text());
        if (bookName.isBlank()) {
            throw new IllegalStateException("Blank book name for link: " + link.attr("href"));
        }
        return bookName;
    }

    protected Integer extractChapterNumber(String href, Element link, String bookCode) {
        String normalizedHref = normalizeHref(href);
        Matcher matcher = chapterLinkPattern(bookCode).matcher(normalizedHref);
        if (!matcher.matches()) {
            return null;
        }
        try {
            return parseNumber(matcher.group(1), "chapter number");
        } catch (IllegalStateException ex) {
            return null;
        }
    }

    protected Element findVerseContainer(Document chapterPage) {
        return chapterPage.selectFirst("dl.bible-chapter-content");
    }

    protected List<VerseTextLocation> locateVerseRows(Element chapterContent, String bookName, int chapterNumber) {
        List<VerseTextLocation> verseRows = new ArrayList<>();
        for (Element verseNumberElement : chapterContent.select("dt")) {
            Element verseTextElement = verseNumberElement.nextElementSibling();
            int verseNumber = extractVerseNumber(verseNumberElement, bookName, chapterNumber);
            if (verseTextElement == null || !"dd".equals(verseTextElement.normalName())) {
                throw new IllegalStateException("Missing verse text for " + bookName + " " + chapterNumber + ":" + verseNumber);
            }
            verseRows.add(new VerseTextLocation(verseNumberElement, verseTextElement));
        }
        return List.copyOf(verseRows);
    }

    protected int extractVerseNumber(Element verseNumberElement, String bookName, int chapterNumber) {
        return parseNumber(normalizeText(verseNumberElement.text()), "verse number");
    }

    protected String extractVerseText(Element verseTextElement, String bookName, int chapterNumber, int verseNumber) {
        Element verseAnchor = verseTextElement.selectFirst("a.vers");
        String verseText = verseAnchor != null
                ? normalizeText(verseAnchor.text())
                : normalizeText(verseTextElement.text());

        if (verseText.isBlank()) {
            throw new IllegalStateException("Blank verse text for " + bookName + " " + chapterNumber + ":" + verseNumber);
        }
        return verseText;
    }

    protected int parseNumber(String value, String label) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            throw new IllegalStateException("Could not parse " + label + ": " + value, ex);
        }
    }

    protected String buildUrl(String path) {
        return baseUrl() + path;
    }

    protected String normalizeHref(String href) {
        Objects.requireNonNull(href, "href must not be null");
        String trimmedHref = href.trim();
        if (trimmedHref.startsWith(baseUrl())) {
            return trimmedHref.substring(baseUrl().length());
        }
        return trimmedHref;
    }

    protected String normalizeText(String text) {
        return text.replace('\u00A0', ' ').trim().replaceAll("\\s+", " ");
    }

    protected record BookLink(String code, String name) {
    }

    protected record VerseTextLocation(Element numberElement, Element textElement) {
    }
}

