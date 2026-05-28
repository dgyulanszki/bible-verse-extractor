package hu.szegedibibliaszol.app.ui.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VerseRowTest {

    @Test
    void referenceCombinesBookChapterAndVerse() {
        VerseRow verseRow = new VerseRow("KJV", "John", 3, 16, "For God so loved the world");

        assertEquals("KJV", verseRow.translation());
        assertEquals("John 3:16", verseRow.reference());
        assertEquals("For God so loved the world", verseRow.text());
    }
}

