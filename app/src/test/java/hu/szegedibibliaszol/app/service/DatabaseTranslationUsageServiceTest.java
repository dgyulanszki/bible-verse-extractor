package hu.szegedibibliaszol.app.service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DatabaseTranslationUsageServiceTest {

    @Test
    void stringConstructorUsesConfiguredDatabasePath() {
        DatabaseTranslationUsageService usageService = new DatabaseTranslationUsageService(
                mock(JdbcTemplate.class),
                "target/non-existent-translation-usage-string.db"
        );

        assertEquals(Optional.empty(), usageService.preferredTranslation());
        usageService.recordUse("Károli");
        usageService.recordUse(" ");
        usageService.recordUse(null);
    }

    @Test
    void recordUseRanksTranslationsAndPrioritizesTheMostFrequentOne() throws Exception {
        Path databasePath = Files.createTempFile("translation-usage", ".db");
        AtomicLong clock = new AtomicLong(1_000);
        DatabaseTranslationUsageService usageService = new DatabaseTranslationUsageService(
                jdbcTemplate(databasePath),
                databasePath,
                clock::getAndIncrement
        );

        usageService.recordUse("Károli");
        usageService.recordUse("Revideált Károli");
        assertEquals(Optional.of("Revideált Károli"), usageService.preferredTranslation());

        usageService.recordUse("Károli");
        assertEquals(Optional.of("Károli"), usageService.preferredTranslation());
    }

    @Test
    void preferredTranslationReturnsEmptyWhenTableOrRowsAreMissing() throws Exception {
        Path missingTablePath = Files.createTempFile("translation-usage-missing-table", ".db");
        JdbcTemplate missingTableJdbc = mock(JdbcTemplate.class);
        when(missingTableJdbc.queryForObject(anyString(), eq(String.class)))
                .thenThrow(new RuntimeException("wrapper", new RuntimeException("no such table: translation_usage")));
        DatabaseTranslationUsageService missingTableService = new DatabaseTranslationUsageService(missingTableJdbc, missingTablePath);

        assertEquals(Optional.empty(), missingTableService.preferredTranslation());

        Path emptyTablePath = Files.createTempFile("translation-usage-empty", ".db");
        JdbcTemplate emptyTableJdbc = jdbcTemplate(emptyTablePath);
        emptyTableJdbc.execute(
                """
                        create table translation_usage (
                            translation text primary key,
                            use_count integer not null,
                            last_used_at integer not null
                        )
                        """
        );
        emptyTableJdbc.update("insert into translation_usage (translation, use_count, last_used_at) values (' ', 3, 5)");
        DatabaseTranslationUsageService emptyTableService = new DatabaseTranslationUsageService(emptyTableJdbc, emptyTablePath);

        assertEquals(Optional.empty(), emptyTableService.preferredTranslation());
        emptyTableJdbc.update("delete from translation_usage");
        assertEquals(Optional.empty(), emptyTableService.preferredTranslation());
    }

    @Test
    void readAndWriteFailuresAreReported() throws Exception {
        Path databasePath = Files.createTempFile("translation-usage-failure", ".db");
        JdbcTemplate failingRead = mock(JdbcTemplate.class);
        when(failingRead.queryForObject(anyString(), eq(String.class))).thenThrow(new RuntimeException("olvasási hiba"));
        DatabaseTranslationUsageService failingReadService = new DatabaseTranslationUsageService(failingRead, databasePath);

        IllegalStateException readException = assertThrows(IllegalStateException.class, failingReadService::preferredTranslation);
        assertEquals("Could not read translation usage from SQLite.", readException.getMessage());
        assertEquals("olvasási hiba", readException.getCause().getMessage());

        JdbcTemplate failingWrite = mock(JdbcTemplate.class);
        doThrow(new RuntimeException("írási hiba")).when(failingWrite).execute(anyString());
        DatabaseTranslationUsageService failingWriteService = new DatabaseTranslationUsageService(failingWrite, databasePath);

        IllegalStateException writeException = assertThrows(IllegalStateException.class, () -> failingWriteService.recordUse("Károli"));
        assertEquals("Could not record translation usage in SQLite.", writeException.getMessage());
        assertTrue(writeException.getCause() instanceof RuntimeException);
    }

    @Test
    void emptyQueryResultIsTreatedAsNoPreference() throws Exception {
        Path databasePath = Files.createTempFile("translation-usage-empty-query", ".db");
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForObject(anyString(), eq(String.class))).thenThrow(new EmptyResultDataAccessException(1));
        DatabaseTranslationUsageService usageService = new DatabaseTranslationUsageService(jdbcTemplate, databasePath);

        assertEquals(Optional.empty(), usageService.preferredTranslation());
    }

    private JdbcTemplate jdbcTemplate(Path databasePath) {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.sqlite.JDBC");
        dataSource.setUrl("jdbc:sqlite:" + databasePath);
        return new JdbcTemplate(dataSource);
    }
}
