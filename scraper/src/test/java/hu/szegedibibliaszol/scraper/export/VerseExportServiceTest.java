package hu.szegedibibliaszol.scraper.export;

import hu.szegedibibliaszol.scraper.model.VerseRecord;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VerseExportServiceTest {

    @Test
    void exportsVersesIntoSqliteDatabase() throws Exception {
        Path tempDirectory = Files.createTempDirectory("verse-export-test");
        Path databasePath = tempDirectory.resolve("verses.db");
        List<VerseRecord> verses = List.of(
                new VerseRecord("KJV", "John", 3, 16, "For God so loved the world")
        );

        new VerseExportService().exportToSqlite(databasePath, verses);

        assertTrue(Files.exists(databasePath));
        assertEquals(1, queryVerseCount(databasePath));
    }

    @Test
    void exportsEmptyVerseListIntoSqliteDatabase() throws Exception {
        Path tempDirectory = Files.createTempDirectory("verse-export-empty-test");
        Path databasePath = tempDirectory.resolve("verses.db");
        List<VerseRecord> verses = List.of();

        new VerseExportService().exportToSqlite(databasePath, verses);

        assertTrue(Files.exists(databasePath));
        assertEquals(0, queryVerseCount(databasePath));
    }

    @Test
    void doesNotInsertDuplicateVersesFromTheSameBatch() throws Exception {
        Path tempDirectory = Files.createTempDirectory("verse-export-duplicate-batch-test");
        Path databasePath = tempDirectory.resolve("verses.db");
        VerseRecord duplicateVerse = new VerseRecord("KAR", "1. Mózes", 1, 1, "Kezdetben teremtette Isten az eget és a földet.");

        new VerseExportService().exportToSqlite(databasePath, List.of(duplicateVerse, duplicateVerse));

        assertEquals(1, queryVerseCount(databasePath));
    }

    @Test
    void preservesExistingDifferentVersesAndSkipsMatchingDuplicates() throws Exception {
        Path tempDirectory = Files.createTempDirectory("verse-export-preserve-test");
        Path databasePath = tempDirectory.resolve("verses.db");
        VerseExportService verseExportService = new VerseExportService();
        VerseRecord verseOne = new VerseRecord("KAR", "1. Mózes", 1, 1, "Kezdetben teremtette Isten az eget és a földet.");
        VerseRecord verseTwo = new VerseRecord("KAR", "1. Mózes", 1, 2, "A föld pedig kietlen és puszta volt.");

        verseExportService.exportToSqlite(databasePath, List.of(verseOne));
        verseExportService.exportToSqlite(databasePath, List.of(verseOne, verseTwo));

        assertEquals(2, queryVerseCount(databasePath));
    }

    @Test
    void removesExistingDuplicateRowsBeforeCreatingUniqueIndex() throws Exception {
        Path tempDirectory = Files.createTempDirectory("verse-export-deduplicate-existing-test");
        Path databasePath = tempDirectory.resolve("verses.db");
        VerseRecord verse = new VerseRecord("KAR", "1. Mózes", 1, 1, "Kezdetben teremtette Isten az eget és a földet.");

        createDatabaseWithDuplicateRows(databasePath, verse);

        new VerseExportService().exportToSqlite(databasePath, List.of());

        assertEquals(1, queryVerseCount(databasePath));
    }

    @Test
    void wrapsDirectoryCreationFailure() {
        VerseExportService verseExportService = new VerseExportService() {
            @Override
            protected void createParentDirectories(Path parentPath) throws IOException {
                throw new IOException("directory failure");
            }
        };

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> verseExportService.exportToSqlite(Path.of("target", "failure.db"), List.of())
        );

        assertInstanceOf(IOException.class, exception.getCause());
    }

    @Test
    void wrapsConnectionFailure() {
        VerseExportService verseExportService = new VerseExportService() {
            @Override
            protected Connection openConnection(String jdbcUrl) throws SQLException {
                throw new SQLException("connection failure");
            }
        };

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> verseExportService.exportToSqlite(Path.of("target", "failure.db"), List.of())
        );

        assertInstanceOf(SQLException.class, exception.getCause());
    }

    @Test
    void addsSuppressedRollbackFailureWhenRollbackAlsoFails() {
        Connection connection = (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class<?>[]{Connection.class},
                (_, method, _) -> {
                    if ("rollback".equals(method.getName())) {
                        throw new SQLException("rollback failure");
                    }
                    if ("toString".equals(method.getName())) {
                        return "FailingRollbackConnection";
                    }
                    throw new UnsupportedOperationException(method.getName());
                }
        );
        IllegalStateException originalFailure = new IllegalStateException("export failure");

        new VerseExportService().rollbackQuietly(connection, originalFailure);

        assertEquals(1, originalFailure.getSuppressed().length);
        assertEquals("rollback failure", originalFailure.getSuppressed()[0].getMessage());
    }

    @Test
    void rollsBackSchemaChangesWhenBatchPreparationFails() throws Exception {
        Path tempDirectory = Files.createTempDirectory("verse-export-rollback-test");
        Path databasePath = tempDirectory.resolve("verses.db");
        VerseExportService verseExportService = new VerseExportService() {
            @Override
            protected String createInsertSql() {
                return "insert into verses (translation, book, chapter, verse, missing_text) values (?, ?, ?, ?, ?)";
            }
        };

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> verseExportService.exportToSqlite(
                        databasePath,
                        List.of(new VerseRecord("KJV", "John", 3, 16, "For God so loved the world"))
                )
        );

        assertInstanceOf(SQLException.class, exception.getCause());
        assertFalse(versesTableExists(databasePath));
    }

    @Test
    void rejectsNullDatabasePath() {
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> new VerseExportService().exportToSqlite(null, List.of())
        );

        assertEquals("databasePath must not be null", exception.getMessage());
    }

    @Test
    void rejectsNullVerseList() {
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> new VerseExportService().exportToSqlite(Path.of("target", "failure.db"), null)
        );

        assertEquals("verses must not be null", exception.getMessage());
    }

    @Test
    void rejectsNullVerseEntries() {
        Path databasePath = Path.of("target", "null-verse-entry.db");

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> new VerseExportService().exportToSqlite(databasePath, java.util.Collections.singletonList(null))
        );

        assertEquals("verses must not contain null entries", exception.getMessage());
    }

    private int queryVerseCount(Path databasePath) throws SQLException {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath);
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("select count(*) from verses")) {
            resultSet.next();
            return resultSet.getInt(1);
        }
    }

    private boolean versesTableExists(Path databasePath) throws SQLException {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath);
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("select count(*) from sqlite_master where type = 'table' and name = 'verses'")) {
            resultSet.next();
            return resultSet.getInt(1) == 1;
        }
    }

    private void createDatabaseWithDuplicateRows(Path databasePath, VerseRecord verse) throws Exception {
        Files.createDirectories(databasePath.getParent());

        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath);
             Statement statement = connection.createStatement()) {
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
            statement.execute(insertVerseSql(verse));
            statement.execute(insertVerseSql(verse));
        }
    }

    private String insertVerseSql(VerseRecord verse) {
        return "insert into verses (translation, book, chapter, verse, text) values ('"
                + verse.translation().replace("'", "''") + "', '"
                + verse.book().replace("'", "''") + "', "
                + verse.chapter() + ", "
                + verse.verse() + ", '"
                + verse.text().replace("'", "''") + "')";
    }
}

