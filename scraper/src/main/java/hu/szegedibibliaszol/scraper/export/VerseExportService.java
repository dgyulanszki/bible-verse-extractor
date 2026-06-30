package hu.szegedibibliaszol.scraper.export;

import hu.szegedibibliaszol.scraper.model.VerseRecord;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Objects;

public class VerseExportService {

    public void exportToSqlite(Path databasePath, List<VerseRecord> verses) {
        Objects.requireNonNull(databasePath, "databasePath must not be null");
        Objects.requireNonNull(verses, "verses must not be null");

        try {
            createParentDirectories(databasePath.getParent());
        } catch (IOException ex) {
            throw new IllegalStateException("Could not create export directory for SQLite database.", ex);
        }

        String jdbcUrl = "jdbc:sqlite:" + databasePath;
        try (Connection connection = openConnection(jdbcUrl)) {
            connection.setAutoCommit(false);
            try {
                try (Statement createTableStatement = connection.createStatement();
                     Statement removeDuplicateRowsStatement = connection.createStatement();
                     Statement createUniqueIndexStatement = connection.createStatement()) {
                    createTableStatement.execute(createTableSql());
                    removeDuplicateRowsStatement.execute(removeDuplicateRowsSql());
                    createUniqueIndexStatement.execute(createUniqueIndexSql());
                }

                try (PreparedStatement statement = connection.prepareStatement(createInsertSql())) {
                    for (VerseRecord verse : verses) {
                        Objects.requireNonNull(verse, "verses must not contain null entries");
                        statement.setString(1, verse.translation());
                        statement.setString(2, verse.book());
                        statement.setInt(3, verse.chapter());
                        statement.setInt(4, verse.verse());
                        statement.setString(5, verse.text());
                        statement.addBatch();
                    }
                    statement.executeBatch();
                }
                connection.commit();
            } catch (RuntimeException | SQLException ex) {
                rollbackQuietly(connection, ex);
                throw ex;
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Could not export collected verses to SQLite.", ex);
        }
    }

    protected void rollbackQuietly(Connection connection, Throwable originalFailure) {
        try {
            connection.rollback();
        } catch (SQLException rollbackFailure) {
            originalFailure.addSuppressed(rollbackFailure);
        }
    }

    protected void createParentDirectories(Path parentPath) throws IOException {
        if (parentPath != null) {
            Files.createDirectories(parentPath);
        }
    }

    protected Connection openConnection(String jdbcUrl) throws SQLException {
        return DriverManager.getConnection(jdbcUrl);
    }

    protected String createTableSql() {
        return """
                create table if not exists verses (
                    id integer primary key autoincrement,
                    translation text not null,
                    book text not null,
                    chapter integer not null,
                    verse integer not null,
                    text text not null
                )
                """;
    }

    protected String createInsertSql() {
        return """
                insert or ignore into verses (translation, book, chapter, verse, text)
                values (?, ?, ?, ?, ?)
                """;
    }

    protected String removeDuplicateRowsSql() {
        return """
                delete from verses
                where id not in (
                    select min(id)
                    from verses
                    group by translation, book, chapter, verse, text
                )
                """;
    }

    protected String createUniqueIndexSql() {
        return """
                create unique index if not exists verses_unique_reference_and_text
                on verses (translation, book, chapter, verse, text)
                """;
    }
}

