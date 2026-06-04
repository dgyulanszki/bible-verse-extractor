package hu.szegedibibliaszol.app.service;

import hu.szegedibibliaszol.app.ui.model.AppSessionSnapshot;
import hu.szegedibibliaszol.app.ui.model.RangeSelectionSnapshot;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DatabaseUiSessionServiceTest {

    @Test
    void loadSessionReturnsEmptyWhenDatabaseFileIsMissing() {
        DatabaseUiSessionService uiSessionService = new DatabaseUiSessionService(
                mock(JdbcTemplate.class),
                Path.of("target/non-existent-ui-session.db")
        );

        assertEquals(Optional.empty(), uiSessionService.loadSession());
    }

    @Test
    void saveAndLoadSessionRoundTripsSelectionsAndClearsPreviousSession() throws Exception {
        Path databasePath = Files.createTempFile("ui-session-service", ".db");
        DatabaseUiSessionService uiSessionService = new DatabaseUiSessionService(jdbcTemplate(databasePath), databasePath);
        AppSessionSnapshot savedSnapshot = new AppSessionSnapshot(
                "Revideált Károli",
                List.of(
                        new RangeSelectionSnapshot("1. Mózes", 4, 4, 4),
                        new RangeSelectionSnapshot("Zsoltárok", 23, 1, 2),
                        new RangeSelectionSnapshot(null, null, null, null)
                )
        );

        uiSessionService.saveSession(savedSnapshot);

        assertEquals(Optional.of(savedSnapshot), uiSessionService.loadSession());

        uiSessionService.saveSession(new AppSessionSnapshot(null, List.of()));

        assertEquals(Optional.empty(), uiSessionService.loadSession());
    }

    @Test
    void loadSessionReturnsEmptyWhenSessionTableIsMissingInNestedCause() throws Exception {
        Path databasePath = Files.createTempFile("ui-session-service-missing-table", ".db");
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.query(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any(org.springframework.jdbc.core.RowMapper.class)))
                .thenThrow(new RuntimeException("wrapper", new RuntimeException("no such table: ui_session_ranges")));
        DatabaseUiSessionService uiSessionService = new DatabaseUiSessionService(jdbcTemplate, databasePath);

        assertEquals(Optional.empty(), uiSessionService.loadSession());
    }

    @Test
    void loadSessionAndSaveSessionWrapUnexpectedFailures() throws Exception {
        Path databasePath = Files.createTempFile("ui-session-service-failure", ".db");
        JdbcTemplate failingLoadJdbcTemplate = mock(JdbcTemplate.class);
        when(failingLoadJdbcTemplate.query(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any(org.springframework.jdbc.core.RowMapper.class)))
                .thenThrow(new RuntimeException("betöltési hiba"));
        DatabaseUiSessionService failingLoadService = new DatabaseUiSessionService(failingLoadJdbcTemplate, databasePath);

        IllegalStateException loadException = assertThrows(IllegalStateException.class, failingLoadService::loadSession);
        assertEquals("Could not load the saved UI session from SQLite.", loadException.getMessage());
        assertEquals("betöltési hiba", loadException.getCause().getMessage());

        JdbcTemplate failingSaveJdbcTemplate = mock(JdbcTemplate.class);
        doThrow(new RuntimeException("mentési hiba"))
                .when(failingSaveJdbcTemplate)
                .execute(org.mockito.ArgumentMatchers.anyString());
        DatabaseUiSessionService failingSaveService = new DatabaseUiSessionService(failingSaveJdbcTemplate, databasePath);

        IllegalStateException saveException = assertThrows(
                IllegalStateException.class,
                () -> failingSaveService.saveSession(new AppSessionSnapshot("Revideált Károli", List.of(new RangeSelectionSnapshot("1. Mózes", 4, 4, 4))))
        );
        assertEquals("Could not save the UI session into SQLite.", saveException.getMessage());
        assertEquals("mentési hiba", saveException.getCause().getMessage());
    }

    @Test
    void appSessionSnapshotCopiesTheProvidedRangeList() {
        List<RangeSelectionSnapshot> ranges = new java.util.ArrayList<>(List.of(new RangeSelectionSnapshot("1. Mózes", 4, 4, 4)));
        AppSessionSnapshot sessionSnapshot = new AppSessionSnapshot("Revideált Károli", ranges);

        ranges.clear();

        assertEquals(List.of(new RangeSelectionSnapshot("1. Mózes", 4, 4, 4)), sessionSnapshot.ranges());
        assertFalse(sessionSnapshot.isEmpty());
        assertTrue(new AppSessionSnapshot(null, List.of()).isEmpty());
    }

    private JdbcTemplate jdbcTemplate(Path databasePath) {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.sqlite.JDBC");
        dataSource.setUrl("jdbc:sqlite:" + databasePath);
        return new JdbcTemplate(dataSource);
    }
}



