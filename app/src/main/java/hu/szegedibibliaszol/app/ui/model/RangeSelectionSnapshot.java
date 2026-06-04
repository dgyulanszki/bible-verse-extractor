package hu.szegedibibliaszol.app.ui.model;

public record RangeSelectionSnapshot(
        String book,
        Integer chapter,
        Integer fromVerse,
        Integer toVerse
) {
}

