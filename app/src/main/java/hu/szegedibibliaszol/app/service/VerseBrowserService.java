package hu.szegedibibliaszol.app.service;

import hu.szegedibibliaszol.app.ui.model.VerseRow;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class VerseBrowserService {

    private static final String VERSES_TABLE_NAME = "verses";
    private static final String FIND_TRANSLATIONS_SQL = """
            select translation
            from verses
            group by translation
            order by min(id)
            """;
    private static final String FIND_BOOKS_BY_TRANSLATION_SQL = """
            select book
            from verses
            where translation = ?
            group by book
            order by min(id)
            """;
    private static final String FIND_CHAPTERS_BY_TRANSLATION_AND_BOOK_SQL = """
            select chapter
            from verses
            where translation = ? and book = ?
            group by chapter
            order by chapter
            """;
    private static final String FIND_VERSES_BY_TRANSLATION_AND_BOOK_AND_CHAPTER_SQL = """
            select verse
            from verses
            where translation = ? and book = ? and chapter = ?
            group by verse
            order by verse
            """;
    private static final String FIND_VERSE_ROWS_BY_CHAPTER_SQL = """
            select translation, book, chapter, verse, text
            from verses
            where translation = ? and book = ? and chapter = ?
            order by verse
            """;
    private static final String FIND_VERSE_ROWS_BY_SINGLE_VERSE_SQL = """
            select translation, book, chapter, verse, text
            from verses
            where translation = ? and book = ? and chapter = ? and verse = ?
            order by verse
            """;
    private static final String FIND_VERSE_ROWS_BY_RANGE_SQL = """
            select translation, book, chapter, verse, text
            from verses
            where translation = ? and book = ? and chapter = ? and verse between ? and ?
            order by verse
            """;

    private final JdbcTemplate jdbcTemplate;
    private final Path databasePath;
    private final List<VerseRow> inMemoryVerseRows;
    private final boolean databaseBacked;
    // These small caches avoid repeating the same lookup queries during startup and normal UI use.
    // They only store selection lists (translations/books/chapters/verses), not whole Bible chapters.
    private volatile List<String> cachedTranslations;
    private final Map<String, List<String>> cachedBooksByTranslation = new ConcurrentHashMap<>();
    private final Map<BookSelection, List<Integer>> cachedChaptersByBook = new ConcurrentHashMap<>();
    private final Map<ChapterSelection, List<Integer>> cachedVersesByChapter = new ConcurrentHashMap<>();

    public VerseBrowserService() {
        this(null, null, List.of(), false);
    }

    @Autowired
    public VerseBrowserService(
            JdbcTemplate jdbcTemplate,
            @Value("${app.database.path:${user.home}/bible-verses.db}") String databasePath
    ) {
        this(jdbcTemplate, Path.of(databasePath), List.of(), true);
    }

    public VerseBrowserService(List<VerseRow> verseRows) {
        this(null, null, verseRows, false);
    }

    VerseBrowserService(JdbcTemplate jdbcTemplate, Path databasePath) {
        this(jdbcTemplate, databasePath, List.of(), true);
    }

    public List<String> getTranslations() {
        if (databaseBacked) {
            // The translation list is needed immediately on startup, so keeping one in-memory copy is cheap and useful.
            List<String> translations = cachedTranslations;
            if (translations != null) {
                return translations;
            }
            List<String> loadedTranslations = queryOrEmpty(() -> queryStrings(FIND_TRANSLATIONS_SQL));
            cachedTranslations = loadedTranslations;
            return loadedTranslations;
        }

        return inMemoryVerseRows.stream()
                .map(VerseRow::translation)
                .distinct()
                .toList();
    }

    public List<String> getBooks(String translation) {
        if (translation == null) {
            return List.of();
        }

        if (databaseBacked) {
            return queryOrEmpty(() -> cachedBooksByTranslation.computeIfAbsent(
                    translation,
                    value -> queryStrings(FIND_BOOKS_BY_TRANSLATION_SQL, value)
            ));
        }

        return streamByTranslation(translation)
                .map(VerseRow::book)
                .distinct()
                .toList();
    }

    public List<Integer> getChapters(String translation, String book) {
        if (translation == null || book == null) {
            return List.of();
        }

        if (databaseBacked) {
            BookSelection selection = new BookSelection(translation, book);
            return queryOrEmpty(() -> cachedChaptersByBook.computeIfAbsent(
                    selection,
                    value -> queryIntegers(FIND_CHAPTERS_BY_TRANSLATION_AND_BOOK_SQL, value.translation(), value.book())
            ));
        }

        return streamByTranslationAndBook(translation, book)
                .map(VerseRow::chapter)
                .distinct()
                .toList();
    }

    public List<Integer> getVerses(String translation, String book, Integer chapter) {
        if (translation == null || book == null || chapter == null) {
            return List.of();
        }

        if (databaseBacked) {
            ChapterSelection selection = new ChapterSelection(translation, book, chapter);
            return queryOrEmpty(() -> cachedVersesByChapter.computeIfAbsent(
                    selection,
                    value -> queryIntegers(
                            FIND_VERSES_BY_TRANSLATION_AND_BOOK_AND_CHAPTER_SQL,
                            value.translation(),
                            value.book(),
                            value.chapter()
                    )
            ));
        }

        return streamByChapterSelection(translation, book, chapter)
                .map(VerseRow::verse)
                .distinct()
                .toList();
    }

    public List<VerseRow> findVerses(String translation, String book, Integer chapter, Integer verse) {
        if (translation == null || book == null || chapter == null) {
            return List.of();
        }

        if (databaseBacked) {
            if (verse == null) {
                return queryOrEmpty(() -> queryVerseRows(FIND_VERSE_ROWS_BY_CHAPTER_SQL, translation, book, chapter));
            }

            return queryOrEmpty(() -> queryVerseRows(FIND_VERSE_ROWS_BY_SINGLE_VERSE_SQL, translation, book, chapter, verse));
        }

        return streamByChapterSelection(translation, book, chapter)
                .filter(verseRow -> verse == null || Objects.equals(verseRow.verse(), verse))
                .toList();
    }

    public List<VerseRow> findVerseRange(
            String translation,
            String book,
            Integer chapter,
            Integer fromVerse,
            Integer toVerse
    ) {
        if (translation == null || book == null || chapter == null || fromVerse == null || toVerse == null || fromVerse > toVerse) {
            return List.of();
        }

        if (databaseBacked) {
            return queryOrEmpty(() -> queryVerseRows(
                    FIND_VERSE_ROWS_BY_RANGE_SQL,
                    translation,
                    book,
                    chapter,
                    fromVerse,
                    toVerse
            ));
        }

        return streamByChapterSelection(translation, book, chapter)
                .filter(verseRow -> verseRow.verse() >= fromVerse && verseRow.verse() <= toVerse)
                .toList();
    }

    private VerseBrowserService(
            JdbcTemplate jdbcTemplate,
            Path databasePath,
            List<VerseRow> verseRows,
            boolean databaseBacked
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.databasePath = databasePath;
        this.inMemoryVerseRows = List.copyOf(verseRows);
        this.databaseBacked = databaseBacked;
    }

    private Stream<VerseRow> streamByTranslation(String translation) {
        return inMemoryVerseRows.stream()
                .filter(verseRow -> Objects.equals(verseRow.translation(), translation));
    }

    private Stream<VerseRow> streamByTranslationAndBook(String translation, String book) {
        return streamByTranslation(translation)
                .filter(verseRow -> Objects.equals(verseRow.book(), book));
    }

    private Stream<VerseRow> streamByChapterSelection(String translation, String book, Integer chapter) {
        return streamByTranslationAndBook(translation, book)
                .filter(verseRow -> Objects.equals(verseRow.chapter(), chapter));
    }

    private <T> List<T> queryOrEmpty(Supplier<List<T>> querySupplier) {
        if (!databaseFileExists()) {
            return List.of();
        }

        try {
            return List.copyOf(querySupplier.get());
        } catch (RuntimeException ex) {
            // A missing or not-yet-generated SQLite file should not stop the UI from opening.
            // We return empty data for this case and only fail on unexpected database errors.
            if (isMissingVersesTable(ex)) {
                return List.of();
            }
            throw new IllegalStateException("Could not read verses from SQLite database.", ex);
        }
    }

    private boolean databaseFileExists() {
        return databasePath != null && Files.isRegularFile(databasePath);
    }

    private boolean isMissingVersesTable(Throwable throwable) {
        String message = throwable.getMessage();
        if (message != null && message.contains("no such table: " + VERSES_TABLE_NAME)) {
            return true;
        }

        Throwable cause = throwable.getCause();
        if (cause == null) {
            return false;
        }

        return isMissingVersesTable(cause);
    }

    private List<String> queryStrings(String sql, Object... args) {
        return List.copyOf(jdbcTemplate.query(sql, (resultSet, _) -> resultSet.getString(1), args));
    }

    private List<Integer> queryIntegers(String sql, Object... args) {
        return List.copyOf(jdbcTemplate.query(sql, (resultSet, _) -> resultSet.getInt(1), args));
    }

    private List<VerseRow> queryVerseRows(String sql, Object... args) {
        return List.copyOf(jdbcTemplate.query(sql, (resultSet, _) -> toVerseRow(resultSet), args));
    }

    private VerseRow toVerseRow(java.sql.ResultSet resultSet) throws java.sql.SQLException {
        return new VerseRow(
                resultSet.getString("translation"),
                resultSet.getString("book"),
                resultSet.getInt("chapter"),
                resultSet.getInt("verse"),
                resultSet.getString("text")
        );
    }

    private record BookSelection(String translation, String book) {
    }

    private record ChapterSelection(String translation, String book, Integer chapter) {
    }
}
