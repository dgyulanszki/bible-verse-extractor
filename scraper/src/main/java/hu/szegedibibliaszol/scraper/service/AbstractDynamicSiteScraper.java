package hu.szegedibibliaszol.scraper.service;

import hu.szegedibibliaszol.scraper.model.VerseRecord;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractDynamicSiteScraper {

    private static final Logger log = LoggerFactory.getLogger(AbstractDynamicSiteScraper.class);

    public abstract String id();

    public abstract String translation();

    public List<VerseRecord> scrape() {
        List<VerseRecord> verses = new ArrayList<>();
        Set<String> visitedChapterUrls = new LinkedHashSet<>();
        String currentChapterUrl = startUrl();

        log.info("Starting dynamic scrape of translation '{}' from {}", translation(), currentChapterUrl);
        while (currentChapterUrl != null) {
            if (!visitedChapterUrls.add(currentChapterUrl)) {
                throw new IllegalStateException(
                        "Detected navigation loop while scraping " + translation() + " at " + currentChapterUrl
                );
            }

            Document chapterPage = loadDocument(currentChapterUrl);
            ensureTranslationSelected(chapterPage, currentChapterUrl);
            ChapterPage parsedChapter = parseChapterPage(chapterPage, currentChapterUrl);
            verses.addAll(parsedChapter.verses());
            currentChapterUrl = parsedChapter.nextChapterUrl();
        }

        log.info(
                "Dynamic scraping collected {} verses for {} across {} chapters.",
                verses.size(),
                translation(),
                visitedChapterUrls.size()
        );
        return List.copyOf(verses);
    }

    protected abstract String startUrl();

    protected abstract String translationButtonText();

    protected Document loadDocument(String url) {
        try {
            return readDocument(url);
        } catch (IOException ex) {
            throw new IllegalStateException("Could not load dynamic page: " + url, ex);
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

    protected ChapterPage parseChapterPage(Document chapterPage, String currentChapterUrl) {
        String bookName = extractBookName(chapterPage, currentChapterUrl);
        Element chapterContent = findVerseContainer(chapterPage, currentChapterUrl);
        List<VerseRecord> verses = extractVerses(chapterContent, bookName, currentChapterUrl);
        if (verses.isEmpty()) {
            throw new IllegalStateException("Could not find non-blank verses for " + currentChapterUrl);
        }

        return new ChapterPage(verses, extractNextChapterUrl(chapterPage));
    }

    protected void ensureTranslationSelected(Document chapterPage, String currentChapterUrl) {
        for (Element versionButtonLabel : chapterPage.select("button div")) {
            if (translationButtonText().equals(normalizeText(versionButtonLabel.text()))) {
                return;
            }
        }

        throw new IllegalStateException(
                "Expected translation dropdown to show " + translationButtonText() + " for " + currentChapterUrl
        );
    }

    protected String extractBookName(Document chapterPage, String currentChapterUrl) {
        Element heading = chapterPage.selectFirst("h1");
        if (heading == null) {
            throw new IllegalStateException("Could not find chapter heading for " + currentChapterUrl);
        }

        String bookName = normalizeText(heading.text()).replaceFirst("\\s+\\d+$", "");
        if (bookName.isBlank()) {
            throw new IllegalStateException("Could not determine book name from heading for " + currentChapterUrl);
        }
        return bookName;
    }

    protected Element findVerseContainer(Document chapterPage, String currentChapterUrl) {
        Element chapterContent = chapterPage.selectFirst("div[data-testid=chapter-content]");
        if (chapterContent == null) {
            throw new IllegalStateException("Could not find verse container for " + currentChapterUrl);
        }
        return chapterContent;
    }

    protected List<VerseRecord> extractVerses(Element chapterContent, String bookName, String currentChapterUrl) {
        Map<UsfmReference, StringBuilder> verseTextsByReference = new LinkedHashMap<>();

        for (Element verseElement : chapterContent.select("span[data-usfm]")) {
            if (!hasClassEnding(verseElement, "__verse")) {
                continue;
            }

            UsfmReference reference = parseReference(verseElement.attr("data-usfm"), currentChapterUrl);
            String verseTextPart = extractVerseTextPart(verseElement);
            if (verseTextPart.isBlank()) {
                continue;
            }

            StringBuilder verseText = verseTextsByReference.computeIfAbsent(reference, ignored -> new StringBuilder());
            if (!verseText.isEmpty()) {
                verseText.append(' ');
            }
            verseText.append(verseTextPart);
        }

        List<VerseRecord> verses = new ArrayList<>();
        for (Map.Entry<UsfmReference, StringBuilder> verseEntry : verseTextsByReference.entrySet()) {
            UsfmReference reference = verseEntry.getKey();
            verses.add(new VerseRecord(
                    translation(),
                    bookName,
                    reference.chapter(),
                    reference.verse(),
                    normalizeText(verseEntry.getValue().toString())
            ));
        }
        return List.copyOf(verses);
    }

    protected UsfmReference parseReference(String dataUsfm, String currentChapterUrl) {
        String normalizedReference = dataUsfm.split("\\+")[0];
        String[] referenceParts = normalizedReference.split("\\.");
        if (referenceParts.length != 3) {
            throw new IllegalStateException("Unexpected verse reference '" + dataUsfm + "' for " + currentChapterUrl);
        }

        try {
            return new UsfmReference(referenceParts[0], Integer.parseInt(referenceParts[1]), Integer.parseInt(referenceParts[2]));
        } catch (NumberFormatException ex) {
            throw new IllegalStateException("Unexpected verse reference '" + dataUsfm + "' for " + currentChapterUrl, ex);
        }
    }

    protected String extractVerseTextPart(Element verseElement) {
        List<String> textParts = new ArrayList<>();

        for (Element child : verseElement.children()) {
            if (!hasClassEnding(child, "__content")) {
                continue;
            }

            String textPart = normalizeText(child.text());
            if (!textPart.isBlank()) {
                textParts.add(textPart);
            }
        }

        return normalizeText(String.join(" ", textParts));
    }

    protected String extractNextChapterUrl(Document chapterPage) {
        for (Element link : chapterPage.select("a[href]")) {
            for (Element title : link.select("title")) {
                if ("Next Chapter".equals(normalizeText(title.text()))) {
                    return link.absUrl("href");
                }
            }
        }
        return null;
    }

    protected boolean hasClassEnding(Element element, String suffix) {
        for (String className : element.classNames()) {
            if (className.endsWith(suffix)) {
                return true;
            }
        }
        return false;
    }

    protected String normalizeText(String text) {
        return text.replace('\u00A0', ' ').trim().replaceAll("\\s+", " ");
    }

    protected record ChapterPage(List<VerseRecord> verses, String nextChapterUrl) {
    }

    protected record UsfmReference(String bookCode, int chapter, int verse) {
    }
}

