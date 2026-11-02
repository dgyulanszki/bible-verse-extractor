package hu.szegedibibliaszol.app;

import javafx.application.Application;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// The desktop app reads SQLite through JdbcTemplate. JPA and Hibernate stay off the classpath
// so Spring does not spend startup time scanning an unused persistence stack.
@SpringBootApplication
public class BibleVerseAppApplication {

    public static void main(String[] args) {
        launchApplication(args);
    }

    static void launchApplication(String[] args) {
        Application.launch(JavaFxApplication.class, args);
    }
}
