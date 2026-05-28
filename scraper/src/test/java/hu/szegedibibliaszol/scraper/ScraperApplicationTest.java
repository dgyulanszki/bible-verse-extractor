package hu.szegedibibliaszol.scraper;

import hu.szegedibibliaszol.scraper.model.ScraperConfig;
import hu.szegedibibliaszol.scraper.service.ScraperCoordinator;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScraperApplicationTest {

    @Test
    void createDefaultConfigUsesExpectedValues() {
        ScraperConfig config = ScraperApplication.createDefaultConfig();

        assertEquals(Path.of("target", "scraper-output", "bible-verses.db"), config.outputDatabasePath());
        assertEquals(250, config.requestDelayMillis());
        assertTrue(config.staticScrapingEnabled());
        assertTrue(config.dynamicScrapingEnabled());
    }

    @Test
    void createCoordinatorBuildsCoordinator() {
        ScraperCoordinator coordinator = ScraperApplication.createCoordinator(
                new ScraperConfig(Path.of("target", "custom.db"), 0, false, false)
        );

        assertNotNull(coordinator);
    }

    @Test
    void mainCreatesSqliteDatabaseFile() throws IOException {
        Path databasePath = Path.of("target", "scraper-output", "bible-verses.db");
        Files.deleteIfExists(databasePath);

        ScraperApplication.main(new String[0]);

        assertTrue(Files.exists(databasePath));
    }

    @Test
    void constructorIsNotInstantiable() throws Exception {
        Constructor<ScraperApplication> constructor = ScraperApplication.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        InvocationTargetException exception = assertThrows(InvocationTargetException.class, constructor::newInstance);

        assertInstanceOf(IllegalStateException.class, exception.getCause());
    }
}


