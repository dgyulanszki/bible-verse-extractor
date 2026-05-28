package hu.szegedibibliaszol.app.service;

import hu.szegedibibliaszol.app.ui.model.VerseRow;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class VerseBrowserService {

    public List<String> getTranslations() {
        return List.of();
    }

    public List<String> getBooks(String translation) {
        return List.of();
    }

    public List<Integer> getChapters(String translation, String book) {
        return List.of();
    }

    public List<Integer> getVerses(String translation, String book, Integer chapter) {
        return List.of();
    }

    public List<VerseRow> findVerses(String translation, String book, Integer chapter, Integer verse) {
        return List.of();
    }
}

