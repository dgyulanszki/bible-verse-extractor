package hu.szegedibibliaszol.app.ui.model;

public record VerseRow(
        String translation,
        String book,
        int chapter,
        int verse,
        String text
) {

    public String reference() {
        return book + " " + chapter + ":" + verse;
    }
}

