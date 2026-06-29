package hu.szegedibibliaszol.scraper.service;

import hu.szegedibibliaszol.scraper.model.VerseRecord;
import java.util.List;
import java.util.Objects;

final class PartialScrapeException extends IllegalStateException {

    private final List<VerseRecord> partialVerses;

    PartialScrapeException(String message, Throwable cause, List<VerseRecord> partialVerses) {
        super(message, cause);
        Objects.requireNonNull(partialVerses, "partialVerses must not be null");
        this.partialVerses = List.copyOf(partialVerses);
    }

    List<VerseRecord> partialVerses() {
        return partialVerses;
    }
}

