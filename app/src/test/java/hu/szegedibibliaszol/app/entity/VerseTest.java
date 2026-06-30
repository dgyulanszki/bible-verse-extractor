package hu.szegedibibliaszol.app.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class VerseTest {

    @Test
    void defaultConstructorCreatesEmptyEntity() {
        Verse verse = new Verse();

        assertNull(verse.getId());
        assertNull(verse.getTranslation());
        assertNull(verse.getBook());
        assertEquals(0, verse.getChapter());
        assertEquals(0, verse.getVerse());
        assertNull(verse.getText());
    }

    @Test
    void verseExposesConfiguredFields() {
        Verse verse = new Verse("Revideált Károli", "1. Mózes", 1, 1, "Kezdetben teremtette Isten az eget és a földet.");

        assertNull(verse.getId());
        assertEquals("Revideált Károli", verse.getTranslation());
        assertEquals("1. Mózes", verse.getBook());
        assertEquals(1, verse.getChapter());
        assertEquals(1, verse.getVerse());
        assertEquals("Kezdetben teremtette Isten az eget és a földet.", verse.getText());
    }

    @Test
    void constructorRejectsNullRequiredFields() {
        NullPointerException translationException = assertThrows(
                NullPointerException.class,
                () -> new Verse(null, "1. Mózes", 1, 1, "Kezdetben")
        );
        assertEquals("translation must not be null", translationException.getMessage());

        NullPointerException bookException = assertThrows(
                NullPointerException.class,
                () -> new Verse("Revideált Károli", null, 1, 1, "Kezdetben")
        );
        assertEquals("book must not be null", bookException.getMessage());

        NullPointerException textException = assertThrows(
                NullPointerException.class,
                () -> new Verse("Revideált Károli", "1. Mózes", 1, 1, null)
        );
        assertEquals("text must not be null", textException.getMessage());
    }

    @Test
    void constructorRejectsNonPositiveChapterAndVerse() {
        IllegalArgumentException chapterException = assertThrows(
                IllegalArgumentException.class,
                () -> new Verse("Revideált Károli", "1. Mózes", 0, 1, "Kezdetben")
        );
        assertEquals("chapter must be greater than 0", chapterException.getMessage());

        IllegalArgumentException verseException = assertThrows(
                IllegalArgumentException.class,
                () -> new Verse("Revideált Károli", "1. Mózes", 1, 0, "Kezdetben")
        );
        assertEquals("verse must be greater than 0", verseException.getMessage());
    }
}
