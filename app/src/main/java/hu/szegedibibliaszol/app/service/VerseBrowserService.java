package hu.szegedibibliaszol.app.service;

import hu.szegedibibliaszol.app.entity.Verse;
import hu.szegedibibliaszol.app.repository.VersesRepository;
import hu.szegedibibliaszol.app.ui.model.VerseRow;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class VerseBrowserService {

    private static final String VERSES_TABLE_NAME = "verses";

    private final VersesRepository versesRepository;
    private final Path databasePath;
    private final List<VerseRow> inMemoryVerseRows;
    private final boolean databaseBacked;

    public VerseBrowserService() {
        this(null, null, List.of(), false);
    }

    @Autowired
    public VerseBrowserService(
            VersesRepository versesRepository,
            @Value("${app.database.path:${user.home}/bible-verses.db}") String databasePath
    ) {
        this(versesRepository, Path.of(databasePath), List.of(), true);
    }

    public VerseBrowserService(List<VerseRow> verseRows) {
        this(null, null, verseRows, false);
    }

    VerseBrowserService(VersesRepository versesRepository, Path databasePath) {
        this(versesRepository, databasePath, List.of(), true);
    }

    public List<String> getTranslations() {
        if (databaseBacked) {
            return queryOrEmpty(versesRepository::findTranslations);
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
            return queryOrEmpty(() -> versesRepository.findBooksByTranslation(translation));
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
            return queryOrEmpty(() -> versesRepository.findChaptersByTranslationAndBook(translation, book));
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
            return queryOrEmpty(() -> versesRepository.findVersesByTranslationAndBookAndChapter(translation, book, chapter));
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
                return queryOrEmpty(() -> versesRepository.findByTranslationAndBookAndChapterOrderByVerseAsc(
                        translation,
                        book,
                        chapter
                )).stream().map(this::toVerseRow).toList();
            }

            return queryOrEmpty(() -> versesRepository.findByTranslationAndBookAndChapterAndVerseOrderByVerseAsc(
                    translation,
                    book,
                    chapter,
                    verse
            )).stream().map(this::toVerseRow).toList();
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
            return queryOrEmpty(() -> versesRepository.findByTranslationAndBookAndChapterAndVerseBetweenOrderByVerseAsc(
                    translation,
                    book,
                    chapter,
                    fromVerse,
                    toVerse
            )).stream().map(this::toVerseRow).toList();
        }

        return streamByChapterSelection(translation, book, chapter)
                .filter(verseRow -> verseRow.verse() >= fromVerse && verseRow.verse() <= toVerse)
                .toList();
    }

    private VerseBrowserService(
            VersesRepository versesRepository,
            Path databasePath,
            List<VerseRow> verseRows,
            boolean databaseBacked
    ) {
        this.versesRepository = versesRepository;
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

    private VerseRow toVerseRow(Verse verse) {
        return new VerseRow(
                verse.getTranslation(),
                verse.getBook(),
                verse.getChapter(),
                verse.getVerse(),
                verse.getText()
        );
    }
}
