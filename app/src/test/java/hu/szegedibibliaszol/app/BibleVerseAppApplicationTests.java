package hu.szegedibibliaszol.app;

import hu.szegedibibliaszol.app.service.VerseBrowserService;
import hu.szegedibibliaszol.app.ui.model.VerseRow;
import javafx.application.Application;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mockStatic;

@SpringBootTest
class BibleVerseAppApplicationTests {

    private static final Path TEST_DATABASE_PATH = createTestDatabasePath();

    @Autowired
    private VerseBrowserService verseBrowserService;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("app.database.path", () -> TEST_DATABASE_PATH.toString());
    }

    @Test
    void contextLoads() {
        assertNotNull(verseBrowserService);
    }

    @Test
    void verseBrowserServiceReadsRepositoryBackedSelectionsFromSqlite() throws Exception {
        populateVersesTable();

        assertEquals(List.of("Revideált Károli"), verseBrowserService.getTranslations());
        assertEquals(List.of("1. Mózes", "2. Mózes"), verseBrowserService.getBooks("Revideált Károli"));
        assertEquals(List.of(1, 2), verseBrowserService.getChapters("Revideált Károli", "1. Mózes"));
        assertEquals(List.of(1, 2), verseBrowserService.getVerses("Revideált Károli", "1. Mózes", 1));
        assertEquals(List.of(
                new VerseRow("Revideált Károli", "1. Mózes", 1, 1, "Kezdetben teremtette Isten az eget és a földet."),
                new VerseRow("Revideált Károli", "1. Mózes", 1, 2, "A föld pedig kietlen és puszta volt.")
        ), verseBrowserService.findVerses("Revideált Károli", "1. Mózes", 1, null));
    }

    @Test
    void mainDelegatesToJavaFxLaunch() {
        String[] args = {"--demo"};

        try (MockedStatic<Application> application = mockStatic(Application.class)) {
            BibleVerseAppApplication.main(args);

            application.verify(() -> Application.launch(JavaFxApplication.class, args));
        }
    }

    private static Path createTestDatabasePath() {
        try {
            return Files.createTempFile("bible-verse-app-test", ".db");
        } catch (Exception ex) {
            throw new IllegalStateException("Could not create test SQLite database file.", ex);
        }
    }

    private static void populateVersesTable() throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + TEST_DATABASE_PATH);
             Statement statement = connection.createStatement()) {
            statement.execute("drop table if exists verses");
            statement.execute("""
                    create table verses (
                        id integer primary key autoincrement,
                        translation text not null,
                        book text not null,
                        chapter integer not null,
                        verse integer not null,
                        text text not null
                    )
                    """);
            statement.execute("""
                    insert into verses (translation, book, chapter, verse, text)
                    values
                    ('Revideált Károli', '1. Mózes', 1, 1, 'Kezdetben teremtette Isten az eget és a földet.'),
                    ('Revideált Károli', '1. Mózes', 1, 2, 'A föld pedig kietlen és puszta volt.'),
                    ('Revideált Károli', '1. Mózes', 2, 1, 'Így készült el az ég és a föld minden seregükkel együtt.'),
                    ('Revideált Károli', '2. Mózes', 1, 1, 'Ezek pedig Izráel fiainak nevei')
                    """);
        }
    }
}
