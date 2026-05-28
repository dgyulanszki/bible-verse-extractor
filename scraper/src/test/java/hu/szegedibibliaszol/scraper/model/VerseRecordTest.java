package hu.szegedibibliaszol.scraper.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}

