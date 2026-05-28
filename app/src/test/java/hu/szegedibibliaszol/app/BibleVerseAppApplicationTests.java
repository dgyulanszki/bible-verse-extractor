package hu.szegedibibliaszol.app;

import hu.szegedibibliaszol.app.service.VerseBrowserService;
import javafx.application.Application;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mockStatic;

@SpringBootTest
class BibleVerseAppApplicationTests {

    @Autowired
    private VerseBrowserService verseBrowserService;

    @Test
    void contextLoads() {
        assertNotNull(verseBrowserService);
    }

  @Test
  void mainDelegatesToJavaFxLaunch() {
    String[] args = {"--demo"};

    try (MockedStatic<Application> application = mockStatic(Application.class)) {
      BibleVerseAppApplication.main(args);

      application.verify(() -> Application.launch(JavaFxApplication.class, args));
    }
  }
}

