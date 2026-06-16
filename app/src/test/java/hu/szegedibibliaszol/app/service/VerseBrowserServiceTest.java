package hu.szegedibibliaszol.app.service;

import hu.szegedibibliaszol.app.ui.model.VerseRow;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class VerseBrowserServiceTest {

    @Test
    void inMemoryServiceReturnsNoDataForAnyQueryByDefault() {
        VerseBrowserService verseBrowserService = new VerseBrowserService();

        assertTrue(verseBrowserService.getTranslations().isEmpty());
        assertTrue(verseBrowserService.getBooks("KJV").isEmpty());
        assertTrue(verseBrowserService.getChapters("KJV", "John").isEmpty());
        assertTrue(verseBrowserService.getVerses("KJV", "John", 3).isEmpty());
        assertEquals(List.<VerseRow>of(), verseBrowserService.findVerses("KJV", "John", 3, 16));
        assertEquals(List.<VerseRow>of(), verseBrowserService.findVerseRange("KJV", "John", 3, 16, 17));
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
                new VerseRow("KJV", "John", 3, 16, "For God so loved the world"),
                new VerseRow("KJV", "John", 3, 17, "For God did not send his Son")
        ), verseBrowserService.findVerseRange("KJV", "John", 3, 16, 17));
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
        assertEquals(List.of(), verseBrowserService.findVerseRange(null, "John", 3, 16, 17));
        assertEquals(List.of(), verseBrowserService.findVerseRange("KJV", null, 3, 16, 17));
        assertEquals(List.of(), verseBrowserService.findVerseRange("KJV", "John", null, 16, 17));
        assertEquals(List.of(), verseBrowserService.findVerseRange("KJV", "John", 3, null, 17));
        assertEquals(List.of(), verseBrowserService.findVerseRange("KJV", "John", 3, 16, null));
        assertEquals(List.of(
                new VerseRow("KJV", "John", 3, 16, "For God so loved the world")
        ), verseBrowserService.findVerseRange("KJV", "John", 3, 16, 16));
        assertEquals(List.of(), verseBrowserService.findVerseRange("KJV", "John", 3, 17, 16));
        assertEquals(List.of(), verseBrowserService.findVerseRange("KJV", "John", 3, 98, 99));
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
        populateVersesTable(databasePath);

        VerseBrowserService verseBrowserService = new VerseBrowserService(jdbcTemplate(databasePath), databasePath);

        assertEquals(List.of("Revideált Károli", "Károli"), verseBrowserService.getTranslations());
        assertEquals(List.of("1. Mózes", "2. Mózes"), verseBrowserService.getBooks("Revideált Károli"));
        assertEquals(List.of(1, 2), verseBrowserService.getChapters("Revideált Károli", "1. Mózes"));
        assertEquals(List.of(1, 2), verseBrowserService.getVerses("Revideált Károli", "1. Mózes", 1));
        assertEquals(List.of(
                new VerseRow("Revideált Károli", "1. Mózes", 1, 1, "Kezdetben teremtette Isten az eget és a földet."),
                new VerseRow("Revideált Károli", "1. Mózes", 1, 2, "A föld pedig kietlen és puszta volt.")
        ), verseBrowserService.findVerses("Revideált Károli", "1. Mózes", 1, null));
        assertEquals(List.of(
                new VerseRow("Revideált Károli", "1. Mózes", 1, 2, "A föld pedig kietlen és puszta volt.")
        ), verseBrowserService.findVerses("Revideált Károli", "1. Mózes", 1, 2));
        assertEquals(List.of(
                new VerseRow("Revideált Károli", "1. Mózes", 1, 1, "Kezdetben teremtette Isten az eget és a földet."),
                new VerseRow("Revideált Károli", "1. Mózes", 1, 2, "A föld pedig kietlen és puszta volt.")
        ), verseBrowserService.findVerseRange("Revideált Károli", "1. Mózes", 1, 1, 2));
    }

    @Test
    void databaseBackedQueriesReturnEmptyListsWhenDatabaseFileIsMissing() {
        VerseBrowserService verseBrowserService = new VerseBrowserService(
                mock(JdbcTemplate.class),
                Path.of("target/non-existent-test-db.db")
        );

        assertEquals(List.of(), verseBrowserService.getTranslations());
        assertEquals(List.of(), verseBrowserService.getBooks("Revideált Károli"));
        assertEquals(List.of(), verseBrowserService.getChapters("Revideált Károli", "1. Mózes"));
        assertEquals(List.of(), verseBrowserService.getVerses("Revideált Károli", "1. Mózes", 1));
        assertEquals(List.of(), verseBrowserService.findVerses("Revideált Károli", "1. Mózes", 1, null));
        assertEquals(List.of(), verseBrowserService.findVerseRange("Revideált Károli", "1. Mózes", 1, 1, 2));
    }

    @Test
    void databaseBackedQueriesReturnEmptyListsWhenVersesTableIsMissing() throws Exception {
        Path databasePath = Files.createTempFile("verse-browser-service-missing-table", ".db");
        VerseBrowserService verseBrowserService = new VerseBrowserService(jdbcTemplate(databasePath), databasePath);

        assertEquals(List.of(), verseBrowserService.getTranslations());
        assertEquals(List.of(), verseBrowserService.getBooks("Revideált Károli"));
        assertEquals(List.of(), verseBrowserService.getChapters("Revideált Károli", "1. Mózes"));
        assertEquals(List.of(), verseBrowserService.getVerses("Revideált Károli", "1. Mózes", 1));
        assertEquals(List.of(), verseBrowserService.findVerses("Revideált Károli", "1. Mózes", 1, null));
        assertEquals(List.of(), verseBrowserService.findVerseRange("Revideált Károli", "1. Mózes", 1, 1, 2));
    }

    @Test
    void databaseBackedQueriesReturnEmptyListsWhenVersesTableIsMissingInNestedCause() throws Exception {
        Path databasePath = Files.createTempFile("verse-browser-service-nested-missing-table", ".db");
        JdbcTemplate jdbcTemplate = new ThrowingJdbcTemplate(new RuntimeException(
                "wrapper",
                new RuntimeException("no such table: verses")
        ));

        VerseBrowserService verseBrowserService = new VerseBrowserService(jdbcTemplate, databasePath);

        assertEquals(List.of(), verseBrowserService.getTranslations());
    }

    @Test
    void databaseBackedQueriesWrapUnexpectedSqlFailures() throws Exception {
        Path databasePath = Files.createTempFile("verse-browser-service-broken-connection", ".db");
        JdbcTemplate jdbcTemplate = new ThrowingJdbcTemplate(new RuntimeException("broken repository"));

        VerseBrowserService verseBrowserService = new VerseBrowserService(jdbcTemplate, databasePath);

        IllegalStateException exception = assertThrows(IllegalStateException.class, verseBrowserService::getTranslations);

        assertEquals("Could not read verses from SQLite database.", exception.getMessage());
        assertEquals("broken repository", exception.getCause().getMessage());
    }

    private JdbcTemplate jdbcTemplate(Path databasePath) {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.sqlite.JDBC");
        dataSource.setUrl("jdbc:sqlite:" + databasePath);
        return new JdbcTemplate(dataSource);
    }

    private void populateVersesTable(Path databasePath) throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath);
             Statement statement = connection.createStatement()) {
            statement.execute("drop table if exists verses");
            statement.execute("""
                    create table verses (
                        id integer primary key autoincrement,
                        translation text not null,
                        book text not null,
                        chapter integer not null,
                        verse integer not null,
                        text text not null
                    )
                    """);
            statement.execute("""
                    insert into verses (translation, book, chapter, verse, text)
                    values
                    ('Revideált Károli', '1. Mózes', 1, 1, 'Kezdetben teremtette Isten az eget és a földet.'),
                    ('Revideált Károli', '1. Mózes', 1, 2, 'A föld pedig kietlen és puszta volt.'),
                    ('Revideált Károli', '1. Mózes', 2, 1, 'Így készült el az ég és a föld minden seregükkel együtt.'),
                    ('Károli', '1. Mózes', 1, 1, 'Kezdetben teremté Isten az eget és a földet.'),
                    ('Revideált Károli', '2. Mózes', 1, 1, 'Ezek pedig Izráel fiainak nevei')
                    """);
        }
    }

    private static final class ThrowingJdbcTemplate extends JdbcTemplate {

        private final RuntimeException exception;

        private ThrowingJdbcTemplate(RuntimeException exception) {
            this.exception = exception;
        }

        @Override
        public <T> List<T> query(String sql, RowMapper<T> rowMapper, Object... args) {
            throw exception;
        }
    }
}
