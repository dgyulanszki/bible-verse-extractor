package hu.szegedibibliaszol.scraper.model;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScraperConfigTest {

    @Test
    void recordExposesConfiguredValues() {
        ScraperConfig config = new ScraperConfig(
                Path.of("verses.db"),
                500,
                true,
                false,
                List.of("revidealt-karoli"),
                List.of("efo"),
                Optional.of("https://www.bible.com/bible/198/DAN.12.EFO")
        );

        assertEquals(Path.of("verses.db"), config.outputDatabasePath());
        assertEquals(500, config.requestDelayMillis());
        assertTrue(config.staticScrapingEnabled());
        assertFalse(config.dynamicScrapingEnabled());
        assertEquals(List.of("revidealt-karoli"), config.staticTranslations());
        assertEquals(List.of("efo"), config.dynamicTranslations());
        assertEquals(Optional.of("https://www.bible.com/bible/198/DAN.12.EFO"), config.dynamicStartUrl());
    }

    @Test
    void recordDefensivelyCopiesTranslationSelections() {
        List<String> configuredStaticTranslations = new ArrayList<>(List.of("all"));
        List<String> configuredDynamicTranslations = new ArrayList<>(List.of("efo"));

        ScraperConfig config = new ScraperConfig(
                Path.of("verses.db"),
                500,
                true,
                false,
                configuredStaticTranslations,
                configuredDynamicTranslations,
                Optional.empty()
        );
        configuredStaticTranslations.add("karoli-gaspar");
        configuredDynamicTranslations.add("niv");

        assertEquals(List.of("all"), config.staticTranslations());
        assertEquals(List.of("efo"), config.dynamicTranslations());
        assertTrue(config.dynamicStartUrl().isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> config.staticTranslations().add("karoli-gaspar"));
        assertThrows(UnsupportedOperationException.class, () -> config.dynamicTranslations().add("niv"));
    }

    @Test
    void recordTrimsAndDropsBlankDynamicStartUrl() {
        ScraperConfig configuredUrl = new ScraperConfig(
                Path.of("verses.db"),
                500,
                true,
                true,
                List.of("all"),
                List.of("efo"),
                Optional.of("  https://www.bible.com/bible/198/DAN.12.EFO  ")
        );
        ScraperConfig blankUrl = new ScraperConfig(
                Path.of("verses.db"),
                500,
                true,
                true,
                List.of("all"),
                List.of("efo"),
                Optional.of("   ")
        );

        assertEquals(Optional.of("https://www.bible.com/bible/198/DAN.12.EFO"), configuredUrl.dynamicStartUrl());
        assertTrue(blankUrl.dynamicStartUrl().isEmpty());
    }
}

