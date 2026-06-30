package hu.szegedibibliaszol.app.service;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SqliteDatabaseSupportTest {

    @Test
    void databaseFileExistsReturnsExpectedValues() throws Exception {
        Path existingDatabasePath = Files.createTempFile("sqlite-database-support", ".db");

        assertFalse(SqliteDatabaseSupport.databaseFileExists(null));
        assertFalse(SqliteDatabaseSupport.databaseFileExists(Path.of("target", "missing-sqlite-database-support.db")));
        assertTrue(SqliteDatabaseSupport.databaseFileExists(existingDatabasePath));
    }

    @Test
    void hasMissingTableChecksTheEntireCauseChain() {
        RuntimeException missingTableFailure = new RuntimeException(
                null,
                new RuntimeException("wrapper", new RuntimeException("no such table: verses"))
        );

        assertTrue(SqliteDatabaseSupport.hasMissingTable(missingTableFailure, "verses"));
        assertFalse(SqliteDatabaseSupport.hasMissingTable(new RuntimeException("different failure"), "verses"));
        assertFalse(SqliteDatabaseSupport.hasMissingTable(new RuntimeException(""), "verses"));
        assertFalse(SqliteDatabaseSupport.hasMissingTable(new RuntimeException((String) null), "verses"));
        assertFalse(SqliteDatabaseSupport.hasMissingTable(null, "verses"));
    }

    @Test
    void utilityClassCanBeConstructedWithinItsPackage() {
        assertNotNull(new SqliteDatabaseSupport());
    }
}



