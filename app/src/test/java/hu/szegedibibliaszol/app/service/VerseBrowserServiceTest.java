package hu.szegedibibliaszol.app.service;

import hu.szegedibibliaszol.app.ui.model.VerseRow;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VerseBrowserServiceTest {

    @Test
    void emptyServiceReturnsNoDataForAnyQuery() {
        VerseBrowserService verseBrowserService = new VerseBrowserService();

        assertTrue(verseBrowserService.getTranslations().isEmpty());
        assertTrue(verseBrowserService.getBooks("KJV").isEmpty());
        assertTrue(verseBrowserService.getChapters("KJV", "John").isEmpty());
        assertTrue(verseBrowserService.getVerses("KJV", "John", 3).isEmpty());
        assertEquals(List.<VerseRow>of(), verseBrowserService.findVerses("KJV", "John", 3, 16));
    }
}

