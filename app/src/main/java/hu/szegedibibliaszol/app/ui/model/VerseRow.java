package hu.szegedibibliaszol.app.ui.model;

import java.util.Objects;

public record VerseRow(
        String translation,
        String book,
        int chapter,
        int verse,
        String text
) {

    public VerseRow {
        Objects.requireNonNull(translation, "translation must not be null");
        Objects.requireNonNull(book, "book must not be null");
        Objects.requireNonNull(text, "text must not be null");
        if (chapter <= 0) {
            throw new IllegalArgumentException("chapter must be greater than 0");
        }
        if (verse <= 0) {
            throw new IllegalArgumentException("verse must be greater than 0");
        }
    }

    public String reference() {
        return book + " " + chapter + ":" + verse;
    }
}

