package hu.szegedibibliaszol.scraper.export;

import hu.szegedibibliaszol.scraper.model.VerseRecord;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

    private int queryVerseCount(Path databasePath) throws SQLException {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath);
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("select count(*) from verses")) {
            resultSet.next();
            return resultSet.getInt(1);
        }
    }
}

