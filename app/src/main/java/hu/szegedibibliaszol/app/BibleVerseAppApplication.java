package hu.szegedibibliaszol.app;

import javafx.application.Application;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BibleVerseAppApplication {

    public static void main(String[] args) {
        launchApplication(args);
    }

    static void launchApplication(String[] args) {
        Application.launch(JavaFxApplication.class, args);
    }
}

