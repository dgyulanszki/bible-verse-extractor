package hu.szegedibibliaszol.scraper.model;

public record VerseRecord(
        String translation,
        String book,
        int chapter,
        int verse,
        String text
) {
}

