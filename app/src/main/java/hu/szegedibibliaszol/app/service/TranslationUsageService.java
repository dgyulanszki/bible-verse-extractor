package hu.szegedibibliaszol.app.service;

import java.util.Optional;

public interface TranslationUsageService {

    void recordUse(String translation);

    Optional<String> preferredTranslation();

    static TranslationUsageService noop() {
        return new TranslationUsageService() {
            @Override
            public void recordUse(String translation) {
            }

            @Override
            public Optional<String> preferredTranslation() {
                return Optional.empty();
            }
        };
    }
}
