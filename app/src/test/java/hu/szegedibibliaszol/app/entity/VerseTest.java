package hu.szegedibibliaszol.app.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class VerseTest {

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

