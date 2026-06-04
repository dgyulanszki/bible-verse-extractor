package hu.szegedibibliaszol.app.service;

import hu.szegedibibliaszol.app.ui.model.AppSessionSnapshot;
import hu.szegedibibliaszol.app.ui.model.RangeSelectionSnapshot;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class DatabaseUiSessionService implements UiSessionService {

    private static final String SESSION_TABLE_NAME = "ui_session_ranges";

    private final JdbcTemplate jdbcTemplate;
    private final Path databasePath;

    @Autowired
    public DatabaseUiSessionService(
            JdbcTemplate jdbcTemplate,
            @Value("${app.database.path:${user.home}/bible-verses.db}") String databasePath
    ) {
        this(jdbcTemplate, Path.of(databasePath));
    }

    DatabaseUiSessionService(JdbcTemplate jdbcTemplate, Path databasePath) {
        this.jdbcTemplate = jdbcTemplate;
        this.databasePath = databasePath;
    }

    @Override
    public Optional<AppSessionSnapshot> loadSession() {
        if (!databaseFileExists()) {
            return Optional.empty();
        }

        try {
            List<StoredSessionRow> storedRows = jdbcTemplate.query(
                    """
                            select translation, order_index, book, chapter, from_verse, to_verse
                            from ui_session_ranges
                            order by order_index
                            """,
                    (resultSet, _) -> new StoredSessionRow(
                            resultSet.getString("translation"),
                            resultSet.getInt("order_index"),
                            resultSet.getString("book"),
                            getNullableInteger(resultSet.getObject("chapter")),
                            getNullableInteger(resultSet.getObject("from_verse")),
                            getNullableInteger(resultSet.getObject("to_verse"))
                    )
            );
            if (storedRows.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(new AppSessionSnapshot(
                    storedRows.getFirst().translation(),
                    storedRows.stream()
                            .map(storedRow -> new RangeSelectionSnapshot(
                                    storedRow.book(),
                                    storedRow.chapter(),
                                    storedRow.fromVerse(),
                                    storedRow.toVerse()
                            ))
                            .toList()
            ));
        } catch (RuntimeException ex) {
            if (isMissingSessionTable(ex)) {
                return Optional.empty();
            }
            throw new IllegalStateException("Could not load the saved UI session from SQLite.", ex);
        }
    }

    @Override
    public void saveSession(AppSessionSnapshot sessionSnapshot) {
        try {
            ensureSessionTableExists();
            jdbcTemplate.update("delete from ui_session_ranges");
            if (sessionSnapshot.isEmpty()) {
                return;
            }
            for (int index = 0; index < sessionSnapshot.ranges().size(); index++) {
                RangeSelectionSnapshot rangeSelectionSnapshot = sessionSnapshot.ranges().get(index);
                jdbcTemplate.update(
                        """
                                insert into ui_session_ranges (
                                    order_index,
                                    translation,
                                    book,
                                    chapter,
                                    from_verse,
                                    to_verse
                                ) values (?, ?, ?, ?, ?, ?)
                                """,
                        index,
                        sessionSnapshot.translation(),
                        rangeSelectionSnapshot.book(),
                        rangeSelectionSnapshot.chapter(),
                        rangeSelectionSnapshot.fromVerse(),
                        rangeSelectionSnapshot.toVerse()
                );
            }
        } catch (RuntimeException ex) {
            throw new IllegalStateException("Could not save the UI session into SQLite.", ex);
        }
    }

    private void ensureSessionTableExists() {
        jdbcTemplate.execute(
                """
                        create table if not exists ui_session_ranges (
                            order_index integer primary key,
                            translation text not null,
                            book text,
                            chapter integer,
                            from_verse integer,
                            to_verse integer
                        )
                        """
        );
    }

    private boolean databaseFileExists() {
        return databasePath != null && Files.isRegularFile(databasePath);
    }

    private boolean isMissingSessionTable(Throwable throwable) {
        String message = throwable.getMessage();
        if (message != null && message.contains("no such table: " + SESSION_TABLE_NAME)) {
            return true;
        }

        Throwable cause = throwable.getCause();
        if (cause == null) {
            return false;
        }
        return isMissingSessionTable(cause);
    }

    private Integer getNullableInteger(Object value) {
        if (value == null) {
            return null;
        }
        return ((Number) value).intValue();
    }

    private record StoredSessionRow(
            String translation,
            int orderIndex,
            String book,
            Integer chapter,
            Integer fromVerse,
            Integer toVerse
    ) {
    }
}

