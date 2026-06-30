package hu.szegedibibliaszol.scraper.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class VerseRecordTest {

    @Test
    void recordExposesVerseFields() {
        VerseRecord verseRecord = new VerseRecord("KAR", "Genesis", 1, 1, "Kezdetben teremté Isten az eget és a földet.");

        assertEquals("KAR", verseRecord.translation());
        assertEquals("Genesis", verseRecord.book());
        assertEquals(1, verseRecord.chapter());
        assertEquals(1, verseRecord.verse());
        assertEquals("Kezdetben teremté Isten az eget és a földet.", verseRecord.text());
    }

    @Test
    void recordRejectsNullRequiredFields() {
        NullPointerException translationException = assertThrows(
                NullPointerException.class,
                () -> new VerseRecord(null, "Genesis", 1, 1, "Kezdetben")
        );
        assertEquals("translation must not be null", translationException.getMessage());

        NullPointerException bookException = assertThrows(
                NullPointerException.class,
                () -> new VerseRecord("KAR", null, 1, 1, "Kezdetben")
        );
        assertEquals("book must not be null", bookException.getMessage());

        NullPointerException textException = assertThrows(
                NullPointerException.class,
                () -> new VerseRecord("KAR", "Genesis", 1, 1, null)
        );
        assertEquals("text must not be null", textException.getMessage());
    }

    @Test
    void recordRejectsNonPositiveChapterAndVerse() {
        IllegalArgumentException chapterException = assertThrows(
                IllegalArgumentException.class,
                () -> new VerseRecord("KAR", "Genesis", 0, 1, "Kezdetben")
        );
        assertEquals("chapter must be greater than 0", chapterException.getMessage());

        IllegalArgumentException verseException = assertThrows(
                IllegalArgumentException.class,
                () -> new VerseRecord("KAR", "Genesis", 1, 0, "Kezdetben")
        );
        assertEquals("verse must be greater than 0", verseException.getMessage());
    }
}
