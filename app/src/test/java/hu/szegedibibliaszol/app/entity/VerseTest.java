package hu.szegedibibliaszol.app.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

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
}

