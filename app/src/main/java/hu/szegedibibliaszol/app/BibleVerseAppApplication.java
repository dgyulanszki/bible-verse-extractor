package hu.szegedibibliaszol.app;

import javafx.application.Application;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;

// The desktop app reads SQLite through JdbcTemplate, so we skip Hibernate/JPA startup work.
// This keeps the Spring context smaller and reduces the time before the JavaFX UI appears.
@SpringBootApplication(exclude = {
        HibernateJpaAutoConfiguration.class,
        DataJpaRepositoriesAutoConfiguration.class
})
public class BibleVerseAppApplication {

    public static void main(String[] args) {
        launchApplication(args);
    }

    static void launchApplication(String[] args) {
        Application.launch(JavaFxApplication.class, args);
    }
}
