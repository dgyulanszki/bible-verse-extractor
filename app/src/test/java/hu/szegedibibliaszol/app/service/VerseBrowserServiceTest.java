package hu.szegedibibliaszol.app.service;

import hu.szegedibibliaszol.app.entity.Verse;
import hu.szegedibibliaszol.app.repository.VersesRepository;
import hu.szegedibibliaszol.app.ui.model.VerseRow;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class VerseBrowserServiceTest {

    @Test
    void inMemoryServiceReturnsNoDataForAnyQueryByDefault() {
        VerseBrowserService verseBrowserService = new VerseBrowserService();

        assertTrue(verseBrowserService.getTranslations().isEmpty());
        assertTrue(verseBrowserService.getBooks("KJV").isEmpty());
        assertTrue(verseBrowserService.getChapters("KJV", "John").isEmpty());
        assertTrue(verseBrowserService.getVerses("KJV", "John", 3).isEmpty());
        assertEquals(List.<VerseRow>of(), verseBrowserService.findVerses("KJV", "John", 3, 16));
    }

    @Test
    void inMemoryQueriesReturnDistinctFilteredData() {
        VerseBrowserService verseBrowserService = new VerseBrowserService(List.of(
                new VerseRow("KJV", "John", 3, 16, "For God so loved the world"),
                new VerseRow("KJV", "John", 3, 17, "For God did not send his Son"),
                new VerseRow("KJV", "Romans", 8, 1, "There is therefore now no condemnation"),
                new VerseRow("NIV", "John", 3, 16, "For God so loved the world")
        ));

        assertEquals(List.of("KJV", "NIV"), verseBrowserService.getTranslations());
        assertEquals(List.of("John", "Romans"), verseBrowserService.getBooks("KJV"));
        assertEquals(List.of(3), verseBrowserService.getChapters("KJV", "John"));
        assertEquals(List.of(16, 17), verseBrowserService.getVerses("KJV", "John", 3));
        assertEquals(List.of(
                new VerseRow("KJV", "John", 3, 16, "For God so loved the world"),
                new VerseRow("KJV", "John", 3, 17, "For God did not send his Son")
        ), verseBrowserService.findVerses("KJV", "John", 3, null));
        assertEquals(List.of(
                new VerseRow("KJV", "John", 3, 16, "For God so loved the world")
        ), verseBrowserService.findVerses("KJV", "John", 3, 16));
    }

    @Test
    void inMemoryQueriesReturnEmptyListsWhenSelectionIsIncompleteOrUnknown() {
        VerseBrowserService verseBrowserService = new VerseBrowserService(List.of(
                new VerseRow("KJV", "John", 3, 16, "For God so loved the world")
        ));

        assertEquals(List.of(), verseBrowserService.getBooks(null));
        assertEquals(List.of(), verseBrowserService.getChapters("KJV", null));
        assertEquals(List.of(), verseBrowserService.getVerses("KJV", "John", null));
        assertEquals(List.of(), verseBrowserService.findVerses(null, "John", 3, 16));
        assertEquals(List.of(), verseBrowserService.findVerses("KJV", null, 3, 16));
        assertEquals(List.of(), verseBrowserService.findVerses("KJV", "John", null, 16));
        assertEquals(List.of(), verseBrowserService.getBooks("NRSV"));
        assertEquals(List.of(), verseBrowserService.getChapters("KJV", "Romans"));
        assertEquals(List.of(), verseBrowserService.getVerses("KJV", "John", 99));
        assertEquals(List.of(), verseBrowserService.findVerses("KJV", "John", 3, 99));
    }

    @Test
    void inMemoryConstructorCopiesProvidedRows() {
        List<VerseRow> sourceRows = new java.util.ArrayList<>(List.of(
                new VerseRow("KJV", "John", 3, 16, "For God so loved the world")
        ));
        VerseBrowserService verseBrowserService = new VerseBrowserService(sourceRows);

        sourceRows.clear();

        assertEquals(List.of("KJV"), verseBrowserService.getTranslations());
    }

    @Test
    void databaseBackedQueriesReadDistinctFilteredData() throws Exception {
        Path databasePath = Files.createTempFile("verse-browser-service", ".db");
        VersesRepository versesRepository = mock(VersesRepository.class);
        when(versesRepository.findTranslations()).thenReturn(List.of("Revideált Károli", "Károli Gáspár Fordítás"));
        when(versesRepository.findBooksByTranslation("Revideált Károli")).thenReturn(List.of("1. Mózes", "2. Mózes"));
        when(versesRepository.findChaptersByTranslationAndBook("Revideált Károli", "1. Mózes")).thenReturn(List.of(1));
        when(versesRepository.findVersesByTranslationAndBookAndChapter("Revideált Károli", "1. Mózes", 1)).thenReturn(List.of(1, 2));
        when(versesRepository.findByTranslationAndBookAndChapterOrderByVerseAsc("Revideált Károli", "1. Mózes", 1)).thenReturn(List.of(
                new Verse("Revideált Károli", "1. Mózes", 1, 1, "Kezdetben teremtette Isten az eget és a földet."),
                new Verse("Revideált Károli", "1. Mózes", 1, 2, "A föld pedig kietlen és puszta volt.")
        ));
        when(versesRepository.findByTranslationAndBookAndChapterAndVerseOrderByVerseAsc("Revideált Károli", "1. Mózes", 1, 2)).thenReturn(List.of(
                new Verse("Revideált Károli", "1. Mózes", 1, 2, "A föld pedig kietlen és puszta volt.")
        ));

        VerseBrowserService verseBrowserService = new VerseBrowserService(versesRepository, databasePath);

        assertEquals(List.of("Revideált Károli", "Károli Gáspár Fordítás"), verseBrowserService.getTranslations());
        assertEquals(List.of("1. Mózes", "2. Mózes"), verseBrowserService.getBooks("Revideált Károli"));
        assertEquals(List.of(1), verseBrowserService.getChapters("Revideált Károli", "1. Mózes"));
        assertEquals(List.of(1, 2), verseBrowserService.getVerses("Revideált Károli", "1. Mózes", 1));
        assertEquals(List.of(
                new VerseRow("Revideált Károli", "1. Mózes", 1, 1, "Kezdetben teremtette Isten az eget és a földet."),
                new VerseRow("Revideált Károli", "1. Mózes", 1, 2, "A föld pedig kietlen és puszta volt.")
        ), verseBrowserService.findVerses("Revideált Károli", "1. Mózes", 1, null));
        assertEquals(List.of(
                new VerseRow("Revideált Károli", "1. Mózes", 1, 2, "A föld pedig kietlen és puszta volt.")
        ), verseBrowserService.findVerses("Revideált Károli", "1. Mózes", 1, 2));
    }

    @Test
    void databaseBackedQueriesReturnEmptyListsWhenDatabaseFileIsMissing() {
        VerseBrowserService verseBrowserService = new VerseBrowserService(
                mock(VersesRepository.class),
                Path.of("target/non-existent-test-db.db")
        );

        assertEquals(List.of(), verseBrowserService.getTranslations());
        assertEquals(List.of(), verseBrowserService.getBooks("Revideált Károli"));
        assertEquals(List.of(), verseBrowserService.getChapters("Revideált Károli", "1. Mózes"));
        assertEquals(List.of(), verseBrowserService.getVerses("Revideált Károli", "1. Mózes", 1));
        assertEquals(List.of(), verseBrowserService.findVerses("Revideált Károli", "1. Mózes", 1, null));
    }

    @Test
    void databaseBackedQueriesReturnEmptyListsWhenVersesTableIsMissing() throws Exception {
        Path databasePath = Files.createTempFile("verse-browser-service-missing-table", ".db");
        VersesRepository versesRepository = mock(VersesRepository.class);
        when(versesRepository.findTranslations()).thenThrow(new RuntimeException("no such table: verses"));
        when(versesRepository.findBooksByTranslation("Revideált Károli")).thenThrow(new RuntimeException("no such table: verses"));
        when(versesRepository.findChaptersByTranslationAndBook("Revideált Károli", "1. Mózes")).thenThrow(new RuntimeException("no such table: verses"));
        when(versesRepository.findVersesByTranslationAndBookAndChapter("Revideált Károli", "1. Mózes", 1)).thenThrow(new RuntimeException("no such table: verses"));
        when(versesRepository.findByTranslationAndBookAndChapterOrderByVerseAsc("Revideált Károli", "1. Mózes", 1)).thenThrow(new RuntimeException("no such table: verses"));
        VerseBrowserService verseBrowserService = new VerseBrowserService(versesRepository, databasePath);

        assertEquals(List.of(), verseBrowserService.getTranslations());
        assertEquals(List.of(), verseBrowserService.getBooks("Revideált Károli"));
        assertEquals(List.of(), verseBrowserService.getChapters("Revideált Károli", "1. Mózes"));
        assertEquals(List.of(), verseBrowserService.getVerses("Revideált Károli", "1. Mózes", 1));
        assertEquals(List.of(), verseBrowserService.findVerses("Revideált Károli", "1. Mózes", 1, null));
    }

    @Test
    void databaseBackedQueriesWrapUnexpectedSqlFailures() throws Exception {
        Path databasePath = Files.createTempFile("verse-browser-service-broken-connection", ".db");
        VersesRepository versesRepository = mock(VersesRepository.class);
        when(versesRepository.findTranslations()).thenThrow(new RuntimeException("broken repository"));

        VerseBrowserService verseBrowserService = new VerseBrowserService(versesRepository, databasePath);

        IllegalStateException exception = assertThrows(IllegalStateException.class, verseBrowserService::getTranslations);

        assertEquals("Could not read verses from SQLite database.", exception.getMessage());
        assertEquals("broken repository", exception.getCause().getMessage());
    }
}
