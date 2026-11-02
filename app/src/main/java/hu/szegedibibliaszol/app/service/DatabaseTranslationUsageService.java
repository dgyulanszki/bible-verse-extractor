package hu.szegedibibliaszol.app.service;

import java.nio.file.Path;
import java.util.Optional;
import java.util.function.LongSupplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class DatabaseTranslationUsageService implements TranslationUsageService {

    private static final String USAGE_TABLE_NAME = "translation_usage";

    private final JdbcTemplate jdbcTemplate;
    private final Path databasePath;
    private final LongSupplier clock;

    @Autowired
    public DatabaseTranslationUsageService(
            JdbcTemplate jdbcTemplate,
            @Value("${app.database.path:${user.home}/bible-verses.db}") String databasePath
    ) {
        this(jdbcTemplate, Path.of(databasePath), System::currentTimeMillis);
    }

    DatabaseTranslationUsageService(JdbcTemplate jdbcTemplate, Path databasePath) {
        this(jdbcTemplate, databasePath, System::currentTimeMillis);
    }

    DatabaseTranslationUsageService(JdbcTemplate jdbcTemplate, Path databasePath, LongSupplier clock) {
        this.jdbcTemplate = jdbcTemplate;
        this.databasePath = databasePath;
        this.clock = clock;
    }

    @Override
    public void recordUse(String translation) {
        if (translation == null || translation.isBlank() || !SqliteDatabaseSupport.databaseFileExists(databasePath)) {
            return;
        }
        try {
            ensureUsageTableExists();
            jdbcTemplate.update(
                    """
                            insert into translation_usage (translation, use_count, last_used_at)
                            values (?, 1, ?)
                            on conflict(translation) do update set
                                use_count = use_count + 1,
                                last_used_at = excluded.last_used_at
                            """,
                    translation,
                    clock.getAsLong()
            );
        } catch (RuntimeException ex) {
            throw new IllegalStateException("Could not record translation usage in SQLite.", ex);
        }
    }

    @Override
    public Optional<String> preferredTranslation() {
        if (!SqliteDatabaseSupport.databaseFileExists(databasePath)) {
            return Optional.empty();
        }
        try {
            String translation = jdbcTemplate.queryForObject(
                    """
                            select translation
                            from translation_usage
                            order by use_count desc, last_used_at desc
                            limit 1
                            """,
                    String.class
            );
            if (translation == null || translation.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(translation);
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        } catch (RuntimeException ex) {
            if (SqliteDatabaseSupport.hasMissingTable(ex, USAGE_TABLE_NAME)) {
                return Optional.empty();
            }
            throw new IllegalStateException("Could not read translation usage from SQLite.", ex);
        }
    }

    private void ensureUsageTableExists() {
        jdbcTemplate.execute(
                """
                        create table if not exists translation_usage (
                            translation text primary key,
                            use_count integer not null,
                            last_used_at integer not null
                        )
                        """
        );
    }
}
