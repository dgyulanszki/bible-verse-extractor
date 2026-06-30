package hu.szegedibibliaszol.app.ui.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class VerseRowTest {

    @Test
    void referenceCombinesBookChapterAndVerse() {
        VerseRow verseRow = new VerseRow("KJV", "John", 3, 16, "For God so loved the world");

        assertEquals("KJV", verseRow.translation());
        assertEquals("John 3:16", verseRow.reference());
        assertEquals("For God so loved the world", verseRow.text());
    }

    @Test
    void constructorRejectsNullRequiredFields() {
        NullPointerException translationException = assertThrows(
                NullPointerException.class,
                () -> new VerseRow(null, "John", 3, 16, "For God so loved the world")
        );
        assertEquals("translation must not be null", translationException.getMessage());

        NullPointerException bookException = assertThrows(
                NullPointerException.class,
                () -> new VerseRow("KJV", null, 3, 16, "For God so loved the world")
        );
        assertEquals("book must not be null", bookException.getMessage());

        NullPointerException textException = assertThrows(
                NullPointerException.class,
                () -> new VerseRow("KJV", "John", 3, 16, null)
        );
        assertEquals("text must not be null", textException.getMessage());
    }

    @Test
    void constructorRejectsNonPositiveChapterAndVerse() {
        IllegalArgumentException chapterException = assertThrows(
                IllegalArgumentException.class,
                () -> new VerseRow("KJV", "John", 0, 16, "For God so loved the world")
        );
        assertEquals("chapter must be greater than 0", chapterException.getMessage());

        IllegalArgumentException verseException = assertThrows(
                IllegalArgumentException.class,
                () -> new VerseRow("KJV", "John", 3, 0, "For God so loved the world")
        );
        assertEquals("verse must be greater than 0", verseException.getMessage());
    }
}
