package hu.szegedibibliaszol.app.ui;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

final class BookNameCanonicalizer {

    private static final String RESOURCE_PATH = "/book-name-canonicalizer.tsv";
    private static final List<String> EXPECTED_HEADER_COLUMNS = List.of(
            "canonical",
            "Revideált Károli",
            "Károli",
            "Revideált Új Fordítás (RÚF)",
            "Egyszerű fordítás (EFO)"
    );
    private static final int EXPECTED_BOOK_COUNT = 66;
    private static final Map<String, String> CANONICAL_BOOK_NAMES_BY_KEY = loadCanonicalBookNamesByKey();

    private BookNameCanonicalizer() {
    }

    static String canonicalBookName(String book) {
        if (book == null) {
            return null;
        }
        return CANONICAL_BOOK_NAMES_BY_KEY.getOrDefault(normalizeBookNameKey(book), book);
    }

    static Map<String, String> canonicalBookNamesByKey() {
        return CANONICAL_BOOK_NAMES_BY_KEY;
    }

    static String normalizeBookNameKey(String book) {
        return Objects.requireNonNull(book, "book must not be null")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[\\s.]", "");
    }

    private static Map<String, String> loadCanonicalBookNamesByKey() {
        return loadCanonicalBookNamesByKey(BookNameCanonicalizer.class.getResourceAsStream(RESOURCE_PATH), RESOURCE_PATH);
    }

    static Map<String, String> loadCanonicalBookNamesByKey(InputStream inputStream, String resourcePath) {
        if (inputStream == null) {
            throw new IllegalStateException("Missing canonical book-name resource: " + resourcePath);
        }
        try (InputStream closeableInputStream = inputStream) {
            List<String> lines = new String(closeableInputStream.readAllBytes(), StandardCharsets.UTF_8).lines().toList();
            return parseCanonicalBookNamesByKey(lines, resourcePath, EXPECTED_BOOK_COUNT);
        } catch (IOException ex) {
            throw new UncheckedIOException("Could not load canonical book-name resource: " + resourcePath, ex);
        }
    }

    static Map<String, String> parseCanonicalBookNamesByKey(List<String> lines, String resourcePath) {
        return parseCanonicalBookNamesByKey(lines, resourcePath, null);
    }

    private static Map<String, String> parseCanonicalBookNamesByKey(
            List<String> lines,
            String resourcePath,
            Integer expectedBookCount
    ) {
        int headerIndex = findHeaderIndex(lines);
        if (headerIndex < 0) {
            throw new IllegalStateException("Missing canonical book-name header in resource: " + resourcePath);
        }

        List<String> headerColumns = parseColumns(lines.get(headerIndex));
        if (!headerColumns.equals(EXPECTED_HEADER_COLUMNS)) {
            throw new IllegalStateException(
                    "Invalid canonical book-name header in resource " + resourcePath + ": " + lines.get(headerIndex)
            );
        }

        Map<String, String> canonicalBookNamesByKey = new LinkedHashMap<>();
        int bookCount = 0;
        for (int index = headerIndex + 1; index < lines.size(); index++) {
            String line = lines.get(index).trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }

            List<String> columns = parseColumns(lines.get(index));
            if (columns.size() != EXPECTED_HEADER_COLUMNS.size() || columns.stream().anyMatch(String::isBlank)) {
                throw new IllegalStateException(
                        "Invalid canonical book-name entry at line " + (index + 1) + " in resource " + resourcePath + ": " + lines.get(index)
                );
            }

            String canonicalBookName = columns.getFirst();
            for (String column : columns) {
                canonicalBookNamesByKey.put(normalizeBookNameKey(column), canonicalBookName);
            }
            bookCount++;
        }

        if (expectedBookCount != null && bookCount != expectedBookCount) {
            throw new IllegalStateException(
                    "Unexpected canonical book-name row count in resource " + resourcePath + ": " + bookCount
            );
        }

        return Map.copyOf(canonicalBookNamesByKey);
    }

    private static int findHeaderIndex(List<String> lines) {
        for (int index = 0; index < lines.size(); index++) {
            String line = lines.get(index).trim();
            if (!line.isEmpty() && !line.startsWith("#")) {
                return index;
            }
        }
        return -1;
    }

    private static List<String> parseColumns(String line) {
        return List.of(line.split("\\t", -1));
    }
}
