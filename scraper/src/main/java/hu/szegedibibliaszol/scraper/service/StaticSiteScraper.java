package hu.szegedibibliaszol.scraper.service;

import hu.szegedibibliaszol.scraper.model.VerseRecord;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StaticSiteScraper {

    private static final String BASE_URL = "https://www.online-biblia.ro";
    private static final String TRANSLATION = "Revideált Károli";
    private static final String ROOT_PATH = "/bible/4";
    private static final Pattern BOOK_LINK_PATTERN = Pattern.compile("^/bible/4/([A-Z0-9]+)$");

    private static final Logger log = LoggerFactory.getLogger(StaticSiteScraper.class);

    public List<VerseRecord> scrape() {
        String mainPageUrl = buildUrl(ROOT_PATH);
        Document mainPage = loadDocument(mainPageUrl);
        List<BookLink> books = extractBooks(mainPage);
        List<VerseRecord> verses = new ArrayList<>();

        log.info("Starting static scrape of {} books from {}", books.size(), mainPageUrl);
        for (BookLink book : books) {
            String bookPageUrl = buildUrl(ROOT_PATH + "/" + book.code());
            List<Integer> chapters = extractChapters(loadDocument(bookPageUrl), book.code());
            log.info("Scraping {} chapters from {}", chapters.size(), book.name());

            for (Integer chapterNumber : chapters) {
                String chapterPageUrl = buildUrl(ROOT_PATH + "/" + book.code() + "/" + chapterNumber);
                verses.addAll(extractVerses(loadDocument(chapterPageUrl), book.name(), chapterNumber));
            }
        }

        log.info("Static scraping collected {} verses.", verses.size());
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

    private List<BookLink> extractBooks(Document mainPage) {
        Map<String, String> booksByCode = new LinkedHashMap<>();

        for (Element link : mainPage.select("a[href]")) {
            Matcher matcher = BOOK_LINK_PATTERN.matcher(link.attr("href"));
            if (matcher.matches()) {
                String bookName = normalizeText(link.text());
                if (!bookName.isBlank()) {
                    booksByCode.putIfAbsent(matcher.group(1), bookName);
                }
            }
        }

        return booksByCode.entrySet().stream()
                .map(entry -> new BookLink(entry.getKey(), entry.getValue()))
                .toList();
    }

    private List<Integer> extractChapters(Document bookPage, String bookCode) {
        Pattern chapterLinkPattern = Pattern.compile("^/bible/4/" + Pattern.quote(bookCode) + "/(\\d+)$");
        Map<Integer, Integer> chaptersByNumber = new LinkedHashMap<>();

        for (Element link : bookPage.select("a[href]")) {
            Matcher matcher = chapterLinkPattern.matcher(link.attr("href"));
            if (matcher.matches()) {
                int chapterNumber = parseNumber(matcher.group(1), "chapter number");
                chaptersByNumber.putIfAbsent(chapterNumber, chapterNumber);
            }
        }

        return List.copyOf(chaptersByNumber.keySet());
    }

    private List<VerseRecord> extractVerses(Document chapterPage, String bookName, int chapterNumber) {
        Element chapterContent = chapterPage.selectFirst("dl.bible-chapter-content");
        if (chapterContent == null) {
            throw new IllegalStateException("Could not find verse container for " + bookName + " " + chapterNumber);
        }

        List<VerseRecord> verses = new ArrayList<>();
        for (Element verseNumberElement : chapterContent.select("dt")) {
            int verseNumber = parseNumber(normalizeText(verseNumberElement.text()), "verse number");
            Element verseTextElement = verseNumberElement.nextElementSibling();
            if (verseTextElement == null || !"dd".equals(verseTextElement.normalName())) {
                throw new IllegalStateException("Missing verse text for " + bookName + " " + chapterNumber + ":" + verseNumber);
            }

            String verseText = extractVerseText(verseTextElement, bookName, chapterNumber, verseNumber);
            verses.add(new VerseRecord(TRANSLATION, bookName, chapterNumber, verseNumber, verseText));
        }
        return verses;
    }

    private String extractVerseText(Element verseTextElement, String bookName, int chapterNumber, int verseNumber) {
        Element verseAnchor = verseTextElement.selectFirst("a.vers");
        String verseText = verseAnchor != null
                ? normalizeText(verseAnchor.text())
                : normalizeText(verseTextElement.text());

        if (verseText.isBlank()) {
            throw new IllegalStateException("Blank verse text for " + bookName + " " + chapterNumber + ":" + verseNumber);
        }
        return verseText;
    }

    private int parseNumber(String value, String label) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            throw new IllegalStateException("Could not parse " + label + ": " + value, ex);
        }
    }

    private String buildUrl(String path) {
        return BASE_URL + path;
    }

    private String normalizeText(String text) {
        return text.replace('\u00A0', ' ').trim().replaceAll("\\s+", " ");
    }

    private record BookLink(String code, String name) {
    }
}

