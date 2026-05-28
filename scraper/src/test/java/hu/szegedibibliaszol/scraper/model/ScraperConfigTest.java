package hu.szegedibibliaszol.scraper.model;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ScraperConfigTest {

    @Test
    void recordExposesConfiguredValues() {
        ScraperConfig config = new ScraperConfig(Path.of("verses.db"), 500, true, false);

        assertEquals(Path.of("verses.db"), config.outputDatabasePath());
        assertEquals(500, config.requestDelayMillis());
        assertTrue(config.staticScrapingEnabled());
        assertFalse(config.dynamicScrapingEnabled());
    }
}

